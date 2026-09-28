import { Component, inject, signal } from '@angular/core';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { provideNativeDateAdapter } from '@angular/material/core';

import { EmployeeApi } from '../../../core/api/employee-api';
import {
  COUNTRIES, Country, DEPARTMENTS, Department, JOB_LEVELS, JobLevel, PAY_CURRENCY,
} from '../../../core/models/employee.model';
import { EnumLabelPipe } from '../../../shared/enum-label-pipe';

/**
 * Adds someone to the organisation, with the salary they start on.
 *
 * <p>Pay is part of hiring rather than a later step: an employee with no salary on record would be
 * a hole in every figure the dashboard reports, so the server will not create one.
 */
@Component({
  selector: 'app-hire-employee-dialog',
  providers: [provideNativeDateAdapter()],
  imports: [
    MatButtonModule, MatDatepickerModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, EnumLabelPipe,
  ],
  templateUrl: './hire-employee-dialog.html',
})
export class HireEmployeeDialog {
  private readonly api = inject(EmployeeApi);
  private readonly dialogRef = inject(MatDialogRef<HireEmployeeDialog>);

  protected readonly countries = COUNTRIES;
  protected readonly departments = DEPARTMENTS;
  protected readonly jobLevels = JOB_LEVELS;

  readonly employeeCode = signal('');
  readonly firstName = signal('');
  readonly lastName = signal('');
  readonly email = signal('');
  readonly country = signal<Country | null>(null);
  readonly department = signal<Department | null>(null);
  readonly jobLevel = signal<JobLevel | null>(null);
  readonly hireDate = signal<Date>(new Date());
  readonly salaryAmount = signal<number | null>(null);
  readonly currency = signal('');

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Nobody should have to remember that Poland pays in zloty. */
  onCountryChange(country: Country): void {
    this.country.set(country);
    this.currency.set(PAY_CURRENCY[country]);
  }

  canSave(): boolean {
    const salary = this.salaryAmount();
    return (
      !this.saving() &&
      this.employeeCode().trim().length > 0 &&
      this.firstName().trim().length > 0 &&
      this.lastName().trim().length > 0 &&
      isEmail(this.email()) &&
      this.country() !== null &&
      this.department() !== null &&
      this.jobLevel() !== null &&
      salary !== null &&
      salary > 0
    );
  }

  save(): void {
    if (!this.canSave()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);

    this.api
      .hire({
        employeeCode: this.employeeCode().trim(),
        firstName: this.firstName().trim(),
        lastName: this.lastName().trim(),
        email: this.email().trim(),
        country: this.country()!,
        department: this.department()!,
        jobLevel: this.jobLevel()!,
        hireDate: toIsoDate(this.hireDate()),
        salaryAmount: this.salaryAmount()!,
        currency: this.currency(),
      })
      .subscribe({
        next: (created) => this.dialogRef.close(created),
        error: (failure) => {
          // A duplicate code or email is the usual reason, and the server says which.
          this.error.set(failure?.error?.detail ?? 'Could not add this employee.');
          this.saving.set(false);
        },
      });
  }
}

function isEmail(value: string): boolean {
  const trimmed = value.trim();
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed);
}

/** The API speaks plain dates; a Date carries a timezone that would shift the day. */
function toIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
