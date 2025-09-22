import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/evnironment.development';
import { LoginRequest, RegisterRequest} from './models/dto';
import { User } from './models/user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private base = `${environment.apiUrl}/auth`;
  currentUser = signal<User | null>(null);

  constructor(private http: HttpClient) {
    // ako želiš: učitaj user-a iz localStorage (npr. /auth/me ping)
  }

  login(data: LoginRequest) {
    return this.http.post<{ token: string; user: User }>(`${this.base}/login`, data);
  }

  register(data: RegisterRequest) {
    return this.http.post<User>(`${this.base}/register`, data);
  }

  setSession(token: string, user: User) {
    localStorage.setItem('token', token);
    this.currentUser.set(user);
  }

  logout() {
    localStorage.removeItem('token');
    this.currentUser.set(null);
  }
}
