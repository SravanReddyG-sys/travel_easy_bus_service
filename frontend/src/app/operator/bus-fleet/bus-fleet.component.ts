import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { BusApiService } from '../../core/services/bus-api.service';
import { AuthService } from '../../core/services/auth.service';
import { Bus, BusType, CreateBusRequest } from '../../models/bus.model';

@Component({
  selector: 'app-bus-fleet',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bus-fleet.component.html',
  styleUrls: ['./bus-fleet.component.scss']
})
export class BusFleetComponent implements OnInit, OnDestroy {
  buses: Bus[] = [];
  loading = true;
  errorMessage = '';
  showCreateModal = false;

  newBus: CreateBusRequest = {
    busNumber: '',
    registrationNumber: '',
    busType: 'AC_SEATER',
    totalSeats: 40,
    hasLowerDeck: true,
    hasUpperDeck: false,
    lowerDeckSeatsCount: 40,
    upperDeckSeatsCount: 0,
    amenities: ['WiFi', 'Charging Point', 'Water Bottle'],
    status: 'ACTIVE'
  };

  availableBusTypes: BusType[] = [
    'AC_SEATER',
    'NON_AC_SEATER',
    'AC_SLEEPER',
    'NON_AC_SLEEPER',
    'AC_SEATER_SLEEPER',
    'VOLVO_MULTI_AXLE'
  ];

  amenityInput = '';
  private authSubscription?: Subscription;

  constructor(
    private busApiService: BusApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authSubscription = this.authService.currentUser$.subscribe((user) => {
      if (user.token) {
        this.loadBuses();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
  }

  loadBuses(): void {
    this.loading = true;
    this.busApiService.getOperatorBuses().subscribe({
      next: (data) => {
        this.buses = data.content || [];
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load buses.';
        this.loading = false;
      }
    });
  }

  openCreateModal(): void {
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  addAmenity(): void {
    if (this.amenityInput.trim() && !this.newBus.amenities.includes(this.amenityInput.trim())) {
      this.newBus.amenities.push(this.amenityInput.trim());
      this.amenityInput = '';
    }
  }

  removeAmenity(index: number): void {
    this.newBus.amenities.splice(index, 1);
  }

  submitCreateBus(): void {
    if (!this.newBus.busNumber || !this.newBus.registrationNumber) {
      alert('Please provide Bus Number and Registration Number.');
      return;
    }

    this.busApiService.createBus(this.newBus).subscribe({
      next: () => {
        alert('Bus registered successfully in fleet!');
        this.closeCreateModal();
        this.loadBuses();
      },
      error: (err) => {
        const msg = err.error?.message || (err.status === 0
          ? 'Backend server is unreachable on http://localhost:8081. Please ensure the Spring Boot backend is started.'
          : 'Failed to create bus.');
        alert(msg);
      }
    });
  }

  deleteBus(bus: Bus): void {
    if (confirm(`Are you sure you want to delete bus ${bus.busNumber}?`)) {
      this.busApiService.deleteBus(bus.id).subscribe({
        next: () => this.loadBuses(),
        error: (err) => alert(err.error?.message || 'Failed to delete bus.')
      });
    }
  }
}
