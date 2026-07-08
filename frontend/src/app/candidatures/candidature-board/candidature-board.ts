import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CandidatureService } from '../candidature.service';
import { ALLOWED_TRANSITIONS, CANDIDATURE_COLUMNS, Candidature, CandidatureStatus } from '../candidature.model';
import { localFreelancerId } from '../../shared/local-freelancer-id';

@Component({
  selector: 'app-candidature-board',
  imports: [FormsModule],
  templateUrl: './candidature-board.html',
  styleUrl: './candidature-board.scss',
})
export class CandidatureBoard implements OnInit {
  private readonly candidatureService = inject(CandidatureService);

  protected freelancerId = localFreelancerId();
  protected readonly candidatures = signal<Candidature[]>([]);
  protected readonly loading = signal(false);
  protected readonly searched = signal(false);
  protected readonly movingId = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly columns = CANDIDATURE_COLUMNS;
  protected readonly allowedTransitions = ALLOWED_TRANSITIONS;

  protected readonly byColumn = computed(() => {
    const candidatures = this.candidatures();
    return new Map(this.columns.map((column) => [column.status, candidatures.filter((c) => c.status === column.status)]));
  });

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    if (!this.freelancerId.trim()) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.candidatureService.getForFreelancer(this.freelancerId.trim()).subscribe({
      next: (candidatures) => {
        this.candidatures.set(candidatures);
        this.loading.set(false);
        this.searched.set(true);
      },
      error: () => {
        this.loading.set(false);
        this.searched.set(true);
        this.errorMessage.set('Could not load the candidature pipeline for this freelancer id.');
      },
    });
  }

  move(candidature: Candidature, newStatus: CandidatureStatus): void {
    this.movingId.set(candidature.id);
    this.errorMessage.set(null);

    this.candidatureService.updateStatus(candidature.id, newStatus).subscribe({
      next: () => {
        this.movingId.set(null);
        this.candidatures.update((all) =>
          all.map((c) => (c.id === candidature.id ? { ...c, status: newStatus } : c)),
        );
      },
      error: () => {
        this.movingId.set(null);
        this.errorMessage.set('Could not move this candidature. It may have already moved on.');
      },
    });
  }

  columnLabel(status: CandidatureStatus): string {
    return this.columns.find((column) => column.status === status)?.label ?? status;
  }
}
