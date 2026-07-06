import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Mission, PublishMissionRequest } from './mission.model';

@Injectable({ providedIn: 'root' })
export class MissionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/missions';

  getAll(): Observable<Mission[]> {
    return this.http.get<Mission[]>(this.baseUrl);
  }

  publish(request: PublishMissionRequest): Observable<Mission> {
    return this.http.post<Mission>(this.baseUrl, request);
  }

  close(missionId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${missionId}/close`, {});
  }
}
