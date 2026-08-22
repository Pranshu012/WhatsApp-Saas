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
  Sparkles,
  MessageCircle,
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
      setSuccessMsg(isEdit ? 'Question updated successfully.' : 'New automated question & answer added!');
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
      setSuccessMsg(`Auto-reply is now ${updated.enabled ? 'turned ON' : 'turned OFF'}.`);
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to update status.');
    },
  });

  // 4. Delete FAQ mutation
  const deleteFaqMutation = useMutation({
    mutationFn: (id: string) => apiClient(`/api/faqs/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['faqs'] });
      setSuccessMsg('Question deleted.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to delete question.');
    },
  });

  const openEditModal = (faq: FaqResponse) => {
    setEditingFaqId(faq.id);
    setQuestion(faq.question);
    setAnswer(faq.answer);
    setShowModal(true);
  };

  const resetForm = () => {
    setEditingFaqId(null);
    setQuestion('');
    setAnswer('');
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
    } catch {
      setTestResult({ confidenceScore: 0, isConfident: false });
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
    });
  };

  return (
    <div className="space-y-6">
      {/* 1. Clear Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2.5">
            <HelpCircle className="w-7 h-7 text-brand-600" />
            Common Questions & Auto-Replies (FAQ)
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Teach your WhatsApp bot how to answer repetitive customer questions about timings, pricing, location, and policies — automatically 24/7.
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-brand-600 text-white font-bold text-sm rounded-xl hover:bg-brand-700 min-h-[44px] transition-all shadow-md shadow-brand-500/20 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          Add New Question
        </button>
      </div>

      {/* 2. Visual "How This Works" Explainer Card */}
      <div className="bg-gradient-to-r from-emerald-50 via-teal-50 to-slate-50 p-5 rounded-2xl border border-emerald-200/80 shadow-sm">
        <h2 className="text-xs font-bold uppercase tracking-wider text-emerald-800 flex items-center gap-1.5 mb-3">
          <Sparkles className="w-4 h-4 text-emerald-600" />
          How Auto-Answers Work For Your Business
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs text-slate-700">
          <div className="bg-white/80 p-3.5 rounded-xl border border-emerald-100 space-y-1">
            <div className="font-bold text-slate-900 flex items-center gap-1.5">
              <span className="w-5 h-5 rounded-full bg-emerald-600 text-white flex items-center justify-center text-[10px] font-black">1</span>
              Add Your Q&A
            </div>
            <p className="text-slate-600 leading-relaxed">
              Add the questions your customers frequently ask (e.g. <em>&ldquo;What are your shop timings?&rdquo;</em>) and the answer you want them to receive.
            </p>
          </div>

          <div className="bg-white/80 p-3.5 rounded-xl border border-emerald-100 space-y-1">
            <div className="font-bold text-slate-900 flex items-center gap-1.5">
              <span className="w-5 h-5 rounded-full bg-emerald-600 text-white flex items-center justify-center text-[10px] font-black">2</span>
              Instant Reply on WhatsApp
            </div>
            <p className="text-slate-600 leading-relaxed">
              When a customer messages your WhatsApp number, our bot automatically matches their question and replies in less than 1 second.
            </p>
          </div>

          <div className="bg-white/80 p-3.5 rounded-xl border border-emerald-100 space-y-1">
            <div className="font-bold text-slate-900 flex items-center gap-1.5">
              <span className="w-5 h-5 rounded-full bg-emerald-600 text-white flex items-center justify-center text-[10px] font-black">3</span>
              Understands Typos & Hinglish
            </div>
            <p className="text-slate-600 leading-relaxed">
              Even if a customer writes with typos (e.g. <em>&ldquo;wat r ur timngs&rdquo;</em> or <em>&ldquo;shop timing kya h&rdquo;</em>), the bot understands and replies accurately!
            </p>
          </div>
        </div>
      </div>

      {/* Notifications */}
      {successMsg && (
        <AlertBanner type="success" message={successMsg} onClose={() => setSuccessMsg(null)} />
      )}
      {errorMsg && (
        <AlertBanner type="error" message={errorMsg} onClose={() => setErrorMsg(null)} />
      )}

      {/* 3. Live Simulator & Typo Tester */}
      <div className="bg-slate-900 text-white p-5 sm:p-6 rounded-2xl shadow-xl border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <TestTube2 className="w-5 h-5 text-emerald-400" />
            <h2 className="font-bold text-base text-white">🧪 Test Bot Reply (Customer Simulator)</h2>
          </div>
          <span className="text-[11px] font-medium text-slate-400 hidden sm:inline">
            Test how the bot replies before real customers message
          </span>
        </div>
        <p className="text-xs sm:text-sm text-slate-300">
          Type any customer question below — try misspelling words or writing in casual language to see if your bot finds the right answer.
        </p>

        <form onSubmit={handleTestFaq} className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={testQuery}
              onChange={(e) => setTestQuery(e.target.value)}
              placeholder="Try typing: 'wat r shop timings' or 'refund policy kya h'"
              className="w-full pl-10 pr-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-400 min-h-[44px] focus:outline-none focus:ring-2 focus:ring-emerald-400"
            />
          </div>
          <button
            type="submit"
            disabled={isTesting || !testQuery.trim()}
            className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-xl min-h-[44px] flex items-center justify-center gap-2 transition-colors disabled:opacity-50 shrink-0"
          >
            {isTesting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Testing...
              </>
            ) : (
              'Test Bot Reply'
            )}
          </button>
        </form>

        {/* Tester Result Box */}
        {testResult && (
          <div className="pt-2">
            {testResult.id && testResult.isConfident ? (
              <div className="p-4 rounded-xl bg-emerald-950/80 border border-emerald-500/40 text-emerald-200 space-y-2">
                <div className="flex items-center justify-between text-xs font-semibold">
                  <span className="flex items-center gap-1.5 text-emerald-400 font-bold">
                    <CheckCircle2 className="w-4 h-4" />
                    Matched Successfully! Bot will send this reply:
                  </span>
                  <span className="bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded text-[11px] font-mono">
                    {Math.round(testResult.confidenceScore * 100)}% Match Accuracy
                  </span>
                </div>
                <div className="text-sm font-bold text-white">Matched FAQ: &ldquo;{testResult.question}&rdquo;</div>
                <div className="text-xs text-emerald-100 pl-3 border-l-2 border-emerald-400">
                  ↳ <strong>Bot Answer:</strong> {testResult.answer}
                </div>
              </div>
            ) : (
              <div className="p-4 rounded-xl bg-amber-950/80 border border-amber-500/40 text-amber-200 space-y-2">
                <div className="flex items-center justify-between text-xs font-semibold">
                  <span className="flex items-center gap-1.5 text-amber-400 font-bold">
                    <AlertTriangle className="w-4 h-4" />
                    No Confident Answer Found
                  </span>
                  {testResult.confidenceScore > 0 && (
                    <span className="bg-amber-500/20 text-amber-300 px-2 py-0.5 rounded text-[11px] font-mono">
                      {Math.round(testResult.confidenceScore * 100)}% Score (Too low to auto-reply safely)
                    </span>
                  )}
                </div>
                <p className="text-xs text-amber-200/90 leading-relaxed">
                  In a real WhatsApp chat, this question will appear in your <strong>&ldquo;Needs Your Reply&rdquo;</strong> inbox so you or your staff can reply manually, or you can add this as a new FAQ question below!
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* 4. FAQ List */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-bold text-gray-900">
            Active Auto-Replies ({faqs?.length || 0})
          </h2>
          <span className="text-xs text-gray-500">
            Click the toggle switch to enable or disable any reply instantly
          </span>
        </div>

        {isLoading ? (
          <div className="space-y-3">
            <Skeleton className="h-24 w-full rounded-2xl" />
            <Skeleton className="h-24 w-full rounded-2xl" />
            <Skeleton className="h-24 w-full rounded-2xl" />
          </div>
        ) : isError ? (
          <ErrorState
            title="Could not load FAQs"
            message={(error as any)?.message || 'Something went wrong.'}
            onRetry={refetch}
          />
        ) : !faqs || faqs.length === 0 ? (
          <EmptyState
            icon={MessageCircle}
            title="You have not added any automated answers yet"
            description="Add common questions your customers ask about shop timings, pricing, location, return policy, and payment options."
            actionLabel="Add Your First Question & Answer"
            onAction={() => {
              resetForm();
              setShowModal(true);
            }}
          />
        ) : (
          <div className="space-y-3">
            {faqs.map((faq) => (
              <div
                key={faq.id}
                className={`bg-white p-5 rounded-2xl border transition-all ${
                  faq.enabled
                    ? 'border-gray-200 shadow-sm hover:border-brand-300'
                    : 'border-gray-200/60 bg-gray-50/50 opacity-70'
                } flex flex-col sm:flex-row sm:items-start justify-between gap-4`}
              >
                <div className="space-y-2 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="w-6 h-6 rounded-lg bg-emerald-100 text-emerald-800 text-xs font-black flex items-center justify-center shrink-0">
                      Q
                    </span>
                    <h3 className="font-bold text-gray-900 text-sm sm:text-base">{faq.question}</h3>
                    {!faq.enabled && (
                      <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-gray-200 text-gray-600">
                        Paused
                      </span>
                    )}
                  </div>
                  <div className="pl-8">
                    <p className="text-xs sm:text-sm text-gray-700 leading-relaxed bg-slate-50 p-3.5 rounded-xl border border-slate-200/60">
                      <span className="font-semibold text-emerald-700 block mb-1 text-xs">
                        ↳ Bot Reply:
                      </span>
                      {faq.answer}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-start pl-8 sm:pl-0">
                  {/* Active Toggle Button */}
                  <button
                    type="button"
                    onClick={() => toggleFaqMutation.mutate(faq.id)}
                    disabled={toggleFaqMutation.isPending}
                    className={`p-2 rounded-xl min-h-[44px] min-w-[44px] flex items-center justify-center transition-colors ${
                      faq.enabled
                        ? 'text-emerald-600 hover:bg-emerald-50'
                        : 'text-gray-400 hover:bg-gray-100'
                    }`}
                    title={faq.enabled ? 'Click to Pause Auto-Reply' : 'Click to Activate Auto-Reply'}
                  >
                    {faq.enabled ? <ToggleRight className="w-7 h-7" /> : <ToggleLeft className="w-7 h-7" />}
                  </button>

                  {/* Edit Button */}
                  <button
                    type="button"
                    onClick={() => openEditModal(faq)}
                    className="p-2 text-gray-500 hover:text-brand-600 hover:bg-brand-50 rounded-xl min-h-[44px] min-w-[44px] flex items-center justify-center transition-colors"
                    title="Edit Question & Answer"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>

                  {/* Delete Button */}
                  <button
                    type="button"
                    onClick={() => {
                      if (window.confirm(`Are you sure you want to delete "${faq.question}"?`)) {
                        deleteFaqMutation.mutate(faq.id);
                      }
                    }}
                    className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-xl min-h-[44px] min-w-[44px] flex items-center justify-center transition-colors"
                    title="Delete"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 5. Create / Edit FAQ Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm overflow-y-auto">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-gray-100 space-y-5 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h2 className="text-lg font-bold text-gray-900">
                  {editingFaqId ? 'Edit Question & Answer' : 'Add New Automated Question'}
                </h2>
                <p className="text-xs text-gray-500 mt-0.5">
                  Set what the customer asks and how your bot should reply.
                </p>
              </div>
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
                  Customer Question <span className="text-brand-600">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={question}
                  onChange={(e) => setQuestion(e.target.value)}
                  className="block w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none placeholder:text-gray-400"
                  placeholder="e.g. What are your opening hours?"
                />
                <p className="text-[11px] text-gray-500 mt-1">
                  💡 Customers can ask this with typos or different wording — the AI will still understand it!
                </p>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Automated Reply to Send <span className="text-brand-600">*</span>
                </label>
                <textarea
                  rows={4}
                  required
                  value={answer}
                  onChange={(e) => setAnswer(e.target.value)}
                  className="block w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-brand-500 focus:outline-none placeholder:text-gray-400"
                  placeholder="We are open Monday to Saturday from 10:00 AM to 8:30 PM. We are closed on Sundays."
                />
                <p className="text-[11px] text-gray-500 mt-1">
                  This exact message will be sent to the customer on WhatsApp instantly.
                </p>
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2.5 text-xs font-bold text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-xl min-h-[44px]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saveFaqMutation.isPending}
                  className="px-5 py-2.5 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl min-h-[44px] shadow-md shadow-brand-500/25 flex items-center gap-2"
                >
                  {saveFaqMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  {editingFaqId ? 'Update Reply' : 'Save & Turn On'}
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
