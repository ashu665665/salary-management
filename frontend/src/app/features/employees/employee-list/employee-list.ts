import { Component } from '@angular/core';

@Component({
  selector: 'app-employee-list',
  imports: [],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.scss',
})
export class EmployeeList {
  onSearchChange(search: string): void {
    throw new Error('not implemented yet');
  }

  onCountryChange(country: string | null): void {
    throw new Error('not implemented yet');
  }

  onDepartmentChange(department: string | null): void {
    throw new Error('not implemented yet');
  }

  onJobLevelChange(jobLevel: string | null): void {
    throw new Error('not implemented yet');
  }

  onIncludeLeaversChange(includeLeavers: boolean): void {
    throw new Error('not implemented yet');
  }

  onPageChange(event: { pageIndex: number; pageSize: number }): void {
    throw new Error('not implemented yet');
  }

  onSortChange(sort: { active: string; direction: string }): void {
    throw new Error('not implemented yet');
  }

  exportUrl(): string {
    throw new Error('not implemented yet');
  }
}
