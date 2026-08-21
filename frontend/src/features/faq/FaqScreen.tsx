import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import { FaqResponse, CreateFaqRequest, TestFaqResponse } from '../../api/types';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Skeleton } from '../../components/Skeleton';
import { AlertBanner } from '../../components/AlertBanner';
import {
  HelpCircle,
  Plus,
  Trash2,
  Edit2,
  TestTube2,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  X,
  Search,
  ToggleLeft,
  ToggleRight,
} from 'lucide-react';

export const FaqScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const [editingFaqId, setEditingFaqId] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Form State
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState('');
  const [category, setCategory] = useState('');

  // Live Tester State
  const [testQuery, setTestQuery] = useState('');
  const [testResult, setTestResult] = useState<TestFaqResponse | null>(null);
  const [isTesting, setIsTesting] = useState(false);

  // 1. Fetch FAQs
  const {
    data: faqs,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<FaqResponse[]>({
    queryKey: ['faqs'],
    queryFn: () => apiClient<FaqResponse[]>('/api/faqs'),
  });

  // 2. Save FAQ mutation (Create or Update)
  const saveFaqMutation = useMutation({
    mutationFn: (req: CreateFaqRequest) => {
      if (editingFaqId) {
        return apiClient<FaqResponse>(`/api/faqs/${editingFaqId}`, {
          method: 'PUT',
          body: JSON.stringify(req),
        });
      }
      return apiClient<FaqResponse>('/api/faqs', {
        method: 'POST',
        body: JSON.stringify(req),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['faqs'] });
      setShowModal(false);
      const isEdit = !!editingFaqId;
      resetForm();
      setSuccessMsg(isEdit ? 'FAQ updated successfully.' : 'FAQ question added successfully.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to save FAQ.');
    },
  });

  // 3. Toggle FAQ active state
  const toggleFaqMutation = useMutation({
    mutationFn: (id: string) =>
      apiClient<FaqResponse>(`/api/faqs/${id}/toggle`, { method: 'PATCH' }),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['faqs'] });
      setSuccessMsg(`FAQ is now ${updated.active ? 'active' : 'disabled'}.`);
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to update FAQ status.');
    },
  });

  // 4. Delete FAQ mutation
  const deleteFaqMutation = useMutation({
    mutationFn: (id: string) => apiClient(`/api/faqs/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['faqs'] });
      setSuccessMsg('FAQ deleted successfully.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to delete FAQ.');
    },
  });

  const openEditModal = (faq: FaqResponse) => {
    setEditingFaqId(faq.id);
    setQuestion(faq.question);
    setAnswer(faq.answer);
    setCategory(faq.category || '');
    setShowModal(true);
  };

  const resetForm = () => {
    setEditingFaqId(null);
    setQuestion('');
    setAnswer('');
    setCategory('');
  };

  const handleTestFaq = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!testQuery.trim()) return;
    setIsTesting(true);
    setTestResult(null);

    try {
      const res = await apiClient<TestFaqResponse>('/api/faqs/test', {
        method: 'POST',
        body: JSON.stringify({ question: testQuery.trim() }),
      });
      setTestResult(res);
    } catch (err: any) {
      setTestResult({ matched: false });
    } finally {
      setIsTesting(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim() || !answer.trim()) return;

    saveFaqMutation.mutate({
      question: question.trim(),
      answer: answer.trim(),
      category: category.trim() || undefined,
      active: true,
    });
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <HelpCircle className="w-6 h-6 text-brand-600" />
            FAQ Knowledge Base
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            PostgreSQL Full-Text & Trigram semantic auto-matching for customer inquiries with typo tolerance.
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-brand-600 text-white font-medium rounded-xl hover:bg-brand-700 min-h-[44px] transition-colors shadow-sm self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          Add Question
        </button>
      </div>

      {/* Notifications */}
      {successMsg && (
        <AlertBanner type="success" message={successMsg} onClose={() => setSuccessMsg(null)} />
      )}
      {errorMsg && (
        <AlertBanner type="error" message={errorMsg} onClose={() => setErrorMsg(null)} />
      )}

      {/* Live FAQ Typo Tester */}
      <div className="bg-slate-900 text-white p-5 sm:p-6 rounded-2xl shadow-lg border border-slate-800 space-y-4">
        <div className="flex items-center gap-2">
          <TestTube2 className="w-5 h-5 text-brand-400" />
          <h2 className="font-bold text-base text-white">Live Semantic FAQ Matcher & Typo Tester</h2>
        </div>
        <p className="text-xs sm:text-sm text-slate-300">
          Type queries with typos or colloquial phrasing to test PostgreSQL <code className="bg-slate-800 px-1.5 py-0.5 rounded text-brand-300">pg_trgm</code> and full-text search matching in real time.
        </p>

        <form onSubmit={handleTestFaq} className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={testQuery}
              onChange={(e) => setTestQuery(e.target.value)}
              placeholder="e.g. wat r ur timings, do u giv refunds"
              className="w-full pl-10 pr-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-400 min-h-[44px] focus:outline-none focus:ring-2 focus:ring-brand-400"
            />
          </div>
          <button
            type="submit"
            disabled={isTesting || !testQuery.trim()}
            className="px-5 py-2.5 bg-brand-600 hover:bg-brand-500 text-white font-semibold rounded-xl min-h-[44px] flex items-center justify-center gap-2 transition-colors disabled:opacity-50 shrink-0"
          >
            {isTesting ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Test Query'}
          </button>
        </form>

        {/* Tester Result Box */}
        {testResult && (
          <div className="pt-2">
            {testResult.matched && testResult.confident ? (
              <div className="p-4 rounded-xl bg-green-950/80 border border-green-500/30 text-green-200 space-y-2">
                <div className="flex items-center justify-between text-xs font-semibold">
                  <span className="flex items-center gap-1.5 text-green-400">
                    <CheckCircle2 className="w-4 h-4" />
                    Confident Match Found
                  </span>
                  <span className="bg-green-500/20 text-green-300 px-2 py-0.5 rounded text-[11px]">
                    {Math.round((testResult.score || 1) * 100)}% Confidence
                  </span>
                </div>
                <div className="text-sm font-bold text-white">Q: {testResult.question}</div>
                <div className="text-xs text-green-100/90 pl-3 border-l-2 border-green-500/50">
                  ↳ A: {testResult.answer}
                </div>
              </div>
            ) : (
              <div className="p-4 rounded-xl bg-amber-950/80 border border-amber-500/30 text-amber-200 space-y-1.5">
                <div className="flex items-center justify-between text-xs font-semibold">
                  <span className="flex items-center gap-1.5 text-amber-400">
                    <AlertTriangle className="w-4 h-4" />
                    No Confident Match
                  </span>
                  {testResult.score && (
                    <span className="bg-amber-500/20 text-amber-300 px-2 py-0.5 rounded text-[11px]">
                      {Math.round(testResult.score * 100)}% Score (Below 35% Threshold)
                    </span>
                  )}
                </div>
                <p className="text-xs text-amber-300/90 leading-relaxed">
                  This query would be handed over to a human or logged to Unmatched Inquiries. Add it as a new FAQ or reword an existing question with customer terms.
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* FAQ List */}
      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full rounded-xl" />
          <Skeleton className="h-20 w-full rounded-xl" />
          <Skeleton className="h-20 w-full rounded-xl" />
        </div>
      ) : isError ? (
        <ErrorState
          title="Could not load FAQs"
          message={(error as any)?.message || 'Something went wrong.'}
          onRetry={refetch}
        />
      ) : !faqs || faqs.length === 0 ? (
        <EmptyState
          icon={HelpCircle}
          title="No FAQ questions added yet"
          description="Add common questions your customers ask about pricing, return policy, delivery times, and locations."
          actionLabel="Add First Question"
          onAction={() => {
            resetForm();
            setShowModal(true);
          }}
        />
      ) : (
        <div className="space-y-4">
          {faqs.map((faq) => (
            <div
              key={faq.id}
              className={`bg-white p-5 rounded-2xl border transition-all ${
                faq.active ? 'border-gray-200 shadow-sm' : 'border-gray-200/60 bg-gray-50/50 opacity-75'
              } flex flex-col sm:flex-row sm:items-start justify-between gap-4`}
            >
              <div className="space-y-2 flex-1">
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-gray-900 text-sm sm:text-base">{faq.question}</h3>
                  {faq.category && (
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-gray-100 text-gray-600 uppercase">
                      {faq.category}
                    </span>
                  )}
                  {!faq.active && (
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-gray-100 text-gray-500">
                      Inactive
                    </span>
                  )}
                </div>
                <p className="text-xs sm:text-sm text-gray-600 leading-relaxed bg-slate-50 p-3 rounded-xl border border-slate-100">
                  {faq.answer}
                </p>
              </div>

              <div className="flex items-center gap-1 self-end sm:self-start">
                {/* Active Toggle Button */}
                <button
                  type="button"
                  onClick={() => toggleFaqMutation.mutate(faq.id)}
                  disabled={toggleFaqMutation.isPending}
                  className={`p-2 rounded-lg min-h-[44px] min-w-[44px] flex items-center justify-center transition-colors ${
                    faq.active
                      ? 'text-emerald-600 hover:bg-emerald-50'
                      : 'text-gray-400 hover:bg-gray-100'
                  }`}
                  title={faq.active ? 'Disable FAQ' : 'Enable FAQ'}
                >
                  {faq.active ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6" />}
                </button>

                {/* Edit Button */}
                <button
                  type="button"
                  onClick={() => openEditModal(faq)}
                  className="p-2 text-gray-500 hover:text-brand-600 hover:bg-brand-50 rounded-lg min-h-[44px] min-w-[44px] flex items-center justify-center transition-colors"
                  title="Edit FAQ"
                >
                  <Edit2 className="w-4 h-4" />
                </button>

                {/* Delete Button */}
                <button
                  type="button"
                  onClick={() => deleteFaqMutation.mutate(faq.id)}
                  className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg min-h-[44px] min-w-[44px] flex items-center justify-center transition-colors"
                  title="Delete FAQ"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create / Edit FAQ Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-gray-100 space-y-5 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <h2 className="text-lg font-bold text-gray-900">
                {editingFaqId ? 'Edit FAQ Question' : 'Add FAQ Question'}
              </h2>
              <button
                type="button"
                onClick={() => setShowModal(false)}
                className="text-gray-400 hover:text-gray-600 p-1 rounded-lg min-h-[44px] min-w-[44px] flex items-center justify-center"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Customer Question
                </label>
                <input
                  type="text"
                  required
                  value={question}
                  onChange={(e) => setQuestion(e.target.value)}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  placeholder="e.g. What are your opening hours?"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Automated Answer
                </label>
                <textarea
                  rows={4}
                  required
                  value={answer}
                  onChange={(e) => setAnswer(e.target.value)}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  placeholder="We are open Monday to Saturday from 10:00 AM to 8:00 PM."
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Category <span className="text-gray-400 font-normal">(optional)</span>
                </label>
                <input
                  type="text"
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  placeholder="e.g. General, Pricing, Delivery"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg min-h-[44px]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saveFaqMutation.isPending}
                  className="px-5 py-2 text-xs font-semibold text-white bg-brand-600 hover:bg-brand-700 rounded-lg min-h-[44px] shadow-sm flex items-center gap-2"
                >
                  {saveFaqMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  {editingFaqId ? 'Update FAQ' : 'Save FAQ'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
export default FaqScreen;
