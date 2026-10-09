import { BusType } from './bus.model';
import { RouteStop, BoardingPoint, DroppingPoint } from './route.model';
import { ScheduleSeatMap, SeatType } from './seat.model';

export type ScheduleStatus = 'SCHEDULED' | 'BOARDING' | 'DEPARTED' | 'IN_TRANSIT' | 'COMPLETED' | 'CANCELLED' | 'DELAYED';
export type AvailabilityStatus = 'AVAILABLE' | 'FEW_SEATS_LEFT' | 'SOLD_OUT' | 'UNAVAILABLE';

export interface FareItem {
  id?: string;
  seatType: SeatType;
  baseAmount: number;
  taxAmount: number;
  totalAmount: number;
  currency: string;
}

export interface Schedule {
  id: string;
  busId: string;
  busNumber: string;
  registrationNumber: string;
  busType: BusType;
  routeId: string;
  routeCode: string;
  origin: string;
  destination: string;
  operatorId: string;
  operatorCompanyName: string;
  scheduleDate: string;
  departureTime: string;
  arrivalTime: string;
  status: ScheduleStatus;
  availabilityStatus: AvailabilityStatus;
  availableSeatsCount: number;
  fares?: FareItem[];
}

export interface CreateScheduleRequest {
  busId: string;
  routeId: string;
  scheduleDate: string;
  departureTime: string;
  arrivalTime: string;
}

export interface TravellerScheduleDetail {
  scheduleId: string;
  scheduleDate: string;
  departureTime: string;
  arrivalTime: string;
  durationMinutes: number;
  status: ScheduleStatus;
  availabilityStatus: AvailabilityStatus;
  cancellationReason?: string;
  busId: string;
  busNumber: string;
  registrationNumber: string;
  busType: BusType;
  totalSeats: number;
  amenities: string[];
  operatorId: string;
  operatorCompanyName: string;
  operatorCode: string;
  contactPhone?: string;
  contactEmail?: string;
  operatorRating: number;
  routeId: string;
  routeCode: string;
  origin: string;
  destination: string;
  distanceKm: number;
  stops: RouteStop[];
  boardingPoints: BoardingPoint[];
  droppingPoints: DroppingPoint[];
  fares: FareItem[];
  seatMap: ScheduleSeatMap;
}
