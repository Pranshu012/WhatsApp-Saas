import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, Link } from 'react-router-dom';
import { apiClient } from '../../api/client';
import {
  DashboardStatsResponse,
  FaqResponse,
  WhatsAppAccountResponse,
  AutomationRuleResponse,
  TestFaqResponse,
} from '../../api/types';
import { ErrorState } from '../../components/ErrorState';
import { Skeleton } from '../../components/Skeleton';
import {
  MessageSquare,
  ShieldCheck,
  Zap,
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  HelpCircle,
  ArrowRight,
  Sparkles,
  Bot,
  Send,
  Smartphone,
  Plus,
  Loader2,
  Clock,
  ExternalLink,
} from 'lucide-react';

export const DashboardScreen: React.FC = () => {
  const navigate = useNavigate();

  // Quick Tester State on Dashboard
  const [testQuery, setTestQuery] = useState('');
  const [testResult, setTestResult] = useState<TestFaqResponse | null>(null);
  const [isTesting, setIsTesting] = useState(false);

  const {
    data: stats,
    isLoading: statsLoading,
    isError,
    error,
    refetch,
  } = useQuery<DashboardStatsResponse>({
    queryKey: ['dashboard-stats'],
    queryFn: () => apiClient<DashboardStatsResponse>('/api/dashboard/stats'),
    refetchInterval: 15000,
  });

  const { data: account } = useQuery<WhatsAppAccountResponse>({
    queryKey: ['whatsapp-account'],
    queryFn: () => apiClient<WhatsAppAccountResponse>('/api/whatsapp/account'),
    retry: false,
  });

  const { data: faqs } = useQuery<FaqResponse[]>({
    queryKey: ['faqs'],
    queryFn: () => apiClient<FaqResponse[]>('/api/faqs'),
  });

  const { data: subscription } = useQuery<any>({
    queryKey: ['subscription'],
    queryFn: () => apiClient<any>('/api/subscription'),
  });

  const { data: rules } = useQuery<AutomationRuleResponse[]>({
    queryKey: ['automation-rules'],
    queryFn: () => apiClient<AutomationRuleResponse[]>('/api/automation-rules'),
  });

  const handleQuickTest = async (e: React.FormEvent) => {
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

  if (statsLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-72" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Skeleton className="h-32 rounded-3xl" />
          <Skeleton className="h-32 rounded-3xl" />
          <Skeleton className="h-32 rounded-3xl" />
        </div>
        <Skeleton className="h-64 rounded-3xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Unable to load dashboard"
        message={(error as any)?.message || 'Failed to fetch usage metrics.'}
        onRetry={() => refetch()}
      />
    );
  }

  const isConnected = account?.status === 'CONNECTED';
  const activeFaqs = faqs?.filter((f) => f.enabled) || [];
  const activeRules = rules?.filter((r) => r.enabled) || [];
  const totalAutomations = activeFaqs.length + activeRules.length;

  // Estimated Savings calculation
  const totalInquiries = stats?.categoryCounts?.INBOUND_FREE || 0;
  const estimatedHoursSaved = Math.max(1, Math.round((totalInquiries * 3.5) / 60));
  const estimatedRupeesSaved = Math.max(250, estimatedHoursSaved * 250);

  return (
    <div className="space-y-8">
      {/* 1. Welcome & Status Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <div className="flex flex-wrap items-center gap-2.5">
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">
              Business Control Center
            </h1>
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              Bot Active 24/7
            </span>

            {subscription && (
              <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ${
                subscription.status === 'TRIALING'
                  ? 'bg-amber-50 text-amber-900 border-amber-200'
                  : subscription.status === 'ACTIVE'
                  ? 'bg-blue-50 text-blue-900 border-blue-200'
                  : 'bg-rose-50 text-rose-900 border-rose-200'
              }`}>
                <Clock className="w-3.5 h-3.5" />
                {subscription.status === 'TRIALING'
                  ? `Free Trial: ${subscription.daysRemaining} Days Left`
                  : subscription.status === 'ACTIVE'
                  ? 'All-in-One Plan Active (₹499/mo)'
                  : 'Subscription Inactive'}
              </span>
            )}
          </div>
          <p className="text-sm text-slate-600 mt-1">
            Manage your automatic replies, customer chats, and broadcast campaigns in one place.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start sm:self-auto">
          <button
            type="button"
            onClick={() => navigate('/guide')}
            className="inline-flex items-center gap-1.5 px-4 py-2.5 text-xs font-bold text-emerald-800 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-xl min-h-[44px] transition-all"
          >
            <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
            Setup Guide
          </button>
          <button
            type="button"
            onClick={() => navigate('/inbox')}
            className="inline-flex items-center gap-2 px-5 py-2.5 text-sm font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl min-h-[44px] shadow-md shadow-brand-500/20 transition-all"
          >
            <MessageSquare className="w-4 h-4" />
            Open Customer Inbox
          </button>
        </div>
      </div>

      {/* Payment Method Missing Warning on Dashboard (Blocker for outbound sends) */}
      {isConnected && account && account.paymentMethodAttached === false && (
        <div className="bg-rose-50 border-2 border-rose-300 rounded-3xl p-5 sm:p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-sm animate-in fade-in">
          <div className="flex items-start gap-3.5">
            <div className="w-10 h-10 rounded-2xl bg-rose-600 text-white flex items-center justify-center shrink-0 shadow-md shadow-rose-600/20 mt-0.5">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-rose-950">
                Action Required: Add Payment Card to Meta WhatsApp Manager
              </h3>
              <p className="text-xs text-rose-800 mt-0.5 leading-relaxed">
                Meta requires a payment card attached to your WhatsApp Business Account. Without this, Meta will reject your customer replies and marketing broadcasts.
              </p>
            </div>
          </div>
          <a
            href="https://business.facebook.com/wa/manage/phone-numbers/"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-xl min-h-[44px] shadow-sm shrink-0 transition-all"
          >
            Attach Card in Meta
            <ArrowRight className="w-3.5 h-3.5" />
          </a>
        </div>
      )}

      {/* Non-Tech Setup Banner */}
      {(!isConnected || totalAutomations < 3) && (
        <div className="bg-gradient-to-r from-emerald-500/10 via-teal-500/10 to-brand-500/10 border border-emerald-300/80 rounded-3xl p-5 sm:p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-sm">
          <div className="flex items-start gap-3.5">
            <div className="w-10 h-10 rounded-2xl bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-md shadow-emerald-600/20 mt-0.5">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                New here? Set up your WhatsApp bot in 4 easy steps
              </h3>
              <p className="text-xs text-slate-600 mt-0.5 leading-relaxed">
                Connect your number, add 3 common questions, and test your bot. No coding or tech knowledge needed!
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => navigate('/guide')}
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-md transition-all self-start sm:self-auto shrink-0 min-h-[40px]"
          >
            <span>Open Quick Setup Guide</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* 2. Top Status & Quick Action Hub (3 Hero Cards) */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {/* Card 1: WhatsApp Connection Status */}
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4 hover:border-brand-300 transition-all">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="w-10 h-10 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <Smartphone className="w-5 h-5" />
              </div>
              <span
                className={`text-[11px] font-black uppercase tracking-wider px-2.5 py-0.5 rounded-full ${
                  isConnected ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                }`}
              >
                {isConnected ? 'Connected' : 'Setup Required'}
              </span>
            </div>
            <h3 className="text-base font-bold text-slate-900">WhatsApp Business Number</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              {isConnected
                ? `Active Number: ${account?.displayPhoneNumber || 'Official Account'}`
                : 'Connect your business number through official Meta Cloud API to start auto-replying.'}
            </p>
          </div>
          <button
            type="button"
            onClick={() => navigate('/whatsapp')}
            className="inline-flex items-center justify-between w-full py-2.5 px-4 bg-slate-50 hover:bg-emerald-50 text-slate-700 hover:text-emerald-700 border border-slate-200 hover:border-emerald-200 rounded-xl text-xs font-bold transition-all"
          >
            <span>{isConnected ? 'Manage WhatsApp Setup' : 'Connect WhatsApp Number'}</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Card 2: Auto-Replies & FAQ Knowledge */}
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4 hover:border-brand-300 transition-all">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="w-10 h-10 rounded-2xl bg-brand-50 text-brand-600 flex items-center justify-center">
                <Bot className="w-5 h-5" />
              </div>
              <span className="text-sm font-black text-brand-700 font-mono">
                {totalAutomations} Active
              </span>
            </div>
            <h3 className="text-base font-bold text-slate-900">Automated Bot Replies</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              {totalAutomations > 0
                ? `Your bot is ready with ${activeFaqs.length} Q&As and ${activeRules.length} keyword triggers.`
                : 'Add answers for timings, price list, address, and refund policy.'}
            </p>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => navigate('/faq')}
              className="flex-1 inline-flex items-center justify-center gap-1 py-2.5 px-3 bg-brand-50 hover:bg-brand-100 text-brand-700 rounded-xl text-xs font-bold transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              Add FAQ
            </button>
            <button
              type="button"
              onClick={() => navigate('/automation')}
              className="flex-1 inline-flex items-center justify-center gap-1 py-2.5 px-3 bg-slate-50 hover:bg-slate-100 text-slate-700 rounded-xl text-xs font-bold transition-colors"
            >
              Keyword Rules
            </button>
          </div>
        </div>

        {/* Card 3: Time & Cost Savings */}
        <div className="bg-gradient-to-br from-slate-900 to-slate-950 text-white p-6 rounded-3xl shadow-xl flex flex-col justify-between space-y-4">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="w-10 h-10 rounded-2xl bg-white/10 text-brand-400 flex items-center justify-center">
                <Sparkles className="w-5 h-5" />
              </div>
              <span className="text-[10px] font-bold text-emerald-400 uppercase tracking-wider bg-emerald-950/60 px-2 py-0.5 rounded-full border border-emerald-500/30">
                Estimated ROI
              </span>
            </div>
            <h3 className="text-base font-bold text-white">Time & Staff Savings</h3>
            <div className="pt-1">
              <div className="text-2xl font-black text-white font-mono">
                ~₹{estimatedRupeesSaved.toLocaleString()}
                <span className="text-xs font-normal text-slate-400"> saved</span>
              </div>
              <p className="text-xs text-brand-300 mt-0.5">
                Saved ~{estimatedHoursSaved} hours of manual typing this month.
              </p>
            </div>
          </div>
          <div className="text-[11px] text-slate-400 border-t border-slate-800 pt-2 flex items-center justify-between">
            <span>⚡ Instant &lt;1s replies</span>
            <span>🔒 Official Meta SLA</span>
          </div>
        </div>
      </div>

      {/* 3. Live Bot Simulator on Dashboard */}
      <div className="bg-slate-900 text-white p-6 sm:p-7 rounded-3xl shadow-xl border border-slate-800 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
              <Bot className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">
                Live Bot Simulator — Test What Your Customers See
              </h2>
              <p className="text-xs text-slate-400">
                Type any customer question to test how your bot replies before real customers message on WhatsApp.
              </p>
            </div>
          </div>
          <Link
            to="/faq"
            className="text-xs text-emerald-400 hover:text-emerald-300 font-bold flex items-center gap-1 self-start sm:self-auto"
          >
            Manage Knowledge Base
            <ExternalLink className="w-3.5 h-3.5" />
          </Link>
        </div>

        <form onSubmit={handleQuickTest} className="flex flex-col sm:flex-row gap-3 pt-2">
          <input
            type="text"
            value={testQuery}
            onChange={(e) => setTestQuery(e.target.value)}
            placeholder="Try typing: 'What are your store hours?', 'wat r ur prices', or 'refund rules'"
            className="flex-1 bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-400 min-h-[44px] focus:outline-none focus:ring-2 focus:ring-emerald-400"
          />
          <button
            type="submit"
            disabled={isTesting || !testQuery.trim()}
            className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white font-bold rounded-xl min-h-[44px] flex items-center justify-center gap-2 transition-all shrink-0"
          >
            {isTesting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Testing...
              </>
            ) : (
              <>
                <Send className="w-4 h-4" />
                Test Reply
              </>
            )}
          </button>
        </form>

        {testResult && (
          <div className="pt-2">
            {testResult.id && testResult.isConfident ? (
              <div className="p-4 rounded-2xl bg-emerald-950/80 border border-emerald-500/40 text-emerald-100 space-y-1.5">
                <div className="flex items-center justify-between text-xs font-semibold">
                  <span className="flex items-center gap-1.5 text-emerald-400 font-bold">
                    <CheckCircle2 className="w-4 h-4" />
                    Bot Response (Matched: &ldquo;{testResult.question}&rdquo;)
                  </span>
                  <span className="bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded text-[11px] font-mono">
                    {Math.round(testResult.confidenceScore * 100)}% Accuracy
                  </span>
                </div>
                <div className="text-xs bg-slate-900/60 p-3 rounded-xl border border-emerald-500/20 mt-1">
                  <strong>💬 Sent to Customer:</strong> &ldquo;{testResult.answer}&rdquo;
                </div>
              </div>
            ) : (
              <div className="p-4 rounded-2xl bg-amber-950/80 border border-amber-500/40 text-amber-200 space-y-1.5">
                <div className="flex items-center gap-1.5 text-xs font-bold text-amber-400">
                  <AlertCircle className="w-4 h-4" />
                  No automated answer found for this question
                </div>
                <p className="text-xs text-amber-200/90 leading-relaxed">
                  This question will appear in your <strong>&ldquo;Needs Your Reply&rdquo;</strong> inbox for human handover, or you can add it now in{' '}
                  <Link to="/faq" className="underline font-bold text-amber-300">
                    Common Questions
                  </Link>
                  .
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* 4. Monthly Messaging Summary & Performance */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Left: Message Volume by Category */}
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div>
              <h2 className="font-bold text-slate-900 text-base">Monthly Activity Overview</h2>
              <p className="text-xs text-slate-500">Official message categories tracked this month</p>
            </div>
            <span className="text-xs font-bold text-brand-700 bg-brand-50 px-2.5 py-1 rounded-full border border-brand-200">
              {stats?.currentMonth || 'Current Month'}
            </span>
          </div>

          <div className="space-y-3 text-xs">
            <div className="flex items-center justify-between p-3.5 rounded-2xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-3">
                <span className="w-3.5 h-3.5 rounded-full bg-emerald-500" />
                <div>
                  <p className="font-bold text-slate-900">Inbound Customer Inquiries</p>
                  <p className="text-[11px] text-slate-500">Messages started by your customers (100% Free)</p>
                </div>
              </div>
              <span className="text-base font-black text-slate-900 font-mono">
                {stats?.categoryCounts?.INBOUND_FREE || 0}
              </span>
            </div>

            <div className="flex items-center justify-between p-3.5 rounded-2xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-3">
                <span className="w-3.5 h-3.5 rounded-full bg-blue-500" />
                <div>
                  <p className="font-bold text-slate-900">Order & Utility Updates</p>
                  <p className="text-[11px] text-slate-500">Confirmations & receipts (~₹0.115/msg)</p>
                </div>
              </div>
              <span className="text-base font-black text-slate-900 font-mono">
                {stats?.categoryCounts?.UTILITY || 0}
              </span>
            </div>

            <div className="flex items-center justify-between p-3.5 rounded-2xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-3">
                <span className="w-3.5 h-3.5 rounded-full bg-purple-500" />
                <div>
                  <p className="font-bold text-slate-900">Promotional & Marketing Offers</p>
                  <p className="text-[11px] text-slate-500">Discounts & festival campaigns (~₹0.86/msg)</p>
                </div>
              </div>
              <span className="text-base font-black text-slate-900 font-mono">
                {stats?.categoryCounts?.MARKETING || 0}
              </span>
            </div>
          </div>
        </div>

        {/* Right: Quick Navigation Hub */}
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div>
              <h2 className="font-bold text-slate-900 text-base">Quick Action Shortcuts</h2>
              <p className="text-xs text-slate-500">Jump directly into any tool</p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => navigate('/inbox')}
              className="p-4 rounded-2xl bg-slate-50 hover:bg-brand-50 border border-slate-200 hover:border-brand-200 transition-all text-left group"
            >
              <MessageSquare className="w-5 h-5 text-brand-600 mb-1.5 group-hover:scale-110 transition-transform" />
              <h3 className="font-bold text-xs text-slate-900">Live Customer Chats</h3>
              <p className="text-[11px] text-slate-500 mt-0.5">Reply within 24h free window</p>
            </button>

            <button
              type="button"
              onClick={() => navigate('/faq')}
              className="p-4 rounded-2xl bg-slate-50 hover:bg-emerald-50 border border-slate-200 hover:border-emerald-200 transition-all text-left group"
            >
              <HelpCircle className="w-5 h-5 text-emerald-600 mb-1.5 group-hover:scale-110 transition-transform" />
              <h3 className="font-bold text-xs text-slate-900">Common Questions</h3>
              <p className="text-[11px] text-slate-500 mt-0.5">Add store hours & prices</p>
            </button>

            <button
              type="button"
              onClick={() => navigate('/automation')}
              className="p-4 rounded-2xl bg-slate-50 hover:bg-amber-50 border border-slate-200 hover:border-amber-200 transition-all text-left group"
            >
              <Zap className="w-5 h-5 text-amber-600 mb-1.5 group-hover:scale-110 transition-transform" />
              <h3 className="font-bold text-xs text-slate-900">Keyword Auto-Replies</h3>
              <p className="text-[11px] text-slate-500 mt-0.5">Trigger words like MENU</p>
            </button>

            <button
              type="button"
              onClick={() => navigate('/templates')}
              className="p-4 rounded-2xl bg-slate-50 hover:bg-blue-50 border border-slate-200 hover:border-blue-200 transition-all text-left group"
            >
              <ShieldCheck className="w-5 h-5 text-blue-600 mb-1.5 group-hover:scale-110 transition-transform" />
              <h3 className="font-bold text-xs text-slate-900">Message Templates</h3>
              <p className="text-[11px] text-slate-500 mt-0.5">Approved Meta templates</p>
            </button>
          </div>

          <div className="p-4 rounded-2xl bg-emerald-50/60 border border-emerald-100 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-emerald-700 shrink-0" />
              <span className="text-emerald-950 font-medium">
                Want to schedule a festival message?
              </span>
            </div>
            <Link
              to="/scheduled"
              className="font-bold text-emerald-700 hover:text-emerald-800 underline"
            >
              Schedule Now
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
export default DashboardScreen;
