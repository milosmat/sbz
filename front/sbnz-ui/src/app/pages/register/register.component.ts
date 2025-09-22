import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../core/auth.service';

@Component({
  standalone: true,
  selector: 'app-register',
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSnackBarModule
  ],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private sb: MatSnackBar
  ) {
    // ✅ inicijalizacija u constructor-u
    this.form = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      city: [''],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  submit() {
    if (this.form.invalid) return;
    this.auth.register(this.form.value as any).subscribe({
      next: () => {
        this.sb.open('Uspešna registracija', '', { duration: 2000 });
        this.router.navigateByUrl('/login');
      },
      error: (err) => {
        const msg = Array.isArray(err?.error?.errors)
          ? err.error.errors.join('\n')
          : (err?.error?.message ?? 'Greška pri registraciji');
        this.sb.open(msg, 'Zatvori', { duration: 5000 });
      }
    });
  }
}
