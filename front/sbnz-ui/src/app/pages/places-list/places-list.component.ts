import { Component, OnInit } from '@angular/core';
import { PlaceService, Place } from '../../core/place.service';
import { RouterLink } from '@angular/router';
import { NgFor, NgIf } from '@angular/common';

@Component({
  standalone: true,
  selector: 'app-places-list',
  templateUrl: './places-list.component.html',
  styleUrls: ['./places-list.component.css'],
  imports: [NgIf, NgFor, RouterLink]
})
export class PlacesListComponent implements OnInit {
  places: Place[] = [];
  loading = false;
  error: string | null = null;

  constructor(private placesService: PlaceService) {}

  ngOnInit(): void {
    this.loading = true;
    this.placesService.list().subscribe({
      next: (data) => { this.places = data || []; this.loading = false; },
      error: (err) => { this.error = err?.error || 'Greška pri učitavanju mesta.'; this.loading = false; }
    });
  }
}
