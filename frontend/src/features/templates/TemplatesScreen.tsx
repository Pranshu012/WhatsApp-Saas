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
      setSuccessMsg(`Template "${newTpl.name}" submitted to Meta for approval.`);
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
      setSuccessMsg('Template category synchronization job triggered. Refreshing status...');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to trigger template sync.');
    },
  });

  const resetForm = () => {
    setName('');
    setCategory('UTILITY');
    setLanguage('en_US');
    setBodyText('');
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
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-green-100 text-green-800">
            <CheckCircle2 className="w-3.5 h-3.5" />
            Approved
          </span>
        );
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">
            <Clock className="w-3.5 h-3.5" />
            Meta Reviewing
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-800">
            <XCircle className="w-3.5 h-3.5" />
            Rejected
          </span>
        );
      case 'PAUSED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">
            <PauseCircle className="w-3.5 h-3.5" />
            Paused
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-800">
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
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-5 border-b border-gray-200">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900">WhatsApp Message Templates</h1>
          <p className="text-sm text-gray-500">
            Pre-approved Meta templates required for initiating outbound conversations outside 24h window.
          </p>
        </div>
        <div className="flex items-center gap-3 self-start sm:self-auto">
          <button
            type="button"
            onClick={() => syncMutation.mutate()}
            disabled={syncMutation.isPending}
            className="inline-flex items-center gap-2 px-3 py-2 text-xs font-semibold text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-lg min-h-[44px] shadow-sm transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${syncMutation.isPending ? 'animate-spin' : ''}`} />
            Sync from Meta
          </button>
          <button
            type="button"
            onClick={() => {
              resetForm();
              setShowModal(true);
            }}
            className="inline-flex items-center gap-2 px-4 py-2 text-xs font-semibold text-white bg-brand-600 hover:bg-brand-700 rounded-lg min-h-[44px] shadow-sm transition-colors"
          >
            <Plus className="w-4 h-4" />
            Submit Template
          </button>
        </div>
      </div>

      {/* Messages */}
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

      {/* Templates List */}
      {!templates || templates.length === 0 ? (
        <EmptyState
          icon={FileText}
          title="No WhatsApp templates found"
          description="Create your first message template for appointment confirmations, shipping updates, or marketing broadcasts."
          actionLabel="Submit First Template"
          onAction={() => {
            resetForm();
            setShowModal(true);
          }}
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {templates.map((tpl) => {
            const isMarketing = (tpl.metaCategory || tpl.category) === 'MARKETING';

            return (
              <div
                key={tpl.id}
                className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm flex flex-col justify-between space-y-4"
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

                  <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-100 text-xs text-slate-800 whitespace-pre-wrap leading-relaxed">
                    {tpl.bodyText}
                  </div>

                  {/* REJECTION REASON ALERT */}
                  {tpl.status === 'REJECTED' && tpl.rejectionReason && (
                    <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-xs text-red-800">
                      <strong>Meta Rejection Reason:</strong> {tpl.rejectionReason}
                    </div>
                  )}

                  {/* MARKETING COST WARNING */}
                  {isMarketing && (
                    <div className="p-3 rounded-lg bg-purple-50 border border-purple-200 text-xs text-purple-900 space-y-1">
                      <div className="font-bold flex items-center gap-1.5 text-purple-950">
                        <AlertTriangle className="w-3.5 h-3.5 text-purple-700" />
                        Marketing Category Cost Alert (~₹0.86 / msg)
                      </div>
                      <p className="text-[11px] text-purple-800 leading-relaxed">
                        Meta classifies this as Marketing (~7.5× cost of Utility ~₹0.115). If this is a transactional update, remove promotional words to qualify for Utility.
                      </p>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Submit Template Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-gray-100 space-y-5 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <h2 className="text-lg font-bold text-gray-900">Submit New Meta Template</h2>
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
                  Template Name <span className="text-gray-400 font-normal">(lowercase, underscores only)</span>
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  placeholder="order_confirmation_v1"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Requested Category
                  </label>
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value as any)}
                    className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 bg-white"
                  >
                    <option value="UTILITY">Utility (~₹0.115/msg)</option>
                    <option value="MARKETING">Marketing (~₹0.86/msg)</option>
                    <option value="AUTHENTICATION">Authentication</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Language
                  </label>
                  <select
                    value={language}
                    onChange={(e) => setLanguage(e.target.value)}
                    className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 bg-white"
                  >
                    <option value="en_US">English (en_US)</option>
                    <option value="en_GB">English (en_GB)</option>
                    <option value="hi">Hindi (hi)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Template Body Text <span className="text-gray-400 font-normal">(use {'{{1}}'}, {'{{2}}'} for variables)</span>
                </label>
                <textarea
                  rows={4}
                  required
                  value={bodyText}
                  onChange={(e) => setBodyText(e.target.value)}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  placeholder="Hi {{1}}, your order #{{2}} has been confirmed and will ship soon!"
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
                  disabled={createTemplateMutation.isPending}
                  className="px-5 py-2 text-xs font-semibold text-white bg-brand-600 hover:bg-brand-700 rounded-lg min-h-[44px] shadow-sm flex items-center gap-2"
                >
                  {createTemplateMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Submit to Meta
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
