import { BusType } from './bus.model';
import { ScheduleStatus, AvailabilityStatus } from './schedule.model';
import { Deck, SeatStatus, SeatType } from './seat.model';

export interface OperatorDashboardSummary {
  operatorId: string;
  companyName: string;
  totalBuses: number;
  activeBuses: number;
  maintenanceBuses: number;
  inactiveBuses: number;
  totalRoutes: number;
  activeRoutes: number;
  totalSchedules: number;
  todaySchedules: number;
  activeSchedules: number;
  completedSchedules: number;
  cancelledSchedules: number;
  totalSeatsCapacity: number;
  totalSeatsBooked: number;
  occupancyRatePercentage: number;
  totalRevenue: number;
  currency: string;
}

export interface OperatorRevenueStat {
  routeId: string;
  routeCode: string;
  origin: string;
  destination: string;
  schedulesCount: number;
  totalBookedSeats: number;
  totalRevenue: number;
  currency: string;
}

export interface OperatorOccupancyStat {
  scheduleId: string;
  busNumber: string;
  origin: string;
  destination: string;
  scheduleDate: string;
  departureTime: string;
  totalSeats: number;
  bookedSeats: number;
  availableSeats: number;
  occupancyPercentage: number;
}

export interface SeatReservationItem {
  seatNumber: string;
  deck: Deck;
  seatType: SeatType;
  status: SeatStatus;
  bookingReference?: string;
  lockedBySession?: string;
  lockExpiryTime?: string;
  seatFare: number;
}

export interface SchedulePassengerManifest {
  scheduleId: string;
  scheduleDate: string;
  departureTime: string;
  arrivalTime: string;
  status: ScheduleStatus;
  availabilityStatus: AvailabilityStatus;
  busId: string;
  busNumber: string;
  registrationNumber: string;
  busType: BusType;
  routeId: string;
  routeCode: string;
  origin: string;
  destination: string;
  totalSeats: number;
  bookedSeatsCount: number;
  lockedSeatsCount: number;
  availableSeatsCount: number;
  blockedSeatsCount: number;
  occupancyPercentage: number;
  grossRevenue: number;
  currency: string;
  seatRoster: SeatReservationItem[];
}

export interface OperatorLiveBookingFeed {
  scheduleId: string;
  busNumber: string;
  routeSummary: string;
  seatNumber: string;
  status: SeatStatus;
  bookingReference: string;
  amount: number;
  eventTimestamp: string;
}
