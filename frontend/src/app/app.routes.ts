import { Routes } from '@angular/router';
import { MissionList } from './missions/mission-list/mission-list';
import { MatchList } from './matches/match-list/match-list';

export const routes: Routes = [
  { path: '', redirectTo: 'missions', pathMatch: 'full' },
  { path: 'missions', component: MissionList },
  { path: 'matches', component: MatchList },
];

