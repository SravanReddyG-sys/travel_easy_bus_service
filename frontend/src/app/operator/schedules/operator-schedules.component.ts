import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { BusApiService } from '../../core/services/bus-api.service';
import { AuthService } from '../../core/services/auth.service';
import { Schedule, CreateScheduleRequest } from '../../models/schedule.model';
import { Bus } from '../../models/bus.model';
import { Route } from '../../models/route.model';

@Component({
  selector: 'app-operator-schedules',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './operator-schedules.component.html',
  styleUrls: ['./operator-schedules.component.scss']
})
export class OperatorSchedulesComponent implements OnInit, OnDestroy {
  schedules: Schedule[] = [];
  buses: Bus[] = [];
  routes: Route[] = [];

  loading = true;
  errorMessage = '';
  showCreateModal = false;

  newSchedule: CreateScheduleRequest = {
    busId: '',
    routeId: '',
    scheduleDate: '',
    departureTime: '',
    arrivalTime: ''
  };

  private authSubscription?: Subscription;

  constructor(
    private busApiService: BusApiService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    this.newSchedule.scheduleDate = tomorrow.toISOString().split('T')[0];
    this.newSchedule.departureTime = `${this.newSchedule.scheduleDate}T06:00:00`;
    this.newSchedule.arrivalTime = `${this.newSchedule.scheduleDate}T12:00:00`;

    this.authSubscription = this.authService.currentUser$.subscribe((user) => {
      if (user.token) {
        this.loadData();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
  }

  loadData(): void {
    this.loading = true;
    this.busApiService.getOperatorSchedules().subscribe({
      next: (data) => {
        this.schedules = data.content || [];
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load schedules.';
        this.loading = false;
      }
    });

    this.busApiService.getOperatorBuses().subscribe({
      next: (res) => {
        this.buses = res.content || [];
        if (this.buses.length > 0 && !this.newSchedule.busId) {
          this.newSchedule.busId = this.buses[0].id;
        }
      }
    });

    this.busApiService.getOperatorRoutes().subscribe({
      next: (res) => {
        this.routes = res.content || [];
        if (this.routes.length > 0 && !this.newSchedule.routeId) {
          this.newSchedule.routeId = this.routes[0].id;
        }
      }
    });
  }

  openCreateModal(): void {
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  submitCreateSchedule(): void {
    if (!this.newSchedule.busId || !this.newSchedule.routeId || !this.newSchedule.scheduleDate) {
      alert('Please fill out all schedule fields.');
      return;
    }

    this.busApiService.createSchedule(this.newSchedule).subscribe({
      next: () => {
        this.closeCreateModal();
        this.loadData();
      },
      error: (err) => {
        alert(err.error?.message || 'Failed to create schedule.');
      }
    });
  }

  cancelSchedule(schedule: Schedule): void {
    const reason = prompt('Please enter cancellation reason:', 'Operational requirements / Weather disruption');
    if (reason) {
      this.busApiService.cancelSchedule(schedule.id, reason).subscribe({
        next: () => this.loadData(),
        error: (err) => alert(err.error?.message || 'Failed to cancel schedule.')
      });
    }
  }

  viewManifest(scheduleId: string): void {
    this.router.navigate(['/operator/schedules', scheduleId, 'manifest']);
  }
}
