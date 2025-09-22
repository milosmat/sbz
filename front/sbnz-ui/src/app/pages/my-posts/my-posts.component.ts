import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PostService } from '../../core/post.service'; // prilagodi putanju ako je drugačije
import { Post } from '../../core/models/post';

@Component({
  selector: 'app-my-posts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-posts.component.html',
  styleUrls: ['./my-posts.component.css']
})
export class MyPostsComponent implements OnInit {
  private api = inject(PostService);

  loading = false;
  error = '';
  posts: Post[] = [];

  private readonly userId = localStorage.getItem('uid') || '';

  ngOnInit(): void {
    if (!this.userId) {
      this.error = 'Niste prijavljeni.';
      return;
    }
    this.load();
  }

  load() {
    this.loading = true; this.error = '';
    this.api.myPosts(this.userId).subscribe({
      next: (res) => { this.posts = res || []; this.loading = false; },
      error: (e) => { this.error = e?.error?.error || 'Greška pri učitavanju.'; this.loading = false; }
    });
  }

  like(p: Post) {
    this.api.like(p.id).subscribe({
      next: (updated) => {
        const i = this.posts.findIndex(x => x.id === updated.id);
        if (i >= 0) this.posts[i] = updated;
      },
      error: (e) => { this.error = e?.error?.error || 'Greška pri lajku.'; }
    });
  }

  report(p: Post) {
    const reason = prompt('Razlog (opciono):') || '';
    this.api.report(p.id, reason).subscribe({
      next: () => { /* možeš reload ili optimističko povećanje */ this.load(); },
      error: (e) => { this.error = e?.error?.error || 'Greška pri prijavi.'; }
    });
  }

  toDate(ms: number): Date { return new Date(ms); }
  tagsJoin(tags?: string[] | null): string { return (tags || []).join(' '); }
}
