import { BusType } from './bus.model';
import { AvailabilityStatus } from './schedule.model';

export interface BusSearchResultItem {
  scheduleId: string;
  scheduleDate?: string;
  departureTime: string;
  arrivalTime: string;
  durationMinutes: number;
  busId: string;
  busNumber: string;
  busType: BusType;
  amenities: string[];
  operatorId: string;
  operatorCompanyName?: string;
  operatorName?: string;
  operatorRating?: number;
  routeId: string;
  origin: string;
  destination: string;
  startingPrice?: number;
  startingFare?: number;
  currency?: string;
  availableSeatsCount: number;
  totalSeatsCount: number;
  availabilityStatus?: AvailabilityStatus;
  boardingPointsCount?: number;
  droppingPointsCount?: number;
  boardingPoints?: any[];
  droppingPoints?: any[];
}

export interface BusSearchFilterSummary {
  totalFound: number;
  minPrice: number;
  maxPrice: number;
  availableBusTypes?: BusType[];
  availableAmenities?: string[];
  availableOperators?: string[];
}

export interface BusSearchResponse {
  content?: BusSearchResultItem[];
  results?: BusSearchResultItem[];
  origin?: string;
  destination?: string;
  travelDate?: string;
  totalElements?: number;
  totalMatches?: number;
  totalPages?: number;
  filterSummary?: BusSearchFilterSummary;
  minPrice?: number;
  maxPrice?: number;
  availableBusTypes?: BusType[];
  availableAmenities?: string[];
  availableOperators?: string[];
}

export interface BusSearchFilter {
  origin: string;
  destination: string;
  travelDate: string;
  busType?: BusType;
  minPrice?: number;
  maxPrice?: number;
  amenities?: string[];
  operatorId?: string;
  timeOfDay?: 'MORNING' | 'AFTERNOON' | 'EVENING' | 'NIGHT';
}
