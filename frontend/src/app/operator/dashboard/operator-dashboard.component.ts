import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { BusApiService } from '../../core/services/bus-api.service';
import { AuthService } from '../../core/services/auth.service';
import {
  OperatorDashboardSummary,
  OperatorRevenueStat,
  OperatorLiveBookingFeed
} from '../../models/dashboard.model';

@Component({
  selector: 'app-operator-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './operator-dashboard.component.html',
  styleUrls: ['./operator-dashboard.component.scss']
})
export class OperatorDashboardComponent implements OnInit, OnDestroy {
  summary: OperatorDashboardSummary | null = null;
  revenueStats: OperatorRevenueStat[] = [];
  liveFeed: OperatorLiveBookingFeed[] = [];
  loading = true;
  errorMessage = '';

  private authSubscription?: Subscription;

  constructor(
    private busApiService: BusApiService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authSubscription = this.authService.currentUser$.subscribe((user) => {
      if (user.token) {
        this.loadDashboardData();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
  }

  loadDashboardData(): void {
    this.loading = true;
    this.errorMessage = '';

    const opId = this.authService.currentUser.operatorId;

    this.busApiService.getDashboardSummary(opId).subscribe({
      next: (data) => {
        this.summary = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load operator dashboard summary.';
        this.loading = false;
      }
    });

    this.busApiService.getRevenueAnalytics(opId).subscribe({
      next: (data) => {
        this.revenueStats = data;
      },
      error: (err) => console.error('Failed to load revenue analytics', err)
    });

    this.busApiService.getLiveBookingFeed(opId, 10).subscribe({
      next: (data) => {
        this.liveFeed = data;
      },
      error: (err) => console.error('Failed to load live booking feed', err)
    });
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }

  viewManifest(scheduleId: string): void {
    this.router.navigate(['/operator/schedules', scheduleId, 'manifest']);
  }
}
