import { Component, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MissionService } from '../mission.service';
import { ChipInput } from '../../shared/chip-input/chip-input';
import { ToastService } from '../../shared/toast/toast.service';

@Component({
  selector: 'app-mission-form',
  imports: [ReactiveFormsModule, ChipInput],
  templateUrl: './mission-form.html',
})
export class MissionForm {
  readonly published = output<void>();

  private readonly formBuilder = inject(FormBuilder);
  private readonly missionService = inject(MissionService);
  private readonly toastService = inject(ToastService);

  protected readonly submitting = signal(false);
  protected readonly skills = signal<string[]>([]);
  protected readonly submitAttempted = signal(false);

  protected readonly form = this.formBuilder.nonNullable.group({
    title: ['', Validators.required],
    clientName: ['', Validators.required],
    dailyRateAmount: [500, [Validators.required, Validators.min(1)]],
    startDate: ['', Validators.required],
  });

  submit(): void {
    this.submitAttempted.set(true);

    if (this.form.invalid || this.skills().length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitting.set(true);

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
          this.submitAttempted.set(false);
          this.toastService.success(`Mission "${value.title}" published.`);
          this.published.emit();
        },
        error: () => {
          this.submitting.set(false);
          this.toastService.error('Could not publish the mission. Please try again.');
        },
      });
  }
}
