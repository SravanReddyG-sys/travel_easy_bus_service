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
  authMode: 'LOGIN' | 'REGISTER' | 'VERIFY_REGISTRATION' | 'FORGOT_PASSWORD' | 'RESET_PASSWORD' = 'LOGIN';
  authErrorMessage = '';
  authSuccessMessage = '';
  authLoading = false;

  loginForm: LoginPayload = {
    email: '',
    password: ''
  };

  registerForm: RegisterPayload = {
    fullName: '',
    email: '',
    password: '',
    role: 'TRAVELLER',
    companyName: '',
    contactPhone: ''
  };

  confirmPassword = '';
  otpCode = '';
  forgotEmail = '';
  newPassword = '';
  confirmNewPassword = '';
  sandboxHint = '';

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  openLoginModal(defaultRole?: 'TRAVELLER' | 'OPERATOR'): void {
    if (defaultRole) {
      this.registerForm.role = defaultRole;
    }
    this.authMode = 'LOGIN';
    this.authErrorMessage = '';
    this.authSuccessMessage = '';
    this.sandboxHint = '';
    this.showAuthModal = true;
  }

  setAuthMode(mode: 'LOGIN' | 'REGISTER' | 'VERIFY_REGISTRATION' | 'FORGOT_PASSWORD' | 'RESET_PASSWORD'): void {
    this.authMode = mode;
    this.authErrorMessage = '';
    this.authSuccessMessage = '';
    this.sandboxHint = '';
  }

  closeAuthModal(): void {
    this.showAuthModal = false;
    this.authErrorMessage = '';
    this.authSuccessMessage = '';
    this.confirmPassword = '';
    this.otpCode = '';
    this.forgotEmail = '';
    this.newPassword = '';
    this.confirmNewPassword = '';
    this.sandboxHint = '';
  }

  isValidEmail(email: string): boolean {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return emailRegex.test(email.trim());
  }

  submitLogin(): void {
    this.authErrorMessage = '';
    this.authSuccessMessage = '';

    const email = (this.loginForm.email || '').trim();
    const password = (this.loginForm.password || '').trim();

    if (!email) {
      this.authErrorMessage = 'Please enter your email address.';
      return;
    }

    if (!this.isValidEmail(email)) {
      this.authErrorMessage = 'Please enter a valid email address (e.g. name@gmail.com).';
      return;
    }

    if (!password) {
      this.authErrorMessage = 'Please enter your password.';
      return;
    }

    this.authLoading = true;

    this.authService.login({ email, password }).subscribe({
      next: (res) => {
        this.authLoading = false;
        this.authSuccessMessage = 'Login successful! Welcome back.';
        setTimeout(() => {
          this.closeAuthModal();
          if (this.authService.isOperator()) {
            this.router.navigate(['/operator/dashboard']);
          } else {
            this.router.navigate(['/traveller/search']);
          }
        }, 500);
      },
      error: (err) => {
        this.authLoading = false;
        this.authErrorMessage = err.error?.message || err.error?.data || 'Login failed. Please check your email and password.';
      }
    });
  }

  submitRegister(): void {
    this.authErrorMessage = '';
    this.authSuccessMessage = '';

    const fullName = (this.registerForm.fullName || '').trim();
    const email = (this.registerForm.email || '').trim();
    const phone = (this.registerForm.contactPhone || '').trim();
    const password = this.registerForm.password || '';
    const confirm = this.confirmPassword || '';

    // Validations
    if (!fullName) {
      this.authErrorMessage = 'Please enter your Full Name.';
      return;
    }

    if (!email) {
      this.authErrorMessage = 'Please enter your Email Address.';
      return;
    }

    if (!this.isValidEmail(email)) {
      this.authErrorMessage = 'Please enter a valid email address (e.g. name@gmail.com).';
      return;
    }

    if (!phone) {
      this.authErrorMessage = 'Please enter your Contact Phone number.';
      return;
    }

    if (this.registerForm.role === 'OPERATOR' && !this.registerForm.companyName?.trim()) {
      this.authErrorMessage = 'Please enter your Company / Travels Name.';
      return;
    }

    // Two-time password validation
    if (!password) {
      this.authErrorMessage = 'Please enter a password.';
      return;
    }

    if (password.length < 6) {
      this.authErrorMessage = 'Password must be at least 6 characters long.';
      return;
    }

    if (!confirm) {
      this.authErrorMessage = 'Please confirm your password.';
      return;
    }

    if (password !== confirm) {
      this.authErrorMessage = 'Passwords do not match. Please verify and re-type identical passwords in both fields.';
      return;
    }

    this.authLoading = true;

    this.authService.sendRegistrationOtp({
      fullName,
      email,
      password,
      role: this.registerForm.role,
      companyName: this.registerForm.companyName,
      contactPhone: phone
    }).subscribe({
      next: (res) => {
        this.authLoading = false;
        this.authMode = 'VERIFY_REGISTRATION';
        this.sandboxHint = res?.data?.sandboxNote || '';
        this.otpCode = res?.data?.demoOtp || '';
        this.authSuccessMessage = `Verification code sent to ${email} via Resend. Please enter it below.`;
      },
      error: (err) => {
        this.authLoading = false;
        this.authErrorMessage = err.error?.message || 'Failed to send verification code. Please check your information and try again.';
      }
    });
  }

  submitVerifyRegisterOtp(): void {
    this.authErrorMessage = '';
    this.authSuccessMessage = '';

    const email = (this.registerForm.email || '').trim();
    const code = (this.otpCode || '').trim();

    if (!code || code.length < 4) {
      this.authErrorMessage = 'Please enter the 6-digit verification code sent to your email.';
      return;
    }

    this.authLoading = true;

    this.authService.verifyRegistrationOtp(email, code).subscribe({
      next: (res) => {
        this.authLoading = false;
        this.authSuccessMessage = 'Email verified! Account created successfully.';
        setTimeout(() => {
          this.closeAuthModal();
          if (this.registerForm.role === 'OPERATOR') {
            this.router.navigate(['/operator/dashboard']);
          } else {
            this.router.navigate(['/traveller/search']);
          }
        }, 600);
      },
      error: (err) => {
        this.authLoading = false;
        this.authErrorMessage = err.error?.message || 'Invalid or expired verification code.';
      }
    });
  }

  resendRegistrationOtp(): void {
    this.authErrorMessage = '';
    this.authSuccessMessage = '';
    this.authLoading = true;

    this.authService.sendRegistrationOtp(this.registerForm).subscribe({
      next: (res) => {
        this.authLoading = false;
        this.sandboxHint = res?.data?.sandboxNote || '';
        this.otpCode = res?.data?.demoOtp || '';
        this.authSuccessMessage = 'A new verification code has been dispatched via Resend.';
      },
      error: (err) => {
        this.authLoading = false;
        this.authErrorMessage = err.error?.message || 'Failed to resend code.';
      }
    });
  }

  submitForgotPassword(): void {
    this.authErrorMessage = '';
    this.authSuccessMessage = '';

    const email = (this.forgotEmail || '').trim();
    if (!email) {
      this.authErrorMessage = 'Please enter your registered email address.';
      return;
    }

    if (!this.isValidEmail(email)) {
      this.authErrorMessage = 'Please enter a valid email address.';
      return;
    }

    this.authLoading = true;

    this.authService.forgotPassword(email).subscribe({
      next: (res) => {
        this.authLoading = false;
        this.authMode = 'RESET_PASSWORD';
        this.sandboxHint = res?.data?.sandboxNote || '';
        this.otpCode = res?.data?.demoOtp || '';
        this.authSuccessMessage = `Password reset code sent to ${email} via Resend.`;
      },
      error: (err) => {
        this.authLoading = false;
        this.authErrorMessage = err.error?.message || 'Failed to send password reset code. Please check your email.';
      }
    });
  }

  submitResetPassword(): void {
    this.authErrorMessage = '';
    this.authSuccessMessage = '';

    const email = (this.forgotEmail || '').trim();
    const code = (this.otpCode || '').trim();
    const newPass = this.newPassword || '';
    const confirm = this.confirmNewPassword || '';

    if (!code || code.length < 4) {
      this.authErrorMessage = 'Please enter the 6-digit reset code.';
      return;
    }

    if (!newPass || newPass.length < 6) {
      this.authErrorMessage = 'New password must be at least 6 characters long.';
      return;
    }

    if (newPass !== confirm) {
      this.authErrorMessage = 'Passwords do not match. Please verify and re-type identical passwords.';
      return;
    }

    this.authLoading = true;

    this.authService.resetPassword(email, code, newPass).subscribe({
      next: (res) => {
        this.authLoading = false;
        this.authSuccessMessage = 'Password reset successfully! Please sign in with your new password.';
        setTimeout(() => {
          this.authMode = 'LOGIN';
          this.loginForm.email = email;
          this.loginForm.password = '';
        }, 1200);
      },
      error: (err) => {
        this.authLoading = false;
        this.authErrorMessage = err.error?.message || 'Failed to reset password. Please check your code.';
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
