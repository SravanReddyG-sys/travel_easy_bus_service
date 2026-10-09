import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ScheduleSeatItem, ScheduleSeatMap } from '../../../models/seat.model';

@Component({
  selector: 'app-seat-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './seat-map.component.html',
  styleUrls: ['./seat-map.component.scss']
})
export class SeatMapComponent implements OnInit, OnDestroy {
  @Input() seatMap!: ScheduleSeatMap;
  @Input() maxSelection = 6;
  @Input() lockExpiresAt?: string;

  @Output() seatSelected = new EventEmitter<ScheduleSeatItem[]>();
  @Output() lockExpired = new EventEmitter<void>();

  activeDeck: 'LOWER' | 'UPPER' = 'LOWER';
  selectedSeats: Map<string, ScheduleSeatItem> = new Map();
  countdownSeconds = 0;
  private timerInterval?: any;

  ngOnInit(): void {
    if (!this.seatMap.hasLowerDeck && this.seatMap.hasUpperDeck) {
      this.activeDeck = 'UPPER';
    }
    this.initCountdown();
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  get currentDeckSeats(): ScheduleSeatItem[] {
    return this.activeDeck === 'LOWER'
      ? (this.seatMap?.lowerDeckSeats || [])
      : (this.seatMap?.upperDeckSeats || []);
  }

  get totalSelectedAmount(): number {
    let total = 0;
    this.selectedSeats.forEach(seat => total += (seat.totalAmount || 0));
    return total;
  }

  get selectedSeatsList(): ScheduleSeatItem[] {
    return Array.from(this.selectedSeats.values());
  }

  toggleDeck(deck: 'LOWER' | 'UPPER'): void {
    this.activeDeck = deck;
  }

  isSeatSelected(seatNumber: string): boolean {
    return this.selectedSeats.has(seatNumber);
  }

  onSeatClick(seat: ScheduleSeatItem): void {
    if (seat.status !== 'AVAILABLE') {
      return;
    }

    if (this.selectedSeats.has(seat.seatNumber)) {
      this.selectedSeats.delete(seat.seatNumber);
    } else {
      if (this.selectedSeats.size >= this.maxSelection) {
        alert(`You can select at most ${this.maxSelection} seats at a time.`);
        return;
      }
      this.selectedSeats.set(seat.seatNumber, seat);
    }

    this.seatSelected.emit(this.selectedSeatsList);
  }

  clearSelection(): void {
    this.selectedSeats.clear();
    this.seatSelected.emit([]);
  }

  getGridRows(): number[] {
    const rows = new Set<number>();
    this.currentDeckSeats.forEach(s => rows.add(s.rowNum));
    return Array.from(rows).sort((a, b) => a - b);
  }

  getGridCols(): number[] {
    const cols = new Set<number>();
    this.currentDeckSeats.forEach(s => cols.add(s.colNum));
    return Array.from(cols).sort((a, b) => a - b);
  }

  getSeatAt(row: number, col: number): ScheduleSeatItem | undefined {
    return this.currentDeckSeats.find(s => s.rowNum === row && s.colNum === col);
  }

  private initCountdown(): void {
    if (!this.lockExpiresAt) return;

    const expiry = new Date(this.lockExpiresAt).getTime();
    this.updateTimer(expiry);

    this.timerInterval = setInterval(() => {
      this.updateTimer(expiry);
    }, 1000);
  }

  private updateTimer(expiry: number): void {
    const now = new Date().getTime();
    const diff = Math.floor((expiry - now) / 1000);

    if (diff <= 0) {
      this.countdownSeconds = 0;
      if (this.timerInterval) clearInterval(this.timerInterval);
      this.lockExpired.emit();
    } else {
      this.countdownSeconds = diff;
    }
  }

  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }
}
