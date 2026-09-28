import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { EmployeeApi } from '../../../core/api/employee-api';
import { EnumLabelPipe } from '../../../shared/enum-label-pipe';
import { MoneyPipe } from '../../../shared/money-pipe';
import {
  COUNTRIES, Country, DEPARTMENTS, Department, EmployeeQuery, EmployeeSummary,
  JOB_LEVELS, JobLevel,
} from '../../../core/models/employee.model';

/** How long to wait after the last keystroke before searching. */
const TYPING_PAUSE_MS = 350;

const DEFAULT_PAGE_SIZE = 25;
const DEFAULT_SORT = 'lastName,asc';

/**
 * The employee list: everyone, narrowed down.
 *
 * <p>Filtering, sorting and paging all happen on the server. With ten thousand employees the
 * browser should never hold more than the page being looked at, so every change here turns into a
 * new request rather than work over an array in memory.
 */
@Component({
  selector: 'app-employee-list',
  imports: [
    DatePipe, FormsModule, RouterLink, EnumLabelPipe, MoneyPipe,
    MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatPaginatorModule, MatProgressBarModule, MatSelectModule, MatSortModule, MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.scss',
})
export class EmployeeList implements OnDestroy {
  private readonly api = inject(EmployeeApi);

  protected readonly countries = COUNTRIES;
  protected readonly departments = DEPARTMENTS;
  protected readonly jobLevels = JOB_LEVELS;
  protected readonly columns = ['employeeCode', 'fullName', 'country', 'department', 'jobLevel', 'salary', 'status'];

  protected readonly employees = signal<EmployeeSummary[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly loading = signal(false);
  protected readonly failed = signal(false);

  protected readonly search = signal('');
  protected readonly country = signal<Country | null>(null);
  protected readonly department = signal<Department | null>(null);
  protected readonly jobLevel = signal<JobLevel | null>(null);
  protected readonly includeLeavers = signal(false);
  protected readonly page = signal(0);
  protected readonly size = signal(DEFAULT_PAGE_SIZE);
  protected readonly sort = signal(DEFAULT_SORT);

  protected readonly isEmpty = computed(() => !this.loading() && !this.failed() && this.employees().length === 0);

  private searchTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.load();
  }

  ngOnDestroy(): void {
    clearTimeout(this.searchTimer);
  }

  /**
   * Waits for a pause in typing. Without this every keystroke is a query against ten thousand
   * rows, and the answers can arrive out of order.
   */
  onSearchChange(search: string): void {
    this.search.set(search);
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.reloadFromFirstPage(), TYPING_PAUSE_MS);
  }

  onCountryChange(country: Country | null): void {
    this.country.set(country);
    this.reloadFromFirstPage();
  }

  onDepartmentChange(department: Department | null): void {
    this.department.set(department);
    this.reloadFromFirstPage();
  }

  onJobLevelChange(jobLevel: JobLevel | null): void {
    this.jobLevel.set(jobLevel);
    this.reloadFromFirstPage();
  }

  onIncludeLeaversChange(includeLeavers: boolean): void {
    this.includeLeavers.set(includeLeavers);
    this.reloadFromFirstPage();
  }

  onPageChange(event: { pageIndex: number; pageSize: number }): void {
    this.page.set(event.pageIndex);
    this.size.set(event.pageSize);
    this.load();
  }

  onSortChange(sort: { active: string; direction: string }): void {
    // Material clears the direction on the third click; keep a deterministic order rather than
    // letting the server fall back to whatever the database returns.
    this.sort.set(`${sort.active},${sort.direction || 'asc'}`);
    this.page.set(0);
    this.load();
  }

  /** A plain link, so the browser downloads the file rather than the app holding it in memory. */
  hireEmployee(): void {
    throw new Error('not implemented yet');
  }

  exportUrl(): string {
    const parameters = new URLSearchParams();
    const filters = this.currentFilters();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== null && value !== undefined && value !== '' && value !== false) {
        parameters.set(key, String(value));
      }
    }
    const query = parameters.toString();
    return query ? `/api/employees/export?${query}` : '/api/employees/export';
  }

  /**
   * A filter change can shrink the result set, and page four of a shorter list is empty, so any
   * change of what is being asked for starts again at the first page.
   */
  private reloadFromFirstPage(): void {
    this.page.set(0);
    this.load();
  }

  private currentFilters() {
    return {
      search: this.search() || null,
      country: this.country(),
      department: this.department(),
      jobLevel: this.jobLevel(),
      includeLeavers: this.includeLeavers(),
    };
  }

  private load(): void {
    const query: EmployeeQuery = {
      ...this.currentFilters(),
      page: this.page(),
      size: this.size(),
      sort: this.sort(),
    };

    this.loading.set(true);
    this.failed.set(false);
    this.api.list(query).subscribe({
      next: (result) => {
        this.employees.set(result.content);
        this.totalElements.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.employees.set([]);
        this.totalElements.set(0);
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }
}
