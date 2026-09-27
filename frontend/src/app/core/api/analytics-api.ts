import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ExchangeRateView, GroupBy, GroupSummary, PayOutlier, PayrollOverview } from '../models/analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsApi {
  overview(): Observable<PayrollOverview> {
    throw new Error('not implemented yet');
  }

  breakdown(groupBy: GroupBy): Observable<GroupSummary[]> {
    throw new Error('not implemented yet');
  }

  outliers(limit: number): Observable<PayOutlier[]> {
    throw new Error('not implemented yet');
  }

  exchangeRates(): Observable<ExchangeRateView[]> {
    throw new Error('not implemented yet');
  }
}
