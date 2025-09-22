import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FeedApiService, Page, PostDTO, RecDTO } from '../../core/feed-api.service';

@Component({
  selector: 'app-feed',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './feed.component.html',
  styleUrls: ['./feed.component.css']
})
export class FeedComponent implements OnInit {
  userId: string | null = null;

  // Friends feed
  friendsPage = 0;
  friendsSize = 20;
  friendsDays = 1;
  friendsLoading = false;
  friendsError: string | null = null;
  friends: PostDTO[] = [];
  friendsTotal = 0;

  // Recommended feed
  recLoading = false;
  recError: string | null = null;
  recs: RecDTO[] = [];

  constructor(private api: FeedApiService, private route: ActivatedRoute) {}

  private readUserIdFromStorage(): string | null {
    const keys = ['user', 'currentUser', 'loggedUser'];
    for (const k of keys) {
      try {
        const v = localStorage.getItem(k);
        const id = v ? JSON.parse(v)?.id : null;
        if (id) return id;
      } catch {}
    }
    return null;
  }

  ngOnInit(): void {
    // auth servis postavlja 'token' i 'uid' u localStorage
    this.userId = localStorage.getItem('uid');  // ⟵ umesto parsiranja 'user'
    if (this.userId) {
      this.loadFriends(true);
    }
    this.loadRecommended(); // preporuke uvek probaj (treba Bearer token; interceptor je već tu)
  }

  toDate(ms: number): Date { return new Date(ms); }
  tagsJoin(tags?: string[]): string { return (tags ?? []).map(t => `#${t}`).join(' '); }

  loadFriends(reset = false): void {
    if (!this.userId) return;
    if (reset) { this.friendsPage = 0; this.friends = []; }
    this.friendsLoading = true; this.friendsError = null;

    this.api.getFriendsFeed(this.userId, this.friendsDays, this.friendsPage, this.friendsSize).subscribe({
      next: (page: Page<PostDTO>) => {
        this.friendsTotal = page.totalElements;
        this.friends = [...this.friends, ...page.content];
        this.friendsPage++;
        this.friendsLoading = false;
      },
      error: (err) => {
        this.friendsError = err?.error?.error ?? 'Greška pri učitavanju.';
        this.friendsLoading = false;
      }
    });
  }

  loadRecommended(limit = 20): void {
    if (!this.userId) { this.recs = []; return; } // isti princip kao friends
    this.recLoading = true; this.recError = null;
    this.api.getRecommended(this.userId, limit).subscribe({
      next: (list) => { this.recs = (list ?? []).filter(r => !!r && !!r.post); this.recLoading = false; },
      error: (err) => { this.recError = err?.error?.error ?? 'Greška pri učitavanju.'; this.recLoading = false; }
    });
  }

}
