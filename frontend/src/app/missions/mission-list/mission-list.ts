import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MissionForm } from '../mission-form/mission-form';
import { MissionService } from '../mission.service';
import { Mission } from '../mission.model';
import { StatusBadge } from '../../shared/status-badge/status-badge';
import { tagColorClass } from '../../shared/tag-color';
import { relativeDate } from '../../shared/relative-date';
import { initials } from '../../shared/initials';

@Component({
  selector: 'app-mission-list',
  imports: [MissionForm, StatusBadge],
  templateUrl: './mission-list.html',
  styleUrl: './mission-list.scss',
})
export class MissionList implements OnInit {
  private readonly missionService = inject(MissionService);

  protected readonly missions = signal<Mission[]>([]);
  protected readonly loading = signal(true);
  protected readonly closingId = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly openCount = computed(() => this.missions().filter((m) => m.status === 'OPEN').length);
  protected readonly averageRate = computed(() => {
    const missions = this.missions();
    if (missions.length === 0) return 0;
    return Math.round(missions.reduce((sum, m) => sum + m.dailyRateAmount, 0) / missions.length);
  });

  protected readonly tagColorClass = tagColorClass;
  protected readonly relativeDate = relativeDate;
  protected readonly initials = initials;

  ngOnInit(): void {
    this.loadMissions();
  }

  loadMissions(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.missionService.getAll().subscribe({
      next: (missions) => {
        this.missions.set(missions);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Could not load missions. Is the backend running?');
      },
    });
  }

  closeMission(missionId: string): void {
    this.closingId.set(missionId);
    this.missionService.close(missionId).subscribe({
      next: () => {
        this.closingId.set(null);
        this.loadMissions();
      },
      error: () => this.closingId.set(null),
    });
  }
}
