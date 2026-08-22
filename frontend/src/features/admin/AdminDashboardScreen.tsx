import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import {
  AdminPlatformStatsDto,
  AdminTenantDto,
  ActivateTenantRequest,
  ExtendSubscriptionRequest,
  SuspendTenantRequest,
} from '../../api/types';
import {
  Building2,
  CheckCircle2,
  AlertTriangle,
  MessageSquare,
  Smartphone,
  Search,
  Shield,
  Filter,
  Play,
  Pause,
  PlusCircle,
  TrendingUp,
  RefreshCw,
  X,
} from 'lucide-react';
import { Skeleton } from '../../components/Skeleton';
import { ErrorState } from '../../components/ErrorState';

export const AdminDashboardScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'TRIALING' | 'SUSPENDED'>('ALL');
  
  // Selected tenant for detail modal
  const [selectedTenant, setSelectedTenant] = useState<AdminTenantDto | null>(null);
  const [actionType, setActionType] = useState<'ACTIVATE' | 'EXTEND' | 'SUSPEND' | null>(null);

  // Form states for modal
  const [planType, setPlanType] = useState<string>('BUSINESS_499');
  const [durationDays, setDurationDays] = useState<number>(30);
  const [extraDays, setExtraDays] = useState<number>(30);
  const [notes, setNotes] = useState<string>('');
  const [suspendReason, setSuspendReason] = useState<string>('Payment past due');

  // 1. Platform Metrics Query
  const {
    data: stats,
    isLoading: statsLoading,
    refetch: refetchStats,
  } = useQuery<AdminPlatformStatsDto>({
    queryKey: ['admin-stats'],
    queryFn: () => apiClient<AdminPlatformStatsDto>('/api/admin/stats'),
    refetchInterval: 20000,
  });

  // 2. All Tenants Query
  const {
    data: tenants,
    isLoading: tenantsLoading,
    isError,
    error,
    refetch: refetchTenants,
  } = useQuery<AdminTenantDto[]>({
    queryKey: ['admin-tenants'],
    queryFn: () => apiClient<AdminTenantDto[]>('/api/admin/tenants'),
    refetchInterval: 15000,
  });

  // Mutations
  const activateMutation = useMutation({
    mutationFn: ({ tenantId, req }: { tenantId: string; req: ActivateTenantRequest }) =>
      apiClient<AdminTenantDto>(`/api/admin/tenants/${tenantId}/activate`, {
        method: 'POST',
        body: JSON.stringify(req),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      closeModal();
    },
  });

  const extendMutation = useMutation({
    mutationFn: ({ tenantId, req }: { tenantId: string; req: ExtendSubscriptionRequest }) =>
      apiClient<AdminTenantDto>(`/api/admin/tenants/${tenantId}/extend`, {
        method: 'POST',
        body: JSON.stringify(req),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      closeModal();
    },
  });

  const suspendMutation = useMutation({
    mutationFn: ({ tenantId, req }: { tenantId: string; req: SuspendTenantRequest }) =>
      apiClient<AdminTenantDto>(`/api/admin/tenants/${tenantId}/suspend`, {
        method: 'POST',
        body: JSON.stringify(req),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      closeModal();
    },
  });

  const closeModal = () => {
    setActionType(null);
    setSelectedTenant(null);
    setNotes('');
  };

  const filteredTenants = (tenants || []).filter((t) => {
    const matchesSearch =
      t.businessName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      t.slug.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (t.ownerEmail && t.ownerEmail.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (t.displayPhoneNumber && t.displayPhoneNumber.includes(searchTerm));

    if (!matchesSearch) return false;

    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'ACTIVE') return t.subscriptionStatus === 'ACTIVE' && t.status === 'ACTIVE';
    if (statusFilter === 'TRIALING') return t.subscriptionStatus === 'TRIALING';
    if (statusFilter === 'SUSPENDED') return t.status === 'SUSPENDED' || t.subscriptionStatus === 'SUSPENDED';

    return true;
  });

  if (isError) {
    return (
      <div className="max-w-6xl mx-auto py-8 px-4">
        <ErrorState
          title="Admin Access Required"
          message={(error as any)?.message || 'Only Super Administrators can view the admin platform.'}
          onRetry={() => refetchTenants()}
        />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto space-y-8 pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-slate-900 text-white">
              <Shield className="w-5 h-5 text-amber-400" />
            </div>
            <div>
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">
                Platform Admin Dashboard
              </h1>
              <p className="text-xs text-slate-500 font-medium">
                Master control center: Activate customers, track subscriptions & manage system health.
              </p>
            </div>
          </div>
        </div>

        <button
          onClick={() => {
            refetchStats();
            refetchTenants();
          }}
          className="inline-flex items-center gap-2 px-4 py-2.5 text-xs font-bold text-slate-700 bg-white hover:bg-slate-50 border border-slate-200 rounded-xl min-h-[44px] shadow-sm transition-all"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Refresh Data
        </button>
      </div>

      {/* 1. Platform Hero Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 rounded-2xl bg-white border border-slate-200/80 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Total Businesses</span>
            <Building2 className="w-4 h-4 text-brand-600" />
          </div>
          <div className="text-2xl font-black text-slate-900">
            {statsLoading ? <Skeleton className="h-8 w-16" /> : stats?.totalTenants || 0}
          </div>
          <div className="text-[11px] text-slate-500 mt-1 font-medium">
            {stats?.activeTenants || 0} active, {stats?.trialingTenants || 0} on free trial
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-white border border-slate-200/80 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Est. Monthly Revenue</span>
            <TrendingUp className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl font-black text-emerald-600">
            {statsLoading ? (
              <Skeleton className="h-8 w-24" />
            ) : (
              `₹${(stats?.estimatedMonthlyRevenueInr || 0).toLocaleString('en-IN')}`
            )}
          </div>
          <div className="text-[11px] text-slate-500 mt-1 font-medium">
            At flat ₹499/month per active business
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-white border border-slate-200/80 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Live WhatsApp Bots</span>
            <Smartphone className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="text-2xl font-black text-slate-900">
            {statsLoading ? <Skeleton className="h-8 w-16" /> : stats?.totalActiveWhatsAppAccounts || 0}
          </div>
          <div className="text-[11px] text-slate-500 mt-1 font-medium">
            Connected via official Meta Cloud API
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-white border border-slate-200/80 shadow-sm">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Messages This Month</span>
            <MessageSquare className="w-4 h-4 text-blue-600" />
          </div>
          <div className="text-2xl font-black text-slate-900">
            {statsLoading ? <Skeleton className="h-8 w-20" /> : (stats?.totalMessagesThisMonth || 0).toLocaleString('en-IN')}
          </div>
          <div className="text-[11px] text-slate-500 mt-1 font-medium">
            Platform-wide customer chats
          </div>
        </div>
      </div>

      {/* 2. Controls & Search Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-4 rounded-2xl bg-white border border-slate-200 shadow-sm">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search by business name, slug, owner email, or phone number..."
            className="w-full pl-10 pr-4 py-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:border-brand-500 focus:ring-1 focus:ring-brand-500 outline-none"
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400 shrink-0 ml-1" />
          {(['ALL', 'ACTIVE', 'TRIALING', 'SUSPENDED'] as const).map((filter) => (
            <button
              key={filter}
              onClick={() => setStatusFilter(filter)}
              className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-all ${
                statusFilter === filter
                  ? 'bg-slate-900 text-white'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {filter === 'ALL' ? 'All Businesses' : filter}
            </button>
          ))}
        </div>
      </div>

      {/* 3. Tenants Table */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                <th className="py-3.5 px-4">Business & Slug</th>
                <th className="py-3.5 px-4">Owner Info</th>
                <th className="py-3.5 px-4">WhatsApp Status</th>
                <th className="py-3.5 px-4">Subscription Plan</th>
                <th className="py-3.5 px-4">Usage This Month</th>
                <th className="py-3.5 px-4 text-right">Admin Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs">
              {tenantsLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    <td colSpan={6} className="py-4 px-4">
                      <Skeleton className="h-6 w-full" />
                    </td>
                  </tr>
                ))
              ) : filteredTenants.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500">
                    No businesses found matching your filters.
                  </td>
                </tr>
              ) : (
                filteredTenants.map((t) => (
                  <tr key={t.tenantId} className="hover:bg-slate-50/60 transition-colors">
                    {/* Business Name */}
                    <td className="py-3.5 px-4">
                      <div className="font-bold text-slate-900">{t.businessName}</div>
                      <div className="text-[11px] text-slate-400 font-mono">/{t.slug}</div>
                    </td>

                    {/* Owner */}
                    <td className="py-3.5 px-4">
                      <div className="font-medium text-slate-800">{t.ownerName || '—'}</div>
                      <div className="text-[11px] text-slate-500">{t.ownerEmail || '—'}</div>
                    </td>

                    {/* WhatsApp */}
                    <td className="py-3.5 px-4">
                      {t.whatsAppConnected ? (
                        <div className="flex items-center gap-1.5 text-emerald-700 font-semibold">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                          <span>{t.displayPhoneNumber || 'Connected'}</span>
                        </div>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-slate-400 font-medium">
                          <AlertTriangle className="w-3.5 h-3.5" />
                          Not Connected
                        </span>
                      )}
                    </td>

                    {/* Plan & Status */}
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-1.5">
                        <span
                          className={`inline-block w-2 h-2 rounded-full ${
                            t.subscriptionStatus === 'ACTIVE'
                              ? 'bg-emerald-500'
                              : t.subscriptionStatus === 'TRIALING'
                              ? 'bg-amber-500'
                              : 'bg-rose-500'
                          }`}
                        />
                        <span className="font-bold text-slate-900">
                          {t.planType === 'FREE_TRIAL' ? '14-Day Free Trial' : 'All-in-One Plan'}
                        </span>
                      </div>
                      <div className="text-[11px] text-slate-500 mt-0.5">
                        {t.subscriptionStatus === 'TRIALING'
                          ? `${t.daysRemaining} days remaining`
                          : t.subscriptionStatus === 'ACTIVE'
                          ? `Valid (${t.daysRemaining}d left)`
                          : 'Suspended / Expired'}
                      </div>
                    </td>

                    {/* Usage */}
                    <td className="py-3.5 px-4 text-slate-700 font-medium">
                      <div>{t.totalMessagesThisMonth} msgs</div>
                      <div className="text-[11px] text-slate-400">
                        {t.totalFaqs} FAQs • {t.totalAutomationRules} rules
                      </div>
                    </td>

                    {/* Actions */}
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {t.subscriptionStatus !== 'ACTIVE' ? (
                          <button
                            onClick={() => {
                              setSelectedTenant(t);
                              setActionType('ACTIVATE');
                            }}
                            className="px-2.5 py-1.5 text-xs font-bold text-emerald-800 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-lg min-h-[32px] transition-all"
                            title="Activate Plan"
                          >
                            <Play className="w-3.5 h-3.5 inline mr-1" />
                            Activate
                          </button>
                        ) : (
                          <button
                            onClick={() => {
                              setSelectedTenant(t);
                              setActionType('EXTEND');
                            }}
                            className="px-2.5 py-1.5 text-xs font-bold text-blue-800 bg-blue-50 hover:bg-blue-100 border border-blue-200 rounded-lg min-h-[32px] transition-all"
                            title="Extend Validity"
                          >
                            <PlusCircle className="w-3.5 h-3.5 inline mr-1" />
                            Extend (+30d)
                          </button>
                        )}

                        {t.status === 'ACTIVE' && t.subscriptionStatus !== 'SUSPENDED' && (
                          <button
                            onClick={() => {
                              setSelectedTenant(t);
                              setActionType('SUSPEND');
                            }}
                            className="px-2 py-1.5 text-xs font-bold text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 rounded-lg min-h-[32px] transition-all"
                            title="Suspend Tenant"
                          >
                            <Pause className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Action Modal (Activate / Extend / Suspend) */}
      {actionType && selectedTenant && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="w-full max-w-md bg-white rounded-3xl p-6 shadow-2xl border border-slate-100 space-y-6">
            <div className="flex items-center justify-between border-b border-slate-100 pb-4">
              <div>
                <h3 className="text-lg font-black text-slate-900">
                  {actionType === 'ACTIVATE' && 'Activate Business Plan'}
                  {actionType === 'EXTEND' && 'Extend Subscription Days'}
                  {actionType === 'SUSPEND' && 'Suspend Business Account'}
                </h3>
                <p className="text-xs text-slate-500 font-medium">
                  {selectedTenant.businessName} (/{selectedTenant.slug})
                </p>
              </div>
              <button
                onClick={closeModal}
                className="p-1 rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            {actionType === 'ACTIVATE' && (
              <div className="space-y-4 text-xs">
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Plan Tier</label>
                  <select
                    value={planType}
                    onChange={(e) => setPlanType(e.target.value)}
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none font-medium"
                  >
                    <option value="BUSINESS_499">All-in-One Business Plan (₹499/mo)</option>
                    <option value="CUSTOM">Custom Enterprise Plan</option>
                  </select>
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Duration (Days)</label>
                  <input
                    type="number"
                    value={durationDays}
                    onChange={(e) => setDurationDays(Number(e.target.value))}
                    min={1}
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none font-medium"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Admin Notes / Receipt Ref</label>
                  <input
                    type="text"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="e.g. Paid via UPI Ref: 9812491"
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none font-medium"
                  />
                </div>
              </div>
            )}

            {actionType === 'EXTEND' && (
              <div className="space-y-4 text-xs">
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Extra Days to Add</label>
                  <input
                    type="number"
                    value={extraDays}
                    onChange={(e) => setExtraDays(Number(e.target.value))}
                    min={1}
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none font-medium"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Admin Notes / Extension Reason</label>
                  <input
                    type="text"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="e.g. Monthly renewal received via UPI"
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none font-medium"
                  />
                </div>
              </div>
            )}

            {actionType === 'SUSPEND' && (
              <div className="space-y-4 text-xs">
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-800">
                  <AlertTriangle className="w-4 h-4 inline mr-1 text-rose-600" />
                  Suspending this business will immediately stop their automated WhatsApp replies and lock outgoing campaigns.
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Reason for Suspension</label>
                  <input
                    type="text"
                    value={suspendReason}
                    onChange={(e) => setSuspendReason(e.target.value)}
                    placeholder="e.g. Non-payment, violation of policy"
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none font-medium"
                  />
                </div>
              </div>
            )}

            {/* Modal Actions */}
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={closeModal}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>

              {actionType === 'ACTIVATE' && (
                <button
                  type="button"
                  disabled={activateMutation.isPending}
                  onClick={() =>
                    activateMutation.mutate({
                      tenantId: selectedTenant.tenantId,
                      req: { planType, durationDays, notes },
                    })
                  }
                  className="px-5 py-2.5 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl shadow-md shadow-emerald-600/20 disabled:opacity-50"
                >
                  {activateMutation.isPending ? 'Activating...' : 'Confirm Activation'}
                </button>
              )}

              {actionType === 'EXTEND' && (
                <button
                  type="button"
                  disabled={extendMutation.isPending}
                  onClick={() =>
                    extendMutation.mutate({
                      tenantId: selectedTenant.tenantId,
                      req: { extraDays, notes },
                    })
                  }
                  className="px-5 py-2.5 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-md shadow-blue-600/20 disabled:opacity-50"
                >
                  {extendMutation.isPending ? 'Extending...' : 'Add Days'}
                </button>
              )}

              {actionType === 'SUSPEND' && (
                <button
                  type="button"
                  disabled={suspendMutation.isPending}
                  onClick={() =>
                    suspendMutation.mutate({
                      tenantId: selectedTenant.tenantId,
                      req: { reason: suspendReason },
                    })
                  }
                  className="px-5 py-2.5 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-xl shadow-md shadow-rose-600/20 disabled:opacity-50"
                >
                  {suspendMutation.isPending ? 'Suspending...' : 'Confirm Suspension'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
