// src/app/core/place.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/evnironment.development';

export interface Place {
  id: string;
  name: string;
  country: string;
  city: string;
  description: string;
  hashtags: string[];
}

export interface CreatePlaceRequest {
  name: string;
  country: string;
  city: string;
  description: string;
  hashtagsLine: string; // npr: "#sbz #putovanja"
}

@Injectable({ providedIn: 'root' })
export class PlaceService {
  private base = `${environment.apiUrl}/places`;
  constructor(private http: HttpClient) {}
  create(body: CreatePlaceRequest) { return this.http.post<Place>(`${this.base}`, body); }
  list() { return this.http.get<Place[]>(`${this.base}/list`); } // ako koristiš GET listing
}
