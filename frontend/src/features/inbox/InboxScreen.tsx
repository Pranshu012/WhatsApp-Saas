/**
 * InboxScreen: Enterprise WhatsApp Customer Chat & Unified CRM Inbox.
 *
 * Capabilities:
 * - Real-time conversational message feed with delivery ticks (Sent, Delivered, Read).
 * - Meta 24-hour customer care messaging window timer and compliance badge.
 * - Customer Context Drawer displaying contact info, lead qualification stage, and opt-in/opt-out status.
 * - Direct opt-in/opt-out manual toggle for customer consent management.
 */
import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { ConversationSummaryDto, ChatMessageDto, LeadDto } from '../../api/types';
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
  Search,
  Bot,
  UserCheck,
  PanelRightClose,
  PanelRightOpen,
  ExternalLink,
  Sparkles,
  ShieldCheck,
  ShieldAlert,
  Users,
  Target,
  Flame,
  IndianRupee,
  ArrowRight,
} from 'lucide-react';

function formatRelativeTime(dateStr?: string): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / (1000 * 60));
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
  if (diffDays === 1) return 'Yesterday';
  if (diffDays < 7) return `${diffDays}d ago`;
  return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

export const InboxScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const contactIdParam = searchParams.get('contactId');

  const [selectedConvId, setSelectedConvId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterTab, setFilterTab] = useState<'ALL' | 'ACTIVE' | 'CLOSED'>('ALL');
  const [showDetailsPanel, setShowDetailsPanel] = useState(false);
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

  // Auto-select conversation by contactId or first conversation on desktop
  useEffect(() => {
    if (!conversations || conversations.length === 0) return;
    if (contactIdParam) {
      const matched = conversations.find((c) => c.contactId === contactIdParam);
      if (matched) {
        setSelectedConvId(matched.id);
        return;
      }
    }
    if (!selectedConvId && window.innerWidth >= 1024) {
      setSelectedConvId(conversations[0].id);
    }
  }, [conversations, contactIdParam, selectedConvId]);

  const selectedConversation = conversations?.find((c) => c.id === selectedConvId);

  // CRM Leads for context
  const { data: leads } = useQuery<LeadDto[]>({
    queryKey: ['leads'],
    queryFn: () => apiClient<LeadDto[]>('/api/leads'),
    staleTime: 10000,
  });

  const activeContactLead = useMemo(() => {
    if (!selectedConversation?.contactId || !leads) return null;
    return leads.find((l) => l.contactId === selectedConversation.contactId);
  }, [selectedConversation?.contactId, leads]);

  // 2. Fetch Messages for selected conversation with 4s polling
  const {
    data: messages,
    isLoading: isMessagesLoading,
  } = useQuery<ChatMessageDto[]>({
    queryKey: ['conversation-messages', selectedConvId],
    queryFn: () => apiClient<ChatMessageDto[]>(`/api/conversations/${selectedConvId}/messages`),
    enabled: !!selectedConvId,
    refetchInterval: 4000,
  });

  // Scroll to bottom when messages update
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // 3. Manual Free-Text Reply Mutation
  const replyMutation = useMutation({
    mutationFn: ({ convId, text, clientRequestId }: { convId: string; text: string; clientRequestId?: string }) =>
      apiClient(`/api/conversations/${convId}/reply`, {
        method: 'POST',
        body: JSON.stringify({ text, clientRequestId }),
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

  const toggleOptMutation = useMutation({
    mutationFn: async ({ contactId, optInStatus }: { contactId: string; optInStatus: string }) => {
      await apiClient(`/api/conversations/contacts/${contactId}/opt-status`, {
        method: 'PATCH',
        body: JSON.stringify({ optInStatus }),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to update opt-in status.');
    },
  });

  const handleSendReply = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedConvId || !replyText.trim()) return;
    setErrorMsg(null);
    const clientRequestId = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : undefined;
    replyMutation.mutate({ convId: selectedConvId, text: replyText.trim(), clientRequestId });
  };

  const calculateWindowRemaining = (expiresAt?: string) => {
    if (!expiresAt) return { active: false, label: 'Window Closed', hours: 0, minutes: 0 };
    const diff = new Date(expiresAt).getTime() - Date.now();
    if (diff <= 0) return { active: false, label: 'Window Closed', hours: 0, minutes: 0 };

    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    return {
      active: true,
      label: `${hours}h ${minutes}m left`,
      hours,
      minutes,
    };
  };

  // Filter conversations based on search and tab
  const filteredConversations = useMemo(() => {
    if (!conversations) return [];
    return conversations.filter((c) => {
      const windowInfo = calculateWindowRemaining(c.serviceWindowExpiresAt);

      if (filterTab === 'ACTIVE' && !windowInfo.active) return false;
      if (filterTab === 'CLOSED' && windowInfo.active) return false;

      if (!searchQuery.trim()) return true;
      const q = searchQuery.toLowerCase();
      const nameMatch = c.contactName?.toLowerCase().includes(q) || false;
      const phoneMatch = c.phoneE164?.toLowerCase().includes(q) || false;
      const msgMatch = c.lastMessageText?.toLowerCase().includes(q) || false;
      return nameMatch || phoneMatch || msgMatch;
    });
  }, [conversations, searchQuery, filterTab]);

  const activeCount = useMemo(() => {
    if (!conversations) return 0;
    return conversations.filter((c) => calculateWindowRemaining(c.serviceWindowExpiresAt).active).length;
  }, [conversations]);

  const getStatusIcon = (status?: string) => {
    switch (status) {
      case 'READ':
        return <span title="Read"><CheckCheck className="w-3.5 h-3.5 text-blue-500" /></span>;
      case 'DELIVERED':
        return <span title="Delivered"><CheckCheck className="w-3.5 h-3.5 text-slate-400" /></span>;
      case 'SENT':
        return <span title="Sent"><Check className="w-3.5 h-3.5 text-slate-400" /></span>;
      case 'FAILED':
        return <span title="Failed"><AlertCircle className="w-3.5 h-3.5 text-red-500" /></span>;
      default:
        return <span title="Pending"><Clock className="w-3.5 h-3.5 text-slate-300" /></span>;
    }
  };

  if (isConversationsLoading) {
    return (
      <div className="space-y-6">
        <div className="pb-5 border-b border-gray-200">
          <Skeleton className="h-8 w-64 mb-2" />
          <Skeleton className="h-4 w-96" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 h-[650px]">
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

  const currentWindow = calculateWindowRemaining(selectedConversation?.serviceWindowExpiresAt);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-4 border-b border-gray-200">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 tracking-tight">Customer Inbox</h1>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-brand-50 text-brand-700 border border-brand-200">
              <Sparkles className="w-3 h-3" />
              AI Assistant Active
            </span>
          </div>
          <p className="text-xs sm:text-sm text-gray-500 mt-1">
            Real-time WhatsApp conversations, AI multi-turn interactions, and customer support.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="px-3 py-1.5 rounded-xl bg-gray-50 border border-gray-200 text-xs font-medium text-gray-600 flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span>{activeCount} Active 24h Sessions</span>
          </div>
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
          title="No conversations yet"
          description="When customers send a WhatsApp message to your connected number, their conversation will appear here automatically."
        />
      ) : (
        <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden flex h-[calc(100vh-210px)] min-h-[520px]">
          {/* Left Panel: Conversation Threads List */}
          <div
            className={`w-full md:w-80 lg:w-96 border-r border-gray-200 flex flex-col bg-slate-50/40 shrink-0 ${
              selectedConvId ? 'hidden md:flex' : 'flex'
            }`}
          >
            {/* Search and Filters */}
            <div className="p-3 bg-white border-b border-gray-200 space-y-2.5">
              <div className="relative">
                <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search contact, phone or message..."
                  className="w-full pl-9 pr-3 py-1.5 text-xs bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white transition-all"
                />
              </div>

              {/* Filter Tabs */}
              <div className="flex items-center gap-1 p-0.5 bg-gray-100 rounded-lg text-xs">
                <button
                  type="button"
                  onClick={() => setFilterTab('ALL')}
                  className={`flex-1 py-1 text-center font-medium rounded-md transition-all ${
                    filterTab === 'ALL'
                      ? 'bg-white text-gray-900 shadow-sm font-semibold'
                      : 'text-gray-500 hover:text-gray-900'
                  }`}
                >
                  All ({conversations.length})
                </button>
                <button
                  type="button"
                  onClick={() => setFilterTab('ACTIVE')}
                  className={`flex-1 py-1 text-center font-medium rounded-md transition-all ${
                    filterTab === 'ACTIVE'
                      ? 'bg-white text-emerald-700 shadow-sm font-semibold'
                      : 'text-gray-500 hover:text-gray-900'
                  }`}
                >
                  24h Open ({activeCount})
                </button>
                <button
                  type="button"
                  onClick={() => setFilterTab('CLOSED')}
                  className={`flex-1 py-1 text-center font-medium rounded-md transition-all ${
                    filterTab === 'CLOSED'
                      ? 'bg-white text-gray-900 shadow-sm font-semibold'
                      : 'text-gray-500 hover:text-gray-900'
                  }`}
                >
                  Closed ({conversations.length - activeCount})
                </button>
              </div>
            </div>

            {/* List */}
            <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
              {filteredConversations.length === 0 ? (
                <div className="p-8 text-center text-xs text-gray-400">
                  No conversations match your filter.
                </div>
              ) : (
                filteredConversations.map((conv) => {
                  const isSelected = conv.id === selectedConvId;
                  const convWindow = calculateWindowRemaining(conv.serviceWindowExpiresAt);
                  const displayName = conv.contactName || formatE164(conv.phoneE164);
                  const initial = (conv.contactName || conv.phoneE164 || '?').trim()[0]?.toUpperCase() || 'C';

                  return (
                    <button
                      key={conv.id}
                      type="button"
                      onClick={() => setSelectedConvId(conv.id)}
                      className={`w-full p-3.5 text-left flex items-start gap-3 transition-colors min-h-[64px] relative ${
                        isSelected
                          ? 'bg-brand-50/80 border-l-4 border-brand-600'
                          : 'hover:bg-white hover:shadow-sm'
                      }`}
                    >
                      {/* Avatar */}
                      <div className="relative shrink-0">
                        <div
                          className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-xs ${
                            isSelected
                              ? 'bg-brand-600 text-white shadow-sm'
                              : 'bg-slate-200 text-slate-700'
                          }`}
                        >
                          {initial}
                        </div>
                        {convWindow.active && (
                          <span
                            className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-emerald-500 border-2 border-white rounded-full"
                            title="24h Window Active"
                          />
                        )}
                      </div>

                      {/* Content */}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-1 mb-0.5">
                          <span className="font-semibold text-gray-900 text-sm truncate">
                            {displayName}
                          </span>
                          <span className="text-[11px] text-gray-400 shrink-0">
                            {formatRelativeTime(conv.lastMessageAt || conv.lastInboundAt)}
                          </span>
                        </div>

                        {/* Phone Subtitle */}
                        <div className="text-[11px] text-gray-500 mb-1">
                          {formatE164(conv.phoneE164)}
                        </div>

                        {/* Last message preview */}
                        <div className="flex items-center justify-between gap-2 text-xs">
                          <p className="text-gray-600 truncate text-[11px]">
                            {conv.lastMessageSender === 'AI_BOT' && (
                              <span className="font-medium text-purple-700 mr-1">[AI Assistant]:</span>
                            )}
                            {conv.lastMessageSender === 'AGENT' && (
                              <span className="font-medium text-brand-700 mr-1">[Agent]:</span>
                            )}
                            {conv.lastMessageText || 'Tap to open conversation'}
                          </p>

                          <span
                            className={`shrink-0 text-[10px] font-semibold px-1.5 py-0.5 rounded ${
                              convWindow.active
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-gray-100 text-gray-500'
                            }`}
                          >
                            {convWindow.active ? '24h Open' : 'Closed'}
                          </span>
                        </div>
                      </div>
                    </button>
                  );
                })
              )}
            </div>
          </div>

          {/* Middle Panel: Active Message Thread */}
          <div
            className={`flex-1 flex flex-col bg-slate-50/30 min-w-0 ${
              !selectedConvId ? 'hidden md:flex items-center justify-center' : 'flex'
            }`}
          >
            {selectedConversation ? (
              <>
                {/* Thread Header */}
                <div className="p-3 px-4 bg-white border-b border-gray-200 flex items-center justify-between gap-3 shadow-xs">
                  <div className="flex items-center gap-3 min-w-0">
                    <button
                      type="button"
                      onClick={() => setSelectedConvId(null)}
                      className="p-1 rounded-lg text-gray-500 hover:bg-gray-100 md:hidden min-h-[38px] min-w-[38px] flex items-center justify-center shrink-0"
                      aria-label="Back to conversations"
                    >
                      <ArrowLeft className="w-5 h-5" />
                    </button>

                    <div className="w-9 h-9 rounded-full bg-brand-100 text-brand-800 flex items-center justify-center font-bold text-sm shrink-0">
                      {(selectedConversation.contactName || selectedConversation.phoneE164 || '?').trim()[0]?.toUpperCase() || 'C'}
                    </div>

                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <h2 className="font-bold text-gray-900 text-sm truncate">
                          {selectedConversation.contactName || formatE164(selectedConversation.phoneE164)}
                        </h2>
                        <span className="text-xs text-gray-400 font-normal hidden sm:inline">
                          ({formatE164(selectedConversation.phoneE164)})
                        </span>
                      </div>
                      <div className="text-[11px] text-gray-500 flex items-center gap-2">
                        <span className="inline-flex items-center gap-1 text-emerald-600 font-medium">
                          <ShieldCheck className="w-3 h-3" />
                          Verified Customer
                        </span>
                        <span>•</span>
                        <span>Multi-Turn AI Enabled</span>
                      </div>
                    </div>
                  </div>

                  {/* Header Actions */}
                  <div className="flex items-center gap-2 shrink-0">
                    {/* 24h Countdown Badge */}
                    <div
                      className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold ${
                        currentWindow.active
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-amber-50 text-amber-800 border border-amber-200'
                      }`}
                      title={
                        currentWindow.active
                          ? `Meta 24h session closes in ${currentWindow.hours} hours and ${currentWindow.minutes} minutes`
                          : 'Meta 24h session closed. Free-text disabled until customer replies or template is sent.'
                      }
                    >
                      <Clock className="w-3.5 h-3.5 shrink-0" />
                      <span className="font-mono">{currentWindow.label}</span>
                    </div>

                    {/* Toggle Customer Info Panel */}
                    <button
                      type="button"
                      onClick={() => setShowDetailsPanel(!showDetailsPanel)}
                      className={`p-2 rounded-xl border transition-colors ${
                        showDetailsPanel
                          ? 'bg-brand-50 border-brand-300 text-brand-700'
                          : 'bg-gray-50 hover:bg-gray-100 border-gray-200 text-gray-600'
                      }`}
                      title="Customer Details & Diagnostics"
                      aria-label="Toggle Details Panel"
                    >
                      {showDetailsPanel ? (
                        <PanelRightClose className="w-4 h-4" />
                      ) : (
                        <PanelRightOpen className="w-4 h-4" />
                      )}
                    </button>
                  </div>
                </div>

                {/* Messages List Area */}
                <div className="flex-1 overflow-y-auto p-4 space-y-4">
                  {isMessagesLoading ? (
                    <div className="space-y-4">
                      <Skeleton className="h-14 w-2/3 rounded-2xl" />
                      <Skeleton className="h-16 w-3/4 rounded-2xl ml-auto" />
                      <Skeleton className="h-12 w-1/2 rounded-2xl" />
                    </div>
                  ) : !messages || messages.length === 0 ? (
                    <div className="text-center py-16 text-gray-400">
                      <MessageSquare className="w-10 h-10 mx-auto text-gray-300 mb-2" />
                      <p className="text-sm font-medium text-gray-600">No messages found</p>
                      <p className="text-xs text-gray-400 mt-1">This contact hasn't exchanged any text messages yet.</p>
                    </div>
                  ) : (
                    messages.map((msg) => {
                      const isCustomer = msg.senderType === 'CUSTOMER';
                      const isAiBot = msg.senderType === 'AI_BOT';
                      const isAgent = msg.senderType === 'AGENT';

                      return (
                        <div
                          key={msg.id}
                          className={`flex flex-col ${isCustomer ? 'items-start' : 'items-end'}`}
                        >
                          {/* Sender Label Badge */}
                          <div className="flex items-center gap-1 text-[11px] font-semibold mb-1 px-1 text-gray-500">
                            {isCustomer && (
                              <span className="flex items-center gap-1 text-slate-700">
                                <User className="w-3 h-3" />
                                {selectedConversation.contactName || 'Customer'}
                              </span>
                            )}
                            {isAiBot && (
                              <span className="flex items-center gap-1 text-purple-700 bg-purple-50 px-2 py-0.5 rounded-full border border-purple-200">
                                <Bot className="w-3 h-3" />
                                AI Assistant
                              </span>
                            )}
                            {isAgent && (
                              <span className="flex items-center gap-1 text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                                <UserCheck className="w-3 h-3" />
                                Support Agent
                              </span>
                            )}
                          </div>

                          {/* Message Bubble */}
                          <div
                            className={`max-w-[85%] sm:max-w-md md:max-w-lg p-3.5 rounded-2xl text-xs sm:text-sm shadow-xs leading-relaxed whitespace-pre-wrap break-words ${
                              isCustomer
                                ? 'bg-white text-gray-900 border border-gray-200 rounded-tl-none'
                                : isAiBot
                                ? 'bg-slate-900 text-slate-50 border border-slate-800 rounded-tr-none'
                                : 'bg-brand-600 text-white rounded-tr-none'
                            }`}
                          >
                            {msg.textContent}
                          </div>

                          {/* Message Metadata & Timestamp */}
                          <div className="flex items-center gap-1.5 mt-1 text-[10px] text-gray-400 px-1">
                            <span>
                              {new Date(msg.createdAt).toLocaleTimeString([], {
                                hour: '2-digit',
                                minute: '2-digit',
                              })}
                            </span>
                            {!isCustomer && getStatusIcon(msg.status)}
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* Reply Composer or 24h Window Closed Banner */}
                <div className="p-3 px-4 bg-white border-t border-gray-200">
                  {currentWindow.active ? (
                    <form onSubmit={handleSendReply} className="flex items-center gap-2">
                      <div className="relative flex-1">
                        <input
                          type="text"
                          value={replyText}
                          onChange={(e) => setReplyText(e.target.value)}
                          placeholder="Type an agent reply to this customer..."
                          className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm focus:bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none min-h-[44px] transition-all"
                        />
                      </div>
                      <button
                        type="submit"
                        disabled={replyMutation.isPending || !replyText.trim()}
                        className="px-4 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white min-h-[44px] flex items-center justify-center gap-1.5 font-medium text-xs sm:text-sm shadow-xs disabled:opacity-50 transition-colors shrink-0"
                        aria-label="Send reply"
                      >
                        {replyMutation.isPending ? (
                          <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                          <>
                            <span>Send</span>
                            <Send className="w-4 h-4" />
                          </>
                        )}
                      </button>
                    </form>
                  ) : (
                    <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-amber-900">
                      <div className="flex items-center gap-2.5">
                        <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
                        <span>
                          24-hour customer care session is closed. Meta requires sending an approved WhatsApp template to re-engage.
                        </span>
                      </div>
                      <button
                        type="button"
                        onClick={() => navigate('/templates')}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 font-semibold text-amber-900 bg-amber-200 hover:bg-amber-300 rounded-lg min-h-[38px] shrink-0 transition-colors"
                      >
                        <FileText className="w-3.5 h-3.5" />
                        Send Template
                      </button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="text-center p-8 text-gray-400 text-sm">
                <MessageSquare className="w-12 h-12 mx-auto text-gray-300 mb-2" />
                <p className="font-semibold text-gray-600">Select a conversation</p>
                <p className="text-xs text-gray-400 mt-1">Pick a thread from the left list to view customer history.</p>
              </div>
            )}
          </div>

          {/* Right Panel: Customer 360 & Session Diagnostics */}
          {selectedConversation && showDetailsPanel && (
            <div className="w-72 lg:w-80 border-l border-gray-200 bg-white flex flex-col shrink-0 animate-in slide-in-from-right duration-200">
              <div className="p-3.5 border-b border-gray-200 flex items-center justify-between bg-slate-50/50">
                <span className="font-bold text-xs text-gray-700 uppercase tracking-wider">
                  Customer Context
                </span>
                <button
                  type="button"
                  onClick={() => setShowDetailsPanel(false)}
                  className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100"
                >
                  <PanelRightClose className="w-4 h-4" />
                </button>
              </div>

              <div className="p-4 space-y-5 overflow-y-auto flex-1">
                {/* Contact Card */}
                <div className="text-center pb-4 border-b border-gray-100">
                  <div className="w-14 h-14 rounded-full bg-brand-100 text-brand-800 flex items-center justify-center font-bold text-lg mx-auto mb-2">
                    {(selectedConversation.contactName || selectedConversation.phoneE164 || '?').trim()[0]?.toUpperCase() || 'C'}
                  </div>
                  <h3 className="font-bold text-gray-900 text-sm">
                    {selectedConversation.contactName || 'Unnamed Contact'}
                  </h3>
                  <p className="text-xs text-gray-500 mt-0.5">
                    {formatE164(selectedConversation.phoneE164)}
                  </p>

                  <div className="mt-3 flex items-center justify-center gap-2">
                    <a
                      href={`https://wa.me/${selectedConversation.phoneE164.replace(/\D/g, '')}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 transition-colors"
                    >
                      <ExternalLink className="w-3 h-3" />
                      Open in WhatsApp
                    </a>
                  </div>
                </div>

                {/* Session Window */}
                <div className="space-y-2">
                  <label className="text-[11px] font-bold uppercase tracking-wider text-gray-400">
                    24h Customer Session
                  </label>
                  <div className="p-3 rounded-xl bg-gray-50 border border-gray-200 space-y-1.5">
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-gray-500">Status</span>
                      <span
                        className={`font-semibold px-2 py-0.5 rounded text-[10px] ${
                          currentWindow.active
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {currentWindow.active ? 'Active' : 'Closed'}
                      </span>
                    </div>
                    {selectedConversation.serviceWindowExpiresAt && (
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-gray-500">Expires</span>
                        <span className="text-gray-700 text-[11px] font-mono">
                          {new Date(selectedConversation.serviceWindowExpiresAt).toLocaleTimeString([], {
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Marketing & Broadcast Consent */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <label className="text-[11px] font-bold uppercase tracking-wider text-gray-400">
                      Broadcast Consent
                    </label>
                    <span
                      className={`font-semibold px-2 py-0.5 rounded text-[10px] ${
                        selectedConversation.optInStatus === 'OPTED_OUT'
                          ? 'bg-rose-100 text-rose-800'
                          : 'bg-emerald-100 text-emerald-800'
                      }`}
                    >
                      {selectedConversation.optInStatus === 'OPTED_OUT' ? 'Opted Out (STOP)' : 'Subscribed'}
                    </span>
                  </div>
                  <div className="p-3 rounded-xl bg-gray-50 border border-gray-200 space-y-2">
                    <p className="text-[11px] text-gray-500 leading-relaxed">
                      {selectedConversation.optInStatus === 'OPTED_OUT'
                        ? 'Customer unsubscribed via STOP. Bulk marketing broadcasts are blocked.'
                        : 'Customer is active and receives bulk promotional broadcasts.'}
                    </p>
                    <button
                      type="button"
                      disabled={toggleOptMutation.isPending}
                      onClick={() =>
                        toggleOptMutation.mutate({
                          contactId: selectedConversation.contactId,
                          optInStatus:
                            selectedConversation.optInStatus === 'OPTED_OUT'
                              ? 'OPTED_IN'
                              : 'OPTED_OUT',
                        })
                      }
                      className="w-full py-1.5 px-2 text-xs font-semibold rounded-lg border border-gray-300 bg-white text-gray-700 hover:bg-gray-100 transition-colors flex items-center justify-center gap-1.5 shadow-2xs"
                    >
                      {selectedConversation.optInStatus === 'OPTED_OUT' ? (
                        <>
                          <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                          Re-Subscribe Contact
                        </>
                      ) : (
                        <>
                          <ShieldAlert className="w-3.5 h-3.5 text-rose-600" />
                          Mark as Opted Out
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {/* CRM Lead Pipeline Status */}
                {activeContactLead && (
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <label className="text-[11px] font-bold uppercase tracking-wider text-gray-400">
                        Lead Pipeline
                      </label>
                      <button
                        type="button"
                        onClick={() => navigate('/leads')}
                        className="text-[11px] font-semibold text-brand-600 hover:text-brand-700 flex items-center gap-0.5"
                      >
                        CRM Board
                        <ArrowRight className="w-3 h-3" />
                      </button>
                    </div>
                    <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2 text-xs">
                      <div className="flex items-center justify-between">
                        <span className="text-gray-500">Stage</span>
                        <span className="font-semibold text-gray-800">
                          {activeContactLead.stage === 'NEW'
                            ? 'New Inquiry'
                            : activeContactLead.stage === 'IN_DISCUSSION'
                            ? 'In Discussion'
                            : activeContactLead.stage === 'PROPOSAL_SENT'
                            ? 'Quotation Sent'
                            : activeContactLead.stage === 'WON'
                            ? 'Deal Won'
                            : 'Lost'}
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-gray-500">Temperature</span>
                        <span
                          className={`inline-flex items-center gap-1 font-bold px-1.5 py-0.5 rounded text-[10px] ${
                            activeContactLead.temperature === 'HOT'
                              ? 'bg-orange-100 text-orange-700'
                              : activeContactLead.temperature === 'WARM'
                              ? 'bg-amber-100 text-amber-700'
                              : 'bg-slate-200 text-slate-700'
                          }`}
                        >
                          {activeContactLead.temperature === 'HOT' && <Flame className="w-3 h-3 text-orange-600" />}
                          {activeContactLead.temperature} ({activeContactLead.score} pts)
                        </span>
                      </div>
                      {activeContactLead.dealValue != null && activeContactLead.dealValue > 0 && (
                        <div className="flex items-center justify-between pt-1 border-t border-slate-200">
                          <span className="text-gray-500">Deal Value</span>
                          <span className="font-bold text-emerald-700 flex items-center">
                            <IndianRupee className="w-3 h-3" />
                            {activeContactLead.dealValue.toLocaleString('en-IN')}
                          </span>
                        </div>
                      )}
                      {activeContactLead.requirementSummary && (
                        <div className="pt-1.5 border-t border-slate-200">
                          <p className="text-[11px] text-gray-600 line-clamp-2 italic">
                            "{activeContactLead.requirementSummary}"
                          </p>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* AI Assistant Context */}
                <div className="space-y-2">
                  <label className="text-[11px] font-bold uppercase tracking-wider text-gray-400">
                    AI Automation
                  </label>
                  <div className="p-3 rounded-xl bg-purple-50/50 border border-purple-100 space-y-1 text-xs">
                    <div className="flex items-center gap-1.5 font-semibold text-purple-900">
                      <Sparkles className="w-3.5 h-3.5 text-purple-600" />
                      <span>Context Memory Enabled</span>
                    </div>
                    <p className="text-[11px] text-purple-700 leading-relaxed">
                      Gemini reviews the last 8 customer messages to remember previous queries and respond accurately.
                    </p>
                  </div>
                </div>

                {/* Quick Actions */}
                <div className="space-y-2 pt-2 border-t border-gray-100">
                  <label className="text-[11px] font-bold uppercase tracking-wider text-gray-400">
                    Quick Navigation
                  </label>
                  <button
                    type="button"
                    onClick={() => navigate('/leads')}
                    className="w-full flex items-center justify-between p-2.5 text-xs text-brand-700 bg-brand-50 hover:bg-brand-100 rounded-xl border border-brand-200 font-semibold transition-colors"
                  >
                    <span className="flex items-center gap-2">
                      <Target className="w-3.5 h-3.5 text-brand-600" />
                      View Leads CRM Pipeline
                    </span>
                    <ArrowRight className="w-3.5 h-3.5 text-brand-600" />
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate('/contacts')}
                    className="w-full flex items-center justify-between p-2.5 text-xs text-gray-700 bg-gray-50 hover:bg-gray-100 rounded-xl border border-gray-200 font-medium transition-colors"
                  >
                    <span className="flex items-center gap-2">
                      <Users className="w-3.5 h-3.5 text-gray-500" />
                      View in Contacts Directory
                    </span>
                    <ExternalLink className="w-3.5 h-3.5 text-gray-400" />
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate('/faq')}
                    className="w-full flex items-center justify-between p-2.5 text-xs text-gray-700 bg-gray-50 hover:bg-gray-100 rounded-xl border border-gray-200 font-medium transition-colors"
                  >
                    <span className="flex items-center gap-2">
                      <Bot className="w-3.5 h-3.5 text-gray-500" />
                      Edit Knowledge Base / FAQs
                    </span>
                    <ExternalLink className="w-3.5 h-3.5 text-gray-400" />
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default InboxScreen;
