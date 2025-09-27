import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FeedApiService, Page, PostDTO, RecDTO } from '../../core/feed-api.service';
import { PostService } from '../../core/post.service';
import { Post } from '../../core/models/post';
import {RecommendedAdsComponent} from '../recommended-ads/recommended-ads.component';

@Component({
  selector: 'app-feed',
  standalone: true,
  imports: [CommonModule, DatePipe, RecommendedAdsComponent],
  templateUrl: './feed.component.html',
  styleUrls: ['./feed.component.css']
})
export class FeedComponent implements OnInit {
  userId: string | null = null;

  // Friends feed (Page<PostDTO>)
  friendsPage = 0;
  friendsSize = 20;
  friendsDays = 1; // backend trenutno ignoriše; DRL filtrira 24h
  friendsLoading = false;
  friendsError: string | null = null;
  friends: PostDTO[] = [];
  friendsTotal = 0;

  // Recommended feed (RecDTO[])
  recLoading = false;
  recError: string | null = null;
  recs: RecDTO[] = [];

  // prikaz prioriteta razloga
  private reasonPriority: Record<string, number> = {
    'boost: popularan & lajkovan hešteg': 1,
    'popularan post': 2,
    'popularan hešteg': 3,
    'lajkovani hešteg': 4,
    'autorski hešteg': 5,
    'nov post (<24h)': 6,
    // NEW-user reasons
    'sličan korisnik lajkovao': 7,
    'slično lajkovanim objavama': 8,
    'preferencija': 9
  };

  constructor(private api: FeedApiService, private postsApi: PostService) {}

  ngOnInit(): void {
    // auth servis postavlja 'token' i 'uid' u localStorage (interceptor šalje Bearer)
    this.userId = localStorage.getItem('uid');
    if (this.userId) {
      this.loadFriends(true);
  this.loadRecommended();
    } else {
      this.friends = [];
      this.recs = [];
    }
  }

  // Helpers
  toDate(ms: number): Date { return new Date(ms); }
  tagsJoin(tags?: string[]): string { return (tags ?? []).join(' '); }
  isBoost(why: string): boolean { return why?.toLowerCase().startsWith('boost'); }

  trackPostId = (_: number, p: PostDTO) => p?.id;
  trackRec = (_: number, r: RecDTO) => r?.post?.id + ':' + r?.score;

  // Friends
  loadFriends(reset = false): void {
    if (!this.userId || this.friendsLoading) return;
    if (reset) { this.friendsPage = 0; this.friends = []; this.friendsTotal = 0; }
    this.friendsLoading = true; this.friendsError = null;

    this.api.getFriendsFeed(this.userId, this.friendsDays, this.friendsPage, this.friendsSize).subscribe({
      next: (page: Page<PostDTO>) => {
        const content = page?.content ?? [];
        this.friendsTotal = page?.totalElements ?? content.length;
        this.friends = [...this.friends, ...content];
        this.friendsPage++;
        this.friendsLoading = false;
      },
      error: (err) => {
        this.friendsError = err?.error?.error ?? 'Greška pri učitavanju.';
        this.friendsLoading = false;
      }
    });
  }

  // Actions: Friends feed
  likeFriend(p: PostDTO): void {
    if (!p?.id) return;
    this.postsApi.like(p.id).subscribe({
      next: (updated) => this.applyFriendUpdate(updated),
      error: (err) => { this.friendsError = err?.error?.error ?? 'Greška pri lajku.'; }
    });
  }

  reportFriend(p: PostDTO): void {
    if (!p?.id) return;
    const reason = prompt('Razlog (opciono):') || '';
    this.postsApi.report(p.id, reason).subscribe({
      next: (updated: any) => {
        // backend vraća ažuriran post; ako ne, fallback bez promene
        if (updated && updated.id) this.applyFriendUpdate(updated as Post);
      },
      error: (err) => { this.friendsError = err?.error?.error ?? 'Greška pri prijavi.'; }
    });
  }

  private applyFriendUpdate(updated: Post): void {
    const i = this.friends.findIndex(x => x.id === updated.id);
    if (i >= 0) this.friends[i] = this.asDto(updated);
  }

  // Recommended
  loadRecommended(limit = 20): void {
    if (this.recLoading) { return; }
    this.recLoading = true; this.recError = null;

    this.api.getRecommended(limit).subscribe({
      next: (list) => {
        const cleaned = (list ?? []).filter(r => !!r && !!r.post);
        // unikatni razlozi + sort po prioritetu
        this.recs = cleaned.map(r => ({
          ...r,
          reasons: Array.from(new Set(r.reasons ?? []))
            .sort((a, b) => (this.reasonPriority[a] ?? 99) - (this.reasonPriority[b] ?? 99))
        }));
        this.recLoading = false;
      },
      error: (err) => {
        this.recError = err?.error?.error ?? 'Greška pri učitavanju.';
        this.recLoading = false;
      }
    });
  }

  // Actions: Recommended feed
  likeRec(r: RecDTO): void {
    const id = r?.post?.id;
    if (!id) return;
    this.postsApi.like(id).subscribe({
      next: (updated) => this.applyRecUpdate(updated, r),
      error: (err) => { this.recError = err?.error?.error ?? 'Greška pri lajku.'; }
    });
  }

  reportRec(r: RecDTO): void {
    const id = r?.post?.id;
    if (!id) return;
    const reason = prompt('Razlog (opciono):') || '';
    this.postsApi.report(id, reason).subscribe({
      next: (updated: any) => { if (updated && updated.id) this.applyRecUpdate(updated as Post, r); },
      error: (err) => { this.recError = err?.error?.error ?? 'Greška pri prijavi.'; }
    });
  }

  private applyRecUpdate(updated: Post, rec: RecDTO): void {
    if (!rec || !rec.post) return;
    rec.post = this.asDto(updated);
    // trigger change detection by reassigning array (immutability)
    this.recs = this.recs.map(x => x === rec ? { ...rec } : x);
  }

  private asDto(p: Post | PostDTO): PostDTO {
    return {
      id: p.id,
      authorId: p.authorId,
      text: p.text,
      hashtags: (p as any).hashtags ?? [],
      likes: (p as any).likes ?? 0,
      reports: (p as any).reports ?? 0,
      createdAtEpochMs: (p as any).createdAtEpochMs ?? 0,
    };
  }
}
