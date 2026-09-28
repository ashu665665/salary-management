import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { EmployeeDetailPage } from './employee-detail';
import { EmployeeApi } from '../../../core/api/employee-api';
import { EmployeeDetail, SalaryRevisionView } from '../../../core/models/employee.model';

function revision(overrides: Partial<SalaryRevisionView> = {}): SalaryRevisionView {
  return {
    id: 1,
    salary: { amount: 1200000, currency: 'INR' },
    effectiveDate: '2020-01-01',
    reason: 'HIRE',
    recordedAt: '2020-01-01T00:00:00Z',
    ...overrides,
  };
}

function anEmployee(overrides: Partial<EmployeeDetail> = {}): EmployeeDetail {
  return {
    id: 7,
    employeeCode: 'ACME-00007',
    firstName: 'Priya',
    lastName: 'Nair',
    fullName: 'Priya Nair',
    email: 'priya@acme.example',
    country: 'INDIA',
    department: 'ENGINEERING',
    jobLevel: 'MID',
    hireDate: '2020-01-01',
    exitDate: null,
    active: true,
    currentSalary: { amount: 1400000, currency: 'INR' },
    revisions: [
      revision(),
      revision({ id: 2, salary: { amount: 1400000, currency: 'INR' }, effectiveDate: '2023-04-01', reason: 'ANNUAL_RAISE' }),
    ],
    ...overrides,
  };
}

describe('EmployeeDetailPage', () => {
  let api: { get: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };

  async function createComponent() {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    await fixture.whenStable();
    return fixture;
  }

  const textOf = (fixture: { nativeElement: unknown }) =>
    (fixture.nativeElement as HTMLElement).textContent ?? '';

  beforeEach(() => {
    api = { get: vi.fn().mockReturnValue(of(anEmployee())) };
    dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(undefined) }) };

    TestBed.configureTestingModule({
      imports: [EmployeeDetailPage],
      providers: [
        provideRouter([]),
        { provide: EmployeeApi, useValue: api },
        { provide: MatDialog, useValue: dialog },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', '7']]) } } },
      ],
    });
  });

  describe('showing the employee', () => {
    it('loads the employee named in the address', async () => {
      await createComponent();

      expect(api.get).toHaveBeenCalledWith(7);
    });

    it('shows who they are and where they sit', async () => {
      const fixture = await createComponent();

      const text = textOf(fixture);
      expect(text).toContain('Priya Nair');
      expect(text).toContain('ACME-00007');
      expect(text).toContain('India');
      expect(text).toContain('Engineering');
      expect(text).toContain('Mid');
    });

    it('shows what they earn now', async () => {
      const fixture = await createComponent();

      expect(textOf(fixture)).toContain('INR 1,400,000');
    });

    it('says so when there is no such employee', async () => {
      api.get.mockReturnValue(throwError(() => ({ status: 404 })));

      const fixture = await createComponent();

      expect(textOf(fixture)).toContain('Could not load');
    });
  });

  describe('salary history', () => {
    it('lists every revision, newest first', async () => {
      const fixture = await createComponent();

      const dates = Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid="revision-date"]'),
      ).map((cell) => cell.textContent?.trim());

      expect(dates).toEqual(['1 Apr 2023', '1 Jan 2020']);
    });

    it('says why each change happened', async () => {
      const fixture = await createComponent();

      expect(textOf(fixture)).toContain('Annual Raise');
      expect(textOf(fixture)).toContain('Hire');
    });

    it('shows how much each change was worth', async () => {
      const fixture = await createComponent();

      // 1,200,000 to 1,400,000 is a 16.7% rise.
      expect(textOf(fixture)).toContain('16.7%');
    });

    it('shows no percentage against the first revision, which has nothing to compare to', async () => {
      const fixture = await createComponent();

      const changes = Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid="revision-change"]'),
      ).map((cell) => cell.textContent?.trim());

      expect(changes[changes.length - 1]).toBe('—');
    });
  });

  describe('changing pay', () => {
    it('opens the pay change form', async () => {
      const fixture = await createComponent();

      fixture.componentInstance.changePay();

      expect(dialog.open).toHaveBeenCalled();
    });

    it('shows the updated employee when a pay change is saved', async () => {
      const updated = anEmployee({ currentSalary: { amount: 1800000, currency: 'INR' } });
      dialog.open.mockReturnValue({ afterClosed: () => of(updated) });
      const fixture = await createComponent();

      fixture.componentInstance.changePay();
      await fixture.whenStable();

      expect(textOf(fixture)).toContain('INR 1,800,000');
    });

    it('leaves the screen alone when the form is cancelled', async () => {
      const fixture = await createComponent();

      fixture.componentInstance.changePay();
      await fixture.whenStable();

      expect(textOf(fixture)).toContain('INR 1,400,000');
    });
  });

  describe('someone who has left', () => {
    beforeEach(() => {
      api.get.mockReturnValue(of(anEmployee({ active: false, exitDate: '2024-12-31' })));
    });

    it('says when they left', async () => {
      const fixture = await createComponent();

      expect(textOf(fixture)).toContain('Left');
      expect(textOf(fixture)).toContain('31 Dec 2024');
    });

    it('does not offer to change the pay of someone who has gone', async () => {
      const fixture = await createComponent();

      expect(fixture.componentInstance.canChangePay()).toBe(false);
    });

    it('keeps their history on screen', async () => {
      const fixture = await createComponent();

      expect(textOf(fixture)).toContain('Annual Raise');
    });
  });

  describe('other changes', () => {
    it('opens the leaver form', async () => {
      const fixture = await createComponent();

      fixture.componentInstance.recordExit();

      expect(dialog.open).toHaveBeenCalled();
    });

    it('opens the details form', async () => {
      const fixture = await createComponent();

      fixture.componentInstance.editDetails();

      expect(dialog.open).toHaveBeenCalled();
    });
  });
});
