import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AnalyticsApi } from './analytics-api';
import { PayrollOverview } from '../models/analytics.model';

describe('AnalyticsApi', () => {
  let api: AnalyticsApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AnalyticsApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AnalyticsApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('reads the payroll overview', () => {
    let received: PayrollOverview | undefined;
    api.overview().subscribe((overview) => (received = overview));

    const request = http.expectOne('/api/analytics/overview');
    expect(request.request.method).toBe('GET');
    request.flush({
      asOf: '2025-06-01',
      baseCurrency: 'USD',
      headcount: 25,
      totalAnnualCost: 1500000,
      medianSalary: 55000,
      averageIncreasePercent: 6.8,
    });

    expect(received?.headcount).toBe(25);
    expect(received?.baseCurrency).toBe('USD');
  });

  it('asks for a breakdown by the chosen dimension', () => {
    api.breakdown('DEPARTMENT').subscribe();

    const request = http.expectOne((r) => r.url === '/api/analytics/breakdown');
    expect(request.request.params.get('groupBy')).toBe('DEPARTMENT');
    request.flush([]);
  });

  it('asks for outliers with a limit', () => {
    api.outliers(5).subscribe();

    const request = http.expectOne((r) => r.url === '/api/analytics/outliers');
    expect(request.request.params.get('limit')).toBe('5');
    request.flush([]);
  });

  it('reads the exchange rates the figures were converted at', () => {
    api.exchangeRates().subscribe();

    http.expectOne('/api/analytics/exchange-rates').flush([]);
  });
});
