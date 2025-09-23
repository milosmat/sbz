import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { PlaceService, Place, CreateRatingRequest } from '../../core/place.service';
import { NgIf, NgFor } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  standalone: true,
  selector: 'app-place-detail',
  templateUrl: './place-detail.component.html',
  styleUrls: ['./place-detail.component.css'],
  imports: [NgIf, NgFor, FormsModule]
})
export class PlaceDetailComponent implements OnInit {
  place: Place | null = null;
  loading = false;
  error: string | null = null;

  // forma za ocenu
  score = 5;
  description = '';
  hashtagsLine = '';
  sending = false;
  sendError: string | null = null;
  sendSuccess = false;

  constructor(private route: ActivatedRoute, private placesService: PlaceService) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loading = true;
    this.placesService.get(id).subscribe({
      next: (p) => { this.place = p; this.loading = false; },
      error: (err) => { this.error = err?.error || 'Greška pri učitavanju mesta.'; this.loading = false; }
    });
  }

  submitRating() {
    if (!this.place) return;
    const req: CreateRatingRequest = {
      placeId: this.place.id,
      score: this.score,
      description: this.description,
      hashtagsLine: this.hashtagsLine
    };
    this.sending = true;
    this.sendError = null; this.sendSuccess = false;
    this.placesService.rate(req).subscribe({
      next: () => { this.sending = false; this.sendSuccess = true; },
      error: (err) => { this.sending = false; this.sendError = err?.error || 'Greška pri slanju ocene.'; }
    });
  }
}
