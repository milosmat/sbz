import { Component, OnInit } from '@angular/core';
import { AdsService, AdDTO } from '../../core/ads.service';
import {RouterLink} from '@angular/router';
import {NgForOf, NgIf} from '@angular/common';

@Component({
  standalone: true,
  selector: 'app-recommended-ads',
  templateUrl: './recommended-ads.component.html',
  styleUrls: ['./recommended-ads.component.css'],
  imports: [
    RouterLink,
    NgIf,
    NgForOf
  ]
})
export class RecommendedAdsComponent implements OnInit {
  ads: AdDTO[] = [];
  loading = false;
  error: string | null = null;

  constructor(private adsService: AdsService) {}

  ngOnInit(): void {
    this.fetch();
  }

  fetch(limit = 20) {
    this.loading = true;
    this.error = null;
    this.adsService.getRecommended(limit).subscribe({
      next: (data) => { this.ads = data || []; this.loading = false; },
      error: (err) => { this.error = err?.error || 'Greška pri učitavanju preporuka.'; this.loading = false; }
    });
  }
}
