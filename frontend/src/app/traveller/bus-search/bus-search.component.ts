import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BusApiService } from '../../core/services/bus-api.service';
import { BusSearchResultItem, BusSearchResponse } from '../../models/search.model';
import { BusType } from '../../models/bus.model';

@Component({
  selector: 'app-bus-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bus-search.component.html',
  styleUrls: ['./bus-search.component.scss']
})
export class BusSearchComponent implements OnInit {
  origin = 'Delhi';
  destination = 'Jaipur';
  travelDate = '';
  selectedBusType: BusType | '' = '';
  maxPrice: number = 2000;
  selectedAmenities: string[] = [];

  loading = false;
  hasSearched = false;
  errorMessage = '';
  searchResponse: BusSearchResponse | null = null;
  filteredResults: BusSearchResultItem[] = [];

  availableBusTypes: { label: string; value: BusType }[] = [
    { label: 'AC Seater', value: 'AC_SEATER' },
    { label: 'AC Sleeper', value: 'AC_SLEEPER' },
    { label: 'Non-AC Seater', value: 'NON_AC_SEATER' },
    { label: 'Non-AC Sleeper', value: 'NON_AC_SLEEPER' },
    { label: 'Volvo Multi-Axle', value: 'VOLVO_MULTI_AXLE' }
  ];

  popularAmenities = ['WiFi', 'Charging Point', 'Blanket', 'Water Bottle', 'Reading Light', 'Emergency Exit'];

  popularRoutes = [
    { from: 'Delhi', to: 'Jaipur' },
    { from: 'Delhi', to: 'Chandigarh' },
    { from: 'Mumbai', to: 'Pune' },
    { from: 'Bangalore', to: 'Chennai' },
    { from: 'Hyderabad', to: 'Vijayawada' },
    { from: 'Bangalore', to: 'Hyderabad' },
    { from: 'Mumbai', to: 'Goa' },
    { from: 'Chennai', to: 'Coimbatore' },
    { from: 'Pune', to: 'Goa' },
    { from: 'Delhi', to: 'Agra' }
  ];

  constructor(
    private busApiService: BusApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const today = new Date();
    today.setDate(today.getDate() + 1);
    this.travelDate = today.toISOString().split('T')[0];
    this.executeSearch();
  }

  selectPopularRoute(from: string, to: string): void {
    this.origin = from;
    this.destination = to;
    this.executeSearch();
  }

  executeSearch(): void {
    if (!this.origin || !this.destination || !this.travelDate) {
      this.errorMessage = 'Please enter origin, destination, and travel date.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.hasSearched = true;

    this.busApiService.searchBuses({
      origin: this.origin.trim(),
      destination: this.destination.trim(),
      travelDate: this.travelDate,
      busType: this.selectedBusType || undefined,
      maxPrice: this.maxPrice || undefined,
      amenities: this.selectedAmenities.length > 0 ? this.selectedAmenities : undefined
    }).subscribe({
      next: (data) => {
        this.searchResponse = data;
        const items = data.content || data.results || [];
        this.filteredResults = items.map((item: any) => ({
          ...item,
          operatorCompanyName: item.operatorCompanyName || item.operatorName || 'Royal Express Travels',
          startingPrice: item.startingPrice != null ? item.startingPrice : (item.startingFare != null ? item.startingFare : 550),
          availableSeatsCount: item.availableSeatsCount != null ? item.availableSeatsCount : 4,
          totalSeatsCount: item.totalSeatsCount != null ? item.totalSeatsCount : 4,
          boardingPointsCount: item.boardingPointsCount != null ? item.boardingPointsCount : (item.boardingPoints?.length || 2),
          droppingPointsCount: item.droppingPointsCount != null ? item.droppingPointsCount : (item.droppingPoints?.length || 2)
        }));
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to search buses. Please try again.';
        this.searchResponse = null;
        this.filteredResults = [];
        this.loading = false;
      }
    });
  }

  toggleAmenity(amenity: string): void {
    const index = this.selectedAmenities.indexOf(amenity);
    if (index > -1) {
      this.selectedAmenities.splice(index, 1);
    } else {
      this.selectedAmenities.push(amenity);
    }
    this.executeSearch();
  }

  isAmenitySelected(amenity: string): boolean {
    return this.selectedAmenities.includes(amenity);
  }

  viewSeats(scheduleId: string): void {
    this.router.navigate(['/traveller/schedules', scheduleId]);
  }

  formatTime(dateTimeStr: string): string {
    if (!dateTimeStr) return '';
    const date = new Date(dateTimeStr);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  formatDuration(minutes: number): string {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return `${h}h ${m > 0 ? m + 'm' : ''}`;
  }
}
