export interface RouteStop {
  id?: string;
  stopName: string;
  stopOrder: number;
  distanceFromOriginKm: number;
  estimatedMinutesFromOrigin: number;
}

export interface BoardingPoint {
  id?: string;
  pointName: string;
  landmark?: string;
  contactNumber?: string;
  sequenceOrder: number;
}

export interface DroppingPoint {
  id?: string;
  pointName: string;
  landmark?: string;
  contactNumber?: string;
  sequenceOrder: number;
}

export interface Route {
  id: string;
  routeCode: string;
  origin: string;
  destination: string;
  distanceKm: number;
  estimatedDurationMinutes: number;
  isActive: boolean;
  operatorId: string;
  stops?: RouteStop[];
  boardingPoints?: BoardingPoint[];
  droppingPoints?: DroppingPoint[];
  createdAt?: string;
}

export interface CreateRouteRequest {
  routeCode: string;
  origin: string;
  destination: string;
  distanceKm: number;
  estimatedDurationMinutes: number;
  isActive?: boolean;
  stops?: RouteStop[];
  boardingPoints?: BoardingPoint[];
  droppingPoints?: DroppingPoint[];
}
