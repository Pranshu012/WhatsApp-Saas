import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import {
  AdminPlatformStatsDto,
  AdminTenantDto,
  ActivateTenantRequest,
  ExtendSubscriptionRequest,
  SuspendTenantRequest,
  UpdateTenantAiLimitRequest,
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
  Sparkles,
} from 'lucide-react';
import { Skeleton } from '../../components/Skeleton';
import { ErrorState } from '../../components/ErrorState';

export const AdminDashboardScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'TRIALING' | 'SUSPENDED'>('ALL');
  
  // Selected tenant for detail modal
  const [selectedTenant, setSelectedTenant] = useState<AdminTenantDto | null>(null);
  const [actionType, setActionType] = useState<'ACTIVATE' | 'EXTEND' | 'SUSPEND' | 'AI_LIMIT' | null>(null);

  // Form states for modal
  const [planType, setPlanType] = useState<string>('BUSINESS_499');
  const [durationDays, setDurationDays] = useState<number>(30);
  const [extraDays, setExtraDays] = useState<number>(30);
  const [notes, setNotes] = useState<string>('');
  const [suspendReason, setSuspendReason] = useState<string>('Payment past due');
  const [newAiLimit, setNewAiLimit] = useState<string>('500');
  const [modalError, setModalError] = useState<string | null>(null);

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

  const updateAiLimitMutation = useMutation({
    mutationFn: ({ tenantId, req }: { tenantId: string; req: UpdateTenantAiLimitRequest }) =>
      apiClient<AdminTenantDto>(`/api/admin/tenants/${tenantId}/ai-limit`, {
        method: 'POST',
        body: JSON.stringify(req),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      closeModal();
    },
    onError: (err: any) => {
      setModalError(err?.message || 'Failed to update AI limit.');
    },
  });

  const closeModal = () => {
    setActionType(null);
    setSelectedTenant(null);
    setNotes('');
    setModalError(null);
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
                <th className="py-3.5 px-4">AI Quota</th>
                <th className="py-3.5 px-4 text-right">Admin Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs">
              {tenantsLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    <td colSpan={7} className="py-4 px-4">
                      <Skeleton className="h-6 w-full" />
                    </td>
                  </tr>
                ))
              ) : filteredTenants.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-500">
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

                    {/* AI Quota */}
                    <td className="py-3.5 px-4 text-slate-700 font-medium">
                      <div className="flex items-center gap-1.5 font-bold text-slate-900">
                        <Sparkles className="w-3.5 h-3.5 text-purple-600" />
                        <span>{t.aiUsedThisMonth || 0} / {t.aiMonthlyLimit || 500}</span>
                      </div>
                      <div className="w-24 bg-slate-100 rounded-full h-1.5 mt-1.5 overflow-hidden">
                        <div
                          className={`h-full rounded-full transition-all ${
                            ((t.aiUsedThisMonth || 0) / (t.aiMonthlyLimit || 500)) >= 0.9
                              ? 'bg-rose-500'
                              : ((t.aiUsedThisMonth || 0) / (t.aiMonthlyLimit || 500)) >= 0.7
                              ? 'bg-amber-500'
                              : 'bg-purple-600'
                          }`}
                          style={{
                            width: `${Math.min(100, Math.round(((t.aiUsedThisMonth || 0) / (t.aiMonthlyLimit || 500)) * 100))}%`,
                          }}
                        />
                      </div>
                      <div className="text-[10px] text-slate-400 mt-0.5">
                        {Math.max(0, (t.aiMonthlyLimit || 500) - (t.aiUsedThisMonth || 0))} msgs left
                      </div>
                    </td>

                    {/* Actions */}
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => {
                            setSelectedTenant(t);
                            setNewAiLimit(String(t.aiMonthlyLimit || 500));
                            setModalError(null);
                            setActionType('AI_LIMIT');
                          }}
                          className="px-2.5 py-1.5 text-xs font-bold text-purple-800 bg-purple-50 hover:bg-purple-100 border border-purple-200 rounded-lg min-h-[32px] transition-all flex items-center gap-1"
                          title="Manage AI Quota Limit"
                        >
                          <Sparkles className="w-3.5 h-3.5 text-purple-600" />
                          <span>AI Limit</span>
                        </button>

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
                  {actionType === 'AI_LIMIT' && 'Configure AI Monthly Limit'}
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

            {modalError && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs font-semibold flex items-center gap-1.5">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>{modalError}</span>
              </div>
            )}

            {actionType === 'AI_LIMIT' && (
              <div className="space-y-4 text-xs">
                <div className="p-3.5 bg-purple-50 border border-purple-200 rounded-2xl text-purple-900 leading-relaxed">
                  <div className="font-bold flex items-center gap-1.5 mb-1 text-purple-950">
                    <Sparkles className="w-4 h-4 text-purple-600" />
                    Monthly AI Quota Limit
                  </div>
                  Set the maximum number of AI Receptionist auto-replies this business can use per month. Default is 500 messages/mo. Tenants cannot edit this limit.
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1.5">Quick Select Presets</label>
                  <div className="grid grid-cols-4 gap-2">
                    {[100, 500, 1000, 2000].map((preset) => (
                      <button
                        key={preset}
                        type="button"
                        onClick={() => {
                          setNewAiLimit(String(preset));
                          if (modalError) setModalError(null);
                        }}
                        className={`py-2 px-1 text-center font-bold rounded-xl border text-xs transition-all ${
                          Number(newAiLimit) === preset
                            ? 'bg-purple-600 text-white border-purple-600 shadow-sm'
                            : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                        }`}
                      >
                        {preset}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Limit (Messages / Month)</label>
                  <input
                    type="number"
                    value={newAiLimit}
                    onChange={(e) => {
                      setNewAiLimit(e.target.value);
                      if (modalError) setModalError(null);
                    }}
                    min={10}
                    step={50}
                    placeholder="e.g. 500"
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none font-bold text-sm text-slate-900 focus:ring-2 focus:ring-purple-500 focus:bg-white transition-all"
                  />
                  <div className="flex items-center justify-between text-[11px] text-slate-400 mt-1">
                    <span>Used this month: <strong className="text-slate-700">{selectedTenant.aiUsedThisMonth || 0}</strong></span>
                    <span>Remaining: <strong className="text-purple-700">{Math.max(0, (parseInt(newAiLimit, 10) || 0) - (selectedTenant.aiUsedThisMonth || 0))}</strong></span>
                  </div>
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

              {actionType === 'AI_LIMIT' && (
                <button
                  type="button"
                  disabled={updateAiLimitMutation.isPending}
                  onClick={() => {
                    const parsed = parseInt(newAiLimit, 10);
                    if (isNaN(parsed) || parsed < 10) {
                      setModalError('Please enter a valid monthly limit of at least 10 messages.');
                      return;
                    }
                    updateAiLimitMutation.mutate({
                      tenantId: selectedTenant.tenantId,
                      req: { monthlyLimit: parsed },
                    });
                  }}
                  className="px-5 py-2.5 text-xs font-bold text-white bg-purple-600 hover:bg-purple-700 rounded-xl shadow-md shadow-purple-600/20 disabled:opacity-50 transition-all flex items-center gap-1.5 cursor-pointer"
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  {updateAiLimitMutation.isPending ? 'Saving Limit...' : 'Save AI Limit'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
