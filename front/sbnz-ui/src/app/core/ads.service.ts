// src/app/services/ads.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from '../../environments/evnironment.development';

export interface AdDTO {
  id: string;
  name: string;
  city: string;
  country: string;
  hashtags: string[];
  why: string;
}

@Injectable({ providedIn: 'root' })
export class AdsService {
  private baseUrl = `${environment.apiUrl}/ads`;

  constructor(private http: HttpClient) {}

  getRecommended(limit = 20): Observable<AdDTO[]> {
    const params = new HttpParams().set('limit', String(limit));
    return this.http.get<AdDTO[]>(`${this.baseUrl}/recommended`, { params, withCredentials: true });
  }
}
