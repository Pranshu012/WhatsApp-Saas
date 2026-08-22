import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import { WhatsAppTemplateResponse, CreateTemplateRequest } from '../../api/types';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Skeleton } from '../../components/Skeleton';
import { AlertBanner } from '../../components/AlertBanner';
import {
  FileText,
  Plus,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  Clock,
  XCircle,
  PauseCircle,
  Loader2,
  X,
  Sparkles,
  Zap,
  Tag,
  ShoppingBag,
} from 'lucide-react';

export const TemplatesScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Form State
  const [name, setName] = useState('');
  const [category, setCategory] = useState<'MARKETING' | 'UTILITY' | 'AUTHENTICATION'>('UTILITY');
  const [language, setLanguage] = useState('en_US');
  const [bodyText, setBodyText] = useState('');

  // 1. Fetch Templates
  const {
    data: templates,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<WhatsAppTemplateResponse[]>({
    queryKey: ['templates'],
    queryFn: () => apiClient<WhatsAppTemplateResponse[]>('/api/templates'),
  });

  // 2. Submit Template Mutation
  const createTemplateMutation = useMutation({
    mutationFn: (req: CreateTemplateRequest) =>
      apiClient<WhatsAppTemplateResponse>('/api/templates', {
        method: 'POST',
        body: JSON.stringify(req),
      }),
    onSuccess: (newTpl) => {
      queryClient.invalidateQueries({ queryKey: ['templates'] });
      setShowModal(false);
      resetForm();
      setSuccessMsg(`Template "${newTpl.name}" submitted to WhatsApp/Meta for approval. Approval usually takes a few minutes!`);
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to submit template to Meta.');
    },
  });

  // 3. Sync Templates Mutation
  const syncMutation = useMutation({
    mutationFn: () => apiClient('/api/templates/sync', { method: 'POST' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['templates'] });
      setSuccessMsg('Checked latest template approval status with Meta.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to sync with Meta.');
    },
  });

  const resetForm = () => {
    setName('');
    setCategory('UTILITY');
    setLanguage('en_US');
    setBodyText('');
  };

  const applySampleTemplate = (sampleName: string, sampleCategory: 'UTILITY' | 'MARKETING', sampleBody: string) => {
    setName(sampleName);
    setCategory(sampleCategory);
    setBodyText(sampleBody);
    setShowModal(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !bodyText.trim()) return;

    createTemplateMutation.mutate({
      name: name.trim().toLowerCase().replace(/[^a-z0-9_]+/g, '_'),
      category,
      language,
      bodyText: bodyText.trim(),
    });
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
            <CheckCircle2 className="w-3.5 h-3.5" />
            Ready to Send (Approved)
          </span>
        );
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-800 border border-amber-200">
            <Clock className="w-3.5 h-3.5 animate-spin" />
            Meta Reviewing (~5-15 mins)
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-red-100 text-red-800 border border-red-200">
            <XCircle className="w-3.5 h-3.5" />
            Rejected by Meta
          </span>
        );
      case 'PAUSED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-800 border border-amber-200">
            <PauseCircle className="w-3.5 h-3.5" />
            Paused by Meta
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-gray-100 text-gray-800">
            {status}
          </span>
        );
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="pb-5 border-b border-gray-200">
          <Skeleton className="h-8 w-64 mb-2" />
          <Skeleton className="h-4 w-96" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Skeleton className="h-44 rounded-2xl" />
          <Skeleton className="h-44 rounded-2xl" />
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Unable to load templates"
        message={(error as any)?.message || 'Failed to fetch templates from backend.'}
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div className="space-y-6">
      {/* 1. Clear Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2.5">
            <FileText className="w-7 h-7 text-brand-600" />
            WhatsApp Message Templates
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Pre-approved message formats required by WhatsApp to send bulk offers, order updates, and reminders to your customers.
          </p>
        </div>
        <div className="flex items-center gap-3 self-start sm:self-auto">
          <button
            type="button"
            onClick={() => syncMutation.mutate()}
            disabled={syncMutation.isPending}
            className="inline-flex items-center gap-2 px-3.5 py-2.5 text-xs font-bold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-xl min-h-[44px] shadow-sm transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${syncMutation.isPending ? 'animate-spin' : ''}`} />
            Refresh Approval Status
          </button>
          <button
            type="button"
            onClick={() => {
              resetForm();
              setShowModal(true);
            }}
            className="inline-flex items-center gap-2 px-5 py-2.5 text-sm font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl min-h-[44px] shadow-md shadow-brand-500/20 transition-all"
          >
            <Plus className="w-4 h-4" />
            Create New Template
          </button>
        </div>
      </div>

      {/* 2. Visual "What Are Templates" Explainer Card */}
      <div className="bg-gradient-to-r from-blue-50/70 via-indigo-50/50 to-slate-50 p-5 rounded-2xl border border-blue-200/80 shadow-sm">
        <h2 className="text-xs font-bold uppercase tracking-wider text-blue-900 flex items-center gap-1.5 mb-3">
          <Sparkles className="w-4 h-4 text-blue-600" />
          Why Are Message Templates Needed?
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs text-slate-700">
          <div className="bg-white/85 p-3.5 rounded-xl border border-blue-100 space-y-1">
            <div className="font-bold text-slate-900 flex items-center gap-1.5">
              <Zap className="w-4 h-4 text-brand-600" />
              1. WhatsApp Anti-Spam Rule
            </div>
            <p className="text-slate-600 leading-relaxed">
              To keep WhatsApp spam-free, Meta requires businesses to get message templates approved before initiating chats or broadcasting offers to customers.
            </p>
          </div>

          <div className="bg-white/85 p-3.5 rounded-xl border border-blue-100 space-y-1">
            <div className="font-bold text-slate-900 flex items-center gap-1.5">
              <Tag className="w-4 h-4 text-purple-600" />
              2. Personalization Placeholders
            </div>
            <p className="text-slate-600 leading-relaxed">
              Use <code className="bg-slate-100 px-1 rounded text-purple-700 font-bold">{`{{1}}`}</code>, <code className="bg-slate-100 px-1 rounded text-purple-700 font-bold">{`{{2}}`}</code> to automatically insert each customer&apos;s name, order ID, or tracking link.
            </p>
          </div>

          <div className="bg-white/85 p-3.5 rounded-xl border border-blue-100 space-y-1">
            <div className="font-bold text-slate-900 flex items-center gap-1.5">
              <ShoppingBag className="w-4 h-4 text-emerald-600" />
              3. Message Categories
            </div>
            <p className="text-slate-600 leading-relaxed">
              <strong>Utility (~₹0.115):</strong> Order & booking updates.<br />
              <strong>Marketing (~₹0.86):</strong> Sales, discounts & promotional offers.
            </p>
          </div>
        </div>
      </div>

      {/* Notifications */}
      {successMsg && (
        <AlertBanner
          type="success"
          message={successMsg}
          onClose={() => setSuccessMsg(null)}
        />
      )}
      {errorMsg && (
        <AlertBanner
          type="error"
          message={errorMsg}
          onClose={() => setErrorMsg(null)}
        />
      )}

      {/* 3. Templates List */}
      {!templates || templates.length === 0 ? (
        <div className="space-y-6">
          <EmptyState
            icon={FileText}
            title="No WhatsApp templates created yet"
            description="Create your first message template to start sending automated order confirmations, booking updates, or festival marketing offers."
            actionLabel="Create First Template"
            onAction={() => {
              resetForm();
              setShowModal(true);
            }}
          />

          {/* Quick Starter Templates */}
          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4">
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-brand-600" />
              <h3 className="font-bold text-base text-slate-900">
                Or click to use a ready-made template:
              </h3>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* Sample 1 */}
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex flex-col justify-between space-y-3">
                <div className="space-y-1.5">
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-blue-100 text-blue-800 uppercase">
                    Utility Update
                  </span>
                  <h4 className="font-bold text-sm text-slate-900">Order Confirmation</h4>
                  <p className="text-xs text-slate-600 leading-relaxed">
                    &ldquo;Hi {`{{1}}`}, your order #{`{{2}}`} has been confirmed and will be delivered by {`{{3}}`}. Track: {`{{4}}`}&rdquo;
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() =>
                    applySampleTemplate(
                      'order_confirmation_v1',
                      'UTILITY',
                      'Hi {{1}}, your order #{{2}} has been confirmed and will be delivered by {{3}}. Track your delivery here: {{4}}'
                    )
                  }
                  className="w-full py-2 px-3 bg-white border border-slate-300 hover:bg-brand-50 hover:text-brand-700 hover:border-brand-300 rounded-xl text-xs font-bold text-slate-700 transition-colors"
                >
                  Use This Template
                </button>
              </div>

              {/* Sample 2 */}
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex flex-col justify-between space-y-3">
                <div className="space-y-1.5">
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-purple-100 text-purple-800 uppercase">
                    Marketing Offer
                  </span>
                  <h4 className="font-bold text-sm text-slate-900">Festival Discount Offer</h4>
                  <p className="text-xs text-slate-600 leading-relaxed">
                    &ldquo;Namaste {`{{1}}`}! Exclusive 20% festive discount on all orders above ₹999. Use code {`{{2}}`}. Shop now: {`{{3}}`}&rdquo;
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() =>
                    applySampleTemplate(
                      'festive_discount_v1',
                      'MARKETING',
                      'Namaste {{1}}! Exclusive 20% festive discount on all orders above ₹999. Use code {{2}}. Shop now: {{3}}'
                    )
                  }
                  className="w-full py-2 px-3 bg-white border border-slate-300 hover:bg-brand-50 hover:text-brand-700 hover:border-brand-300 rounded-xl text-xs font-bold text-slate-700 transition-colors"
                >
                  Use This Template
                </button>
              </div>

              {/* Sample 3 */}
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex flex-col justify-between space-y-3">
                <div className="space-y-1.5">
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-blue-100 text-blue-800 uppercase">
                    Utility Update
                  </span>
                  <h4 className="font-bold text-sm text-slate-900">Appointment Reminder</h4>
                  <p className="text-xs text-slate-600 leading-relaxed">
                    &ldquo;Hello {`{{1}}`}, reminder for your scheduled visit on {`{{2}}`} at {`{{3}}`}. Reply YES to confirm or RESCHEDULE.&rdquo;
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() =>
                    applySampleTemplate(
                      'appointment_reminder_v1',
                      'UTILITY',
                      'Hello {{1}}, reminder for your scheduled visit on {{2}} at {{3}}. Reply YES to confirm or RESCHEDULE.'
                    )
                  }
                  className="w-full py-2 px-3 bg-white border border-slate-300 hover:bg-brand-50 hover:text-brand-700 hover:border-brand-300 rounded-xl text-xs font-bold text-slate-700 transition-colors"
                >
                  Use This Template
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {templates.map((tpl) => {
            const isMarketing = (tpl.metaCategory || tpl.category) === 'MARKETING';

            return (
              <div
                key={tpl.id}
                className="bg-white p-5 rounded-3xl border border-gray-200 shadow-sm flex flex-col justify-between space-y-4 hover:border-brand-300 transition-all"
              >
                <div className="space-y-3">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <h3 className="font-bold text-gray-900 text-sm sm:text-base">{tpl.name}</h3>
                      <div className="flex items-center gap-2 mt-1">
                        <span className="text-xs text-gray-500 font-medium">Lang: {tpl.language}</span>
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                            isMarketing
                              ? 'bg-purple-100 text-purple-800'
                              : 'bg-blue-100 text-blue-800'
                          }`}
                        >
                          {tpl.metaCategory || tpl.category}
                        </span>
                      </div>
                    </div>
                    {getStatusBadge(tpl.status)}
                  </div>

                  <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200/70 text-xs text-slate-800 whitespace-pre-wrap leading-relaxed">
                    {tpl.bodyText}
                  </div>

                  {/* REJECTION REASON ALERT */}
                  {tpl.status === 'REJECTED' && tpl.rejectionReason && (
                    <div className="p-3.5 rounded-xl bg-red-50 border border-red-200 text-xs text-red-800">
                      <strong>Meta Reason:</strong> {tpl.rejectionReason}
                    </div>
                  )}

                  {/* MARKETING COST WARNING */}
                  {isMarketing && (
                    <div className="p-3.5 rounded-xl bg-purple-50 border border-purple-200 text-xs text-purple-900 space-y-1">
                      <div className="font-bold flex items-center gap-1.5 text-purple-950">
                        <AlertTriangle className="w-3.5 h-3.5 text-purple-700" />
                        Marketing Message Pricing (~₹0.86 / msg)
                      </div>
                      <p className="text-[11px] text-purple-800 leading-relaxed">
                        Meta charges marketing rates for promotional words. If this is purely a transactional receipt or order status, remove promotional words to qualify for Utility (~₹0.115).
                      </p>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* 4. Submit Template Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm overflow-y-auto">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-gray-100 space-y-5 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h2 className="text-lg font-bold text-gray-900">Create New WhatsApp Template</h2>
                <p className="text-xs text-gray-500 mt-0.5">
                  Submit message format to WhatsApp for fast approval (~5-15 mins).
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
                  Template Name <span className="text-gray-400 font-normal lowercase">(letters, numbers, underscores only)</span>
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="block w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none placeholder:text-gray-400 font-mono text-xs"
                  placeholder="e.g. order_confirmation_v1"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Message Type / Category
                  </label>
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value as any)}
                    className="block w-full px-3 py-2.5 border border-gray-300 rounded-xl text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 bg-white"
                  >
                    <option value="UTILITY">Utility (~₹0.115/msg) - Receipts, Bookings</option>
                    <option value="MARKETING">Marketing (~₹0.86/msg) - Sales & Offers</option>
                    <option value="AUTHENTICATION">Authentication - OTPs</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Language
                  </label>
                  <select
                    value={language}
                    onChange={(e) => setLanguage(e.target.value)}
                    className="block w-full px-3 py-2.5 border border-gray-300 rounded-xl text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 bg-white"
                  >
                    <option value="en_US">English (en_US)</option>
                    <option value="en_GB">English (en_GB)</option>
                    <option value="hi">Hindi (hi)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Message Body Text <span className="text-gray-400 font-normal lowercase">(use {`{{1}}`}, {`{{2}}`} for customer variables)</span>
                </label>
                <textarea
                  rows={4}
                  required
                  value={bodyText}
                  onChange={(e) => setBodyText(e.target.value)}
                  className="block w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-brand-500 focus:outline-none placeholder:text-gray-400"
                  placeholder="Hi {{1}}, your order #{{2}} is confirmed and will be delivered by {{3}}! Track: {{4}}"
                />
                <p className="text-[11px] text-slate-500 mt-1">
                  💡 Example: In <em>&ldquo;Hi {`{{1}}`}&rdquo;</em>, <code className="bg-slate-100 px-1 rounded text-brand-700 font-bold">{`{{1}}`}</code> will be automatically replaced with the customer&apos;s real name when you send it.
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
                  disabled={createTemplateMutation.isPending}
                  className="px-5 py-2.5 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl min-h-[44px] shadow-md shadow-brand-500/25 flex items-center gap-2"
                >
                  {createTemplateMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Submit to WhatsApp for Approval
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
export default TemplatesScreen;
