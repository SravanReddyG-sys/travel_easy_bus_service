import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { BusApiService } from '../../core/services/bus-api.service';
import { BookingService } from '../../core/services/booking.service';
import { AuthService } from '../../core/services/auth.service';
import { TravellerScheduleDetail } from '../../models/schedule.model';
import { ScheduleSeatItem, LockSeatsResponse } from '../../models/seat.model';
import { BookingRecord } from '../../models/booking.model';
import { SeatMapComponent } from '../../shared/components/seat-map/seat-map.component';

@Component({
  selector: 'app-schedule-details',
  standalone: true,
  imports: [CommonModule, FormsModule, SeatMapComponent],
  templateUrl: './schedule-details.component.html',
  styleUrls: ['./schedule-details.component.scss']
})
export class ScheduleDetailsComponent implements OnInit {
  scheduleId = '';
  scheduleDetail: TravellerScheduleDetail | null = null;
  loading = true;
  errorMessage = '';

  selectedSeats: ScheduleSeatItem[] = [];
  lockingSeats = false;
  lockResult: LockSeatsResponse | null = null;
  confirmedBookingReference = '';
  selectedBoardingPoint = '';
  selectedDroppingPoint = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private busApiService: BusApiService,
    private bookingService: BookingService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.scheduleId = this.route.snapshot.paramMap.get('id') || '';
    if (this.scheduleId) {
      this.loadScheduleDetails();
    } else {
      this.errorMessage = 'Invalid schedule ID';
      this.loading = false;
    }
  }

  loadScheduleDetails(): void {
    this.loading = true;
    this.errorMessage = '';

    this.busApiService.getTravellerScheduleDetails(this.scheduleId).subscribe({
      next: (data) => {
        this.scheduleDetail = data;
        if (data.boardingPoints && data.boardingPoints.length > 0) {
          this.selectedBoardingPoint = data.boardingPoints[0].pointName;
        }
        if (data.droppingPoints && data.droppingPoints.length > 0) {
          this.selectedDroppingPoint = data.droppingPoints[0].pointName;
        }
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load schedule details.';
        this.loading = false;
      }
    });
  }

  onSeatSelectionChanged(seats: ScheduleSeatItem[]): void {
    this.selectedSeats = seats;
  }

  get selectedSeatNumbersJoined(): string {
    return this.selectedSeats.map(s => s.seatNumber).join(', ');
  }

  onLockExpired(): void {
    alert('Your seat hold has expired. The seats have been released back to general inventory.');
    this.lockResult = null;
    this.selectedSeats = [];
    this.loadScheduleDetails();
  }

  get totalAmount(): number {
    return this.selectedSeats.reduce((sum, s) => {
      const price = (s.totalAmount && s.totalAmount > 0) ? s.totalAmount : ((s.baseAmount && s.baseAmount > 0) ? s.baseAmount : (s.seatType === 'SLEEPER' ? 750 : 550));
      return sum + price;
    }, 0);
  }

  lockAndHoldSeats(): void {
    if (this.selectedSeats.length === 0) {
      alert('Please select at least 1 seat to hold.');
      return;
    }

    this.lockingSeats = true;
    const seatNumbers = this.selectedSeats.map(s => s.seatNumber);

    const sessionId = this.authService.isLoggedIn()
      ? ('user_' + (this.authService.currentUser.userId || 'guest'))
      : ('sess_' + Math.floor(100000 + Math.random() * 900000));

    this.busApiService.lockSeats(this.scheduleId, {
      seatNumbers,
      bookingSessionId: sessionId,
      ttlMinutes: 10
    }).subscribe({
      next: (res) => {
        this.lockResult = res;
        this.lockingSeats = false;

        // Save into traveller's bookings history
        if (this.scheduleDetail) {
          const passenger = this.authService.isLoggedIn()
            ? (this.authService.currentUser.fullName || this.authService.currentUser.email)
            : 'Traveller Guest';

          const newBooking: BookingRecord = {
            bookingId: 'BKG-' + Math.floor(100000 + Math.random() * 900000),
            bookingReference: 'TE-REF-' + Math.floor(1000 + Math.random() * 9000),
            scheduleId: this.scheduleId,
            busNumber: this.scheduleDetail.busNumber,
            operatorCompanyName: this.scheduleDetail.operatorCompanyName,
            busType: this.scheduleDetail.busType,
            origin: this.scheduleDetail.origin,
            destination: this.scheduleDetail.destination,
            travelDate: this.scheduleDetail.scheduleDate,
            departureTime: this.scheduleDetail.departureTime,
            arrivalTime: this.scheduleDetail.arrivalTime,
            seats: res.lockedSeats || seatNumbers,
            totalAmount: this.totalAmount,
            currency: 'INR',
            status: 'CONFIRMED',
            boardingPoint: this.selectedBoardingPoint,
            droppingPoint: this.selectedDroppingPoint,
            passengerName: passenger,
            lockToken: res.lockToken,
            lockExpiresAt: res.lockExpiresAt,
            createdAt: new Date().toISOString()
          };
          this.confirmedBookingReference = newBooking.bookingReference;
          this.bookingService.addBooking(newBooking);

          // Permanently confirm seats in backend PostgreSQL inventory & link PNR
          this.busApiService.confirmSeats(this.scheduleId, {
            seatNumbers: res.lockedSeats || seatNumbers,
            lockToken: res.lockToken,
            bookingReference: newBooking.bookingReference
          }).subscribe({
            next: (confirmRes) => {
              console.log('Seats permanently confirmed in database inventory:', confirmRes);
            },
            error: (err) => console.warn('Non-blocking: Seat confirmation sync warning:', err)
          });

          // Dispatch booking confirmation e-ticket via Resend if email is present
          const userEmail = (this.authService.isLoggedIn() && this.authService.currentUser.email)
            ? this.authService.currentUser.email
            : null;

          if (userEmail && userEmail.includes('@')) {
            this.busApiService.sendBookingConfirmationEmail({
              toEmail: userEmail,
              passengerName: passenger,
              bookingReference: newBooking.bookingReference,
              busNumber: newBooking.busNumber,
              operatorName: newBooking.operatorCompanyName,
              origin: newBooking.origin,
              destination: newBooking.destination,
              travelDate: newBooking.travelDate,
              departureTime: newBooking.departureTime,
              seats: newBooking.seats,
              totalAmount: newBooking.totalAmount,
              boardingPoint: newBooking.boardingPoint,
              droppingPoint: newBooking.droppingPoint
            }).subscribe({
              next: (emailRes) => console.log('Booking email delivered via Resend:', emailRes),
              error: (err) => console.warn('Non-blocking: Resend email failed:', err)
            });
          }
        }
      },
      error: (err) => {
        alert(err.error?.message || 'Seat conflict detected. One or more seats may have been locked by another user.');
        this.lockingSeats = false;
        this.loadScheduleDetails();
      }
    });
  }

  goToBookings(): void {
    this.router.navigate(['/traveller/bookings']);
  }

  backToSearch(): void {
    this.router.navigate(['/traveller/search']);
  }

  formatTime(dateTimeStr: string): string {
    if (!dateTimeStr) return '';
    const date = new Date(dateTimeStr);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  formatDuration(minutes: number): string {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return `${h}h ${m > 0 ? m + 'm' : ''}`;
  }
}
