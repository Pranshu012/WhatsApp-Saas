import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { DashboardStatsResponse } from '../../api/types';
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

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-5 border-b border-gray-200">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900">Workspace Dashboard</h1>
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-brand-100 text-brand-800">
              {stats?.currentMonth || 'Current Month'}
            </span>
          </div>
          <p className="text-sm text-gray-500">
            Real-time monthly WhatsApp message volume, category usage, and delivery analytics.
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
          <p className="text-xs text-gray-400">All inbound & outbound events</p>
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
            <span>Free Inbound</span>
            <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <MessageSquare className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-black text-emerald-700">{categoryCounts.INBOUND_FREE || 0}</div>
          <p className="text-xs text-emerald-600/80">₹0 Meta charge on customer initiated</p>
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
          <p className="text-xs text-purple-600/80">~₹0.86 / msg tier</p>
        </div>
      </div>

      {/* Breakdown Grids */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Category Breakdown */}
        <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-gray-100">
            <h2 className="font-bold text-gray-900 text-base">Meta Billing Category Breakdown</h2>
            <span className="text-xs text-gray-400 font-medium">{stats?.currentMonth}</span>
          </div>

          <div className="space-y-3.5 text-xs">
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-2.5">
                <span className="w-3 h-3 rounded-full bg-emerald-500" />
                <span className="font-semibold text-gray-800">Inbound Messages (Customer Initiated)</span>
              </div>
              <div className="font-bold text-gray-900 text-sm">{categoryCounts.INBOUND_FREE || 0}</div>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-2.5">
                <span className="w-3 h-3 rounded-full bg-blue-500" />
                <span className="font-semibold text-gray-800">Utility Messages (~₹0.115/msg)</span>
              </div>
              <div className="font-bold text-gray-900 text-sm">{categoryCounts.UTILITY || 0}</div>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-2.5">
                <span className="w-3 h-3 rounded-full bg-purple-500" />
                <span className="font-semibold text-gray-800">Marketing Messages (~₹0.86/msg)</span>
              </div>
              <div className="font-bold text-gray-900 text-sm">{categoryCounts.MARKETING || 0}</div>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-2.5">
                <span className="w-3 h-3 rounded-full bg-indigo-500" />
                <span className="font-semibold text-gray-800">Service Free-Text Replies (24h Window)</span>
              </div>
              <div className="font-bold text-gray-900 text-sm">{categoryCounts.SERVICE || 0}</div>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-2.5">
                <span className="w-3 h-3 rounded-full bg-amber-500" />
                <span className="font-semibold text-gray-800">Authentication / OTP Messages</span>
              </div>
              <div className="font-bold text-gray-900 text-sm">{categoryCounts.AUTHENTICATION || 0}</div>
            </div>
          </div>
        </div>

        {/* Delivery Outcomes */}
        <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-gray-100">
            <h2 className="font-bold text-gray-900 text-base">Delivery Outcome Status</h2>
            <span className="text-xs text-gray-400 font-medium">Outbound Journey</span>
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
              {stats?.note || 'These counts reflect our immutable ledger records. Meta invoices you directly in WhatsApp Manager.'}
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
          <h3 className="font-bold text-gray-900 text-sm">Configure Automation</h3>
          <p className="text-xs text-gray-500 mt-1">Set 24/7 instant replies for keywords</p>
        </button>

        <button
          type="button"
          onClick={() => navigate('/faq')}
          className="p-5 rounded-2xl bg-white border border-gray-200 hover:border-brand-300 hover:shadow-sm transition-all text-left group"
        >
          <HelpCircle className="w-6 h-6 text-blue-600 mb-2 group-hover:scale-110 transition-transform" />
          <h3 className="font-bold text-gray-900 text-sm">FAQ Knowledge Base</h3>
          <p className="text-xs text-gray-500 mt-1">Typo-tolerant automated question answering</p>
        </button>

        <button
          type="button"
          onClick={() => navigate('/whatsapp')}
          className="p-5 rounded-2xl bg-white border border-gray-200 hover:border-brand-300 hover:shadow-sm transition-all text-left group"
        >
          <ShieldCheck className="w-6 h-6 text-green-600 mb-2 group-hover:scale-110 transition-transform" />
          <h3 className="font-bold text-gray-900 text-sm">WhatsApp Account Health</h3>
          <p className="text-xs text-gray-500 mt-1">Check quality score & messaging limits</p>
        </button>
      </div>
    </div>
  );
};
export default DashboardScreen;
