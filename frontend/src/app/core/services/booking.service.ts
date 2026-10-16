import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { BookingRecord, BookingStatus } from '../../models/booking.model';

@Injectable({
  providedIn: 'root'
})
export class BookingService {
  private static readonly STORAGE_KEY = 'traveleasy_traveller_bookings';

  private bookingsSubject = new BehaviorSubject<BookingRecord[]>(this.loadBookings());
  public bookings$: Observable<BookingRecord[]> = this.bookingsSubject.asObservable();

  constructor() {}

  private loadBookings(): BookingRecord[] {
    if (typeof localStorage !== 'undefined') {
      const data = localStorage.getItem(BookingService.STORAGE_KEY);
      if (data) {
        try {
          return JSON.parse(data);
        } catch (e) {
          console.error('Failed to parse stored bookings', e);
        }
      }
    }
    return [];
  }

  private saveBookings(bookings: BookingRecord[]): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(BookingService.STORAGE_KEY, JSON.stringify(bookings));
    }
    this.bookingsSubject.next(bookings);
  }

  getBookings(): BookingRecord[] {
    return this.bookingsSubject.value;
  }

  addBooking(booking: BookingRecord): void {
    const current = this.getBookings();
    // Prepend new booking to top of list
    const updated = [booking, ...current.filter(b => b.bookingId !== booking.bookingId)];
    this.saveBookings(updated);
  }

  updateBookingStatus(bookingId: string, status: BookingStatus): void {
    const updated = this.getBookings().map(b => {
      if (b.bookingId === bookingId) {
        return { ...b, status };
      }
      return b;
    });
    this.saveBookings(updated);
  }

  cancelBooking(bookingId: string): void {
    this.updateBookingStatus(bookingId, 'CANCELLED');
  }

  confirmBooking(bookingId: string): void {
    this.updateBookingStatus(bookingId, 'CONFIRMED');
  }

  deleteBooking(bookingId: string): void {
    const updated = this.getBookings().filter(b => b.bookingId !== bookingId);
    this.saveBookings(updated);
  }

  clearAll(): void {
    this.saveBookings([]);
  }

  createSampleBooking(): BookingRecord {
    const sample: BookingRecord = {
      bookingId: 'BKG-' + Math.floor(100000 + Math.random() * 900000),
      bookingReference: 'TE-REF-' + Math.floor(1000 + Math.random() * 9000),
      scheduleId: '00000000-0000-0002-0001-000000000001',
      busNumber: 'ROYAL-DELJAI-01',
      operatorCompanyName: 'Royal Express Travels',
      busType: 'AC_SEATER',
      origin: 'Delhi',
      destination: 'Jaipur',
      travelDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
      departureTime: new Date(Date.now() + 86400000).toISOString().split('T')[0] + 'T06:00:00',
      arrivalTime: new Date(Date.now() + 86400000).toISOString().split('T')[0] + 'T11:30:00',
      seats: ['1A', '1B'],
      totalAmount: 1100,
      currency: 'INR',
      status: 'CONFIRMED',
      boardingPoint: 'ISBT Kashmiri Gate',
      droppingPoint: 'Sindhi Camp Jaipur',
      passengerName: 'Traveller Guest',
      createdAt: new Date().toISOString()
    };
    this.addBooking(sample);
    return sample;
  }
}
