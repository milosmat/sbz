import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PlaceService } from '../../core/place.service';
import { Router } from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-new-place',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './new-place.component.html',
  styleUrls: ['./new-place.component.css']
})
export class NewPlaceComponent {
  private fb = inject(FormBuilder);
  private api = inject(PlaceService);
  private router = inject(Router);

  msg = '';
  err = '';
  loading = false;

  form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    country: ['', Validators.required],
    city: ['', Validators.required],
    description: [''],
    hashtagsLine: ['']   // npr: "#more #planina"
  });

  submit() {
    if (this.form.invalid) { this.err = 'Popunite obavezna polja.'; return; }
    this.err = ''; this.msg = ''; this.loading = true;

    this.api.create(this.form.value as any).subscribe({
      next: (p) => {
        this.loading = false;
        this.msg = 'Mesto uspešno dodato.';
        // po želji: this.router.navigateByUrl('/'); ili na listu mesta
      },
      error: (e) => {
        this.loading = false;
        this.err = e?.error?.error || 'Greška pri dodavanju mesta.';
      }
    });
  }
}
