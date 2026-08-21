import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import {
  ScheduledMessageResponse,
  CreateScheduleRequest,
  WhatsAppTemplateResponse,
  WhatsAppAccountResponse,
} from '../../api/types';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Skeleton } from '../../components/Skeleton';
import { AlertBanner } from '../../components/AlertBanner';
import {
  Calendar,
  Plus,
  Trash2,
  Clock,
  CheckCircle2,
  AlertCircle,
  XCircle,
  Loader2,
  X,
} from 'lucide-react';

export const ScheduledMessagesScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Form State
  const [templateId, setTemplateId] = useState('');
  const [contactId, setContactId] = useState('');
  const [scheduledDate, setScheduledDate] = useState('');
  const [scheduledTime, setScheduledTime] = useState('');

  // 1. Fetch Scheduled Messages
  const {
    data: messages,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<ScheduledMessageResponse[]>({
    queryKey: ['scheduled-messages'],
    queryFn: () => apiClient<ScheduledMessageResponse[]>('/api/scheduled-messages'),
  });

  // 2. Fetch Approved Templates for Dropdown
  const { data: templates } = useQuery<WhatsAppTemplateResponse[]>({
    queryKey: ['templates'],
    queryFn: () => apiClient<WhatsAppTemplateResponse[]>('/api/templates'),
  });

  // 3. Fetch Primary WhatsApp Account
  const { data: account } = useQuery<WhatsAppAccountResponse>({
    queryKey: ['whatsapp-account'],
    queryFn: () => apiClient<WhatsAppAccountResponse>('/api/whatsapp/account'),
  });

  // 4. Cancel Mutation
  const cancelMutation = useMutation({
    mutationFn: (id: string) =>
      apiClient(`/api/scheduled-messages/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scheduled-messages'] });
      setSuccessMsg('Scheduled broadcast cancelled successfully.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to cancel scheduled message.');
    },
  });

  // 5. Create Schedule Mutation
  const scheduleMutation = useMutation({
    mutationFn: (req: CreateScheduleRequest) =>
      apiClient<ScheduledMessageResponse>('/api/scheduled-messages', {
        method: 'POST',
        body: JSON.stringify(req),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scheduled-messages'] });
      setShowModal(false);
      resetForm();
      setSuccessMsg('Message scheduled successfully.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to schedule message.');
    },
  });

  const resetForm = () => {
    setTemplateId('');
    setContactId('');
    setScheduledDate('');
    setScheduledTime('');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!templateId || !contactId || !scheduledDate || !scheduledTime || !account?.id) {
      setErrorMsg('Please fill in all fields including valid date and time.');
      return;
    }

    // Convert local input to UTC ISO string
    const localDateTime = new Date(`${scheduledDate}T${scheduledTime}:00`);
    if (localDateTime.getTime() <= Date.now()) {
      setErrorMsg('Scheduled time must be in the future.');
      return;
    }

    scheduleMutation.mutate({
      contactId: contactId.trim(),
      templateId,
      whatsappAccountId: account.id,
      scheduledFor: localDateTime.toISOString(),
      timezone: 'Asia/Kolkata',
    });
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'DISPATCHED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-green-100 text-green-800">
            <CheckCircle2 className="w-3.5 h-3.5" />
            Dispatched
          </span>
        );
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800">
            <Clock className="w-3.5 h-3.5" />
            Scheduled
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-700">
            <XCircle className="w-3.5 h-3.5" />
            Cancelled
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-800">
            <AlertCircle className="w-3.5 h-3.5" />
            Failed
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
        <div className="space-y-3">
          <Skeleton className="h-20 rounded-xl" />
          <Skeleton className="h-20 rounded-xl" />
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Unable to load scheduled messages"
        message={(error as any)?.message || 'Failed to fetch scheduled messages from server.'}
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-5 border-b border-gray-200">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900">Scheduled Message Broadcasts</h1>
          <p className="text-sm text-gray-500">
            Plan and broadcast approved template messages at future dates and times.
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-brand-600 hover:bg-brand-700 rounded-lg min-h-[44px] shadow-sm transition-colors self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          Schedule New Message
        </button>
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

      {/* Scheduled Messages List */}
      {!messages || messages.length === 0 ? (
        <EmptyState
          icon={Calendar}
          title="No scheduled broadcasts"
          description="Schedule appointment reminders, festive greetings, or order status updates in advance."
          actionLabel="Schedule First Message"
          onAction={() => {
            resetForm();
            setShowModal(true);
          }}
        />
      ) : (
        <div className="space-y-3">
          {messages.map((msg) => {
            const template = templates?.find((t) => t.id === msg.templateId);

            return (
              <div
                key={msg.id}
                className="bg-white p-4 sm:p-5 rounded-xl border border-gray-200 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4"
              >
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0">
                    <Clock className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-bold text-gray-900 text-sm sm:text-base">
                        {template?.name || 'Template Message'}
                      </span>
                      {getStatusBadge(msg.status)}
                    </div>
                    <div className="text-xs text-gray-500 flex items-center gap-2">
                      <span className="font-medium text-gray-700">
                        Delivery: {new Date(msg.scheduledFor).toLocaleString('en-IN')}
                      </span>
                      <span>•</span>
                      <span>Timezone: {msg.timezone || 'Asia/Kolkata'}</span>
                    </div>
                  </div>
                </div>

                {/* Cancel Action */}
                {msg.status === 'PENDING' && (
                  <button
                    type="button"
                    onClick={() => cancelMutation.mutate(msg.id)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-red-600 bg-red-50 hover:bg-red-100 border border-red-200 rounded-lg min-h-[44px] self-end sm:self-center transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                    Cancel Broadcast
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Schedule Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-gray-100 space-y-5 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <h2 className="text-lg font-bold text-gray-900">Schedule WhatsApp Broadcast</h2>
              <button
                type="button"
                onClick={() => setShowModal(false)}
                className="text-gray-400 hover:text-gray-600 p-1 rounded-lg min-h-[44px] min-w-[44px] flex items-center justify-center"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Template Select */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Approved WhatsApp Template
                </label>
                <select
                  required
                  value={templateId}
                  onChange={(e) => setTemplateId(e.target.value)}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 bg-white"
                >
                  <option value="">-- Select an approved template --</option>
                  {templates
                    ?.filter((t) => t.status === 'APPROVED')
                    .map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name} ({t.metaCategory || t.category})
                      </option>
                    ))}
                </select>
              </div>

              {/* Target Contact ID */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Recipient Contact UUID
                </label>
                <input
                  type="text"
                  required
                  value={contactId}
                  onChange={(e) => setContactId(e.target.value)}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  placeholder="e.g. contact UUID"
                />
              </div>

              {/* Date & Time */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Scheduled Date
                  </label>
                  <input
                    type="date"
                    required
                    value={scheduledDate}
                    onChange={(e) => setScheduledDate(e.target.value)}
                    className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Scheduled Time (IST)
                  </label>
                  <input
                    type="time"
                    required
                    value={scheduledTime}
                    onChange={(e) => setScheduledTime(e.target.value)}
                    className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500"
                  />
                </div>
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
                  disabled={scheduleMutation.isPending}
                  className="px-5 py-2 text-xs font-semibold text-white bg-brand-600 hover:bg-brand-700 rounded-lg min-h-[44px] shadow-sm flex items-center gap-2"
                >
                  {scheduleMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Confirm Schedule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
export default ScheduledMessagesScreen;
