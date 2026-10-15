import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Bus, CreateBusRequest } from '../../models/bus.model';
import { Route, CreateRouteRequest } from '../../models/route.model';
import { Schedule, CreateScheduleRequest, TravellerScheduleDetail } from '../../models/schedule.model';
import { BusSearchResponse } from '../../models/search.model';
import { LockSeatsRequest, LockSeatsResponse, ScheduleSeatMap } from '../../models/seat.model';
import {
  OperatorDashboardSummary,
  OperatorRevenueStat,
  OperatorOccupancyStat,
  SchedulePassengerManifest,
  OperatorLiveBookingFeed
} from '../../models/dashboard.model';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class BusApiService {
  private baseUrl = (typeof window !== 'undefined' && window.location.port && window.location.port !== '80')
    ? `http://${window.location.hostname}:8081/api/v1`
    : '/api/v1';

  constructor(private http: HttpClient) {}

  // ==========================================
  // TRAVELLER / PUBLIC ENDPOINTS
  // ==========================================

  searchBuses(params: {
    origin: string;
    destination: string;
    travelDate: string;
    busType?: string;
    minPrice?: number;
    maxPrice?: number;
    amenities?: string[];
  }): Observable<BusSearchResponse> {
    let httpParams = new HttpParams()
      .set('origin', params.origin)
      .set('destination', params.destination)
      .set('travelDate', params.travelDate);

    if (params.busType) httpParams = httpParams.set('busType', params.busType);
    if (params.minPrice != null) httpParams = httpParams.set('minPrice', params.minPrice.toString());
    if (params.maxPrice != null) httpParams = httpParams.set('maxPrice', params.maxPrice.toString());
    if (params.amenities && params.amenities.length > 0) {
      params.amenities.forEach(a => {
        httpParams = httpParams.append('amenities', a);
      });
    }

    return this.http.get<ApiResponse<BusSearchResponse>>(`${this.baseUrl}/buses/search`, { params: httpParams })
      .pipe(map(res => res.data));
  }

  getTravellerScheduleDetails(scheduleId: string): Observable<TravellerScheduleDetail> {
    return this.http.get<ApiResponse<TravellerScheduleDetail>>(`${this.baseUrl}/schedules/${scheduleId}/details`)
      .pipe(map(res => res.data));
  }

  getScheduleSeatMap(scheduleId: string): Observable<ScheduleSeatMap> {
    return this.http.get<ApiResponse<ScheduleSeatMap>>(`${this.baseUrl}/schedules/${scheduleId}/seat-map`)
      .pipe(map(res => res.data));
  }

  lockSeats(scheduleId: string, request: LockSeatsRequest): Observable<LockSeatsResponse> {
    return this.http.post<ApiResponse<LockSeatsResponse>>(`${this.baseUrl}/schedules/${scheduleId}/seats/lock`, request)
      .pipe(map(res => res.data));
  }

  releaseSeats(scheduleId: string, seatNumbers: string[], lockToken?: string): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/schedules/${scheduleId}/seats/release`, {
      seatNumbers,
      lockToken
    }).pipe(map(res => res.data));
  }

  // ==========================================
  // RESEND EMAIL NOTIFICATIONS (resend.com)
  // ==========================================

  sendBookingConfirmationEmail(payload: {
    toEmail: string;
    passengerName: string;
    bookingReference: string;
    busNumber: string;
    operatorName: string;
    origin: string;
    destination: string;
    travelDate: string;
    departureTime: string;
    seats: string[];
    totalAmount: number;
    boardingPoint?: string;
    droppingPoint?: string;
  }): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/notifications/email/booking-confirmation`, payload)
      .pipe(map(res => res.data));
  }

  sendTestEmail(toEmail: string, subject?: string): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/notifications/email/test`, {
      to: toEmail,
      subject: subject || 'Test Email from TravelEasy via Resend'
    }).pipe(map(res => res.data));
  }

  getResendEmailStatus(): Observable<any> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/notifications/email/status`)
      .pipe(map(res => res.data));
  }

  // ==========================================
  // OPERATOR FLEET & ROUTE MANAGEMENT
  // ==========================================

  getOperatorBuses(page = 0, size = 20): Observable<PageResponse<Bus>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<Bus>>>(`${this.baseUrl}/buses`, { params })
      .pipe(map(res => res.data));
  }

  createBus(bus: CreateBusRequest): Observable<Bus> {
    return this.http.post<ApiResponse<Bus>>(`${this.baseUrl}/buses`, bus)
      .pipe(map(res => res.data));
  }

  updateBus(busId: string, bus: Partial<CreateBusRequest>): Observable<Bus> {
    return this.http.put<ApiResponse<Bus>>(`${this.baseUrl}/buses/${busId}`, bus)
      .pipe(map(res => res.data));
  }

  deleteBus(busId: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/buses/${busId}`)
      .pipe(map(res => res.data));
  }

  getOperatorRoutes(page = 0, size = 20): Observable<PageResponse<Route>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<Route>>>(`${this.baseUrl}/routes`, { params })
      .pipe(map(res => res.data));
  }

  createRoute(route: CreateRouteRequest): Observable<Route> {
    return this.http.post<ApiResponse<Route>>(`${this.baseUrl}/routes`, route)
      .pipe(map(res => res.data));
  }

  // ==========================================
  // OPERATOR SCHEDULE MANAGEMENT
  // ==========================================

  getOperatorSchedules(page = 0, size = 20): Observable<PageResponse<Schedule>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<Schedule>>>(`${this.baseUrl}/schedules`, { params })
      .pipe(map(res => res.data));
  }

  createSchedule(schedule: CreateScheduleRequest): Observable<Schedule> {
    return this.http.post<ApiResponse<Schedule>>(`${this.baseUrl}/schedules`, schedule)
      .pipe(map(res => res.data));
  }

  cancelSchedule(scheduleId: string, reason: string): Observable<Schedule> {
    return this.http.patch<ApiResponse<Schedule>>(`${this.baseUrl}/schedules/${scheduleId}/cancel`, {
      cancellationReason: reason
    }).pipe(map(res => res.data));
  }

  updateScheduleLifecycle(scheduleId: string, status: string): Observable<Schedule> {
    return this.http.patch<ApiResponse<Schedule>>(`${this.baseUrl}/schedules/${scheduleId}/status`, {
      status
    }).pipe(map(res => res.data));
  }

  toggleSeatBlock(scheduleId: string, seatNumber: string, blocked: boolean): Observable<any> {
    return this.http.patch<ApiResponse<any>>(`${this.baseUrl}/schedules/${scheduleId}/seats/${seatNumber}/block`, null, {
      params: new HttpParams().set('blocked', blocked.toString())
    }).pipe(map(res => res.data));
  }

  // ==========================================
  // OPERATOR DASHBOARD & ANALYTICS
  // ==========================================

  getDashboardSummary(operatorId?: string): Observable<OperatorDashboardSummary> {
    let params = new HttpParams();
    if (operatorId) params = params.set('operatorId', operatorId);
    return this.http.get<ApiResponse<OperatorDashboardSummary>>(`${this.baseUrl}/operator/dashboard/summary`, { params })
      .pipe(map(res => res.data));
  }

  getRevenueAnalytics(operatorId?: string): Observable<OperatorRevenueStat[]> {
    let params = new HttpParams();
    if (operatorId) params = params.set('operatorId', operatorId);
    return this.http.get<ApiResponse<OperatorRevenueStat[]>>(`${this.baseUrl}/operator/dashboard/revenue`, { params })
      .pipe(map(res => res.data));
  }

  getOccupancyAnalytics(operatorId?: string, startDate?: string, endDate?: string): Observable<OperatorOccupancyStat[]> {
    let params = new HttpParams();
    if (operatorId) params = params.set('operatorId', operatorId);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<OperatorOccupancyStat[]>>(`${this.baseUrl}/operator/dashboard/occupancy`, { params })
      .pipe(map(res => res.data));
  }

  getScheduleManifest(scheduleId: string): Observable<SchedulePassengerManifest> {
    return this.http.get<ApiResponse<SchedulePassengerManifest>>(`${this.baseUrl}/operator/dashboard/schedules/${scheduleId}/manifest`)
      .pipe(map(res => res.data));
  }

  getLiveBookingFeed(operatorId?: string, limit = 20): Observable<OperatorLiveBookingFeed[]> {
    let params = new HttpParams().set('limit', limit.toString());
    if (operatorId) params = params.set('operatorId', operatorId);
    return this.http.get<ApiResponse<OperatorLiveBookingFeed[]>>(`${this.baseUrl}/operator/dashboard/feed/live-bookings`, { params })
      .pipe(map(res => res.data));
  }
}
