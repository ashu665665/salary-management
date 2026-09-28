import { TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { HireEmployeeDialog } from './hire-employee-dialog';
import { EmployeeApi } from '../../../core/api/employee-api';
import { EmployeeDetail } from '../../../core/models/employee.model';

describe('HireEmployeeDialog', () => {
  let api: { hire: ReturnType<typeof vi.fn> };
  let dialogRef: { close: ReturnType<typeof vi.fn> };

  async function createComponent() {
    const fixture = TestBed.createComponent(HireEmployeeDialog);
    await fixture.whenStable();
    return fixture;
  }

  /** Fills in everything the server requires, so a test can leave out just one thing. */
  function completeForm(form: HireEmployeeDialog) {
    form.employeeCode.set('ACME-00099');
    form.firstName.set('Dana');
    form.lastName.set('Brooks');
    form.email.set('dana.brooks@acme.example');
    form.onCountryChange('UNITED_STATES');
    form.department.set('SALES');
    form.jobLevel.set('MANAGER');
    form.salaryAmount.set(150000);
  }

  beforeEach(() => {
    api = { hire: vi.fn().mockReturnValue(of({ id: 42 } as EmployeeDetail)) };
    dialogRef = { close: vi.fn() };

    TestBed.configureTestingModule({
      imports: [HireEmployeeDialog],
      providers: [
        { provide: EmployeeApi, useValue: api },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    });
  });

  describe('what the form will accept', () => {
    it('will not save an empty form', async () => {
      const fixture = await createComponent();

      expect(fixture.componentInstance.canSave()).toBe(false);
    });

    it('will save once everything required is filled in', async () => {
      const fixture = await createComponent();

      completeForm(fixture.componentInstance);

      expect(fixture.componentInstance.canSave()).toBe(true);
    });

    it('will not save without an employee code', async () => {
      const fixture = await createComponent();
      completeForm(fixture.componentInstance);

      fixture.componentInstance.employeeCode.set('   ');

      expect(fixture.componentInstance.canSave()).toBe(false);
    });

    it('will not save an address that is not an email', async () => {
      const fixture = await createComponent();
      completeForm(fixture.componentInstance);

      fixture.componentInstance.email.set('dana-at-acme');

      expect(fixture.componentInstance.canSave()).toBe(false);
    });

    it('will not save a salary of nothing', async () => {
      const fixture = await createComponent();
      completeForm(fixture.componentInstance);

      fixture.componentInstance.salaryAmount.set(0);

      expect(fixture.componentInstance.canSave()).toBe(false);
    });
  });

  describe('the currency', () => {
    it('follows the country, because that is what the person will be paid in', async () => {
      const fixture = await createComponent();

      fixture.componentInstance.onCountryChange('INDIA');
      expect(fixture.componentInstance.currency()).toBe('INR');

      fixture.componentInstance.onCountryChange('JAPAN');
      expect(fixture.componentInstance.currency()).toBe('JPY');
    });
  });

  describe('saving', () => {
    it('sends what was typed', async () => {
      const fixture = await createComponent();
      completeForm(fixture.componentInstance);

      fixture.componentInstance.save();

      expect(api.hire).toHaveBeenCalledWith(
        expect.objectContaining({
          employeeCode: 'ACME-00099',
          firstName: 'Dana',
          lastName: 'Brooks',
          email: 'dana.brooks@acme.example',
          country: 'UNITED_STATES',
          department: 'SALES',
          jobLevel: 'MANAGER',
          salaryAmount: 150000,
          currency: 'USD',
        }),
      );
    });

    it('trims stray spaces rather than saving them', async () => {
      const fixture = await createComponent();
      completeForm(fixture.componentInstance);
      fixture.componentInstance.firstName.set('  Dana  ');

      fixture.componentInstance.save();

      expect(api.hire).toHaveBeenCalledWith(expect.objectContaining({ firstName: 'Dana' }));
    });

    it('hands the new employee back to whoever opened the form', async () => {
      const fixture = await createComponent();
      completeForm(fixture.componentInstance);

      fixture.componentInstance.save();

      expect(dialogRef.close).toHaveBeenCalledWith({ id: 42 });
    });

    it('shows what the server said when it refuses, rather than a generic failure', async () => {
      api.hire.mockReturnValue(
        throwError(() => ({ error: { detail: 'That employee code or email address is already in use' } })),
      );
      const fixture = await createComponent();
      completeForm(fixture.componentInstance);

      fixture.componentInstance.save();
      await fixture.whenStable();

      expect((fixture.nativeElement as HTMLElement).textContent).toContain('already in use');
      expect(dialogRef.close).not.toHaveBeenCalled();
    });

    it('lets you try again after a failure', async () => {
      api.hire.mockReturnValue(throwError(() => ({ error: { detail: 'nope' } })));
      const fixture = await createComponent();
      completeForm(fixture.componentInstance);

      fixture.componentInstance.save();
      await fixture.whenStable();

      expect(fixture.componentInstance.canSave()).toBe(true);
    });
  });
});
