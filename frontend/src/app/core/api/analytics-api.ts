import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ExchangeRateView, GroupBy, GroupSummary, PayOutlier, PayrollOverview } from '../models/analytics.model';

/** The dashboard's questions about how the organisation pays people. */
@Injectable({ providedIn: 'root' })
export class AnalyticsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/analytics';

  overview(): Observable<PayrollOverview> {
    return this.http.get<PayrollOverview>(`${this.baseUrl}/overview`);
  }

  breakdown(groupBy: GroupBy): Observable<GroupSummary[]> {
    return this.http.get<GroupSummary[]>(`${this.baseUrl}/breakdown`, {
      params: new HttpParams().set('groupBy', groupBy),
    });
  }

  outliers(limit: number): Observable<PayOutlier[]> {
    return this.http.get<PayOutlier[]>(`${this.baseUrl}/outliers`, {
      params: new HttpParams().set('limit', limit),
    });
  }

  /** Shown alongside converted totals so a figure can be traced back to the rates behind it. */
  exchangeRates(): Observable<ExchangeRateView[]> {
    return this.http.get<ExchangeRateView[]>(`${this.baseUrl}/exchange-rates`);
  }
}
