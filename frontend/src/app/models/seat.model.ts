export type Deck = 'LOWER' | 'UPPER';
export type SeatType = 'SEATER' | 'SLEEPER' | 'SEMI_SLEEPER';
export type SeatStatus = 'AVAILABLE' | 'LOCKED' | 'BOOKED' | 'BLOCKED';

export interface PhysicalSeat {
  id?: string;
  seatNumber: string;
  deck: Deck;
  seatType: SeatType;
  rowNum: number;
  colNum: number;
  isWindow: boolean;
  isActive: boolean;
}

export interface ScheduleSeatItem {
  seatId: string;
  seatNumber: string;
  deck: Deck;
  seatType: SeatType;
  rowNum: number;
  colNum: number;
  isWindow: boolean;
  status: SeatStatus;
  baseAmount: number;
  taxAmount: number;
  totalAmount: number;
  currency: string;
}

export interface ScheduleSeatMap {
  scheduleId: string;
  busId: string;
  busNumber: string;
  hasLowerDeck: boolean;
  hasUpperDeck: boolean;
  lowerDeckSeats: ScheduleSeatItem[];
  upperDeckSeats: ScheduleSeatItem[];
  availableSeatsCount: number;
  totalSeatsCount: number;
}

export interface LockSeatsRequest {
  seatNumbers: string[];
  bookingSessionId?: string;
  ttlMinutes?: number;
}

export interface LockSeatsResponse {
  scheduleId: string;
  lockedSeats: string[];
  lockToken: string;
  lockExpiresAt: string;
  totalAmount: number;
  currency: string;
}
