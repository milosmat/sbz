import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/evnironment.development';

export interface PostDTO {
  id: string; authorId: string; text: string;
  hashtags: string[]; likes: number; reports: number;
  createdAtEpochMs: number;
}
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; }
export interface RecDTO { post: PostDTO; score: number; reasons: string[]; }

@Injectable({ providedIn: 'root' })
export class FeedApiService {
  private feedBase = `${environment.apiUrl}/feed`;
  private postsBase = `${environment.apiUrl}/posts`;

  constructor(private http: HttpClient) {}

  getFriendsFeed(userId: string, days = 1, page = 0, size = 20): Observable<Page<PostDTO>> {
    const params = new HttpParams()
      .set('userId', userId).set('days', days).set('page', page).set('size', size);
    return this.http.get<Page<PostDTO>>(`${this.feedBase}/friends`, { params });
  }

  getRecommended(userId: string, limit = 20): Observable<RecDTO[]> {
    const params = new HttpParams().set('userId', userId).set('limit', limit);
    return this.http.get<RecDTO[]>(`${environment.apiUrl}/feed/recommended`, { params });
  }


  getPostsByAuthor(authorId: string): Observable<PostDTO[]> {
    const params = new HttpParams().set('authorId', authorId);
    return this.http.get<PostDTO[]>(`${this.postsBase}/by-author`, { params });
  }
}
