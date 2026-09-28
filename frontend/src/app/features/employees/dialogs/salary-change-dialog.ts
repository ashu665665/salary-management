import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { provideNativeDateAdapter } from '@angular/material/core';

import { EmployeeApi } from '../../../core/api/employee-api';
import { EmployeeDetail, JOB_LEVELS, JobLevel } from '../../../core/models/employee.model';
import { EnumLabelPipe } from '../../../shared/enum-label-pipe';

type ChangeKind = 'ANNUAL_RAISE' | 'MARKET_CORRECTION' | 'PROMOTION';

/**
 * One form for every kind of pay change.
 *
 * <p>A promotion is not a different form, it is a pay change that also moves someone's level, so
 * it lives here with an extra field rather than in a screen of its own. It still goes to the
 * promotion endpoint, because the server keeps the level and the pay together.
 */
@Component({
  selector: 'app-salary-change-dialog',
  providers: [provideNativeDateAdapter()],
  imports: [
    FormsModule, MatButtonModule, MatDatepickerModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, EnumLabelPipe,
  ],
  templateUrl: './salary-change-dialog.html',
})
export class SalaryChangeDialog {
  private readonly api = inject(EmployeeApi);
  private readonly dialogRef = inject(MatDialogRef<SalaryChangeDialog>);
  protected readonly employee = inject<EmployeeDetail>(MAT_DIALOG_DATA);

  protected readonly kinds: ChangeKind[] = ['ANNUAL_RAISE', 'MARKET_CORRECTION', 'PROMOTION'];
  protected readonly levels = JOB_LEVELS.filter((level) => this.isAbove(level, this.employee.jobLevel));

  protected readonly kind = signal<ChangeKind>('ANNUAL_RAISE');
  protected readonly amount = signal<number | null>(this.employee.currentSalary?.amount ?? null);
  protected readonly effectiveDate = signal<Date>(new Date());
  protected readonly newLevel = signal<JobLevel | null>(this.levels[0] ?? null);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly currency = this.employee.currentSalary?.currency ?? 'USD';

  protected canSave(): boolean {
    const amount = this.amount();
    if (amount === null || amount <= 0 || this.saving()) {
      return false;
    }
    return this.kind() !== 'PROMOTION' || this.newLevel() !== null;
  }

  protected save(): void {
    if (!this.canSave()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);

    const request = {
      amount: this.amount()!,
      currency: this.currency,
      effectiveDate: toIsoDate(this.effectiveDate()),
    };

    const call =
      this.kind() === 'PROMOTION'
        ? this.api.promote(this.employee.id, { ...request, newLevel: this.newLevel()! })
        : this.api.recordRevision(this.employee.id, { ...request, reason: this.kind() as 'ANNUAL_RAISE' | 'MARKET_CORRECTION' });

    call.subscribe({
      next: (updated) => this.dialogRef.close(updated),
      // The server owns the rules, so show what it said rather than guessing at the cause.
      error: (failure) => {
        this.error.set(failure?.error?.detail ?? 'Could not save this change.');
        this.saving.set(false);
      },
    });
  }

  private isAbove(level: JobLevel, current: JobLevel): boolean {
    return JOB_LEVELS.indexOf(level) > JOB_LEVELS.indexOf(current);
  }
}

/** The API speaks plain dates; a Date carries a timezone that would shift the day. */
function toIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
