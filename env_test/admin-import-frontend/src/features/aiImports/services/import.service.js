import axiosClient from '@/api/axiosClient';

const root = '/api/ai/imports';
const idPath = id => `${root}/${encodeURIComponent(id)}`;
const options = () => ({ timeout: 120000, headers: { 'X-Request-ID': crypto.randomUUID() } });
export const importService = {
  access: signal => axiosClient.get('/api/v1/user/access', { signal }),
  tools: signal => axiosClient.get(`${root}/tools`, { signal }),
  list: (review, page, assigned, signal) => axiosClient.get(root, { params: { review, page, size: 20, assigned }, signal }),
  preview: (id, page, status, signal) => axiosClient.get(`${idPath(id)}/preview`, { params: { page, size: 50, ...(status ? { status } : {}) }, signal }),
  upload: file => {
    const body = new FormData(); body.append('file', file);
    return axiosClient.post(`${root}/upload`, body, { ...options(), headers: { ...options().headers, 'Content-Type': undefined } });
  },
  action: (batch, action, extra = {}) => axiosClient.post(`${idPath(batch.id)}/${action}`, { version: batch.revision, ...extra }, options()),
  mapping: (batch, sheet, tool, columns) => axiosClient.patch(`${idPath(batch.id)}/sheets/${encodeURIComponent(sheet.id)}`, { version: batch.revision, tool, columns }, options()),
  edit: (batch, row, values) => axiosClient.patch(`${idPath(batch.id)}/rows/${encodeURIComponent(row.id)}`, { version: batch.revision, values }, options()),
  skip: (batch, row) => axiosClient.patch(`${idPath(batch.id)}/rows/${encodeURIComponent(row.id)}/skip`, { version: batch.revision, skipped: !row.skipped }, options()),
  history: (id, signal) => axiosClient.get(`${idPath(id)}/history`, { signal }),
  executions: (id, signal) => axiosClient.get(`${idPath(id)}/executions`, { signal }),
  download: (id, kind) => axiosClient.get(`${idPath(id)}/${kind}`, { responseType: 'blob' }),
  chat: (message, conversationId, administrator, language) => axiosClient.post(administrator ? '/api/ai/admin/chat' : '/api/ai/chat', { message, conversationId, language }, options()),
  confirm: id => axiosClient.post(`/api/ai/confirm/${encodeURIComponent(id)}`, {}, options()),
};
