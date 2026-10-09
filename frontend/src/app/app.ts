import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService, LoginPayload, RegisterPayload } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {
  title = 'TravelEasy Bus Microservice';

  showAuthModal = false;
  authMode: 'LOGIN' | 'REGISTER' = 'LOGIN';
  authErrorMessage = '';
  authSuccessMessage = '';
  authLoading = false;

  loginForm: LoginPayload = {
    email: '',
    password: '',
    role: 'TRAVELLER'
  };

  registerForm: RegisterPayload = {
    fullName: '',
    email: '',
    password: '',
    role: 'TRAVELLER',
    companyName: '',
    contactPhone: ''
  };

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  openLoginModal(defaultRole?: 'TRAVELLER' | 'OPERATOR'): void {
    if (defaultRole) {
      this.loginForm.role = defaultRole;
      this.registerForm.role = defaultRole;
    }
    this.authMode = 'LOGIN';
    this.authErrorMessage = '';
    this.authSuccessMessage = '';
    this.showAuthModal = true;
  }

  closeAuthModal(): void {
    this.showAuthModal = false;
    this.authErrorMessage = '';
    this.authSuccessMessage = '';
  }

  quickLogin(role: 'TRAVELLER' | 'OPERATOR', operatorId?: string, companyName?: string): void {
    this.authService.quickLogin(role, operatorId, companyName);
    this.closeAuthModal();
    if (role === 'OPERATOR') {
      this.router.navigate(['/operator/dashboard']);
    } else {
      this.router.navigate(['/traveller/search']);
    }
  }

  submitLogin(): void {
    if (!this.loginForm.email) {
      this.authErrorMessage = 'Please enter an email address.';
      return;
    }

    this.authLoading = true;
    this.authErrorMessage = '';

    this.authService.login(this.loginForm).subscribe({
      next: () => {
        this.authLoading = false;
        this.closeAuthModal();
        if (this.loginForm.role === 'OPERATOR') {
          this.router.navigate(['/operator/dashboard']);
        } else {
          this.router.navigate(['/traveller/search']);
        }
      },
      error: (err) => {
        this.authLoading = false;
        this.authErrorMessage = err.error?.message || 'Login failed. Please check your credentials.';
      }
    });
  }

  submitRegister(): void {
    if (!this.registerForm.fullName || !this.registerForm.email) {
      this.authErrorMessage = 'Please fill all required fields.';
      return;
    }

    this.authLoading = true;
    this.authErrorMessage = '';

    this.authService.register(this.registerForm).subscribe({
      next: () => {
        this.authLoading = false;
        this.closeAuthModal();
        if (this.registerForm.role === 'OPERATOR') {
          this.router.navigate(['/operator/dashboard']);
        } else {
          this.router.navigate(['/traveller/search']);
        }
      },
      error: (err) => {
        this.authLoading = false;
        this.authErrorMessage = err.error?.message || 'Registration failed. Please try again.';
      }
    });
  }

  switchRole(role: 'TRAVELLER' | 'OPERATOR'): void {
    this.authService.switchRole(role);
    if (role === 'OPERATOR') {
      this.router.navigate(['/operator/dashboard']);
    } else {
      this.router.navigate(['/traveller/search']);
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/traveller/search']);
  }
}
