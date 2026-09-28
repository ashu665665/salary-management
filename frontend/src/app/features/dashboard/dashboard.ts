import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AnalyticsApi } from '../../core/api/analytics-api';
import { ExchangeRateView, GroupBy, GroupSummary, PayOutlier, PayrollOverview } from '../../core/models/analytics.model';
import { EnumLabelPipe } from '../../shared/enum-label-pipe';

/** Peer groups smaller than this are not reported on; the server applies the same rule. */
const SMALLEST_PEER_GROUP = 5;

/**
 * What the organisation costs, and who sits outside the range for their job.
 *
 * <p>The headline figures are numbers rather than charts: a single total does not need a plot to
 * be understood. The breakdown is a chart, because comparing the size of groups is exactly what a
 * bar is for, and the table underneath carries the spread that a bar length cannot show.
 */
@Component({
  selector: 'app-dashboard',
  imports: [
    DatePipe, DecimalPipe, MatButtonToggleModule, MatCardModule, MatIconModule,
    MatProgressBarModule, MatTableModule, MatTooltipModule, EnumLabelPipe,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly api = inject(AnalyticsApi);

  protected readonly smallestPeerGroup = SMALLEST_PEER_GROUP;
  protected readonly dimensions: GroupBy[] = ['COUNTRY', 'DEPARTMENT', 'JOB_LEVEL'];
  protected readonly breakdownColumns = ['group', 'headcount', 'totalAnnualCost', 'lowerQuartile', 'medianSalary', 'upperQuartile'];
  protected readonly outlierColumns = ['employee', 'peerGroup', 'salary', 'range', 'position'];

  protected readonly overview = signal<PayrollOverview | null>(null);
  protected readonly breakdown = signal<GroupSummary[]>([]);
  protected readonly outliers = signal<PayOutlier[]>([]);
  protected readonly rates = signal<ExchangeRateView[]>([]);
  protected readonly dimension = signal<GroupBy>('COUNTRY');
  protected readonly loading = signal(false);
  protected readonly failed = signal(false);

  /**
   * Each bar as a share of the largest group, so the longest bar fills the track and the rest are
   * read against it. Against the total instead, every bar would be a sliver.
   */
  barWidths(): number[] {
    const costs = this.breakdown().map((row) => row.totalAnnualCost);
    const largest = Math.max(0, ...costs);
    return costs.map((cost) => (largest === 0 ? 0 : (cost / largest) * 100));
  }

  protected readonly bars = computed(() =>
    this.breakdown().map((row, index) => ({ row, width: this.barWidths()[index] })),
  );

  constructor() {
    this.load();
  }

  onDimensionChange(groupBy: GroupBy): void {
    this.dimension.set(groupBy);
    this.loadBreakdown();
  }

  private load(): void {
    this.loading.set(true);
    this.failed.set(false);

    this.api.overview().subscribe({
      next: (overview) => {
        this.overview.set(overview);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      },
    });

    this.loadBreakdown();
    this.api.outliers(10).subscribe({ next: (outliers) => this.outliers.set(outliers) });
    this.api.exchangeRates().subscribe({ next: (rates) => this.rates.set(rates) });
  }

  private loadBreakdown(): void {
    this.api.breakdown(this.dimension()).subscribe({
      next: (rows) => this.breakdown.set(rows),
      error: () => this.breakdown.set([]),
    });
  }
}
