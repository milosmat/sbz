import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PostService } from '../../core/post.service';
import { Post } from '../../core/models/post';
import { PostCardComponent } from '../../shared/post-card/post-card.component';

@Component({
  standalone: true,
  selector: 'app-feed',
  imports: [CommonModule, PostCardComponent],
  templateUrl: './feed.component.html',
  styleUrls: ['./feed.component.css']
})
export class FeedComponent implements OnInit {
  posts: Post[] = [];

  constructor(private postSvc: PostService) {}

  ngOnInit() {
    this.refresh();
  }

  refresh() {
    this.postSvc.feed().subscribe(p => this.posts = p);
  }
}
