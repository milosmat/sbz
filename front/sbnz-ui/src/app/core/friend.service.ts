import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/evnironment.development';
import { User } from './models/user';

@Injectable({ providedIn: 'root' })
export class FriendService {
  private base = `${environment.apiUrl}/friends`;
  constructor(private http: HttpClient) {}

  search(q: string) {
    const params = new HttpParams().set('q', q);
    return this.http.get<User[]>(`${this.base}/search`, { params });
  }
  add(friendId: string) { return this.http.post(`${this.base}`, { friendId }); }
  block(friendId: string) { return this.http.post(`${this.base}/block`, { friendId }); }
}
