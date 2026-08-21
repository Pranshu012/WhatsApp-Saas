import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { UnmatchedMessageResponse } from '../../api/types';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Skeleton } from '../../components/Skeleton';
import { AlertBanner } from '../../components/AlertBanner';
import { formatE164 } from '../../lib/phoneUtils';
import {
  CheckCircle2,
  PlusCircle,
  Trash2,
  Clock,
} from 'lucide-react';

export const UnmatchedMessagesScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // 1. Fetch Unmatched Messages
  const {
    data: messages,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<UnmatchedMessageResponse[]>({
    queryKey: ['unmatched-messages'],
    queryFn: () => apiClient<UnmatchedMessageResponse[]>('/api/unmatched-messages'),
  });

  // 2. Delete Mutation
  const deleteMutation = useMutation({
    mutationFn: (id: string) => apiClient(`/api/unmatched-messages/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['unmatched-messages'] });
      setSuccessMsg('Inquiry resolved and dismissed.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to dismiss message.');
    },
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="pb-5 border-b border-gray-200">
          <Skeleton className="h-8 w-64 mb-2" />
          <Skeleton className="h-4 w-96" />
        </div>
        <div className="space-y-3">
          <Skeleton className="h-20 rounded-xl" />
          <Skeleton className="h-20 rounded-xl" />
          <Skeleton className="h-20 rounded-xl" />
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Unable to load unmatched messages"
        message={(error as any)?.message || 'Failed to load unhandled inquiries.'}
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-5 border-b border-gray-200">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900">Unmatched Inquiries</h1>
          <p className="text-sm text-gray-500">
            Customer questions that the automation engine and FAQ bot could not answer with high confidence.
          </p>
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

      {/* Unmatched List */}
      {!messages || messages.length === 0 ? (
        <EmptyState
          icon={CheckCircle2}
          title="All customer inquiries resolved"
          description="Every incoming customer message has matched an existing keyword automation rule or FAQ question."
        />
      ) : (
        <div className="space-y-3">
          {messages.map((msg) => (
            <div
              key={msg.id}
              className="bg-white p-4 sm:p-5 rounded-xl border border-gray-200 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="space-y-1.5 flex-1">
                <div className="flex items-center gap-2 text-xs text-gray-500">
                  <span className="font-semibold text-gray-800">
                    {formatE164(msg.senderPhone)}
                  </span>
                  <span>•</span>
                  <span className="flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5" />
                    {new Date(msg.receivedAt).toLocaleString('en-IN')}
                  </span>
                </div>
                <div className="text-sm font-semibold text-gray-900 bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                  "{msg.messageText}"
                </div>
              </div>

              {/* Quick Actions */}
              <div className="flex items-center gap-2 self-end sm:self-center">
                <button
                  type="button"
                  onClick={() => navigate('/faq')}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-brand-700 bg-brand-50 hover:bg-brand-100 border border-brand-200 rounded-lg min-h-[44px] transition-colors"
                >
                  <PlusCircle className="w-4 h-4" />
                  Turn into FAQ
                </button>
                <button
                  type="button"
                  onClick={() => deleteMutation.mutate(msg.id)}
                  className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg min-h-[44px] min-w-[44px] flex items-center justify-center"
                  title="Dismiss inquiry"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
export default UnmatchedMessagesScreen;
