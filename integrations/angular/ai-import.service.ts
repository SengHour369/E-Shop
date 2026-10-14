import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

export interface ImportBatch { id: string; revision: number; status: string; ownerId: number; assignedReviewerId: number | null; }
export interface ImportSheet { id: string; name: string; tool: string | null; confirmed: boolean; headers: string; }
export interface ImportRow { id: string; sheetId: string; rowNumber: number; originalValues: string; valuesJson: string; status: string; edited: boolean; skipped: boolean; resourceId: string | null; affectedResourceId: string | null; }
export interface ImportTool { name: string; permission: string; enabled: boolean; unavailableReason: string; schema: Record<string, { type: string; required: boolean }>; }
export interface ImportPage<T> { content: T[]; totalPages: number; totalElements: number; }
export interface ImportPreview { batch: ImportBatch; sheets: ImportSheet[]; mappings: { sheetId: string; suggestions: string; confirmedValues: string }[]; rows: ImportPage<ImportRow>; errors: { rowId: string; message: string; explanation?: string }[]; summary: { totalRows: number; editedRows: number; statuses: Record<string, number> }; }

@Injectable({ providedIn: 'root' })
export class AiImportService {
  private readonly http = inject(HttpClient);
  private readonly root = '/api/ai/imports';
  tools() { return this.http.get<ImportTool[]>(`${this.root}/tools`); }
  list(review: boolean, page = 0) { return this.http.get<ImportPage<ImportBatch>>(this.root, { params: { review, page } }); }
  upload(file: File) { const data = new FormData(); data.append('file', file); return this.http.post<ImportBatch>(`${this.root}/upload`, data); }
  preview(id: string, page = 0, status = '') { let params = new HttpParams().set('page', page); if (status) params = params.set('status', status); return this.http.get<ImportPreview>(`${this.root}/${encodeURIComponent(id)}/preview`, { params }); }
  action(batch: ImportBatch, action: string, extra: object = {}) { return this.http.post<ImportBatch>(`${this.root}/${encodeURIComponent(batch.id)}/${action}`, { version: batch.revision, ...extra }); }
  mapping(batch: ImportBatch, sheetId: string, tool: string, columns: Record<string, string>) { return this.http.patch<ImportBatch>(`${this.root}/${encodeURIComponent(batch.id)}/sheets/${encodeURIComponent(sheetId)}`, { version: batch.revision, tool, columns }); }
  edit(batch: ImportBatch, rowId: string, values: Record<string, string>) { return this.http.patch<ImportBatch>(`${this.root}/${encodeURIComponent(batch.id)}/rows/${encodeURIComponent(rowId)}`, { version: batch.revision, values }); }
  skip(batch: ImportBatch, row: ImportRow) { return this.http.patch<ImportBatch>(`${this.root}/${encodeURIComponent(batch.id)}/rows/${encodeURIComponent(row.id)}/skip`, { version: batch.revision, skipped: !row.skipped }); }
  executions(id: string) { return this.http.get(`${this.root}/${encodeURIComponent(id)}/executions`); }
  history(id: string) { return this.http.get(`${this.root}/${encodeURIComponent(id)}/history`); }
  download(id: string, kind: 'report' | 'original') { return this.http.get(`${this.root}/${encodeURIComponent(id)}/${kind}`, { responseType: 'blob' }); }
}
