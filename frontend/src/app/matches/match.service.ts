import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Match } from './match.model';

@Injectable({ providedIn: 'root' })
export class MatchService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/matches';

  getForFreelancer(freelancerId: string): Observable<Match[]> {
    const params = new HttpParams().set('freelancerId', freelancerId);
    return this.http.get<Match[]>(this.baseUrl, { params });
  }
}
