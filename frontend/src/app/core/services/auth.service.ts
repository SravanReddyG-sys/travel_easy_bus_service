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
  role?: 'TRAVELLER' | 'OPERATOR' | 'ADMIN';
}

export interface RegisterPayload {
  fullName: string;
  email: string;
  password?: string;
  confirmPassword?: string;
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
    // If no stored user session, user stays logged out as guest
  }

  private loadStoredUser(): AuthUser {
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(AuthService.STORAGE_KEY);
      if (stored) {
        try {
          const parsed = JSON.parse(stored);
          if (parsed && parsed.token) {
            return parsed;
          }
        } catch (e) {
          // ignore corrupted JSON
        }
      }
    }
    return {
      userId: '',
      email: '',
      fullName: 'Guest User',
      role: 'TRAVELLER',
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
      email: payload.email.trim().toLowerCase(),
      password: payload.password ? payload.password.trim() : ''
    }).pipe(
      tap((res) => {
        if (res && res.data) {
          const user: AuthUser = {
            userId: res.data.userId,
            email: res.data.email,
            fullName: res.data.fullName,
            role: (res.data.role || 'ROLE_TRAVELLER').replace('ROLE_', '') as any,
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
      fullName: payload.fullName.trim(),
      email: payload.email.trim().toLowerCase(),
      password: payload.password ? payload.password.trim() : '',
      role: payload.role,
      companyName: payload.companyName ? payload.companyName.trim() : undefined,
      contactPhone: payload.contactPhone ? payload.contactPhone.trim() : ''
    }).pipe(
      tap((res) => {
        if (res && res.data) {
          const user: AuthUser = {
            userId: res.data.userId,
            email: res.data.email,
            fullName: res.data.fullName,
            role: (res.data.role || 'ROLE_TRAVELLER').replace('ROLE_', '') as any,
            operatorId: res.data.operatorId,
            companyName: res.data.companyName,
            token: res.data.token
          };
          this.setCurrentUser(user);
        }
      })
    );
  }

  sendRegistrationOtp(payload: RegisterPayload): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auth/register/send-otp`, {
      fullName: payload.fullName.trim(),
      email: payload.email.trim().toLowerCase(),
      password: payload.password ? payload.password.trim() : '',
      role: payload.role,
      companyName: payload.companyName ? payload.companyName.trim() : undefined,
      contactPhone: payload.contactPhone ? payload.contactPhone.trim() : ''
    });
  }

  verifyRegistrationOtp(email: string, otpCode: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auth/register/verify-otp`, {
      email: email.trim().toLowerCase(),
      otpCode: otpCode.trim()
    }).pipe(
      tap((res) => {
        if (res && res.data) {
          const user: AuthUser = {
            userId: res.data.userId,
            email: res.data.email,
            fullName: res.data.fullName,
            role: (res.data.role || 'ROLE_TRAVELLER').replace('ROLE_', '') as any,
            operatorId: res.data.operatorId,
            companyName: res.data.companyName,
            token: res.data.token
          };
          this.setCurrentUser(user);
        }
      })
    );
  }

  forgotPassword(email: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auth/password/forgot`, {
      email: email.trim().toLowerCase()
    });
  }

  resetPassword(email: string, otpCode: string, newPassword: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auth/password/reset`, {
      email: email.trim().toLowerCase(),
      otpCode: otpCode.trim(),
      newPassword: newPassword.trim()
    });
  }

  switchRole(role: 'TRAVELLER' | 'OPERATOR' | 'ADMIN'): void {
    const current = this.currentUserSubject.value;
    const updated: AuthUser = {
      ...current,
      role
    };
    this.setCurrentUser(updated);
  }

  logout(): void {
    const defaultGuest: AuthUser = {
      userId: '',
      email: '',
      fullName: 'Guest User',
      role: 'TRAVELLER',
      token: ''
    };
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(AuthService.STORAGE_KEY);
    }
    this.currentUserSubject.next(defaultGuest);
  }

  public setCurrentUser(user: AuthUser): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(AuthService.STORAGE_KEY, JSON.stringify(user));
    }
    this.currentUserSubject.next(user);
  }
}
