import { Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { EmployeeApi } from '../../../core/api/employee-api';
import { DEPARTMENTS, Department, EmployeeDetail } from '../../../core/models/employee.model';
import { EnumLabelPipe } from '../../../shared/enum-label-pipe';

/**
 * The details that can be corrected without it being a pay decision.
 *
 * <p>Country and level are not here on purpose: level moves through a promotion, and a country
 * change means a different pay currency, which is a bigger decision than a typo fix.
 */
@Component({
  selector: 'app-edit-employee-dialog',
  imports: [
    MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, EnumLabelPipe,
  ],
  templateUrl: './edit-employee-dialog.html',
})
export class EditEmployeeDialog {
  private readonly api = inject(EmployeeApi);
  private readonly dialogRef = inject(MatDialogRef<EditEmployeeDialog>);
  protected readonly employee = inject<EmployeeDetail>(MAT_DIALOG_DATA);

  protected readonly departments = DEPARTMENTS;
  protected readonly firstName = signal(this.employee.firstName);
  protected readonly lastName = signal(this.employee.lastName);
  protected readonly email = signal(this.employee.email);
  protected readonly department = signal<Department>(this.employee.department);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected canSave(): boolean {
    return (
      !this.saving() &&
      this.firstName().trim().length > 0 &&
      this.lastName().trim().length > 0 &&
      this.email().includes('@')
    );
  }

  protected save(): void {
    if (!this.canSave()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);

    this.api
      .update(this.employee.id, {
        firstName: this.firstName().trim(),
        lastName: this.lastName().trim(),
        email: this.email().trim(),
        department: this.department(),
      })
      .subscribe({
        next: (updated) => this.dialogRef.close(updated),
        error: (failure) => {
          this.error.set(failure?.error?.detail ?? 'Could not save these details.');
          this.saving.set(false);
        },
      });
  }
}
