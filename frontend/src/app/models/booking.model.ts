export type BookingStatus = 'CONFIRMED' | 'HELD' | 'CANCELLED';

export interface BookingRecord {
  bookingId: string;
  bookingReference: string;
  scheduleId: string;
  busNumber: string;
  operatorCompanyName: string;
  busType: string;
  origin: string;
  destination: string;
  travelDate: string;
  departureTime: string;
  arrivalTime: string;
  seats: string[];
  totalAmount: number;
  currency: string;
  status: BookingStatus;
  boardingPoint?: string;
  droppingPoint?: string;
  passengerName?: string;
  lockToken?: string;
  lockExpiresAt?: string;
  createdAt: string;
}
