import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { EmployeeApi } from './employee-api';
import { EmployeeDetail, PageResponse, EmployeeSummary } from '../models/employee.model';

describe('EmployeeApi', () => {
  let api: EmployeeApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [EmployeeApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(EmployeeApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const aPage: PageResponse<EmployeeSummary> = {
    content: [],
    page: 0,
    size: 25,
    totalElements: 0,
    totalPages: 0,
  };

  describe('listing employees', () => {
    it('asks for the first page when nothing is specified', () => {
      api.list({}).subscribe();

      const request = http.expectOne((r) => r.url === '/api/employees');
      expect(request.request.method).toBe('GET');
      request.flush(aPage);
    });

    it('passes every filter that was set', () => {
      api
        .list({
          search: 'nair',
          country: 'INDIA',
          department: 'ENGINEERING',
          jobLevel: 'MID',
          includeLeavers: true,
          page: 2,
          size: 50,
          sort: 'lastName,desc',
        })
        .subscribe();

      const request = http.expectOne((r) => r.url === '/api/employees');
      const parameters = request.request.params;
      expect(parameters.get('search')).toBe('nair');
      expect(parameters.get('country')).toBe('INDIA');
      expect(parameters.get('department')).toBe('ENGINEERING');
      expect(parameters.get('jobLevel')).toBe('MID');
      expect(parameters.get('includeLeavers')).toBe('true');
      expect(parameters.get('page')).toBe('2');
      expect(parameters.get('size')).toBe('50');
      expect(parameters.get('sort')).toBe('lastName,desc');
      request.flush(aPage);
    });

    it('leaves out filters that were not set, rather than sending empty ones', () => {
      api.list({ search: '', country: null, department: undefined }).subscribe();

      const request = http.expectOne((r) => r.url === '/api/employees');
      expect(request.request.params.has('search')).toBe(false);
      expect(request.request.params.has('country')).toBe(false);
      expect(request.request.params.has('department')).toBe(false);
      request.flush(aPage);
    });

    it('returns the page the server sent', () => {
      let received: PageResponse<EmployeeSummary> | undefined;
      api.list({}).subscribe((page) => (received = page));

      http.expectOne((r) => r.url === '/api/employees').flush({ ...aPage, totalElements: 25 });

      expect(received?.totalElements).toBe(25);
    });
  });

  describe('one employee', () => {
    const detail = { id: 7, employeeCode: 'ACME-00007' } as EmployeeDetail;

    it('reads an employee by id', () => {
      let received: EmployeeDetail | undefined;
      api.get(7).subscribe((employee) => (received = employee));

      const request = http.expectOne('/api/employees/7');
      expect(request.request.method).toBe('GET');
      request.flush(detail);

      expect(received?.employeeCode).toBe('ACME-00007');
    });

    it('hires a new employee', () => {
      api
        .hire({
          employeeCode: 'ACME-00099',
          firstName: 'Dana',
          lastName: 'Brooks',
          email: 'dana@acme.example',
          country: 'UNITED_STATES',
          department: 'SALES',
          jobLevel: 'MANAGER',
          hireDate: '2025-01-06',
          salaryAmount: 150000,
          currency: 'USD',
        })
        .subscribe();

      const request = http.expectOne('/api/employees');
      expect(request.request.method).toBe('POST');
      expect(request.request.body.employeeCode).toBe('ACME-00099');
      request.flush(detail);
    });

    it('updates the details that can change', () => {
      api
        .update(7, {
          firstName: 'Priya',
          lastName: 'Nair-Kumar',
          email: 'priya.kumar@acme.example',
          department: 'PRODUCT',
        })
        .subscribe();

      const request = http.expectOne('/api/employees/7');
      expect(request.request.method).toBe('PATCH');
      request.flush(detail);
    });
  });

  describe('changing pay', () => {
    const detail = { id: 7 } as EmployeeDetail;

    it('records a raise', () => {
      api
        .recordRevision(7, {
          amount: 1400000,
          currency: 'INR',
          effectiveDate: '2025-04-01',
          reason: 'ANNUAL_RAISE',
        })
        .subscribe();

      const request = http.expectOne('/api/employees/7/revisions');
      expect(request.request.method).toBe('POST');
      expect(request.request.body.reason).toBe('ANNUAL_RAISE');
      request.flush(detail);
    });

    it('promotes someone through its own endpoint, so the level moves with the pay', () => {
      api
        .promote(7, {
          newLevel: 'SENIOR',
          amount: 1800000,
          currency: 'INR',
          effectiveDate: '2025-04-01',
        })
        .subscribe();

      const request = http.expectOne('/api/employees/7/promotion');
      expect(request.request.method).toBe('POST');
      expect(request.request.body.newLevel).toBe('SENIOR');
      request.flush(detail);
    });

    it('records a leaver', () => {
      api.markExit(7, { lastWorkingDay: '2025-05-31' }).subscribe();

      const request = http.expectOne('/api/employees/7/exit');
      expect(request.request.method).toBe('POST');
      expect(request.request.body.lastWorkingDay).toBe('2025-05-31');
      request.flush(detail);
    });
  });
});
