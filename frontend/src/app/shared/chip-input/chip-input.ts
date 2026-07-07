import { Component, input, model, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { tagColorClass } from '../tag-color';

@Component({
  selector: 'app-chip-input',
  imports: [FormsModule],
  templateUrl: './chip-input.html',
  styleUrl: './chip-input.scss',
})
export class ChipInput {
  readonly skills = model.required<string[]>();
  readonly placeholder = input('Type a skill and press Enter');
  readonly submitted = input(false);

  protected draft = '';
  protected readonly touched = signal(false);
  protected readonly tagColorClass = tagColorClass;

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.commitDraft();
    } else if (event.key === 'Backspace' && !this.draft && this.skills().length > 0) {
      this.skills.update((skills) => skills.slice(0, -1));
    }
  }

  commitDraft(): void {
    const value = this.draft.trim().toLowerCase();
    this.draft = '';
    this.touched.set(true);
    if (value && !this.skills().includes(value)) {
      this.skills.update((skills) => [...skills, value]);
    }
  }

  remove(skill: string): void {
    this.skills.update((skills) => skills.filter((s) => s !== skill));
  }

  isInvalid(): boolean {
    return (this.touched() || this.submitted()) && this.skills().length === 0;
  }
}
