import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import {
  BusinessSettingsDto,
  UpdateBusinessSettingsRequest,
  SubscriptionDto,
} from '../../api/types';
import { Skeleton } from '../../components/Skeleton';
import { ErrorState } from '../../components/ErrorState';
import { AlertBanner } from '../../components/AlertBanner';
import { WhatsAppConnectionScreen } from '../whatsapp/WhatsAppConnectionScreen';
import {
  Building2,
  Save,
  Globe2,
  FileText,
  MapPin,
  Smartphone,
  CreditCard,
  Clock,
  Sparkles,
} from 'lucide-react';

export const SettingsScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'whatsapp' | 'profile' | 'subscription'>('whatsapp');
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const [businessName, setBusinessName] = useState('');
  const [timezone, setTimezone] = useState('Asia/Kolkata');
  const [gstin, setGstin] = useState('');
  const [legalName, setLegalName] = useState('');
  const [billingAddress, setBillingAddress] = useState('');

  // 1. Fetch Business Settings
  const {
    data: settings,
    isLoading: isLoadingSettings,
    isError: isErrorSettings,
    error: errorSettings,
    refetch,
  } = useQuery<BusinessSettingsDto>({
    queryKey: ['settings-business'],
    queryFn: () => apiClient<BusinessSettingsDto>('/api/settings/business'),
  });

  // 2. Fetch Subscription Info
  const { data: subscription } = useQuery<SubscriptionDto>({
    queryKey: ['subscription'],
    queryFn: () => apiClient<SubscriptionDto>('/api/subscription'),
  });

  useEffect(() => {
    if (settings) {
      setBusinessName(settings.businessName || '');
      setTimezone(settings.timezone || 'Asia/Kolkata');
      setGstin(settings.gstin || '');
      setLegalName(settings.legalName || '');
      setBillingAddress(settings.billingAddress || '');
    }
  }, [settings]);

  const updateMutation = useMutation({
    mutationFn: (req: UpdateBusinessSettingsRequest) =>
      apiClient<BusinessSettingsDto>('/api/settings/business', {
        method: 'PUT',
        body: JSON.stringify(req),
      }),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['settings-business'] });
      setSuccessMsg('Business settings updated successfully.');
      setBusinessName(updated.businessName);
      setTimezone(updated.timezone);
      setGstin(updated.gstin || '');
      setLegalName(updated.legalName || '');
      setBillingAddress(updated.billingAddress || '');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to update settings.');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!businessName.trim()) return;

    updateMutation.mutate({
      businessName: businessName.trim(),
      timezone: timezone.trim(),
      gstin: gstin.trim() || undefined,
      legalName: legalName.trim() || undefined,
      billingAddress: billingAddress.trim() || undefined,
    });
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Header */}
      <div className="pb-4 border-b border-gray-200">
        <h1 className="text-xl sm:text-2xl font-bold text-gray-900 flex items-center gap-2.5">
          <Building2 className="w-6 h-6 text-brand-600" />
          WhatsApp & Business Settings
        </h1>
        <p className="text-xs sm:text-sm text-gray-500 mt-1">
          Manage your WhatsApp phone connection, business profile, Indian GST, and subscription plan.
        </p>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 p-1.5 bg-gray-100/80 rounded-2xl border border-gray-200">
        <button
          type="button"
          onClick={() => setActiveTab('whatsapp')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs sm:text-sm font-bold transition-all ${
            activeTab === 'whatsapp'
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          <Smartphone className="w-4 h-4 text-emerald-600" />
          <span>WhatsApp Connection</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('profile')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs sm:text-sm font-bold transition-all ${
            activeTab === 'profile'
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          <Building2 className="w-4 h-4 text-blue-600" />
          <span>Business Profile & GST</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('subscription')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs sm:text-sm font-bold transition-all ${
            activeTab === 'subscription'
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          <CreditCard className="w-4 h-4 text-purple-600" />
          <span>Subscription Plan</span>
          {subscription?.status && (
            <span className="px-2 py-0.5 rounded-full text-[10px] bg-brand-50 text-brand-700 font-semibold border border-brand-200">
              {subscription.planType === 'FREE_TRIAL' ? '14-Day Trial' : 'Active'}
            </span>
          )}
        </button>
      </div>

      {/* Notifications */}
      {successMsg && (
        <AlertBanner type="success" message={successMsg} onClose={() => setSuccessMsg(null)} />
      )}
      {errorMsg && (
        <AlertBanner type="error" message={errorMsg} onClose={() => setErrorMsg(null)} />
      )}

      {/* ======================================================== */}
      {/* TAB 1: WHATSAPP CONNECTION                               */}
      {/* ======================================================== */}
      {activeTab === 'whatsapp' && (
        <div>
          <WhatsAppConnectionScreen />
        </div>
      )}

      {/* ======================================================== */}
      {/* TAB 2: BUSINESS PROFILE & GST                            */}
      {/* ======================================================== */}
      {activeTab === 'profile' && (
        <div>
          {isLoadingSettings ? (
            <div className="space-y-4">
              <Skeleton className="h-40 rounded-2xl" />
              <Skeleton className="h-40 rounded-2xl" />
            </div>
          ) : isErrorSettings ? (
            <ErrorState
              title="Could not load business settings"
              message={(errorSettings as any)?.message || 'Something went wrong.'}
              onRetry={refetch}
            />
          ) : (
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Card 1: Core Business Details */}
              <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
                <h2 className="font-bold text-gray-900 text-base flex items-center gap-2">
                  <Building2 className="w-5 h-5 text-gray-400" />
                  General Information
                </h2>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                      Display Business Name <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      required
                      value={businessName}
                      onChange={(e) => setBusinessName(e.target.value)}
                      className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none"
                      placeholder="e.g. Trust Align Dental Clinic"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                      <Globe2 className="w-4 h-4 text-gray-400" />
                      Default Timezone
                    </label>
                    <select
                      value={timezone}
                      onChange={(e) => setTimezone(e.target.value)}
                      className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm min-h-[44px] bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none"
                    >
                      <option value="Asia/Kolkata">Asia/Kolkata (IST +05:30)</option>
                      <option value="Asia/Dubai">Asia/Dubai (GST +04:00)</option>
                      <option value="Asia/Singapore">Asia/Singapore (SGT +08:00)</option>
                      <option value="UTC">UTC (Coordinated Universal Time)</option>
                    </select>
                  </div>
                </div>
              </div>

              {/* Card 2: Legal & Tax Invoicing (GST) */}
              <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
                <h2 className="font-bold text-gray-900 text-base flex items-center gap-2">
                  <FileText className="w-5 h-5 text-gray-400" />
                  Tax & Invoicing Details (India GST)
                </h2>
                <p className="text-xs text-gray-500">
                  Provide your legal registered entity name and GSTIN to receive GST-compliant B2B invoices.
                </p>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                      Legal Entity Name
                    </label>
                    <input
                      type="text"
                      value={legalName}
                      onChange={(e) => setLegalName(e.target.value)}
                      className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none"
                      placeholder="e.g. Trust Align Healthcare LLP"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                      GSTIN (15-character alphanumeric)
                    </label>
                    <input
                      type="text"
                      maxLength={15}
                      value={gstin}
                      onChange={(e) => setGstin(e.target.value.toUpperCase())}
                      className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm font-mono uppercase min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none"
                      placeholder="27AAAAA0000A1Z5"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                    <MapPin className="w-4 h-4 text-gray-400" />
                    Registered Billing Address
                  </label>
                  <textarea
                    rows={3}
                    value={billingAddress}
                    onChange={(e) => setBillingAddress(e.target.value)}
                    className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none"
                    placeholder="123 Health Plaza, Bangalore, Karnataka 560001"
                  />
                </div>
              </div>

              {/* Submit Button */}
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={updateMutation.isPending}
                  className="inline-flex items-center justify-center gap-2 px-6 py-2.5 bg-brand-600 hover:bg-brand-700 text-white font-semibold rounded-xl min-h-[44px] shadow-sm transition-colors disabled:opacity-50"
                >
                  {updateMutation.isPending ? (
                    'Saving Changes...'
                  ) : (
                    <>
                      <Save className="w-4 h-4" />
                      Save Business Profile
                    </>
                  )}
                </button>
              </div>
            </form>
          )}
        </div>
      )}

      {/* ======================================================== */}
      {/* TAB 3: SUBSCRIPTION PLAN                                 */}
      {/* ======================================================== */}
      {activeTab === 'subscription' && (
        <div className="space-y-6">
          <div className="bg-white p-6 sm:p-8 rounded-2xl border border-gray-200 shadow-sm space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-6 border-b border-gray-100">
              <div>
                <span className="text-xs font-bold uppercase tracking-wider text-brand-600 bg-brand-50 px-2.5 py-1 rounded-full border border-brand-200">
                  Current Plan
                </span>
                <h3 className="text-2xl font-bold text-gray-900 mt-2">
                  {subscription?.planType === 'FREE_TRIAL'
                    ? '14-Day Free Trial (₹0)'
                    : 'All-in-One SMB Plan (₹499/mo)'}
                </h3>
                <p className="text-sm text-gray-500 mt-1">
                  Status:{' '}
                  <span className="font-bold text-emerald-600">
                    {subscription?.status || 'Active'}
                  </span>
                </p>
              </div>

              <div className="p-4 bg-gray-50 rounded-2xl border border-gray-200 text-right">
                <div className="text-xs text-gray-500 font-medium">Valid Until</div>
                <div className="text-base font-bold text-gray-900 mt-0.5 flex items-center gap-1.5 justify-end">
                  <Clock className="w-4 h-4 text-brand-500" />
                  {subscription?.currentPeriodEnd
                    ? new Date(subscription.currentPeriodEnd).toLocaleDateString('en-IN', {
                        day: 'numeric',
                        month: 'short',
                        year: 'numeric',
                      })
                    : '14 Days from Signup'}
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="p-4 bg-gray-50 rounded-xl border border-gray-200 space-y-1">
                <div className="text-xs text-gray-500 font-semibold uppercase">Auto-Replies</div>
                <div className="text-lg font-bold text-gray-900">Unlimited</div>
                <p className="text-[11px] text-gray-500">24/7 instant replies on WhatsApp</p>
              </div>

              <div className="p-4 bg-gray-50 rounded-xl border border-gray-200 space-y-1">
                <div className="text-xs text-gray-500 font-semibold uppercase">Smart FAQs</div>
                <div className="text-lg font-bold text-gray-900">Unlimited</div>
                <p className="text-[11px] text-gray-500">Typo-tolerant question matcher</p>
              </div>

              <div className="p-4 bg-gray-50 rounded-xl border border-gray-200 space-y-1">
                <div className="text-xs text-gray-500 font-semibold uppercase">Shared Live Inbox</div>
                <div className="text-lg font-bold text-gray-900">Included</div>
                <p className="text-[11px] text-gray-500">1-on-1 human customer chat</p>
              </div>
            </div>

            <div className="p-5 bg-gradient-to-r from-brand-50 to-indigo-50 rounded-2xl border border-brand-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <h4 className="font-bold text-sm text-brand-950 flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-brand-600" />
                  Transparent Two-Bill Pricing
                </h4>
                <p className="text-xs text-brand-800 leading-relaxed">
                  Flat ₹499/month for your automation software. You pay Meta directly for wholesale message utility rates (~₹0.11/message). No hidden markups.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
