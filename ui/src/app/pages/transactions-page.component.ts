import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService } from '../core/api.service';
import { CategoryDto, TransactionDto, TransactionType } from '../core/api.models';

interface NewTransactionForm {
  date: string;
  amount: number;
  description: string;
  categoryId: number | null;
}

@Component({
  selector: 'app-transactions-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transactions-page.component.html',
  styleUrl: './transactions-page.component.scss'
})
export class TransactionsPageComponent implements OnInit {
  month = this.currentMonth();
  loading = false;
  error = '';

  expenseCategories: CategoryDto[] = [];
  incomeCategories: CategoryDto[] = [];
  expenses: TransactionDto[] = [];
  incomes: TransactionDto[] = [];

  expenseForm: NewTransactionForm = this.createDefaultForm();
  incomeForm: NewTransactionForm = this.createDefaultForm();

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.syncFormDates();
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      expenseCategories: this.api.listCategories('EXPENSE'),
      incomeCategories: this.api.listCategories('INCOME'),
      expenses: this.api.listTransactions(this.month, 'EXPENSE'),
      incomes: this.api.listTransactions(this.month, 'INCOME')
    }).subscribe({
      next: (data) => {
        this.expenseCategories = data.expenseCategories.filter((c) => c.active);
        this.incomeCategories = data.incomeCategories.filter((c) => c.active);
        this.expenses = data.expenses;
        this.incomes = data.incomes;
        this.loading = false;
      },
      error: (error) => {
        this.error = this.toMessage(error);
        this.loading = false;
      }
    });
  }

  onMonthChanged(): void {
    this.syncFormDates();
    this.load();
  }

  addExpense(): void {
    this.addTransaction('EXPENSE', this.expenseForm);
  }

  addIncome(): void {
    this.addTransaction('INCOME', this.incomeForm);
  }

  deleteTransaction(type: TransactionType, id: number): void {
    this.api.deleteTransaction(this.month, type, id).subscribe({
      next: () => this.load(),
      error: (error) => (this.error = this.toMessage(error))
    });
  }

  private addTransaction(type: TransactionType, form: NewTransactionForm): void {
    if (!form.categoryId) {
      this.error = 'Category is required';
      return;
    }
    this.api
      .createTransaction(this.month, type, {
        date: form.date,
        amount: form.amount,
        description: form.description,
        categoryId: form.categoryId
      })
      .subscribe({
        next: () => {
          this.load();
          if (type === 'EXPENSE') {
            this.expenseForm = this.createDefaultForm(this.expenseForm.date);
          } else {
            this.incomeForm = this.createDefaultForm(this.incomeForm.date);
          }
        },
        error: (error) => (this.error = this.toMessage(error))
      });
  }

  private syncFormDates(): void {
    const firstDay = `${this.month}-01`;
    this.expenseForm.date = firstDay;
    this.incomeForm.date = firstDay;
  }

  private createDefaultForm(date = ''): NewTransactionForm {
    return {
      date,
      amount: 0,
      description: '',
      categoryId: null
    };
  }

  private currentMonth(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }

  private toMessage(error: unknown): string {
    const payload = (error as { error?: { message?: string } }).error;
    return payload?.message ?? 'Request failed';
  }
}

