import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BookingService } from '../../core/services/booking.service';
import { BusApiService } from '../../core/services/bus-api.service';
import { AuthService } from '../../core/services/auth.service';
import { BookingRecord, BookingStatus } from '../../models/booking.model';

@Component({
  selector: 'app-traveller-bookings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './traveller-bookings.component.html',
  styleUrls: ['./traveller-bookings.component.scss']
})
export class TravellerBookingsComponent implements OnInit {
  bookings: BookingRecord[] = [];
  selectedFilter: 'ALL' | BookingStatus = 'ALL';
  searchQuery: string = '';
  selectedBookingForView: BookingRecord | null = null;
  notificationMessage: string = '';
  sendingEmailForId: string | null = null;

  constructor(
    private bookingService: BookingService,
    private busApiService: BusApiService,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.bookingService.bookings$.subscribe(data => {
      this.bookings = data;
    });
  }

  get filteredBookings(): BookingRecord[] {
    return this.bookings.filter(b => {
      const matchesFilter = this.selectedFilter === 'ALL' || b.status === this.selectedFilter;
      const q = this.searchQuery.trim().toLowerCase();
      const matchesQuery = !q ||
        b.bookingReference.toLowerCase().includes(q) ||
        b.bookingId.toLowerCase().includes(q) ||
        b.origin.toLowerCase().includes(q) ||
        b.destination.toLowerCase().includes(q) ||
        b.busNumber.toLowerCase().includes(q) ||
        b.operatorCompanyName.toLowerCase().includes(q);
      return matchesFilter && matchesQuery;
    });
  }

  get countAll(): number { return this.bookings.length; }
  get countConfirmed(): number { return this.bookings.filter(b => b.status === 'CONFIRMED').length; }
  get countHeld(): number { return this.bookings.filter(b => b.status === 'HELD').length; }
  get countCancelled(): number { return this.bookings.filter(b => b.status === 'CANCELLED').length; }

  setFilter(filter: 'ALL' | BookingStatus): void {
    this.selectedFilter = filter;
  }

  confirmBooking(booking: BookingRecord): void {
    this.bookingService.confirmBooking(booking.bookingId);
    this.showNotification(`Booking ${booking.bookingReference} has been successfully CONFIRMED!`);
  }

  cancelBooking(booking: BookingRecord): void {
    if (confirm(`Are you sure you want to cancel booking ${booking.bookingReference}?`)) {
      this.bookingService.cancelBooking(booking.bookingId);
      this.showNotification(`Booking ${booking.bookingReference} has been cancelled.`);
    }
  }

  deleteBooking(booking: BookingRecord): void {
    if (confirm(`Remove booking ${booking.bookingReference} from history?`)) {
      this.bookingService.deleteBooking(booking.bookingId);
      this.showNotification(`Booking removed from your history.`);
    }
  }

  viewTicket(booking: BookingRecord): void {
    this.selectedBookingForView = booking;
  }

  closeTicketModal(): void {
    this.selectedBookingForView = null;
  }

  printTicket(): void {
    window.print();
  }

  sendEmailTicket(booking: BookingRecord): void {
    const defaultEmail = (this.authService.isLoggedIn() && this.authService.currentUser.email)
      ? this.authService.currentUser.email
      : '';

    const email = prompt('Enter email address to receive your e-ticket:', defaultEmail);

    if (!email || !email.includes('@')) {
      if (email !== null) alert('Please enter a valid email address.');
      return;
    }

    this.sendingEmailForId = booking.bookingId;
    this.busApiService.sendBookingConfirmationEmail({
      toEmail: email.trim(),
      passengerName: booking.passengerName || 'Traveller',
      bookingReference: booking.bookingReference,
      busNumber: booking.busNumber,
      operatorName: booking.operatorCompanyName,
      origin: booking.origin,
      destination: booking.destination,
      travelDate: booking.travelDate,
      departureTime: booking.departureTime,
      seats: booking.seats,
      totalAmount: booking.totalAmount,
      boardingPoint: booking.boardingPoint,
      droppingPoint: booking.droppingPoint
    }).subscribe({
      next: (res) => {
        this.sendingEmailForId = null;
        this.showNotification(`📧 Ticket successfully sent via Resend to ${email}!`);
      },
      error: (err) => {
        this.sendingEmailForId = null;
        const msg = err.error?.message || err.message || 'Failed to dispatch email via Resend.';
        alert(msg);
      }
    });
  }

  clearAll(): void {
    if (confirm('Clear all bookings from your device?')) {
      this.bookingService.clearAll();
      this.showNotification('Booking history cleared.');
    }
  }

  goToSearch(): void {
    this.router.navigate(['/traveller/search']);
  }

  formatTime(dateTimeStr: string): string {
    if (!dateTimeStr) return '';
    try {
      const d = new Date(dateTimeStr);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return dateTimeStr;
    }
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString([], { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' });
    } catch {
      return dateStr;
    }
  }

  private showNotification(msg: string): void {
    this.notificationMessage = msg;
    setTimeout(() => {
      this.notificationMessage = '';
    }, 4000);
  }
}
