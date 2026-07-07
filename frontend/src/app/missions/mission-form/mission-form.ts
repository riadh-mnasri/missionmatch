import { Component, inject, output, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MissionService } from '../mission.service';
import { tagColorClass } from '../../shared/tag-color';

@Component({
  selector: 'app-mission-form',
  imports: [ReactiveFormsModule, FormsModule],
  templateUrl: './mission-form.html',
  styleUrl: './mission-form.scss',
})
export class MissionForm {
  readonly published = output<void>();

  private readonly formBuilder = inject(FormBuilder);
  private readonly missionService = inject(MissionService);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly skills = signal<string[]>([]);
  protected readonly skillsTouched = signal(false);
  protected skillDraft = '';

  protected readonly tagColorClass = tagColorClass;

  protected readonly form = this.formBuilder.nonNullable.group({
    title: ['', Validators.required],
    clientName: ['', Validators.required],
    dailyRateAmount: [500, [Validators.required, Validators.min(1)]],
    startDate: ['', Validators.required],
  });

  onSkillKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.commitSkillDraft();
    } else if (event.key === 'Backspace' && !this.skillDraft && this.skills().length > 0) {
      this.skills.update((skills) => skills.slice(0, -1));
    }
  }

  commitSkillDraft(): void {
    const value = this.skillDraft.trim().toLowerCase();
    this.skillDraft = '';
    this.skillsTouched.set(true);
    if (value && !this.skills().includes(value)) {
      this.skills.update((skills) => [...skills, value]);
    }
  }

  removeSkill(skill: string): void {
    this.skills.update((skills) => skills.filter((s) => s !== skill));
  }

  submit(): void {
    this.commitSkillDraft();
    this.skillsTouched.set(true);

    if (this.form.invalid || this.skills().length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);

    this.missionService
      .publish({
        title: value.title,
        clientName: value.clientName,
        requiredSkills: this.skills(),
        dailyRateAmount: value.dailyRateAmount,
        startDate: value.startDate,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.form.reset({ dailyRateAmount: 500 });
          this.skills.set([]);
          this.skillsTouched.set(false);
          this.published.emit();
        },
        error: () => {
          this.submitting.set(false);
          this.errorMessage.set('Could not publish the mission. Please try again.');
        },
      });
  }
}
