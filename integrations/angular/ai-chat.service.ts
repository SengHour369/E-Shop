import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface AiResult {
  executionId: string;
  requestId: string;
  intent: string;
  status: 'SUCCESS' | 'NEEDS_INPUT' | 'DENIED' | 'FAILURE' | 'UNKNOWN' | 'RUNNING';
  message: string;
  errorCode: string | null;
  data: unknown;
}

export interface AiChatResponse {
  conversationId: string;
  mode: 'CUSTOMER' | 'SUPER_ADMIN';
  cardType: string;
  suggestions: string[];
  result: AiResult;
}

@Injectable({ providedIn: 'root' })
export class AiChatService {
  private readonly http = inject(HttpClient);

  // Uses the application's existing authentication interceptor/session.
  send(
    message: string,
    conversationId: string | null,
    administrator: boolean,
    language: 'en' | 'km',
  ): Observable<AiChatResponse> {
    const endpoint = administrator ? '/api/ai/admin/chat' : '/api/ai/chat';

    return this.http.post<AiChatResponse>(endpoint, {
      message,
      conversationId,
      language,
    });
  }

  confirm(confirmationId: string): Observable<AiResult> {
    return this.http.post<AiResult>(
      `/api/ai/confirm/${encodeURIComponent(confirmationId)}`,
      {},
    );
  }
}
