import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AiImportService, ImportBatch, ImportPreview, ImportRow, ImportSheet, ImportTool } from './ai-import.service';

@Component({ selector: 'app-ai-import', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './ai-import.component.html', styleUrls: ['./ai-import.component.scss'] })
export class AiImportComponent implements OnInit {
  @Input() permissions: string[] = [];
  @Input() reviewer = false;
  @Input() initialBatchId = '';
  private readonly api = inject(AiImportService);
  tools: ImportTool[] = []; batches: ImportBatch[] = []; preview?: ImportPreview;
  private working = false; private loading = false; private previewRequest = 0;
  get busy() { return this.working || this.loading; }
  error = ''; filter = ''; page = 0; listPage = 0; listPages = 0; batchCount = 0; reviewerId: number | null = null; reason = '';
  sheetTools: Record<string, string> = {}; columns: Record<string, Record<string, string>> = {}; rowValues: Record<string, Record<string, string>> = {};
  executionHistory: unknown; auditHistory: unknown;
  readonly filters = ['', 'VALID', 'INVALID', 'DUPLICATE', 'EDITED', 'CREATED', 'FAILED', 'SKIPPED', 'UNVALIDATED'];
  readonly Object = Object;
  has(permission: string) { return this.permissions.includes(permission); }
  ngOnInit() { this.api.tools().subscribe({ next: tools => this.tools = tools, error: () => this.error = 'Import tools unavailable.' }); this.refreshList(); if (this.initialBatchId) this.load(this.initialBatchId); }
  refreshList() { this.run(this.api.list(this.reviewer, this.listPage), result => { this.batches = result.content; this.listPages = result.totalPages; this.batchCount = result.totalElements; }); }
  paginateList(delta: number) { if (this.busy) return; this.listPage = Math.max(0, this.listPage + delta); this.refreshList(); }
  private run<T>(operation: Observable<T>, accept: (result: T) => void) {
    if (this.busy) return;
    this.working = true; this.error = '';
    operation.pipe(finalize(() => this.working = false)).subscribe({ next: accept, error: (failure: HttpErrorResponse) => this.error = failure.status === 409 ? 'This import changed or the workbook is already staged. Reload before continuing.' : 'The import action failed. Check permissions, mappings and the current batch status.' });
  }
  upload(event: Event) {
    const input = event.target as HTMLInputElement; const file = input.files?.[0]; if (!file) return;
    this.run(this.api.upload(file), batch => { this.load(batch.id); input.value = ''; });
  }
  load(id: string) {
    // Loading after a mutation is a separate read; never replay a write on an HTTP timeout.
    if (this.preview?.batch.id !== id) { this.page = 0; this.filter = ''; this.executionHistory = undefined; this.auditHistory = undefined; this.reason = ''; }
    const request = ++this.previewRequest; this.loading = true; this.error = '';
    this.api.preview(id, this.page, this.filter).pipe(finalize(() => { if (request === this.previewRequest) this.loading = false; })).subscribe({ next: preview => {
      if (request !== this.previewRequest) return;
      this.preview = preview;
      this.sheetTools = {}; this.columns = {}; this.rowValues = {};
      for (const sheet of preview.sheets) {
        this.sheetTools[sheet.id] = sheet.tool ?? '';
        const mapping = preview.mappings.find(m => m.sheetId === sheet.id);
        this.columns[sheet.id] = this.object(sheet.confirmed ? mapping?.confirmedValues : mapping?.suggestions);
      }
      for (const row of preview.rows.content) this.rowValues[row.id] = this.object(row.valuesJson);
    }, error: () => { if (request === this.previewRequest) this.error = 'Import preview unavailable. Reload before continuing.'; } });
  }
  object(text?: string): Record<string, string> { try { const value = JSON.parse(text ?? '{}'); return value && typeof value === 'object' && !Array.isArray(value) ? value : {}; } catch { return {}; } }
  headers(sheet: ImportSheet): string[] { try { return JSON.parse(sheet.headers); } catch { return []; } }
  fields(sheet: ImportSheet) { return Object.keys(this.tools.find(t => t.name === this.sheetTools[sheet.id])?.schema ?? {}); }
  values(row: ImportRow) {
    const tool = this.preview?.sheets.find(sheet => sheet.id === row.sheetId)?.tool;
    return [...new Set([...Object.keys(this.tools.find(t => t.name === tool)?.schema ?? {}), ...Object.keys(this.rowValues[row.id] ?? {})])];
  }
  sheetName(id: string) { return this.preview?.sheets.find(sheet => sheet.id === id)?.name ?? id; }
  suggestions(sheet: ImportSheet) { return this.object(this.preview?.mappings.find(mapping => mapping.sheetId === sheet.id)?.suggestions); }
  messages(row: ImportRow) { return this.preview?.errors.filter(e => e.rowId === row.id).map(e => e.message + (e.explanation ? ` — AI suggestion: ${e.explanation}` : '')).join('; '); }
  editable() { return this.preview && !['PROCESSING', 'COMPLETED', 'PARTIAL_SUCCESS', 'FAILED', 'REJECTED'].includes(this.preview.batch.status); }
  action(action: string) {
    if (!this.preview) return;
    const extra = action === 'submit' ? { reviewerId: this.reviewerId } : ['approve', 'reject', 'request-changes'].includes(action) ? { reason: this.reason } : {};
    this.run(this.api.action(this.preview.batch, action, extra), batch => this.load(batch.id));
  }
  confirm(sheet: ImportSheet) {
    if (!this.preview) return;
    const columns = Object.fromEntries(Object.entries(this.columns[sheet.id]).filter(([, field]) => !!field));
    this.run(this.api.mapping(this.preview.batch, sheet.id, this.sheetTools[sheet.id], columns), batch => this.load(batch.id));
  }
  save(row: ImportRow) { if (this.preview) this.run(this.api.edit(this.preview.batch, row.id, this.rowValues[row.id]), batch => this.load(batch.id)); }
  skip(row: ImportRow) { if (this.preview) this.run(this.api.skip(this.preview.batch, row), batch => this.load(batch.id)); }
  paginate(delta: number) { if (this.preview && !this.busy) { this.page = Math.max(0, this.page + delta); this.load(this.preview.batch.id); } }
  changeFilter() { this.page = 0; if (this.preview) this.load(this.preview.batch.id); }
  history() { if (this.preview) { this.api.executions(this.preview.batch.id).subscribe({ next: value => this.executionHistory = value, error: () => this.error = 'Execution history unavailable.' }); this.api.history(this.preview.batch.id).subscribe({ next: value => this.auditHistory = value, error: () => this.error = 'Audit history unavailable.' }); } }
  download(kind: 'report' | 'original') {
    if (!this.preview) return;
    this.run(this.api.download(this.preview.batch.id, kind), blob => { const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = kind === 'report' ? 'import-report.csv' : 'original-workbook.bin'; link.click(); URL.revokeObjectURL(url); });
  }
}
