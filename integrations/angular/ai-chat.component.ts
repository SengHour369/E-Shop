import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, Input, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AiChatResponse, AiChatService, AiResult } from './ai-chat.service';

interface Message {
  author: 'You' | 'Assistant';
  text: string;
  result?: AiResult;
  confirmationId?: string;
  rows?: DisplayRow[];
  sources?: string[];
}

interface DisplayRow {
  title: string;
  fields: { label: string; value: string }[];
}

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-chat.component.html',
  styleUrls: ['./ai-chat.component.scss'],
})
export class AiChatComponent {
  @Input() administrator = false;

  readonly messages: Message[] = [];
  message = '';
  language: 'en' | 'km' = 'en';
  busy = false;
  error = '';
  private conversationId: string | null = null;
  private readonly chat = inject(AiChatService);

  send(): void {
    const text = this.message.trim();
    if (!text || this.busy || text.length > 4000) {
      return;
    }

    this.messages.push({ author: 'You', text });
    this.message = '';
    this.error = '';
    this.busy = true;

    this.chat.send(text, this.conversationId, this.administrator, this.language)
      .pipe(finalize(() => this.busy = false))
      .subscribe({
        next: response => this.accept(response),
        error: (failure: HttpErrorResponse) => {
          // NEEDS_INPUT uses HTTP 422 and can carry a reviewable confirmation.
          if (failure.error?.result?.status) {
            this.accept(failure.error as AiChatResponse);
          } else {
            this.error = 'Chat is unavailable. Please try again.';
          }
        },
      });
  }

  confirm(message: Message): void {
    if (!message.confirmationId || this.busy) {
      return;
    }

    const id = message.confirmationId;
    message.confirmationId = undefined;
    this.busy = true;
    this.chat.confirm(id)
      .pipe(finalize(() => this.busy = false))
      .subscribe({
        next: result => this.appendResult(result),
        error: (failure: HttpErrorResponse) => {
          if (failure.error?.status && failure.error?.message) {
            this.appendResult(failure.error as AiResult);
          } else {
            this.error = 'The action could not be confirmed. Check its status before trying again.';
          }
        },
      });
  }

  newConversation(): void {
    this.conversationId = null;
    this.messages.length = 0;
    this.error = '';
  }

  private accept(response: AiChatResponse): void {
    this.conversationId = response.conversationId;
    this.appendResult(response.result);
  }

  private appendResult(result: AiResult): void {
    const data = result.data as { confirmationId?: unknown } | null;
    const confirmationId = result.errorCode === 'CONFIRMATION_REQUIRED'
      && typeof data?.confirmationId === 'string'
      ? data.confirmationId
      : undefined;

    this.messages.push({
      author: 'Assistant',
      text: result.message,
      result,
      confirmationId,
      rows: this.displayRows(result),
      sources: this.knowledgeSources(result),
    });
  }

  private displayRows(result: AiResult): DisplayRow[] {
    if (!result.data || result.intent === 'KNOWLEDGE_SEARCH') {
      return [];
    }

    const data = result.data as Record<string, unknown>;
    let payload = result.errorCode === 'CONFIRMATION_REQUIRED' ? data['parameters'] : result.data;
    // Existing services can wrap a bounded list in a Spring Page or response envelope.
    if (payload && typeof payload === 'object' && !Array.isArray(payload)) {
      const envelope = payload as Record<string, unknown>;
      payload = envelope['content'] ?? envelope['data'] ?? payload;
    }
    const records = Array.isArray(payload) ? payload : [payload];
    const labels: Record<string, string> = {
      id: 'ID',
      skuId: 'SKU ID',
      name: 'Name',
      username: 'Account',
      description: 'Description',
      sku: 'SKU',
      orderNumber: 'Order number',
      orderId: 'Order ID',
      status: 'Status',
      enabled: 'Enabled',
      total: 'Total',
      totalAmount: 'Total',
      amount: 'Amount',
      currency: 'Currency',
      basePrice: 'Regular price',
      effectivePrice: 'Current price',
      available: 'Available',
      paymentCount: 'Completed payments',
      count: 'Count',
      quantity: 'Quantity',
      stock_qty: 'Stock',
      available_qty: 'Available',
      reserved_qty: 'Reserved',
      product_name: 'Product',
      product_sku_id: 'SKU ID',
      stock_status: 'Stock status',
      discount: 'Discount (%)',
      startAt: 'Starts',
      endAt: 'Ends',
      date: 'Date',
      orderDate: 'Ordered',
      paymentDate: 'Paid',
      requestedAt: 'Requested',
      createdAt: 'Created',
      occurredAt: 'Time',
      action: 'Action',
      resourceType: 'Record type',
      resourceId: 'Record ID',
      result: 'Result',
      message: 'Message',
      title: 'Title',
    };

    const rows: DisplayRow[] = [];
    for (const record of records) {
      if (!record || typeof record !== 'object') {
        continue;
      }
      const item = record as Record<string, unknown>;
      const fields = Object.entries(item)
        .filter(([key, value]) => labels[key] && value !== null
          && ['string', 'number', 'boolean'].includes(typeof value))
        .map(([key, value]) => ({ label: labels[key], value: String(value) }));
      rows.push({ title: String(item['name'] ?? item['orderNumber'] ?? item['title'] ?? 'Details'), fields });
      if (Array.isArray(item['skus'])) {
        rows.push(...this.displayRows({ ...result, data: item['skus'] }));
      }
    }
    return rows;
  }

  private knowledgeSources(result: AiResult): string[] {
    if (result.intent !== 'KNOWLEDGE_SEARCH' || !result.data) {
      return [];
    }
    const sources = (result.data as { sources?: unknown }).sources;
    return Array.isArray(sources)
      ? sources.map(source => `${source.title ?? 'Approved policy'} — ${source.updatedAt ?? ''}`)
      : [];
  }
}
