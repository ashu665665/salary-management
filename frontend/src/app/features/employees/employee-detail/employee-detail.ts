import { Component } from '@angular/core';

@Component({
  selector: 'app-employee-detail',
  imports: [],
  templateUrl: './employee-detail.html',
  styleUrl: './employee-detail.scss',
})
export class EmployeeDetailPage {
  canChangePay(): boolean {
    throw new Error('not implemented yet');
  }

  changePay(): void {
    throw new Error('not implemented yet');
  }

  recordExit(): void {
    throw new Error('not implemented yet');
  }

  editDetails(): void {
    throw new Error('not implemented yet');
  }
}
