import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import { WhatsAppAccountResponse } from '../../api/types';
import { useMetaEmbeddedSignup } from './useMetaEmbeddedSignup';
import { AlertBanner } from '../../components/AlertBanner';
import { Skeleton } from '../../components/Skeleton';
import { formatE164 } from '../../lib/phoneUtils';
import {
  Smartphone,
  CheckCircle2,
  AlertTriangle,
  ExternalLink,
  ShieldCheck,
  Zap,
  Info,
  Clock,
  Layers,
  Unplug,
  Loader2,
} from 'lucide-react';

export const WhatsAppConnectionScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [showDisconnectModal, setShowDisconnectModal] = useState(false);

  // Fetch current primary WhatsApp Account
  const {
    data: account,
    isLoading,
  } = useQuery<WhatsAppAccountResponse>({
    queryKey: ['whatsapp-account'],
    queryFn: () => apiClient<WhatsAppAccountResponse>('/api/whatsapp/account'),
    retry: false,
  });

  // Disconnect Mutation
  const disconnectMutation = useMutation({
    mutationFn: (id: string) =>
      apiClient(`/api/whatsapp/accounts/${id}/disconnect`, { method: 'POST' }),
    onSuccess: () => {
      queryClient.setQueryData(['whatsapp-account'], null);
      setShowDisconnectModal(false);
      setSuccessMsg('WhatsApp account disconnected successfully.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to disconnect WhatsApp account.');
    },
  });

  const { isConnecting, launchSignup, cancelSignup } = useMetaEmbeddedSignup({
    onSuccess: (newAccount) => {
      queryClient.setQueryData(['whatsapp-account'], newAccount);
      setSuccessMsg(`Successfully connected WhatsApp number: ${newAccount.displayPhoneNumber}`);
      setErrorMsg(null);
    },
    onError: (err) => {
      setErrorMsg(err);
    },
  });

  const isConnected = !!account && account.status === 'CONNECTED';
  const isTokenExpired = account?.status === 'DEAUTHORIZED' || account?.status === 'ERROR';

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="pb-5 border-b border-gray-200">
          <Skeleton className="h-8 w-64 mb-2" />
          <Skeleton className="h-4 w-96" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Skeleton className="h-48 rounded-xl" />
          <Skeleton className="h-48 rounded-xl" />
          <Skeleton className="h-48 rounded-xl" />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-5 border-b border-gray-200">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900">WhatsApp Onboarding & Status</h1>
            <span
              className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                isConnected
                  ? 'bg-green-100 text-green-800'
                  : isTokenExpired
                  ? 'bg-amber-100 text-amber-800'
                  : 'bg-gray-100 text-gray-800'
              }`}
            >
              {isConnected ? 'Connected' : isTokenExpired ? 'Action Required' : 'Not Connected'}
            </span>
          </div>
          <p className="text-sm text-gray-500">
            Official Meta Cloud API connection for automated responses and notifications.
          </p>
        </div>

        {isConnected && (
          <button
            type="button"
            onClick={() => setShowDisconnectModal(true)}
            className="inline-flex items-center gap-2 px-3 py-2 text-sm font-medium text-red-600 bg-white border border-red-200 hover:bg-red-50 rounded-lg transition-colors min-h-[44px] self-start sm:self-auto"
          >
            <Unplug className="w-4 h-4" />
            Disconnect Number
          </button>
        )}
      </div>

      {/* Notifications / Alerts */}
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

      {/* CRITICAL: Payment Method Warning Banner for Connected Accounts */}
      {isConnected && (
        <div className="p-5 rounded-xl border border-amber-300 bg-amber-50 text-amber-900 shadow-sm">
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-6 h-6 text-amber-600 shrink-0 mt-0.5" />
            <div className="flex-1">
              <h3 className="font-bold text-base text-amber-950 mb-1">
                ⚠️ Ensure payment method is attached in Meta WhatsApp Manager
              </h3>
              <p className="text-sm text-amber-800 mb-3 leading-relaxed">
                Meta bills you directly for outbound and template messages. If your WhatsApp Business
                Account has no valid card on file with Meta, outbound messages will fail with billing errors.
                Add the payment method directly in Meta WhatsApp Manager before you start sending messages.
              </p>
              <a
                href="https://business.facebook.com/wa/manage/phone-numbers/"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-amber-950 bg-amber-200/80 hover:bg-amber-200 border border-amber-300 rounded-lg min-h-[44px] transition-colors"
              >
                Add Payment Method in WhatsApp Manager
                <ExternalLink className="w-4 h-4" />
              </a>
            </div>
          </div>
        </div>
      )}

      {/* STATE 1: CONNECTING (In-Progress Spinner) */}
      {isConnecting && (
        <div className="bg-white p-8 rounded-2xl border border-gray-200 text-center shadow-sm max-w-lg mx-auto space-y-4">
          <div className="w-14 h-14 rounded-full bg-brand-50 flex items-center justify-center text-brand-600 mx-auto">
            <Loader2 className="w-8 h-8 animate-spin" />
          </div>
          <h2 className="text-lg font-bold text-gray-900">Waiting for Meta Embedded Signup...</h2>
          <p className="text-sm text-gray-600">
            Please complete the Facebook login and number verification in the popup window.
          </p>
          <div className="pt-2">
            <button
              type="button"
              onClick={cancelSignup}
              className="px-4 py-2 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg min-h-[44px]"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* STATE 2: CONNECTED (Health Card & Live Metrics) */}
      {!isConnecting && isConnected && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Card 1: Account Info */}
          <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                Phone Number
              </span>
              <div className="w-8 h-8 rounded-lg bg-green-50 text-green-600 flex items-center justify-center">
                <Smartphone className="w-4 h-4" />
              </div>
            </div>
            <div>
              <div className="text-xl font-bold text-gray-900">
                {formatE164(account.displayPhoneNumber || 'WhatsApp Account')}
              </div>
              <div className="text-sm text-gray-500 font-medium mt-0.5">
                {account.verifiedName || 'Verified WhatsApp Business'}
              </div>
            </div>
            <div className="pt-2 border-t border-gray-100 text-xs text-gray-400 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5" />
              Connected: {account.connectedAt ? new Date(account.connectedAt).toLocaleDateString('en-IN') : 'Active'}
            </div>
          </div>

          {/* Card 2: Quality Rating */}
          <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                Quality Rating
              </span>
              <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                <ShieldCheck className="w-4 h-4" />
              </div>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span
                  className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold ${
                    account.qualityRating === 'GREEN'
                      ? 'bg-green-100 text-green-800'
                      : account.qualityRating === 'YELLOW'
                      ? 'bg-yellow-100 text-yellow-800'
                      : 'bg-red-100 text-red-800'
                  }`}
                >
                  {account.qualityRating || 'GREEN'}
                </span>
                <span className="text-xs text-gray-500 font-medium">
                  {account.qualityRating === 'RED' ? 'High Report Rate' : 'Healthy Status'}
                </span>
              </div>
              <p className="text-xs text-gray-500 mt-2 leading-relaxed">
                Rating reflects customer feedback. Keep opt-out easy to avoid spam flags.
              </p>
            </div>
          </div>

          {/* Card 3: Messaging Limit Tier */}
          <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                24h Messaging Tier
              </span>
              <div className="w-8 h-8 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
                <Layers className="w-4 h-4" />
              </div>
            </div>
            <div>
              <div className="text-xl font-bold text-gray-900">
                {account.messagingLimitTier || 'TIER_1K'}
              </div>
              <p className="text-xs text-gray-500 mt-2 leading-relaxed">
                Max unique customer conversations initiated in rolling 24 hours. Meta automatically upgrades high-quality numbers.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* STATE 3: NOT CONNECTED / TOKEN EXPIRED */}
      {!isConnecting && !isConnected && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Left Column: What you'll need & Expectations */}
          <div className="lg:col-span-7 space-y-6">
            <div className="bg-white p-6 sm:p-8 rounded-2xl border border-gray-200 shadow-sm space-y-6">
              <div>
                <h2 className="text-lg font-bold text-gray-900 mb-1">
                  Connect your WhatsApp Business Account
                </h2>
                <p className="text-sm text-gray-600">
                  Launch the official Meta Embedded Signup flow to link your phone number in under 10 minutes.
                </p>
              </div>

              {/* Requirements Checklist */}
              <div className="bg-slate-50 p-5 rounded-xl border border-slate-200 space-y-3">
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-brand-600" />
                  What you'll need before starting:
                </h3>
                <ul className="text-xs text-slate-600 space-y-2.5">
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-brand-500 mt-1.5 shrink-0" />
                    <span>A personal Facebook login with admin rights for your business</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-brand-500 mt-1.5 shrink-0" />
                    <span>
                      A phone number <strong>not currently active on WhatsApp</strong> (must delete existing personal/business WhatsApp account on this SIM)
                    </span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-brand-500 mt-1.5 shrink-0" />
                    <span>Access to receive an SMS or Phone Call OTP on that SIM card</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-brand-500 mt-1.5 shrink-0" />
                    <span>Official business name, address, and website details</span>
                  </li>
                </ul>
              </div>

              {/* CTA Button */}
              <div>
                <button
                  type="button"
                  onClick={launchSignup}
                  className="w-full sm:w-auto inline-flex items-center justify-center gap-3 px-6 py-3 text-sm font-semibold text-white bg-[#1877F2] hover:bg-[#166fe5] rounded-xl shadow-sm min-h-[48px] transition-colors"
                >
                  <Smartphone className="w-5 h-5" />
                  Connect WhatsApp with Facebook
                </button>
              </div>
            </div>
          </div>

          {/* Right Column: Two-Bill Model Explanation */}
          <div className="lg:col-span-5 space-y-6">
            <div className="bg-gradient-to-br from-slate-900 to-slate-800 text-white p-6 sm:p-8 rounded-2xl shadow-sm space-y-4">
              <div className="flex items-center gap-2 text-brand-400 text-xs font-bold uppercase tracking-wider">
                <Zap className="w-4 h-4" />
                Transparent Pricing
              </div>
              <h3 className="text-base font-bold text-white">The Two-Bill Model Explained</h3>
              <p className="text-xs text-slate-300 leading-relaxed">
                We believe in total transparency. Running WhatsApp SaaS involves two separate bills:
              </p>

              <div className="space-y-3 pt-2 text-xs">
                <div className="p-3 rounded-lg bg-white/10 border border-white/10">
                  <div className="font-semibold text-white mb-0.5">1. Our Software Platform</div>
                  <div className="text-slate-300">
                    ₹1,999 / month flat for automation rules, FAQ bot, inbox, and campaign scheduler.
                  </div>
                </div>

                <div className="p-3 rounded-lg bg-white/10 border border-white/10">
                  <div className="font-semibold text-white mb-0.5">2. Meta Direct Messaging Charges</div>
                  <div className="text-slate-300">
                    Billed directly to your card by Meta (~₹0.115/utility message, ~₹0.86/marketing message in India + GST).
                  </div>
                </div>
              </div>

              <div className="text-[11px] text-slate-400 pt-2 flex items-center gap-1.5">
                <Info className="w-3.5 h-3.5 shrink-0" />
                You own 100% of your Meta WABA assets and can manage cards in WhatsApp Manager.
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Disconnect Confirmation Modal */}
      {showDisconnectModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-gray-100 space-y-4">
            <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center text-red-600 mx-auto">
              <Unplug className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-gray-900 text-center">
              Disconnect WhatsApp Account?
            </h3>
            <p className="text-xs text-gray-600 text-center leading-relaxed">
              Disconnecting will pause all keyword automation rules, FAQ auto-replies, and scheduled template broadcasts until reconnected.
            </p>
            <div className="flex items-center gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowDisconnectModal(false)}
                className="flex-1 py-2.5 px-4 rounded-lg bg-gray-100 hover:bg-gray-200 text-xs font-semibold text-gray-700 min-h-[44px]"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => disconnectMutation.mutate(account!.id)}
                disabled={disconnectMutation.isPending}
                className="flex-1 py-2.5 px-4 rounded-lg bg-red-600 hover:bg-red-700 text-xs font-semibold text-white min-h-[44px]"
              >
                {disconnectMutation.isPending ? 'Disconnecting...' : 'Yes, Disconnect'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
export default WhatsAppConnectionScreen;
