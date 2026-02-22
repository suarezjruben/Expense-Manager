export type CategoryType = 'EXPENSE' | 'INCOME';
export type TransactionType = 'EXPENSE' | 'INCOME';

export interface CategoryDto {
  id: number;
  name: string;
  type: CategoryType;
  sortOrder: number;
  active: boolean;
}

export interface CreateCategoryRequest {
  name: string;
  type: CategoryType;
  sortOrder?: number;
  active?: boolean;
}

export interface UpdateCategoryRequest {
  name?: string;
  sortOrder?: number;
  active?: boolean;
}

export interface MonthSettingsDto {
  month: string;
  startingBalance: number;
}

export interface SummaryCategoryDto {
  categoryId: number;
  categoryName: string;
  planned: number;
  actual: number;
  diff: number;
}

export interface SummaryTotalsDto {
  planned: number;
  actual: number;
  diff: number;
}

export interface MonthSummaryDto {
  month: string;
  startingBalance: number;
  netChange: number;
  endingBalance: number;
  savingsLabel: string;
  expenseTotals: SummaryTotalsDto;
  incomeTotals: SummaryTotalsDto;
  expenseCategories: SummaryCategoryDto[];
  incomeCategories: SummaryCategoryDto[];
}

export interface PlanItemDto {
  categoryId: number;
  categoryName: string;
  categoryType: CategoryType;
  sortOrder: number;
  plannedAmount: number;
}

export interface PlanItemRequest {
  categoryId: number;
  plannedAmount: number;
}

export interface TransactionDto {
  id: number;
  month: string;
  type: TransactionType;
  date: string;
  amount: number;
  description: string;
  categoryId: number;
  categoryName: string;
}

export interface TransactionRequest {
  date: string;
  amount: number;
  description: string;
  categoryId: number;
}

