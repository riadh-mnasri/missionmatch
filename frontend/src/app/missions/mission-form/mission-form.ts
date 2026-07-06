import { Component, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MissionService } from '../mission.service';

@Component({
  selector: 'app-mission-form',
  imports: [ReactiveFormsModule],
  templateUrl: './mission-form.html',
  styleUrl: './mission-form.scss',
})
export class MissionForm {
  readonly published = output<void>();

  private readonly formBuilder = inject(FormBuilder);
  private readonly missionService = inject(MissionService);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    title: ['', Validators.required],
    clientName: ['', Validators.required],
    requiredSkills: ['', Validators.required],
    dailyRateAmount: [500, [Validators.required, Validators.min(1)]],
    startDate: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
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
        requiredSkills: value.requiredSkills
          .split(',')
          .map((skill) => skill.trim())
          .filter(Boolean),
        dailyRateAmount: value.dailyRateAmount,
        startDate: value.startDate,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.form.reset({ dailyRateAmount: 500 });
          this.published.emit();
        },
        error: () => {
          this.submitting.set(false);
          this.errorMessage.set('Could not publish the mission. Please try again.');
        },
      });
  }
}
