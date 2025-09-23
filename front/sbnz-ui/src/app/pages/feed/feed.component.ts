import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FeedApiService, Page, PostDTO, RecDTO } from '../../core/feed-api.service';
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
    'nov post (<24h)': 6
  };

  constructor(private api: FeedApiService) {}

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
  tagsJoin(tags?: string[]): string { return (tags ?? []).map(t => `#${t}`).join(' '); }
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

  // Recommended
  loadRecommended(limit = 20): void {
    if (!this.userId || this.recLoading) { this.recs = []; return; }
    this.recLoading = true; this.recError = null;

    this.api.getRecommended(this.userId, limit).subscribe({
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
}
