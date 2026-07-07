import { Routes } from '@angular/router';
import { MissionList } from './missions/mission-list/mission-list';
import { MatchList } from './matches/match-list/match-list';
import { ProfilePage } from './profile/profile-page/profile-page';

export const routes: Routes = [
  { path: '', redirectTo: 'missions', pathMatch: 'full' },
  { path: 'missions', component: MissionList },
  { path: 'profile', component: ProfilePage },
  { path: 'matches', component: MatchList },
];
