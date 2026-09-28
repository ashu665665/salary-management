import { Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { provideNativeDateAdapter } from '@angular/material/core';

import { EmployeeApi } from '../../../core/api/employee-api';
import { EmployeeDetail } from '../../../core/models/employee.model';

/** Records a leaver. Their history stays; they stop counting towards current payroll. */
@Component({
  selector: 'app-exit-dialog',
  providers: [provideNativeDateAdapter()],
  imports: [MatButtonModule, MatDatepickerModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './exit-dialog.html',
})
export class ExitDialog {
  private readonly api = inject(EmployeeApi);
  private readonly dialogRef = inject(MatDialogRef<ExitDialog>);
  protected readonly employee = inject<EmployeeDetail>(MAT_DIALOG_DATA);

  protected readonly lastWorkingDay = signal<Date>(new Date());
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected save(): void {
    this.saving.set(true);
    this.error.set(null);

    this.api
      .markExit(this.employee.id, { lastWorkingDay: toIsoDate(this.lastWorkingDay()) })
      .subscribe({
        next: (updated) => this.dialogRef.close(updated),
        error: (failure) => {
          this.error.set(failure?.error?.detail ?? 'Could not record this exit.');
          this.saving.set(false);
        },
      });
  }
}

function toIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
