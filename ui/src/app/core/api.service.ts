import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  CategoryDto,
  CategoryType,
  CreateCategoryRequest,
  MonthSettingsDto,
  MonthSummaryDto,
  PlanItemDto,
  PlanItemRequest,
  TransactionDto,
  TransactionRequest,
  TransactionType,
  UpdateCategoryRequest
} from './api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly base = '/api';

  constructor(private readonly http: HttpClient) {}

  getSummary(month: string) {
    return this.http.get<MonthSummaryDto>(`${this.base}/months/${month}/summary`);
  }

  getMonthSettings(month: string) {
    return this.http.get<MonthSettingsDto>(`${this.base}/months/${month}/settings`);
  }

  updateMonthSettings(month: string, startingBalance: number) {
    return this.http.put<MonthSettingsDto>(`${this.base}/months/${month}/settings`, { startingBalance });
  }

  listCategories(type?: CategoryType) {
    const params = type ? new HttpParams().set('type', type) : undefined;
    return this.http.get<CategoryDto[]>(`${this.base}/categories`, { params });
  }

  createCategory(request: CreateCategoryRequest) {
    return this.http.post<CategoryDto>(`${this.base}/categories`, request);
  }

  updateCategory(id: number, request: UpdateCategoryRequest) {
    return this.http.put<CategoryDto>(`${this.base}/categories/${id}`, request);
  }

  deleteCategory(id: number) {
    return this.http.delete<void>(`${this.base}/categories/${id}`);
  }

  listPlans(month: string, type: CategoryType) {
    return this.http.get<PlanItemDto[]>(`${this.base}/months/${month}/plans`, {
      params: new HttpParams().set('type', type)
    });
  }

  upsertPlans(month: string, type: CategoryType, request: PlanItemRequest[]) {
    return this.http.put<PlanItemDto[]>(`${this.base}/months/${month}/plans`, request, {
      params: new HttpParams().set('type', type)
    });
  }

  listTransactions(month: string, type: TransactionType) {
    return this.http.get<TransactionDto[]>(`${this.base}/months/${month}/transactions`, {
      params: new HttpParams().set('type', type)
    });
  }

  createTransaction(month: string, type: TransactionType, request: TransactionRequest) {
    return this.http.post<TransactionDto>(`${this.base}/months/${month}/transactions`, request, {
      params: new HttpParams().set('type', type)
    });
  }

  deleteTransaction(month: string, type: TransactionType, id: number) {
    return this.http.delete<void>(`${this.base}/months/${month}/transactions/${id}`, {
      params: new HttpParams().set('type', type)
    });
  }
}

