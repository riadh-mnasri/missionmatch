import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Profile, UpdateProfileRequest } from './profile.model';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/profile';

  getById(freelancerId: string): Observable<Profile> {
    return this.http.get<Profile>(`${this.baseUrl}/${freelancerId}`);
  }

  update(freelancerId: string, request: UpdateProfileRequest): Observable<Profile> {
    return this.http.put<Profile>(`${this.baseUrl}/${freelancerId}`, request);
  }
}
