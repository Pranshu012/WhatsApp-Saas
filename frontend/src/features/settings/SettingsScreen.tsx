import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import { BusinessSettingsDto, UpdateBusinessSettingsRequest } from '../../api/types';
import { Skeleton } from '../../components/Skeleton';
import { ErrorState } from '../../components/ErrorState';
import { AlertBanner } from '../../components/AlertBanner';
import { Building2, Save, Globe2, FileText, MapPin } from 'lucide-react';

export const SettingsScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const [businessName, setBusinessName] = useState('');
  const [timezone, setTimezone] = useState('Asia/Kolkata');
  const [gstin, setGstin] = useState('');
  const [legalName, setLegalName] = useState('');
  const [billingAddress, setBillingAddress] = useState('');

  const {
    data: settings,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<BusinessSettingsDto>({
    queryKey: ['settings-business'],
    queryFn: () => apiClient<BusinessSettingsDto>('/api/settings/business'),
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

  if (isLoading) {
    return (
      <div className="space-y-6 max-w-4xl">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full rounded-2xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Could not load business settings"
        message={(error as any)?.message || 'Something went wrong.'}
        onRetry={refetch}
      />
    );
  }

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <Building2 className="w-6 h-6 text-brand-600" />
          Business Profile & Settings
        </h1>
        <p className="text-sm text-gray-500 mt-1">
          Configure your business profile, Indian GSTIN for tax invoices, and default scheduling timezone.
        </p>
      </div>

      {/* Notifications */}
      {successMsg && (
        <AlertBanner type="success" message={successMsg} onClose={() => setSuccessMsg(null)} />
      )}
      {errorMsg && (
        <AlertBanner type="error" message={errorMsg} onClose={() => setErrorMsg(null)} />
      )}

      {/* Form */}
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
                placeholder="e.g. Acme Retailers"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                Account Slug (Workspace ID)
              </label>
              <input
                type="text"
                disabled
                value={settings?.slug || ''}
                className="w-full px-3.5 py-2.5 border border-gray-200 bg-gray-50 text-gray-500 rounded-xl text-sm min-h-[44px] cursor-not-allowed"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1 flex items-center gap-1.5">
              <Globe2 className="w-4 h-4 text-gray-400" />
              Default Timezone
            </label>
            <select
              value={timezone}
              onChange={(e) => setTimezone(e.target.value)}
              className="w-full sm:w-1/2 px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm min-h-[44px] bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none"
            >
              <option value="Asia/Kolkata">Asia/Kolkata (IST +05:30)</option>
              <option value="Asia/Dubai">Asia/Dubai (GST +04:00)</option>
              <option value="Asia/Singapore">Asia/Singapore (SGT +08:00)</option>
              <option value="UTC">UTC (Coordinated Universal Time)</option>
            </select>
          </div>
        </div>

        {/* Card 2: Legal & Tax Invoicing (GST) */}
        <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
          <h2 className="font-bold text-gray-900 text-base flex items-center gap-2">
            <FileText className="w-5 h-5 text-gray-400" />
            Tax & Invoicing Details (India GST)
          </h2>
          <p className="text-xs text-gray-500">
            Provide your legal registered entity name and GSTIN to receive GST-compliant B2B invoices with input tax credit.
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
                placeholder="e.g. Acme Private Limited"
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
              placeholder="123 Commercial Plaza, MG Road, Bengaluru, Karnataka 560001"
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
                Save Business Settings
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};
