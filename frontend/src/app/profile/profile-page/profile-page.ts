import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProfileService } from '../profile.service';
import { ChipInput } from '../../shared/chip-input/chip-input';
import { localFreelancerId } from '../../shared/local-freelancer-id';
import { ToastService } from '../../shared/toast/toast.service';

@Component({
  selector: 'app-profile-page',
  imports: [ReactiveFormsModule, ChipInput],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
})
export class ProfilePage implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly toastService = inject(ToastService);

  protected readonly freelancerId = localFreelancerId();

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly skills = signal<string[]>([]);
  protected readonly submitAttempted = signal(false);

  protected readonly form = this.formBuilder.nonNullable.group({
    expectedDailyRateAmount: [500, [Validators.required, Validators.min(1)]],
  });

  ngOnInit(): void {
    this.profileService.getById(this.freelancerId).subscribe({
      next: (profile) => {
        this.skills.set(profile.skills);
        this.form.patchValue({ expectedDailyRateAmount: profile.expectedDailyRateAmount });
        this.loading.set(false);
      },
      error: () => {
        // No profile yet for this browser: start from a blank form.
        this.loading.set(false);
      },
    });
  }

  save(): void {
    this.submitAttempted.set(true);

    if (this.form.invalid || this.skills().length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);

    this.profileService
      .update(this.freelancerId, {
        skills: this.skills(),
        expectedDailyRateAmount: this.form.getRawValue().expectedDailyRateAmount,
        expectedDailyRateCurrency: 'EUR',
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.toastService.success('Profile saved.');
        },
        error: () => {
          this.saving.set(false);
          this.toastService.error('Could not save your profile. Please try again.');
        },
      });
  }
}
