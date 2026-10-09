import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';

export interface AuthUser {
  userId: string;
  email: string;
  role: 'TRAVELLER' | 'OPERATOR' | 'ADMIN';
  operatorId?: string;
  token?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<AuthUser>({
    userId: '11111111-1111-1111-1111-111111111111',
    email: 'operator@traveleasy.com',
    role: 'OPERATOR',
    operatorId: '00000000-0000-0000-0000-000000000001',
    token: ''
  });

  public currentUser$: Observable<AuthUser> = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.fetchToken('OPERATOR', '00000000-0000-0000-0000-000000000001');
  }

  get currentUser(): AuthUser {
    return this.currentUserSubject.value;
  }

  get token(): string | undefined {
    return this.currentUserSubject.value.token;
  }

  isOperator(): boolean {
    return this.currentUserSubject.value.role === 'OPERATOR' || this.currentUserSubject.value.role === 'ADMIN';
  }

  switchRole(role: 'TRAVELLER' | 'OPERATOR' | 'ADMIN', operatorId?: string): void {
    this.fetchToken(role, operatorId);
  }

  private fetchToken(role: 'TRAVELLER' | 'OPERATOR' | 'ADMIN', operatorId?: string): void {
    let url = `/api/v1/auth/token?role=${role}`;
    if (operatorId) {
      url += `&operatorId=${operatorId}`;
    }

    this.http.get<{ success: boolean; data: { token: string; email: string; operatorId: string } }>(url)
      .subscribe({
        next: (res) => {
          if (res && res.data) {
            this.currentUserSubject.next({
              userId: this.currentUser.userId,
              email: res.data.email,
              role,
              operatorId: res.data.operatorId || undefined,
              token: res.data.token
            });
          }
        },
        error: (err) => {
          console.warn('Could not retrieve dev auth token from backend', err);
        }
      });
  }
}
