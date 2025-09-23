// core/admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/evnironment.development';
import {Flagged} from './models/moderation';

export interface FlagDTO {
  userId: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  reason: string;
  untilMs: number;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private base = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  getFlags(sinceHours = 168, limit = 200) {
    const params = new HttpParams()
      .set('sinceHours', sinceHours)
      .set('limit', limit);
    return this.http.get<FlagDTO[]>(`${this.base}/mod/flags`, { params });
  }

  detectBadUsers() { return this.http.post<Flagged[]>(`${this.base}/detect`, {}); }
}
