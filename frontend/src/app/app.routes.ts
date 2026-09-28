import { Routes } from '@angular/router';

/**
 * Screens are loaded on demand, so opening the employee list does not also download the dashboard.
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    title: 'Dashboard',
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'employees',
    title: 'Employees',
    loadComponent: () =>
      import('./features/employees/employee-list/employee-list').then((m) => m.EmployeeList),
  },
  {
    path: 'employees/:id',
    title: 'Employee',
    loadComponent: () =>
      import('./features/employees/employee-detail/employee-detail').then((m) => m.EmployeeDetailPage),
  },
  { path: '**', redirectTo: 'dashboard' },
];
