import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  EmployeeDetail, EmployeeQuery, EmployeeSummary, ExitRequest, HireEmployeeRequest,
  PageResponse, PromoteRequest, RecordRevisionRequest, UpdateEmployeeRequest,
} from '../models/employee.model';

/**
 * Everything the UI does to employee records.
 *
 * <p>Relative URLs on purpose: the API is served from the same origin as the page, whether that is
 * the dev server proxying to Spring or the single container serving both.
 */
@Injectable({ providedIn: 'root' })
export class EmployeeApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/employees';

  list(query: EmployeeQuery): Observable<PageResponse<EmployeeSummary>> {
    return this.http.get<PageResponse<EmployeeSummary>>(this.baseUrl, {
      params: toParams(query),
    });
  }

  get(id: number): Observable<EmployeeDetail> {
    return this.http.get<EmployeeDetail>(`${this.baseUrl}/${id}`);
  }

  hire(request: HireEmployeeRequest): Observable<EmployeeDetail> {
    return this.http.post<EmployeeDetail>(this.baseUrl, request);
  }

  update(id: number, request: UpdateEmployeeRequest): Observable<EmployeeDetail> {
    return this.http.patch<EmployeeDetail>(`${this.baseUrl}/${id}`, request);
  }

  recordRevision(id: number, request: RecordRevisionRequest): Observable<EmployeeDetail> {
    return this.http.post<EmployeeDetail>(`${this.baseUrl}/${id}/revisions`, request);
  }

  /** Promotions have their own endpoint so the level and the pay always move together. */
  promote(id: number, request: PromoteRequest): Observable<EmployeeDetail> {
    return this.http.post<EmployeeDetail>(`${this.baseUrl}/${id}/promotion`, request);
  }

  markExit(id: number, request: ExitRequest): Observable<EmployeeDetail> {
    return this.http.post<EmployeeDetail>(`${this.baseUrl}/${id}/exit`, request);
  }
}

/**
 * Only sends the filters that were actually set. An empty search box should mean "no filter", not
 * "match the empty string", which the server would treat as a real, and useless, criterion.
 */
function toParams(query: EmployeeQuery): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== null && value !== undefined && value !== '') {
      params = params.set(key, String(value));
    }
  }
  return params;
}
