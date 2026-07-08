import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MissionForm } from '../mission-form/mission-form';
import { MissionService } from '../mission.service';
import { Mission, MissionStatus } from '../mission.model';
import { StatusBadge } from '../../shared/status-badge/status-badge';
import { tagColorClass } from '../../shared/tag-color';
import { relativeDate } from '../../shared/relative-date';
import { initials } from '../../shared/initials';
import { ToastService } from '../../shared/toast/toast.service';

type StatusFilter = 'ALL' | MissionStatus;
type SortOption = 'start-date' | 'rate-desc' | 'rate-asc';

@Component({
  selector: 'app-mission-list',
  imports: [MissionForm, StatusBadge, FormsModule],
  templateUrl: './mission-list.html',
  styleUrl: './mission-list.scss',
})
export class MissionList implements OnInit {
  private readonly missionService = inject(MissionService);
  private readonly toastService = inject(ToastService);

  protected readonly missions = signal<Mission[]>([]);
  protected readonly loading = signal(true);
  protected readonly closingId = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly searchQuery = signal('');
  protected readonly statusFilter = signal<StatusFilter>('ALL');
  protected readonly sortBy = signal<SortOption>('start-date');

  protected readonly openCount = computed(() => this.missions().filter((m) => m.status === 'OPEN').length);
  protected readonly averageRate = computed(() => {
    const missions = this.missions();
    if (missions.length === 0) return 0;
    return Math.round(missions.reduce((sum, m) => sum + m.dailyRateAmount, 0) / missions.length);
  });

  protected readonly filteredMissions = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();

    let result = this.missions().filter((mission) => {
      const matchesStatus = status === 'ALL' || mission.status === status;
      const matchesQuery =
        query.length === 0 ||
        mission.title.toLowerCase().includes(query) ||
        mission.clientName.toLowerCase().includes(query) ||
        mission.requiredSkills.some((skill) => skill.toLowerCase().includes(query));
      return matchesStatus && matchesQuery;
    });

    result = [...result].sort((a, b) => {
      switch (this.sortBy()) {
        case 'rate-desc':
          return b.dailyRateAmount - a.dailyRateAmount;
        case 'rate-asc':
          return a.dailyRateAmount - b.dailyRateAmount;
        case 'start-date':
        default:
          return new Date(a.startDate).getTime() - new Date(b.startDate).getTime();
      }
    });

    return result;
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
        this.toastService.success('Mission closed.');
        this.loadMissions();
      },
      error: () => {
        this.closingId.set(null);
        this.toastService.error('Could not close this mission. Please try again.');
      },
    });
  }
}
