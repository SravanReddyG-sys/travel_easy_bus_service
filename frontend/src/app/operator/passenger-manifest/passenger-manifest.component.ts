import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { BusApiService } from '../../core/services/bus-api.service';
import { SchedulePassengerManifest, SeatReservationItem } from '../../models/dashboard.model';

@Component({
  selector: 'app-passenger-manifest',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './passenger-manifest.component.html',
  styleUrls: ['./passenger-manifest.component.scss']
})
export class PassengerManifestComponent implements OnInit {
  scheduleId = '';
  manifest: SchedulePassengerManifest | null = null;
  loading = true;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private busApiService: BusApiService
  ) {}

  ngOnInit(): void {
    this.scheduleId = this.route.snapshot.paramMap.get('id') || '';
    if (this.scheduleId) {
      this.loadManifest();
    } else {
      this.errorMessage = 'Invalid schedule ID';
      this.loading = false;
    }
  }

  loadManifest(): void {
    this.loading = true;
    this.busApiService.getScheduleManifest(this.scheduleId).subscribe({
      next: (data) => {
        this.manifest = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load passenger manifest.';
        this.loading = false;
      }
    });
  }

  toggleBlockSeat(seat: SeatReservationItem): void {
    const isBlocked = seat.status === 'BLOCKED';
    const action = isBlocked ? 'Unblock' : 'Block';

    if (confirm(`Are you sure you want to ${action} seat ${seat.seatNumber}?`)) {
      this.busApiService.toggleSeatBlock(this.scheduleId, seat.seatNumber, !isBlocked).subscribe({
        next: () => this.loadManifest(),
        error: (err) => alert(err.error?.message || `Failed to ${action.toLowerCase()} seat.`)
      });
    }
  }

  backToSchedules(): void {
    this.router.navigate(['/operator/schedules']);
  }
}
