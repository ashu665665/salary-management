import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  EmployeeDetail, EmployeeQuery, EmployeeSummary, ExitRequest, HireEmployeeRequest,
  PageResponse, PromoteRequest, RecordRevisionRequest, UpdateEmployeeRequest,
} from '../models/employee.model';

@Injectable({ providedIn: 'root' })
export class EmployeeApi {
  list(query: EmployeeQuery): Observable<PageResponse<EmployeeSummary>> {
    throw new Error('not implemented yet');
  }

  get(id: number): Observable<EmployeeDetail> {
    throw new Error('not implemented yet');
  }

  hire(request: HireEmployeeRequest): Observable<EmployeeDetail> {
    throw new Error('not implemented yet');
  }

  update(id: number, request: UpdateEmployeeRequest): Observable<EmployeeDetail> {
    throw new Error('not implemented yet');
  }

  recordRevision(id: number, request: RecordRevisionRequest): Observable<EmployeeDetail> {
    throw new Error('not implemented yet');
  }

  promote(id: number, request: PromoteRequest): Observable<EmployeeDetail> {
    throw new Error('not implemented yet');
  }

  markExit(id: number, request: ExitRequest): Observable<EmployeeDetail> {
    throw new Error('not implemented yet');
  }
}
