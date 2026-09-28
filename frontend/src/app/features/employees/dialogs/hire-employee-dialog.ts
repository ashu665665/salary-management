import { Component, signal } from '@angular/core';

@Component({
  selector: 'app-hire-employee-dialog',
  imports: [],
  templateUrl: './hire-employee-dialog.html',
})
export class HireEmployeeDialog {
  readonly employeeCode = signal('');
  readonly firstName = signal('');
  readonly lastName = signal('');
  readonly email = signal('');
  readonly department = signal<string | null>(null);
  readonly jobLevel = signal<string | null>(null);
  readonly salaryAmount = signal<number | null>(null);
  readonly currency = signal('');

  onCountryChange(country: string): void {
    throw new Error('not implemented yet');
  }

  canSave(): boolean {
    throw new Error('not implemented yet');
  }

  save(): void {
    throw new Error('not implemented yet');
  }
}
