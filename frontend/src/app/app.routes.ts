import { Routes } from '@angular/router';
import { MissionList } from './missions/mission-list/mission-list';
import { MatchList } from './matches/match-list/match-list';
import { ProfilePage } from './profile/profile-page/profile-page';
import { CandidatureBoard } from './candidatures/candidature-board/candidature-board';

export const routes: Routes = [
  { path: '', redirectTo: 'missions', pathMatch: 'full' },
  { path: 'missions', component: MissionList },
  { path: 'profile', component: ProfilePage },
  { path: 'matches', component: MatchList },
  { path: 'candidatures', component: CandidatureBoard },
];
