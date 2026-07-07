import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatchService } from '../match.service';
import { Match } from '../match.model';
import { localFreelancerId } from '../../shared/local-freelancer-id';

@Component({
  selector: 'app-match-list',
  imports: [FormsModule],
  templateUrl: './match-list.html',
  styleUrl: './match-list.scss',
})
export class MatchList implements OnInit {
  private readonly matchService = inject(MatchService);

  protected freelancerId = localFreelancerId();
  protected readonly matches = signal<Match[]>([]);
  protected readonly loading = signal(false);
  protected readonly searched = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    if (!this.freelancerId.trim()) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.matchService.getForFreelancer(this.freelancerId.trim()).subscribe({
      next: (matches) => {
        this.matches.set(matches.sort((a, b) => b.score - a.score));
        this.loading.set(false);
        this.searched.set(true);
      },
      error: () => {
        this.loading.set(false);
        this.searched.set(true);
        this.errorMessage.set('Could not load matches for this freelancer id.');
      },
    });
  }
}
