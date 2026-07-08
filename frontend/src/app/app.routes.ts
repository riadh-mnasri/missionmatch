import { Routes } from '@angular/router';
import { DashboardHome } from './dashboard/dashboard-home/dashboard-home';
import { MissionList } from './missions/mission-list/mission-list';
import { MatchList } from './matches/match-list/match-list';
import { ProfilePage } from './profile/profile-page/profile-page';
import { CandidatureBoard } from './candidatures/candidature-board/candidature-board';

export const routes: Routes = [
  { path: '', component: DashboardHome },
  { path: 'missions', component: MissionList },
  { path: 'profile', component: ProfilePage },
  { path: 'matches', component: MatchList },
  { path: 'candidatures', component: CandidatureBoard },
];
