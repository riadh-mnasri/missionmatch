import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { MissionService } from '../../missions/mission.service';
import { Mission } from '../../missions/mission.model';
import { MatchService } from '../../matches/match.service';
import { Match } from '../../matches/match.model';
import { CandidatureService } from '../../candidatures/candidature.service';
import { CANDIDATURE_COLUMNS, Candidature } from '../../candidatures/candidature.model';
import { localFreelancerId } from '../../shared/local-freelancer-id';

interface SkillCount {
  skill: string;
  count: number;
}

@Component({
  selector: 'app-dashboard-home',
  imports: [RouterLink],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
})
export class DashboardHome implements OnInit {
  private readonly missionService = inject(MissionService);
  private readonly matchService = inject(MatchService);
  private readonly candidatureService = inject(CandidatureService);

  protected readonly freelancerId = localFreelancerId();
  protected readonly loading = signal(true);

  protected readonly missions = signal<Mission[]>([]);
  protected readonly matches = signal<Match[]>([]);
  protected readonly candidatures = signal<Candidature[]>([]);

  protected readonly openMissions = computed(() => this.missions().filter((m) => m.status === 'OPEN').length);

  protected readonly averageRate = computed(() => {
    const missions = this.missions();
    if (missions.length === 0) return 0;
    return Math.round(missions.reduce((sum, m) => sum + m.dailyRateAmount, 0) / missions.length);
  });

  protected readonly topSkills = computed<SkillCount[]>(() => {
    const counts = new Map<string, number>();
    for (const mission of this.missions()) {
      for (const skill of mission.requiredSkills) {
        counts.set(skill, (counts.get(skill) ?? 0) + 1);
      }
    }
    return [...counts.entries()]
      .map(([skill, count]) => ({ skill, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 6);
  });

  protected readonly maxSkillCount = computed(() => Math.max(1, ...this.topSkills().map((s) => s.count)));

  protected readonly pipelineColumns = CANDIDATURE_COLUMNS;

  protected readonly pipelineByStatus = computed(() => {
    const candidatures = this.candidatures();
    return new Map(this.pipelineColumns.map((column) => [column.status, candidatures.filter((c) => c.status === column.status).length]));
  });

  ngOnInit(): void {
    forkJoin({
      missions: this.missionService.getAll().pipe(catchError(() => of<Mission[]>([]))),
      matches: this.matchService.getForFreelancer(this.freelancerId).pipe(catchError(() => of<Match[]>([]))),
      candidatures: this.candidatureService.getForFreelancer(this.freelancerId).pipe(catchError(() => of<Candidature[]>([]))),
    }).subscribe(({ missions, matches, candidatures }) => {
      this.missions.set(missions);
      this.matches.set(matches);
      this.candidatures.set(candidatures);
      this.loading.set(false);
    });
  }
}
