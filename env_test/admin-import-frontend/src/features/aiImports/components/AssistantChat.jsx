import { useState } from 'react';
import Button from '@/components/ui/Button';
import { importService } from '../services/import.service';
import { errorMessage } from '../importLogic';

export default function AssistantChat({ administrator }) {
  const [messages, setMessages] = useState([]);
  const [message, setMessage] = useState('');
  const [conversationId, setConversationId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [language, setLanguage] = useState('en');
  const send = async event => {
    event.preventDefault(); if (!message.trim() || busy) return;
    const text = message.trim(); setBusy(true); setError(''); setMessage('');
    setMessages(current => [...current, { id: crypto.randomUUID(), author: 'You', text }]);
    try {
      const result = await importService.chat(text, conversationId, administrator, language);
      setConversationId(result.conversationId);
      setMessages(current => [...current, { id: crypto.randomUUID(), author: 'Assistant', text: result.result?.message || 'Response received.', result: result.result }]);
    } catch (failure) {
      // The backend returns HTTP 422 for clarification and reviewable confirmations.
      const response = failure?.response?.data;
      if (response?.result?.status) {
        setConversationId(response.conversationId);
        setMessages(current => [...current, { id: crypto.randomUUID(), author: 'Assistant', text: response.result.message, result: response.result }]);
      } else setError(errorMessage(failure));
    } finally { setBusy(false); }
  };
  const confirm = async (item, id) => {
    if (busy) return; setBusy(true); setError('');
    setMessages(current => current.map(value => value.id === item.id ? { ...value, confirming: true } : value));
    try { const result = await importService.confirm(id); setMessages(current => current.map(value => value.id === item.id ? { ...value, text: result.message, result } : value)); }
    catch { setError('The action could not be confirmed. Check its status before requesting another confirmation.'); } finally { setBusy(false); }
  };
  return <div className="space-y-4">
    <div className="flex items-center justify-between"><p className="text-sm text-slate-500">Ask about your business data or attach a workbook below.</p><Button size="sm" variant="ghost" disabled={busy} onClick={() => { setMessages([]); setConversationId(null); }}>New chat</Button></div>
    <div className="max-h-96 overflow-y-auto space-y-3" aria-live="polite">{messages.map(item => <article key={item.id} className={`rounded-xl p-4 text-sm ${item.author === 'You' ? 'bg-indigo-50' : 'bg-slate-50'}`}><strong className="text-xs text-slate-500">{item.author}</strong><p className="mt-1 whitespace-pre-wrap">{item.text}</p>{item.result?.data && <details className="mt-2 text-xs"><summary>View result</summary><pre className="whitespace-pre-wrap break-words">{JSON.stringify(item.result.data, null, 2)}</pre></details>}{item.result?.errorCode === 'CONFIRMATION_REQUIRED' && typeof item.result?.data?.confirmationId === 'string' && !item.confirming && <Button className="mt-2" size="sm" disabled={busy} onClick={() => confirm(item, item.result.data.confirmationId)}>Confirm action</Button>}</article>)}</div>
    {error && <p role="alert" className="text-sm text-rose-700">{error}</p>}
    <form onSubmit={send} className="flex flex-wrap gap-2"><label className="sr-only" htmlFor="assistant-message">Message</label><input id="assistant-message" className="min-w-48 flex-1 rounded-lg border border-slate-200 px-3 py-2 text-sm" value={message} maxLength={4000} placeholder="Ask the E-Shop assistant…" onChange={event => setMessage(event.target.value)}/><label className="sr-only" htmlFor="assistant-language">Language</label><select id="assistant-language" value={language} onChange={event => setLanguage(event.target.value)} className="rounded-lg border border-slate-200 p-2 text-sm"><option value="en">English</option><option value="km">ខ្មែរ</option></select><Button type="submit" loading={busy} disabled={!message.trim()}>Send</Button></form>
  </div>;
}
