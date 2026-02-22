import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  AccountDto,
  CategoryDto,
  CategoryType,
  CsvHeaderMappingInput,
  CreateAccountRequest,
  CreateCategoryRequest,
  MonthSettingsDto,
  MonthSummaryDto,
  PlanItemDto,
  StatementImportResponseDto,
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

  listAccounts(includeInactive = false) {
    return this.http.get<AccountDto[]>(`${this.base}/accounts`, {
      params: new HttpParams().set('includeInactive', String(includeInactive))
    });
  }

  createAccount(request: CreateAccountRequest) {
    return this.http.post<AccountDto>(`${this.base}/accounts`, request);
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

  listTransactions(month: string, type: TransactionType, accountId?: number) {
    let params = new HttpParams().set('type', type);
    if (accountId != null) {
      params = params.set('accountId', String(accountId));
    }
    return this.http.get<TransactionDto[]>(`${this.base}/months/${month}/transactions`, { params });
  }

  createTransaction(month: string, type: TransactionType, request: TransactionRequest, accountId?: number) {
    let params = new HttpParams().set('type', type);
    if (accountId != null) {
      params = params.set('accountId', String(accountId));
    }
    return this.http.post<TransactionDto>(`${this.base}/months/${month}/transactions`, request, {
      params
    });
  }

  updateTransaction(month: string, type: TransactionType, id: number, request: TransactionRequest, accountId?: number) {
    let params = new HttpParams().set('type', type);
    if (accountId != null) {
      params = params.set('accountId', String(accountId));
    }
    return this.http.put<TransactionDto>(`${this.base}/months/${month}/transactions/${id}`, request, {
      params
    });
  }

  deleteTransaction(month: string, type: TransactionType, id: number, accountId?: number) {
    let params = new HttpParams().set('type', type);
    if (accountId != null) {
      params = params.set('accountId', String(accountId));
    }
    return this.http.delete<void>(`${this.base}/months/${month}/transactions/${id}`, {
      params
    });
  }

  importStatement(accountId: number, file: File, mapping?: CsvHeaderMappingInput) {
    const formData = new FormData();
    formData.append('file', file, file.name);
    if (mapping) {
      formData.append('dateColumnIndex', String(mapping.dateColumnIndex));
      formData.append('amountColumnIndex', String(mapping.amountColumnIndex));
      formData.append('descriptionColumnIndex', String(mapping.descriptionColumnIndex));
      if (mapping.categoryColumnIndex != null) {
        formData.append('categoryColumnIndex', String(mapping.categoryColumnIndex));
      }
      if (mapping.externalIdColumnIndex != null) {
        formData.append('externalIdColumnIndex', String(mapping.externalIdColumnIndex));
      }
      if (mapping.saveHeaderMapping) {
        formData.append('saveHeaderMapping', 'true');
      }
    }
    return this.http.post<StatementImportResponseDto>(`${this.base}/accounts/${accountId}/statement-imports`, formData);
  }
}
