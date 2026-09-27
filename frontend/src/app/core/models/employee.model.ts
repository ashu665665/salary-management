/**
 * The shapes the API returns, mirroring the backend DTOs.
 *
 * The enums are string unions rather than TypeScript enums: the values arrive as strings over
 * JSON, and a union gives the same checking without a runtime construct that has to be kept in
 * step with the backend.
 */

export const COUNTRIES = [
  'INDIA', 'UNITED_STATES', 'UNITED_KINGDOM', 'GERMANY', 'POLAND',
  'SINGAPORE', 'AUSTRALIA', 'CANADA', 'BRAZIL', 'JAPAN',
] as const;
export type Country = (typeof COUNTRIES)[number];

export const DEPARTMENTS = [
  'ENGINEERING', 'PRODUCT', 'SALES', 'MARKETING', 'CUSTOMER_SUPPORT',
  'FINANCE', 'HR', 'LEGAL', 'OPERATIONS',
] as const;
export type Department = (typeof DEPARTMENTS)[number];

/** Ordered from most junior, so the UI can show them in a sensible order. */
export const JOB_LEVELS = [
  'JUNIOR', 'MID', 'SENIOR', 'LEAD', 'MANAGER', 'DIRECTOR', 'EXECUTIVE',
] as const;
export type JobLevel = (typeof JOB_LEVELS)[number];

export type RevisionReason = 'HIRE' | 'ANNUAL_RAISE' | 'PROMOTION' | 'MARKET_CORRECTION';

export interface MoneyView {
  amount: number;
  currency: string;
}

export interface EmployeeSummary {
  id: number;
  employeeCode: string;
  fullName: string;
  email: string;
  country: Country;
  department: Department;
  jobLevel: JobLevel;
  hireDate: string;
  exitDate: string | null;
  active: boolean;
  currentSalary: MoneyView | null;
  salaryEffectiveFrom: string | null;
}

export interface SalaryRevisionView {
  id: number;
  salary: MoneyView;
  effectiveDate: string;
  reason: RevisionReason;
  recordedAt: string;
}

export interface EmployeeDetail {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  country: Country;
  department: Department;
  jobLevel: JobLevel;
  hireDate: string;
  exitDate: string | null;
  active: boolean;
  currentSalary: MoneyView | null;
  revisions: SalaryRevisionView[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** What the employee list is currently filtered by. Everything is optional. */
export interface EmployeeQuery {
  search?: string | null;
  country?: Country | null;
  department?: Department | null;
  jobLevel?: JobLevel | null;
  includeLeavers?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface HireEmployeeRequest {
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  country: Country;
  department: Department;
  jobLevel: JobLevel;
  hireDate: string;
  salaryAmount: number;
  currency: string;
}

export interface UpdateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  department: Department;
}

export interface RecordRevisionRequest {
  amount: number;
  currency: string;
  effectiveDate: string;
  reason: Extract<RevisionReason, 'ANNUAL_RAISE' | 'MARKET_CORRECTION'>;
}

export interface PromoteRequest {
  newLevel: JobLevel;
  amount: number;
  currency: string;
  effectiveDate: string;
}

export interface ExitRequest {
  lastWorkingDay: string;
}
