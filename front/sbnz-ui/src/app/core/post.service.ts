import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/evnironment.development';
import { Post } from './models/post';
import { CreatePostRequest } from './models/dto';

@Injectable({ providedIn: 'root' })
export class PostService {
  private base = `${environment.apiUrl}/posts`;
  constructor(private http: HttpClient) {}

  feed() { return this.http.get<Post[]>(`${this.base}`); }
  myPosts(userId: string) { return this.http.get<Post[]>(`${this.base}/by/${userId}`); }
  create(body: CreatePostRequest) { return this.http.post<Post>(`${this.base}`, body); }
  like(postId: string) { return this.http.post<Post>(`${this.base}/${postId}/like`, {}); }
  report(postId: string, reason: string) { return this.http.post(`${this.base}/${postId}/report`, { reason }); }
}
