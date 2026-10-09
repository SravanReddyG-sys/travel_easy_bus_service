import { Routes } from '@angular/router';
import { BusSearchComponent } from './traveller/bus-search/bus-search.component';
import { ScheduleDetailsComponent } from './traveller/schedule-details/schedule-details.component';
import { OperatorDashboardComponent } from './operator/dashboard/operator-dashboard.component';
import { BusFleetComponent } from './operator/bus-fleet/bus-fleet.component';
import { OperatorSchedulesComponent } from './operator/schedules/operator-schedules.component';
import { PassengerManifestComponent } from './operator/passenger-manifest/passenger-manifest.component';

export const routes: Routes = [
  { path: '', redirectTo: 'traveller/search', pathMatch: 'full' },
  { path: 'traveller/search', component: BusSearchComponent },
  { path: 'traveller/schedules/:id', component: ScheduleDetailsComponent },
  { path: 'operator/dashboard', component: OperatorDashboardComponent },
  { path: 'operator/buses', component: BusFleetComponent },
  { path: 'operator/schedules', component: OperatorSchedulesComponent },
  { path: 'operator/schedules/:id/manifest', component: PassengerManifestComponent },
  { path: '**', redirectTo: 'traveller/search' }
];
