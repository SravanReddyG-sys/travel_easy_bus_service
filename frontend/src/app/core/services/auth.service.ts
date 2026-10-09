import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface AuthUser {
  userId: string;
  email: string;
  fullName: string;
  role: 'TRAVELLER' | 'OPERATOR' | 'ADMIN';
  operatorId?: string;
  companyName?: string;
  token?: string;
}

export interface LoginPayload {
  email: string;
  password?: string;
  role: 'TRAVELLER' | 'OPERATOR' | 'ADMIN';
}

export interface RegisterPayload {
  fullName: string;
  email: string;
  password?: string;
  role: 'TRAVELLER' | 'OPERATOR';
  companyName?: string;
  contactPhone?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private static readonly STORAGE_KEY = 'traveleasy_auth_user';

  private baseUrl = (typeof window !== 'undefined' && window.location.port && window.location.port !== '80')
    ? `http://${window.location.hostname}:8081/api/v1`
    : '/api/v1';

  private currentUserSubject = new BehaviorSubject<AuthUser>(this.loadStoredUser());
  public currentUser$: Observable<AuthUser> = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    if (!this.currentUserSubject.value.token) {
      this.quickLogin('OPERATOR', '00000000-0000-0000-0000-000000000001');
    }
  }

  private loadStoredUser(): AuthUser {
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(AuthService.STORAGE_KEY);
      if (stored) {
        try {
          return JSON.parse(stored);
        } catch (e) {
          // ignore corrupted JSON
        }
      }
    }
    return {
      userId: '00000000-0000-0000-0000-000000000001',
      email: 'operator@traveleasy.com',
      fullName: 'Royal Express Travels',
      role: 'OPERATOR',
      operatorId: '00000000-0000-0000-0000-000000000001',
      companyName: 'Royal Express Travels',
      token: ''
    };
  }

  get currentUser(): AuthUser {
    return this.currentUserSubject.value;
  }

  get token(): string | undefined {
    return this.currentUserSubject.value.token;
  }

  isLoggedIn(): boolean {
    return !!this.currentUserSubject.value.token;
  }

  isOperator(): boolean {
    const role = this.currentUserSubject.value.role;
    return role === 'OPERATOR' || role === 'ADMIN';
  }

  isTraveller(): boolean {
    return this.currentUserSubject.value.role === 'TRAVELLER';
  }

  login(payload: LoginPayload): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auth/login`, {
      email: payload.email,
      password: payload.password || 'password123',
      role: payload.role
    }).pipe(
      tap((res) => {
        if (res && res.data) {
          const user: AuthUser = {
            userId: res.data.userId,
            email: res.data.email,
            fullName: res.data.fullName || (payload.role === 'OPERATOR' ? 'Bus Operator' : 'Traveller'),
            role: res.data.role.replace('ROLE_', '') as any,
            operatorId: res.data.operatorId,
            companyName: res.data.companyName,
            token: res.data.token
          };
          this.setCurrentUser(user);
        }
      })
    );
  }

  register(payload: RegisterPayload): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auth/register`, {
      fullName: payload.fullName,
      email: payload.email,
      password: payload.password || 'password123',
      role: payload.role,
      companyName: payload.companyName,
      contactPhone: payload.contactPhone || '+91-9876543210'
    }).pipe(
      tap((res) => {
        if (res && res.data) {
          const user: AuthUser = {
            userId: res.data.userId,
            email: res.data.email,
            fullName: res.data.fullName,
            role: res.data.role.replace('ROLE_', '') as any,
            operatorId: res.data.operatorId,
            companyName: res.data.companyName,
            token: res.data.token
          };
          this.setCurrentUser(user);
        }
      })
    );
  }

  quickLogin(role: 'TRAVELLER' | 'OPERATOR' | 'ADMIN', operatorId?: string, companyName?: string): void {
    let url = `${this.baseUrl}/auth/token?role=${role}`;
    if (operatorId) {
      url += `&operatorId=${operatorId}`;
    }

    this.http.get<{ success: boolean; data: { token: string; email: string; operatorId: string; role: string } }>(url)
      .subscribe({
        next: (res) => {
          if (res && res.data) {
            const user: AuthUser = {
              userId: res.data.operatorId || '00000000-0000-0000-0000-000000000001',
              email: res.data.email,
              fullName: role === 'OPERATOR' ? (companyName || 'Royal Express Travels') : 'Demo Traveller',
              role,
              operatorId: res.data.operatorId,
              companyName: role === 'OPERATOR' ? (companyName || 'Royal Express Travels') : undefined,
              token: res.data.token
            };
            this.setCurrentUser(user);
          }
        }
      });
  }

  switchRole(role: 'TRAVELLER' | 'OPERATOR' | 'ADMIN', operatorId?: string): void {
    const defaultOp = role === 'OPERATOR' ? (operatorId || '00000000-0000-0000-0000-000000000001') : undefined;
    this.quickLogin(role, defaultOp);
  }

  logout(): void {
    const defaultTraveller: AuthUser = {
      userId: '',
      email: '',
      fullName: 'Guest User',
      role: 'TRAVELLER',
      token: ''
    };
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(AuthService.STORAGE_KEY);
    }
    this.currentUserSubject.next(defaultTraveller);
  }

  private setCurrentUser(user: AuthUser): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(AuthService.STORAGE_KEY, JSON.stringify(user));
    }
    this.currentUserSubject.next(user);
  }
}
