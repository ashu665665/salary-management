import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { EmployeeList } from './employee-list';
import { EmployeeApi } from '../../../core/api/employee-api';
import { EmployeeSummary, PageResponse } from '../../../core/models/employee.model';

function anEmployee(overrides: Partial<EmployeeSummary> = {}): EmployeeSummary {
  return {
    id: 7,
    employeeCode: 'ACME-00007',
    fullName: 'Priya Nair',
    email: 'priya@acme.example',
    country: 'INDIA',
    department: 'ENGINEERING',
    jobLevel: 'MID',
    hireDate: '2020-01-01',
    exitDate: null,
    active: true,
    currentSalary: { amount: 1600000, currency: 'INR' },
    salaryEffectiveFrom: '2024-04-01',
    ...overrides,
  };
}

function aPage(content: EmployeeSummary[], totalElements = content.length): PageResponse<EmployeeSummary> {
  return { content, page: 0, size: 25, totalElements, totalPages: Math.ceil(totalElements / 25) };
}

describe('EmployeeList', () => {
  let api: { list: ReturnType<typeof vi.fn>; exportUrl?: unknown };

  async function createComponent() {
    const fixture = TestBed.createComponent(EmployeeList);
    await fixture.whenStable();
    return fixture;
  }

  beforeEach(() => {
    api = { list: vi.fn().mockReturnValue(of(aPage([anEmployee()]))) };

    TestBed.configureTestingModule({
      imports: [EmployeeList],
      providers: [provideRouter([]), { provide: EmployeeApi, useValue: api }],
    });
  });

  describe('loading the list', () => {
    it('asks for the first page as soon as it opens', async () => {
      await createComponent();

      expect(api.list).toHaveBeenCalledTimes(1);
      expect(api.list.mock.calls[0][0]).toMatchObject({ page: 0, size: 25 });
    });

    it('shows a row for each employee', async () => {
      const fixture = await createComponent();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('Priya Nair');
      expect(text).toContain('ACME-00007');
    });

    it('shows the salary with its own currency, not a converted one', async () => {
      const fixture = await createComponent();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('INR');
      expect(text).toMatch(/16,00,000|1,600,000/);
    });

    it('says so when nobody matches, instead of showing an empty table', async () => {
      api.list.mockReturnValue(of(aPage([])));

      const fixture = await createComponent();

      expect((fixture.nativeElement as HTMLElement).textContent).toContain('No employees match');
    });

    it('reports a failure instead of showing a blank screen', async () => {
      api.list.mockReturnValue(throwError(() => new Error('boom')));

      const fixture = await createComponent();

      expect((fixture.nativeElement as HTMLElement).textContent).toContain('Could not load');
    });
  });

  describe('filtering', () => {
    it('reloads when a country is chosen', async () => {
      const fixture = await createComponent();

      fixture.componentInstance.onCountryChange('INDIA');
      await fixture.whenStable();

      expect(api.list).toHaveBeenLastCalledWith(expect.objectContaining({ country: 'INDIA' }));
    });

    it('goes back to the first page when the filter changes', async () => {
      const fixture = await createComponent();
      fixture.componentInstance.onPageChange({ pageIndex: 3, pageSize: 25 });
      await fixture.whenStable();

      fixture.componentInstance.onDepartmentChange('SALES');
      await fixture.whenStable();

      // Staying on page 4 of a different, smaller result set would show an empty table.
      expect(api.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0, department: 'SALES' }));
    });

    it('waits for typing to stop before searching', async () => {
      // Created with real timers first: Angular's whenStable() needs them to settle.
      const fixture = await createComponent();
      api.list.mockClear();

      vi.useFakeTimers();
      try {
        fixture.componentInstance.onSearchChange('na');
        fixture.componentInstance.onSearchChange('nai');
        fixture.componentInstance.onSearchChange('nair');
        expect(api.list).not.toHaveBeenCalled();

        vi.advanceTimersByTime(400);
        expect(api.list).toHaveBeenCalledTimes(1);
        expect(api.list).toHaveBeenLastCalledWith(expect.objectContaining({ search: 'nair' }));
      } finally {
        vi.useRealTimers();
      }
    });

    it('can include people who have left', async () => {
      const fixture = await createComponent();

      fixture.componentInstance.onIncludeLeaversChange(true);
      await fixture.whenStable();

      expect(api.list).toHaveBeenLastCalledWith(expect.objectContaining({ includeLeavers: true }));
    });
  });

  describe('paging and sorting', () => {
    it('asks the server for the next page rather than slicing in the browser', async () => {
      const fixture = await createComponent();

      fixture.componentInstance.onPageChange({ pageIndex: 2, pageSize: 50 });
      await fixture.whenStable();

      expect(api.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2, size: 50 }));
    });

    it('asks the server to sort', async () => {
      const fixture = await createComponent();

      fixture.componentInstance.onSortChange({ active: 'lastName', direction: 'desc' });
      await fixture.whenStable();

      expect(api.list).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'lastName,desc' }));
    });

    it('falls back to a stable order when sorting is cleared', async () => {
      const fixture = await createComponent();

      fixture.componentInstance.onSortChange({ active: 'lastName', direction: '' });
      await fixture.whenStable();

      expect(api.list).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'lastName,asc' }));
    });
  });

  describe('exporting', () => {
    it('builds a download link carrying the same filters as the screen', async () => {
      const fixture = await createComponent();
      fixture.componentInstance.onCountryChange('INDIA');
      fixture.componentInstance.onIncludeLeaversChange(true);
      await fixture.whenStable();

      const url = fixture.componentInstance.exportUrl();

      expect(url).toContain('/api/employees/export');
      expect(url).toContain('country=INDIA');
      expect(url).toContain('includeLeavers=true');
    });

    it('does not put paging in the export link, because an export is the whole list', async () => {
      const fixture = await createComponent();
      fixture.componentInstance.onPageChange({ pageIndex: 2, pageSize: 50 });
      await fixture.whenStable();

      expect(fixture.componentInstance.exportUrl()).not.toContain('page=');
    });
  });
});
