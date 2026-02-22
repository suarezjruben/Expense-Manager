import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService } from '../core/api.service';
import { MonthSummaryDto } from '../core/api.models';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss'
})
export class DashboardPageComponent implements OnInit {
  month = this.currentMonth();
  loading = false;
  error = '';
  summary: MonthSummaryDto | null = null;
  startingBalance = 0;

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      summary: this.api.getSummary(this.month),
      settings: this.api.getMonthSettings(this.month)
    }).subscribe({
      next: ({ summary, settings }) => {
        this.summary = summary;
        this.startingBalance = settings.startingBalance;
        this.loading = false;
      },
      error: (error) => {
        this.error = this.toMessage(error);
        this.loading = false;
      }
    });
  }

  saveStartingBalance(): void {
    this.api.updateMonthSettings(this.month, this.startingBalance).subscribe({
      next: () => this.load(),
      error: (error) => (this.error = this.toMessage(error))
    });
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

