import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/evnironment.development';

@Component({
  selector: 'app-new-post',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './new-post.component.html',
  styleUrls: ['./new-post.component.css'],
})
export class NewPostComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private router = inject(Router);

  loading = false;
  error = '';
  success = '';

  // pretpostavka: posle logina čuvaš korisnikov id u localStorage
  private readonly currentUserId = localStorage.getItem('uid') || '';

  form = this.fb.group({
    text: ['', [Validators.required, Validators.minLength(1)]],
    tags: [''], // format: "#sbz #java"
  });

  submit() {
    this.error = '';
    this.success = '';

    if (!this.currentUserId) { this.error = 'Niste prijavljeni.'; return; }
    if (this.form.invalid)    { this.error = 'Popunite tekst.'; return; }

    this.loading = true;
    const body = {
      authorId: this.currentUserId,
      text: this.form.value.text!.trim(),
      tags: (this.form.value.tags || '').toString().trim(),
    };

    this.http.post(`${environment.apiUrl}/posts`, body).subscribe({
      next: () => {
        this.loading = false;
        this.success = 'Objava sačuvana.';
        // preusmeri na moje objave (po želji promeni)
        this.router.navigateByUrl('/me/posts');
      },
      error: (e) => {
        this.loading = false;
        this.error = e?.error?.error || 'Greška pri čuvanju objave.';
      },
    });
  }
}
