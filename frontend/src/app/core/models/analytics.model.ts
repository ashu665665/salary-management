export type GroupBy = 'COUNTRY' | 'DEPARTMENT' | 'JOB_LEVEL';

export interface PayrollOverview {
  asOf: string;
  baseCurrency: string;
  headcount: number;
  totalAnnualCost: number;
  medianSalary: number;
  averageIncreasePercent: number;
}

export interface GroupSummary {
  group: string;
  headcount: number;
  totalAnnualCost: number;
  medianSalary: number;
  lowerQuartile: number;
  upperQuartile: number;
}

export interface PayOutlier {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  country: string;
  department: string;
  jobLevel: string;
  salary: number;
  peerMedian: number;
  lowerQuartile: number;
  upperQuartile: number;
  position: 'BELOW_RANGE' | 'ABOVE_RANGE';
}

export interface ExchangeRateView {
  currency: string;
  unitsPerUsd: number;
}
