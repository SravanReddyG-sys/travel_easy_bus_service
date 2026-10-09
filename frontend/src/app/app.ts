import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {
  title = 'TravelEasy Bus Microservice';

  constructor(public authService: AuthService) {}

  switchRole(role: 'TRAVELLER' | 'OPERATOR'): void {
    this.authService.switchRole(role);
  }
}
