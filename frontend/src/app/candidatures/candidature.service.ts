import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Candidature, CandidatureStatus } from './candidature.model';

@Injectable({ providedIn: 'root' })
export class CandidatureService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/candidatures';

  getForFreelancer(freelancerId: string): Observable<Candidature[]> {
    const params = new HttpParams().set('freelancerId', freelancerId);
    return this.http.get<Candidature[]>(this.baseUrl, { params });
  }

  updateStatus(id: string, status: CandidatureStatus): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/status`, { status });
  }
}
