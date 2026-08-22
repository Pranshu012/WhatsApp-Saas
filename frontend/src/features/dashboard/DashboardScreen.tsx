import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { DashboardStatsResponse, FaqResponse, WhatsAppAccountResponse } from '../../api/types';
import { ErrorState } from '../../components/ErrorState';
import { Skeleton } from '../../components/Skeleton';
import {
  MessageSquare,
  TrendingUp,
  ShieldCheck,
  Zap,
  CheckCircle2,
  Clock,
  AlertCircle,
  Eye,
  Info,
  HelpCircle,
  ArrowRight,
} from 'lucide-react';

export const DashboardScreen: React.FC = () => {
  const navigate = useNavigate();

  const {
    data: stats,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<DashboardStatsResponse>({
    queryKey: ['dashboard-stats'],
    queryFn: () => apiClient<DashboardStatsResponse>('/api/dashboard/stats'),
    refetchInterval: 15000, // Refresh every 15s
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

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="pb-5 border-b border-gray-200">
          <Skeleton className="h-8 w-64 mb-2" />
          <Skeleton className="h-4 w-96" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Skeleton className="h-32 rounded-2xl" />
          <Skeleton className="h-32 rounded-2xl" />
          <Skeleton className="h-32 rounded-2xl" />
          <Skeleton className="h-32 rounded-2xl" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Skeleton className="h-64 rounded-2xl" />
          <Skeleton className="h-64 rounded-2xl" />
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Unable to load dashboard metrics"
        message={(error as any)?.message || 'Failed to fetch usage metrics from message ledger.'}
        onRetry={() => refetch()}
      />
    );
  }

  const categoryCounts = stats?.categoryCounts || {
    MARKETING: 0,
    UTILITY: 0,
    AUTHENTICATION: 0,
    SERVICE: 0,
    INBOUND_FREE: 0,
  };

  const deliveryOutcomes = stats?.deliveryOutcomes || {
    INTENT: 0,
    SENT: 0,
    DELIVERED: 0,
    READ: 0,
    FAILED: 0,
  };
  const connected = account?.status === 'CONNECTED';
  const faqCount = faqs?.filter((faq) => faq.enabled).length || 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-5 border-b border-gray-200">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900">Your WhatsApp Business Home</h1>
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-brand-100 text-brand-800">
              {stats?.currentMonth || 'Current Month'}
            </span>
          </div>
          <p className="text-sm text-gray-500">
            See your customer messages and set up automatic replies in one place.
          </p>
        </div>

        <button
          type="button"
          onClick={() => navigate('/inbox')}
          className="inline-flex items-center justify-center gap-2 px-4 py-2 text-xs font-semibold text-white bg-brand-600 hover:bg-brand-700 rounded-lg min-h-[44px] shadow-sm transition-colors self-start sm:self-auto"
        >
          <MessageSquare className="w-4 h-4" />
          Open Inbox
        </button>
      </div>

      {(!connected || faqCount < 3) && (
        <section className="bg-brand-50 border border-brand-200 rounded-2xl p-5 sm:p-6">
          <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
            <div>
              <h2 className="font-bold text-gray-900">Start here: set up your automatic replies</h2>
              <p className="text-sm text-gray-600 mt-1">Complete these small steps so customers can get help even when you are busy.</p>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mt-4">
            <SetupStep done={connected} number="1" title="Connect WhatsApp" description="Link your business number through Meta." onClick={() => navigate('/whatsapp')} />
            <SetupStep done={faqCount >= 3} number="2" title="Add 3 common questions" description={`${faqCount}/3 added — start with price, timings and location.`} onClick={() => navigate('/faq')} />
            <SetupStep done={false} number="3" title="Test an auto reply" description="Send a test message after the first two steps." onClick={() => navigate('/automation')} />
          </div>
        </section>
      )}

      {/* Top Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Volume */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm space-y-2">
          <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase tracking-wider">
            <span>Total Messages</span>
            <div className="w-7 h-7 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-black text-gray-900">{stats?.totalMessages || 0}</div>
          <p className="text-xs text-gray-400">Customer messages and your replies</p>
        </div>

        {/* Delivery Rate */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm space-y-2">
          <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase tracking-wider">
            <span>Delivery Rate</span>
            <div className="w-7 h-7 rounded-lg bg-green-50 text-green-600 flex items-center justify-center">
              <ShieldCheck className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-black text-green-600">{stats?.deliveryRatePercent || 100}%</div>
          <p className="text-xs text-gray-400">Delivered / read successfully</p>
        </div>

        {/* Inbound Inquiries */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm space-y-2">
          <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase tracking-wider">
            <span>Customer Messages</span>
            <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <MessageSquare className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-black text-emerald-700">{categoryCounts.INBOUND_FREE || 0}</div>
          <p className="text-xs text-emerald-600/80">Messages started by customers</p>
        </div>

        {/* Marketing Sends */}
        <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm space-y-2">
          <div className="flex items-center justify-between text-gray-500 text-xs font-semibold uppercase tracking-wider">
            <span>Marketing Sent</span>
            <div className="w-7 h-7 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
              <Zap className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-black text-purple-700">{categoryCounts.MARKETING || 0}</div>
          <p className="text-xs text-purple-600/80">Promotional messages sent</p>
        </div>
      </div>

      {/* Breakdown Grids */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Category Breakdown */}
        <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-gray-100">
            <h2 className="font-bold text-gray-900 text-base">Message types this month</h2>
            <span className="text-xs text-gray-400 font-medium">{stats?.currentMonth}</span>
          </div>

          <div className="space-y-3.5 text-xs">
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-2.5">
                <span className="w-3 h-3 rounded-full bg-emerald-500" />
                <span className="font-semibold text-gray-800">Customer started the conversation</span>
              </div>
              <div className="font-bold text-gray-900 text-sm">{categoryCounts.INBOUND_FREE || 0}</div>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-2.5">
                <span className="w-3 h-3 rounded-full bg-blue-500" />
                <span className="font-semibold text-gray-800">Useful updates (for example, order updates)</span>
              </div>
              <div className="font-bold text-gray-900 text-sm">{categoryCounts.UTILITY || 0}</div>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-2.5">
                <span className="w-3 h-3 rounded-full bg-purple-500" />
                <span className="font-semibold text-gray-800">Promotional messages</span>
              </div>
              <div className="font-bold text-gray-900 text-sm">{categoryCounts.MARKETING || 0}</div>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-2.5">
                <span className="w-3 h-3 rounded-full bg-indigo-500" />
                <span className="font-semibold text-gray-800">Your replies within 24 hours</span>
              </div>
              <div className="font-bold text-gray-900 text-sm">{categoryCounts.SERVICE || 0}</div>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-2.5">
                <span className="w-3 h-3 rounded-full bg-amber-500" />
                <span className="font-semibold text-gray-800">Login and verification messages</span>
              </div>
              <div className="font-bold text-gray-900 text-sm">{categoryCounts.AUTHENTICATION || 0}</div>
            </div>
          </div>
        </div>

        {/* Delivery Outcomes */}
        <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-gray-100">
            <h2 className="font-bold text-gray-900 text-base">Did customers receive your messages?</h2>
            <span className="text-xs text-gray-400 font-medium">This month</span>
          </div>

          <div className="grid grid-cols-2 gap-3 text-xs">
            <div className="p-4 rounded-xl bg-green-50 border border-green-100 space-y-1">
              <div className="flex items-center gap-1.5 text-green-700 font-semibold">
                <CheckCircle2 className="w-4 h-4" />
                Delivered
              </div>
              <div className="text-xl font-black text-green-900">{deliveryOutcomes.DELIVERED || 0}</div>
            </div>

            <div className="p-4 rounded-xl bg-blue-50 border border-blue-100 space-y-1">
              <div className="flex items-center gap-1.5 text-blue-700 font-semibold">
                <Eye className="w-4 h-4" />
                Read by Customer
              </div>
              <div className="text-xl font-black text-blue-900">{deliveryOutcomes.READ || 0}</div>
            </div>

            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
              <div className="flex items-center gap-1.5 text-slate-700 font-semibold">
                <Clock className="w-4 h-4" />
                Sent / In Transit
              </div>
              <div className="text-xl font-black text-slate-900">{deliveryOutcomes.SENT || 0}</div>
            </div>

            <div className="p-4 rounded-xl bg-red-50 border border-red-100 space-y-1">
              <div className="flex items-center gap-1.5 text-red-700 font-semibold">
                <AlertCircle className="w-4 h-4" />
                Failed Delivery
              </div>
              <div className="text-xl font-black text-red-900">{deliveryOutcomes.FAILED || 0}</div>
            </div>
          </div>

          {/* Quick Guidance Alert */}
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex items-start gap-2.5 text-xs text-slate-600">
            <Info className="w-4 h-4 text-slate-500 shrink-0 mt-0.5" />
            <p className="leading-relaxed">
              {stats?.note || 'These are the messages recorded in this app. Check WhatsApp Manager for Meta’s official bill.'}
            </p>
          </div>
        </div>
      </div>

      {/* Quick Access Actions */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
        <button
          type="button"
          onClick={() => navigate('/automation')}
          className="p-5 rounded-2xl bg-white border border-gray-200 hover:border-brand-300 hover:shadow-sm transition-all text-left group"
        >
          <Zap className="w-6 h-6 text-brand-600 mb-2 group-hover:scale-110 transition-transform" />
          <h3 className="font-bold text-gray-900 text-sm">Set up auto replies</h3>
          <p className="text-xs text-gray-500 mt-1">Reply instantly when customers use words you choose</p>
        </button>

        <button
          type="button"
          onClick={() => navigate('/faq')}
          className="p-5 rounded-2xl bg-white border border-gray-200 hover:border-brand-300 hover:shadow-sm transition-all text-left group"
        >
          <HelpCircle className="w-6 h-6 text-blue-600 mb-2 group-hover:scale-110 transition-transform" />
          <h3 className="font-bold text-gray-900 text-sm">Add common questions</h3>
          <p className="text-xs text-gray-500 mt-1">Save answers for price, timings, address and more</p>
        </button>

        <button
          type="button"
          onClick={() => navigate('/whatsapp')}
          className="p-5 rounded-2xl bg-white border border-gray-200 hover:border-brand-300 hover:shadow-sm transition-all text-left group"
        >
          <ShieldCheck className="w-6 h-6 text-green-600 mb-2 group-hover:scale-110 transition-transform" />
          <h3 className="font-bold text-gray-900 text-sm">Connect WhatsApp</h3>
          <p className="text-xs text-gray-500 mt-1">Check your number, message limit and connection status</p>
        </button>
      </div>
    </div>
  );
};

const SetupStep: React.FC<{ done: boolean; number: string; title: string; description: string; onClick: () => void }> = ({ done, number, title, description, onClick }) => (
  <button type="button" onClick={onClick} className="text-left bg-white rounded-xl border border-brand-100 p-4 hover:border-brand-300 hover:shadow-sm transition-all">
    <div className="flex items-center gap-2">
      <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${done ? 'bg-green-500 text-white' : 'bg-brand-100 text-brand-700'}`}>{done ? '✓' : number}</span>
      <span className="font-semibold text-sm text-gray-900">{title}</span>
      {!done && <ArrowRight className="w-4 h-4 ml-auto text-brand-600" />}
    </div>
    <p className="text-xs text-gray-500 mt-2 leading-relaxed">{description}</p>
  </button>
);
export default DashboardScreen;
