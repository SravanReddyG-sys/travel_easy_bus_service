export type BusType = 'AC_SEATER' | 'NON_AC_SEATER' | 'AC_SLEEPER' | 'NON_AC_SLEEPER' | 'AC_SEATER_SLEEPER' | 'VOLVO_MULTI_AXLE';
export type BusStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE';

export interface Bus {
  id: string;
  busNumber: string;
  registrationNumber: string;
  busType: BusType;
  totalSeats: number;
  hasLowerDeck: boolean;
  hasUpperDeck: boolean;
  lowerDeckSeatsCount: number;
  upperDeckSeatsCount: number;
  amenities: string[];
  status: BusStatus;
  operatorId: string;
  operatorCompanyName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateBusRequest {
  busNumber: string;
  registrationNumber: string;
  busType: BusType;
  totalSeats: number;
  hasLowerDeck: boolean;
  hasUpperDeck: boolean;
  lowerDeckSeatsCount: number;
  upperDeckSeatsCount: number;
  amenities: string[];
  status?: BusStatus;
}
