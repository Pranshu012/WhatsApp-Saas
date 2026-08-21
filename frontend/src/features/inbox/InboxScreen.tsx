import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { ConversationSummaryDto, MessageLedgerDto } from '../../api/types';
import { formatE164 } from '../../lib/phoneUtils';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Skeleton } from '../../components/Skeleton';
import { AlertBanner } from '../../components/AlertBanner';
import {
  MessageSquare,
  Send,
  Clock,
  AlertTriangle,
  User,
  ArrowLeft,
  Loader2,
  Check,
  CheckCheck,
  AlertCircle,
  FileText,
} from 'lucide-react';

export const InboxScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [selectedConvId, setSelectedConvId] = useState<string | null>(null);
  const [replyText, setReplyText] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 1. Fetch Conversations with 10s polling
  const {
    data: conversations,
    isLoading: isConversationsLoading,
    isError: isConversationsError,
    error: convError,
    refetch: refetchConversations,
  } = useQuery<ConversationSummaryDto[]>({
    queryKey: ['conversations'],
    queryFn: () => apiClient<ConversationSummaryDto[]>('/api/conversations'),
    refetchInterval: 10000,
  });

  // Auto-select first conversation on wide screen if none selected
  useEffect(() => {
    if (conversations && conversations.length > 0 && !selectedConvId && window.innerWidth >= 768) {
      setSelectedConvId(conversations[0].id);
    }
  }, [conversations, selectedConvId]);

  const selectedConversation = conversations?.find((c) => c.id === selectedConvId);

  // 2. Fetch Messages for selected conversation with 5s polling
  const {
    data: messages,
    isLoading: isMessagesLoading,
  } = useQuery<MessageLedgerDto[]>({
    queryKey: ['conversation-messages', selectedConvId],
    queryFn: () => apiClient<MessageLedgerDto[]>(`/api/conversations/${selectedConvId}/messages`),
    enabled: !!selectedConvId,
    refetchInterval: 5000,
  });

  // Scroll to bottom of message list on updates
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // 3. Manual Free-Text Reply Mutation
  const replyMutation = useMutation({
    mutationFn: ({ convId, text }: { convId: string; text: string }) =>
      apiClient(`/api/conversations/${convId}/reply`, {
        method: 'POST',
        body: JSON.stringify({ text }),
      }),
    onSuccess: () => {
      setReplyText('');
      queryClient.invalidateQueries({ queryKey: ['conversation-messages', selectedConvId] });
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to send reply.');
    },
  });

  const handleSendReply = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedConvId || !replyText.trim()) return;
    setErrorMsg(null);
    replyMutation.mutate({ convId: selectedConvId, text: replyText.trim() });
  };

  const calculateWindowRemaining = (expiresAt?: string) => {
    if (!expiresAt) return { active: false, label: 'Window Closed' };
    const diff = new Date(expiresAt).getTime() - Date.now();
    if (diff <= 0) return { active: false, label: 'Window Closed' };

    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    return {
      active: true,
      label: `${hours}h ${minutes}m remaining`,
    };
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'READ':
        return (
          <span title="Read">
            <CheckCheck className="w-3.5 h-3.5 text-blue-500" />
          </span>
        );
      case 'DELIVERED':
        return (
          <span title="Delivered">
            <CheckCheck className="w-3.5 h-3.5 text-slate-400" />
          </span>
        );
      case 'SENT':
        return (
          <span title="Sent">
            <Check className="w-3.5 h-3.5 text-slate-400" />
          </span>
        );
      case 'FAILED':
        return (
          <span title="Failed">
            <AlertCircle className="w-3.5 h-3.5 text-red-500" />
          </span>
        );
      default:
        return (
          <span title="Pending">
            <Clock className="w-3.5 h-3.5 text-slate-300" />
          </span>
        );
    }
  };

  if (isConversationsLoading) {
    return (
      <div className="space-y-6">
        <div className="pb-5 border-b border-gray-200">
          <Skeleton className="h-8 w-64 mb-2" />
          <Skeleton className="h-4 w-96" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 h-[600px]">
          <Skeleton className="h-full rounded-2xl md:col-span-1" />
          <Skeleton className="h-full rounded-2xl md:col-span-2" />
        </div>
      </div>
    );
  }

  if (isConversationsError) {
    return (
      <ErrorState
        title="Unable to load inbox"
        message={(convError as any)?.message || 'Failed to fetch conversations.'}
        onRetry={() => refetchConversations()}
      />
    );
  }

  const windowStatus = calculateWindowRemaining(selectedConversation?.serviceWindowExpiresAt);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pb-4 border-b border-gray-200">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900">Inbox & Live Chat</h1>
          <p className="text-xs sm:text-sm text-gray-500">
            View customer message threads and reply inside the official 24-hour Meta service window.
          </p>
        </div>
      </div>

      {errorMsg && (
        <AlertBanner
          type="error"
          message={errorMsg}
          onClose={() => setErrorMsg(null)}
        />
      )}

      {/* Main Inbox Container */}
      {!conversations || conversations.length === 0 ? (
        <EmptyState
          icon={MessageSquare}
          title="Your inbox is empty"
          description="When customers message your connected WhatsApp number, their conversation will appear here instantly."
        />
      ) : (
        <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden flex h-[calc(100vh-220px)] min-h-[480px]">
          {/* Left Panel: Conversation Thread List */}
          <div
            className={`w-full md:w-80 lg:w-96 border-r border-gray-200 flex flex-col ${
              selectedConvId ? 'hidden md:flex' : 'flex'
            }`}
          >
            <div className="p-3.5 bg-slate-50 border-b border-gray-200 font-bold text-xs text-gray-600 uppercase tracking-wider">
              Conversations ({conversations.length})
            </div>
            <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
              {conversations.map((conv) => {
                const isSelected = conv.id === selectedConvId;
                const convWindow = calculateWindowRemaining(conv.serviceWindowExpiresAt);

                return (
                  <button
                    key={conv.id}
                    type="button"
                    onClick={() => setSelectedConvId(conv.id)}
                    className={`w-full p-4 text-left flex items-start gap-3 transition-colors hover:bg-slate-50 min-h-[44px] ${
                      isSelected ? 'bg-brand-50/70 border-l-4 border-brand-600' : ''
                    }`}
                  >
                    <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center text-slate-600 shrink-0 font-bold text-sm">
                      <User className="w-5 h-5" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-1 mb-1">
                        <span className="font-bold text-gray-900 text-sm truncate">
                          {conv.contactName || formatE164(conv.phoneE164)}
                        </span>
                        {conv.lastInboundAt && (
                          <span className="text-[10px] text-gray-400 shrink-0">
                            {new Date(conv.lastInboundAt).toLocaleTimeString([], {
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </span>
                        )}
                      </div>

                      <div className="flex items-center justify-between text-xs">
                        <span className="text-gray-500 truncate">
                          {formatE164(conv.phoneE164)}
                        </span>
                        <span
                          className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                            convWindow.active
                              ? 'bg-green-100 text-green-800'
                              : 'bg-gray-100 text-gray-600'
                          }`}
                        >
                          {convWindow.active ? '24h Open' : 'Closed'}
                        </span>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Right Panel: Selected Message Thread */}
          <div
            className={`flex-1 flex flex-col bg-slate-50/50 ${
              !selectedConvId ? 'hidden md:flex items-center justify-center' : 'flex'
            }`}
          >
            {selectedConversation ? (
              <>
                {/* Thread Header */}
                <div className="p-3.5 bg-white border-b border-gray-200 flex items-center justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <button
                      type="button"
                      onClick={() => setSelectedConvId(null)}
                      className="p-1 rounded-lg text-gray-500 hover:bg-gray-100 md:hidden min-h-[44px] min-w-[44px] flex items-center justify-center"
                      aria-label="Back to conversations"
                    >
                      <ArrowLeft className="w-5 h-5" />
                    </button>
                    <div>
                      <h2 className="font-bold text-gray-900 text-sm sm:text-base">
                        {selectedConversation.contactName || formatE164(selectedConversation.phoneE164)}
                      </h2>
                      <div className="text-xs text-gray-500">
                        {formatE164(selectedConversation.phoneE164)}
                      </div>
                    </div>
                  </div>

                  {/* Service Window Countdown Pill */}
                  <div
                    className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold ${
                      windowStatus.active
                        ? 'bg-green-100 text-green-800'
                        : 'bg-amber-100 text-amber-800'
                    }`}
                  >
                    <Clock className="w-3.5 h-3.5" />
                    <span>{windowStatus.label}</span>
                  </div>
                </div>

                {/* Messages List Area */}
                <div className="flex-1 overflow-y-auto p-4 space-y-3">
                  {isMessagesLoading ? (
                    <div className="space-y-3">
                      <Skeleton className="h-12 w-2/3 rounded-2xl" />
                      <Skeleton className="h-12 w-2/3 rounded-2xl ml-auto" />
                    </div>
                  ) : !messages || messages.length === 0 ? (
                    <div className="text-center py-12 text-gray-400 text-xs">
                      No messages recorded yet for this contact.
                    </div>
                  ) : (
                    messages.map((msg) => {
                      const isOutbound = msg.direction === 'OUTBOUND';

                      return (
                        <div
                          key={msg.id}
                          className={`flex flex-col ${isOutbound ? 'items-end' : 'items-start'}`}
                        >
                          <div
                            className={`max-w-[85%] sm:max-w-md p-3 rounded-2xl text-xs sm:text-sm shadow-sm leading-relaxed ${
                              isOutbound
                                ? 'bg-brand-600 text-white rounded-br-none'
                                : 'bg-white text-gray-900 border border-gray-200 rounded-bl-none'
                            }`}
                          >
                            <div>{msg.templateName ? `[Template: ${msg.templateName}]` : 'WhatsApp Message'}</div>
                          </div>

                          <div className="flex items-center gap-1 mt-1 text-[10px] text-gray-400 px-1">
                            <span>
                              {new Date(msg.createdAt).toLocaleTimeString([], {
                                hour: '2-digit',
                                minute: '2-digit',
                              })}
                            </span>
                            {isOutbound && getStatusIcon(msg.status)}
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* Reply Input or Service Window Closed Banner */}
                <div className="p-3 bg-white border-t border-gray-200">
                  {windowStatus.active ? (
                    <form onSubmit={handleSendReply} className="flex items-center gap-2">
                      <input
                        type="text"
                        value={replyText}
                        onChange={(e) => setReplyText(e.target.value)}
                        placeholder="Type free-text reply inside 24h window..."
                        className="flex-1 px-4 py-2.5 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-brand-500 focus:outline-none min-h-[44px]"
                      />
                      <button
                        type="submit"
                        disabled={replyMutation.isPending || !replyText.trim()}
                        className="p-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white min-h-[44px] min-w-[44px] flex items-center justify-center shadow-sm disabled:opacity-50 transition-colors"
                        aria-label="Send reply"
                      >
                        {replyMutation.isPending ? (
                          <Loader2 className="w-5 h-5 animate-spin" />
                        ) : (
                          <Send className="w-5 h-5" />
                        )}
                      </button>
                    </form>
                  ) : (
                    <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-amber-900">
                      <div className="flex items-center gap-2">
                        <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
                        <span>
                          24-hour service window closed. Free-text reply is disabled by Meta. Send an approved template instead.
                        </span>
                      </div>
                      <button
                        type="button"
                        onClick={() => navigate('/templates')}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 font-bold text-amber-950 bg-amber-200 hover:bg-amber-300 rounded-lg min-h-[40px] shrink-0"
                      >
                        <FileText className="w-3.5 h-3.5" />
                        Templates
                      </button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="text-center p-8 text-gray-400 text-sm">
                Select a conversation from the left to view message history.
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
export default InboxScreen;
