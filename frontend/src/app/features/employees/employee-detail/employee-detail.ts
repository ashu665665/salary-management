import { Component, computed, inject, signal } from '@angular/core';
import { ComponentType } from '@angular/cdk/portal';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';

import { EmployeeApi } from '../../../core/api/employee-api';
import { EmployeeDetail, SalaryRevisionView } from '../../../core/models/employee.model';
import { EnumLabelPipe } from '../../../shared/enum-label-pipe';
import { MoneyPipe } from '../../../shared/money-pipe';
import { EditEmployeeDialog } from '../dialogs/edit-employee-dialog';
import { ExitDialog } from '../dialogs/exit-dialog';
import { SalaryChangeDialog } from '../dialogs/salary-change-dialog';

/** A revision with what it was worth compared to the one before it. */
interface RevisionRow {
  revision: SalaryRevisionView;
  changePercent: number | null;
}

/**
 * One employee, and the whole story of what they have been paid.
 *
 * <p>The history is the point of this screen: a current salary on its own says what someone earns,
 * the history says how they got there.
 */
@Component({
  selector: 'app-employee-detail',
  imports: [
    DatePipe, DecimalPipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule,
    MatProgressBarModule, MatTableModule, EnumLabelPipe, MoneyPipe,
  ],
  templateUrl: './employee-detail.html',
  styleUrl: './employee-detail.scss',
})
export class EmployeeDetailPage {
  private readonly api = inject(EmployeeApi);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);

  private readonly employeeId = Number(this.route.snapshot.paramMap.get('id'));

  protected readonly employee = signal<EmployeeDetail | null>(null);
  protected readonly loading = signal(false);
  protected readonly failed = signal(false);
  protected readonly columns = ['effectiveDate', 'salary', 'change', 'reason'];

  /** Newest first: the most recent change is the one being looked for. */
  protected readonly history = computed<RevisionRow[]>(() => {
    const revisions = [...(this.employee()?.revisions ?? [])].sort(
      (a, b) => a.effectiveDate.localeCompare(b.effectiveDate),
    );

    return revisions
      .map((revision, index) => ({
        revision,
        changePercent: percentChange(revisions[index - 1], revision),
      }))
      .reverse();
  });

  constructor() {
    this.load();
  }

  /** Pay cannot change after someone has left, so the action is not offered. */
  canChangePay(): boolean {
    const employee = this.employee();
    return employee !== null && employee.active;
  }

  changePay(): void {
    this.openAndRefresh(SalaryChangeDialog);
  }

  recordExit(): void {
    this.openAndRefresh(ExitDialog);
  }

  editDetails(): void {
    this.openAndRefresh(EditEmployeeDialog);
  }

  /**
   * Every dialog returns the updated employee, so the screen refreshes from the answer the server
   * gave rather than re-reading it or patching the local copy by hand.
   */
  private openAndRefresh<T>(dialog: ComponentType<T>): void {
    const employee = this.employee();
    if (!employee) {
      return;
    }
    this.dialog
      .open<T, EmployeeDetail, EmployeeDetail>(dialog, { data: employee, width: '32rem' })
      .afterClosed()
      .subscribe((updated?: EmployeeDetail) => {
        if (updated) {
          this.employee.set(updated);
        }
      });
  }

  private load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.api.get(this.employeeId).subscribe({
      next: (employee) => {
        this.employee.set(employee);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }
}

/**
 * How much a revision raised the pay. Empty for the first one, which has nothing before it, and
 * for a change of currency, where a percentage would be meaningless.
 */
function percentChange(previous: SalaryRevisionView | undefined, current: SalaryRevisionView): number | null {
  if (!previous || previous.salary.currency !== current.salary.currency || previous.salary.amount === 0) {
    return null;
  }
  return ((current.salary.amount - previous.salary.amount) / previous.salary.amount) * 100;
}
