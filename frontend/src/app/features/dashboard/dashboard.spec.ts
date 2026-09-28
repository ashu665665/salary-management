import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Dashboard } from './dashboard';
import { AnalyticsApi } from '../../core/api/analytics-api';
import { GroupSummary, PayOutlier, PayrollOverview } from '../../core/models/analytics.model';

const anOverview: PayrollOverview = {
  asOf: '2026-09-28',
  baseCurrency: 'USD',
  headcount: 24,
  totalAnnualCost: 1445139.88,
  medianSalary: 45240.97,
  averageIncreasePercent: 5.54,
};

function aGroup(group: string, totalAnnualCost: number, overrides: Partial<GroupSummary> = {}): GroupSummary {
  return {
    group,
    headcount: 8,
    totalAnnualCost,
    medianSalary: 60000,
    lowerQuartile: 45000,
    upperQuartile: 90000,
    ...overrides,
  };
}

const anOutlier: PayOutlier = {
  employeeId: 7,
  employeeCode: 'ACME-00007',
  fullName: 'Priya Nair',
  country: 'INDIA',
  department: 'ENGINEERING',
  jobLevel: 'MID',
  salary: 14000,
  peerMedian: 28000,
  lowerQuartile: 22000,
  upperQuartile: 34000,
  position: 'BELOW_RANGE',
};

describe('Dashboard', () => {
  let api: {
    overview: ReturnType<typeof vi.fn>;
    breakdown: ReturnType<typeof vi.fn>;
    outliers: ReturnType<typeof vi.fn>;
    exchangeRates: ReturnType<typeof vi.fn>;
  };

  async function createComponent() {
    const fixture = TestBed.createComponent(Dashboard);
    await fixture.whenStable();
    return fixture;
  }

  const textOf = (fixture: { nativeElement: unknown }) =>
    (fixture.nativeElement as HTMLElement).textContent ?? '';

  beforeEach(() => {
    api = {
      overview: vi.fn().mockReturnValue(of(anOverview)),
      breakdown: vi.fn().mockReturnValue(of([aGroup('UNITED_STATES', 805500), aGroup('INDIA', 320301)])),
      outliers: vi.fn().mockReturnValue(of([anOutlier])),
      exchangeRates: vi.fn().mockReturnValue(of([{ currency: 'INR', unitsPerUsd: 83 }])),
    };

    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideRouter([]), { provide: AnalyticsApi, useValue: api }],
    });
  });

  describe('the headline figures', () => {
    it('asks for everything it needs when it opens', async () => {
      await createComponent();

      expect(api.overview).toHaveBeenCalled();
      expect(api.breakdown).toHaveBeenCalledWith('COUNTRY');
      expect(api.outliers).toHaveBeenCalled();
    });

    it('shows headcount, cost, median and the year on year change', async () => {
      const fixture = await createComponent();

      const text = textOf(fixture);
      expect(text).toContain('24');
      expect(text).toContain('1,445,140');
      expect(text).toContain('45,241');
      expect(text).toContain('5.5%');
    });

    it('says which currency the converted figures are in', async () => {
      const fixture = await createComponent();

      expect(textOf(fixture)).toContain('USD');
    });

    it('reports a failure rather than showing nothing', async () => {
      api.overview.mockReturnValue(throwError(() => new Error('boom')));

      const fixture = await createComponent();

      expect(textOf(fixture)).toContain('Could not load');
    });
  });

  describe('the breakdown', () => {
    it('draws one bar per group', async () => {
      const fixture = await createComponent();

      const bars = (fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid="bar"]');
      expect(bars.length).toBe(2);
    });

    it('sizes the bars against the largest group, not against each other', async () => {
      const fixture = await createComponent();

      const widths = fixture.componentInstance.barWidths();

      expect(widths[0]).toBe(100);
      expect(widths[1]).toBeCloseTo(39.8, 0);
    });

    it('draws nothing rather than dividing by zero when every group is empty', async () => {
      api.breakdown.mockReturnValue(of([aGroup('INDIA', 0)]));

      const fixture = await createComponent();

      expect(fixture.componentInstance.barWidths()).toEqual([0]);
    });

    it('reloads when a different dimension is chosen', async () => {
      const fixture = await createComponent();

      fixture.componentInstance.onDimensionChange('DEPARTMENT');
      await fixture.whenStable();

      expect(api.breakdown).toHaveBeenLastCalledWith('DEPARTMENT');
    });

    it('shows the spread as well as the middle, so two groups with the same median can be told apart', async () => {
      const fixture = await createComponent();

      const text = textOf(fixture);
      expect(text).toContain('45,000');
      expect(text).toContain('90,000');
    });
  });

  describe('pay outliers', () => {
    it('lists who sits outside their peer range, and against what', async () => {
      const fixture = await createComponent();

      const text = textOf(fixture);
      expect(text).toContain('Priya Nair');
      expect(text).toContain('Below range');
      expect(text).toContain('22,000');
    });

    it('says plainly when nobody is out of range, rather than showing an empty table', async () => {
      api.outliers.mockReturnValue(of([]));

      const fixture = await createComponent();

      expect(textOf(fixture)).toContain('Nobody is paid outside');
    });

    it('explains that small teams are not compared, so an empty list is not a surprise', async () => {
      api.outliers.mockReturnValue(of([]));

      const fixture = await createComponent();

      expect(textOf(fixture)).toContain('fewer than 5');
    });
  });

  describe('showing its working', () => {
    it('names the rates that converted the figures', async () => {
      const fixture = await createComponent();

      expect(textOf(fixture)).toContain('INR');
      expect(textOf(fixture)).toContain('83');
    });

    it('says which date the figures describe', async () => {
      const fixture = await createComponent();

      expect(textOf(fixture)).toContain('28 Sep 2026');
    });
  });
});
