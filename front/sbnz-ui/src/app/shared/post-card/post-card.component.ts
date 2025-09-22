import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Post } from '../../core/models/post';
import { PostService } from '../../core/post.service';

@Component({
  standalone: true,
  selector: 'app-post-card',
  imports: [CommonModule, DatePipe, MatCardModule, MatButtonModule, MatIconModule],
  templateUrl: './post-card.component.html',
  styleUrls: ['./post-card.component.css']
})
export class PostCardComponent {
  @Input({ required: true }) post!: Post;
  @Output() liked = new EventEmitter<void>();

  private alreadyLiked = signal(false);
  likedOnce = computed(() => this.alreadyLiked());

  constructor(private posts: PostService) {}

  like() {
    this.posts.like(this.post.id).subscribe({
      next: (p) => {
        this.post.likes = p.likes;
        this.alreadyLiked.set(true);
        this.liked.emit();
      },
      error: () => this.alreadyLiked.set(true) // idempotentno
    });
  }
}
