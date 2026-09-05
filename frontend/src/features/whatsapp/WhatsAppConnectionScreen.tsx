import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { WhatsAppAccountResponse } from '../../api/types';
import { useMetaEmbeddedSignup } from './useMetaEmbeddedSignup';
import { AlertBanner } from '../../components/AlertBanner';
import { Skeleton } from '../../components/Skeleton';
import { formatE164 } from '../../lib/phoneUtils';
import {
  Smartphone,
  Unplug,
  Loader2,
  Key,
  Zap,
  MessageSquare,
  FileText,
} from 'lucide-react';

export const WhatsAppConnectionScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [showDisconnectModal, setShowDisconnectModal] = useState(false);
  const [showManualModal, setShowManualModal] = useState(false);

  // Manual connect state
  const [manualForm, setManualForm] = useState({
    wabaId: '',
    phoneNumberId: '',
    displayPhoneNumber: '',
    verifiedName: 'Test Business WhatsApp',
    rawAccessToken: '',
  });

  // Direct manual connect mutation
  const directConnectMutation = useMutation({
    mutationFn: (data: typeof manualForm) =>
      apiClient<WhatsAppAccountResponse>('/api/whatsapp/connect-direct', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    onSuccess: (newAccount) => {
      queryClient.setQueryData(['whatsapp-account'], newAccount);
      setShowManualModal(false);
      setSuccessMsg(`Successfully connected WhatsApp: ${newAccount.displayPhoneNumber}`);
      setErrorMsg(null);
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to connect with credentials.');
    },
  });

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

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-28 rounded-2xl" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Skeleton className="h-36 rounded-2xl" />
          <Skeleton className="h-36 rounded-2xl" />
          <Skeleton className="h-36 rounded-2xl" />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
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

      {/* STATE 2: CONNECTED (Clean Customer-Friendly View) */}
      {!isConnecting && isConnected && (
        <div className="space-y-6">
          {/* Main WhatsApp Account Card */}
          <div className="bg-white p-6 sm:p-7 rounded-2xl border border-gray-200 shadow-sm flex flex-col sm:flex-row sm:items-center sm:justify-between gap-5">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-2xl bg-emerald-600 text-white flex items-center justify-center shadow-md shadow-emerald-600/20 shrink-0">
                <Smartphone className="w-6 h-6" />
              </div>

              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg sm:text-xl font-bold text-gray-900">
                    {account.verifiedName || 'Business WhatsApp'}
                  </h2>
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                    <span className="w-2 h-2 rounded-full bg-emerald-500" />
                    Connected
                  </span>
                </div>

                <p className="text-base font-semibold text-gray-700 mt-0.5 font-mono">
                  {formatE164(account.displayPhoneNumber || 'Connected Number')}
                </p>
              </div>
            </div>

            {/* Actions: Update Token & Disconnect */}
            <div className="flex items-center gap-2.5 self-start sm:self-center">
              <button
                type="button"
                onClick={() => {
                  setManualForm({
                    wabaId: account.wabaId,
                    phoneNumberId: account.phoneNumberId,
                    displayPhoneNumber: account.displayPhoneNumber,
                    verifiedName: account.verifiedName || 'Test Business WhatsApp',
                    rawAccessToken: '',
                  });
                  setShowManualModal(true);
                }}
                className="inline-flex items-center gap-1.5 px-4 py-2 text-xs sm:text-sm font-semibold text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-xl transition-colors min-h-[40px]"
              >
                <Key className="w-4 h-4 text-gray-500" />
                Update Token
              </button>

              <button
                type="button"
                onClick={() => setShowDisconnectModal(true)}
                className="inline-flex items-center gap-1.5 px-4 py-2 text-xs sm:text-sm font-semibold text-red-600 bg-white border border-red-200 hover:bg-red-50 rounded-xl transition-colors min-h-[40px]"
              >
                <Unplug className="w-4 h-4" />
                Disconnect
              </button>
            </div>
          </div>

          {/* Business Application Features (Where to go next) */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div
              onClick={() => navigate('/automation')}
              className="p-5 rounded-2xl bg-white border border-gray-200 shadow-sm hover:border-brand-300 hover:shadow-md transition-all cursor-pointer group"
            >
              <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
                <Zap className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-bold text-gray-900 group-hover:text-brand-600 transition-colors">
                Bot Auto-Replies
              </h3>
              <p className="text-xs text-gray-500 mt-1 leading-relaxed">
                Set up automated answers and Gemini AI bot for customer questions.
              </p>
            </div>

            <div
              onClick={() => navigate('/inbox')}
              className="p-5 rounded-2xl bg-white border border-gray-200 shadow-sm hover:border-brand-300 hover:shadow-md transition-all cursor-pointer group"
            >
              <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
                <MessageSquare className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-bold text-gray-900 group-hover:text-brand-600 transition-colors">
                Customer Chats
              </h3>
              <p className="text-xs text-gray-500 mt-1 leading-relaxed">
                Chat live with customers and view full conversation history.
              </p>
            </div>

            <div
              onClick={() => navigate('/templates')}
              className="p-5 rounded-2xl bg-white border border-gray-200 shadow-sm hover:border-brand-300 hover:shadow-md transition-all cursor-pointer group"
            >
              <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
                <FileText className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-bold text-gray-900 group-hover:text-brand-600 transition-colors">
                WhatsApp Templates
              </h3>
              <p className="text-xs text-gray-500 mt-1 leading-relaxed">
                Manage approved templates for customer notifications and outreach.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* STATE 3: NOT CONNECTED */}
      {!isConnecting && !isConnected && (
        <div className="bg-white p-6 sm:p-8 rounded-2xl border border-gray-200 shadow-sm max-w-2xl mx-auto space-y-6 text-center">
          <div className="w-16 h-16 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto">
            <Smartphone className="w-8 h-8" />
          </div>

          <div>
            <h2 className="text-xl font-bold text-gray-900">
              Connect Your WhatsApp Business Number
            </h2>
            <p className="text-sm text-gray-600 mt-1 max-w-md mx-auto">
              Link your official WhatsApp Business number via Meta Cloud API to start automating customer replies.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
            <button
              type="button"
              onClick={launchSignup}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2.5 px-6 py-3 text-sm font-semibold text-white bg-[#1877F2] hover:bg-[#166fe5] rounded-xl shadow-sm min-h-[46px] transition-colors"
            >
              <Smartphone className="w-5 h-5" />
              Connect with Facebook
            </button>
            <button
              type="button"
              onClick={() => setShowManualModal(true)}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-3 text-sm font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 border border-slate-300 rounded-xl min-h-[46px] transition-colors"
            >
              Enter Meta Credentials
            </button>
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

      {/* Manual Meta Developer Credentials Modal */}
      {showManualModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-gray-100 space-y-5 animate-in fade-in">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h3 className="text-lg font-bold text-gray-900">
                  Update Meta Access Token
                </h3>
                <p className="text-xs text-gray-500 mt-0.5">
                  Paste credentials from developers.facebook.com WhatsApp Quickstart
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowManualModal(false)}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100 text-lg leading-none"
              >
                ✕
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                directConnectMutation.mutate(manualForm);
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Phone Number ID <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 102938475610293"
                  value={manualForm.phoneNumberId}
                  onChange={(e) => setManualForm({ ...manualForm, phoneNumberId: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-sm focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  WhatsApp Business Account ID (WABA ID) <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 987654321098765"
                  value={manualForm.wabaId}
                  onChange={(e) => setManualForm({ ...manualForm, wabaId: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-sm focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Display WhatsApp Number <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. +91 9876543210"
                  value={manualForm.displayPhoneNumber}
                  onChange={(e) => setManualForm({ ...manualForm, displayPhoneNumber: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-sm focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Meta Access Token <span className="text-red-500">*</span>
                </label>
                <textarea
                  required
                  rows={3}
                  placeholder="Paste your Temporary or System User Access Token (EAAG...)"
                  value={manualForm.rawAccessToken}
                  onChange={(e) => setManualForm({ ...manualForm, rawAccessToken: e.target.value })}
                  className="w-full px-3.5 py-2 bg-gray-50 border border-gray-300 rounded-xl text-xs font-mono focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowManualModal(false)}
                  className="px-4 py-2.5 rounded-xl bg-gray-100 hover:bg-gray-200 text-xs font-semibold text-gray-700 min-h-[44px]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={directConnectMutation.isPending}
                  className="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-xs font-bold text-white shadow-md shadow-brand-500/20 min-h-[44px]"
                >
                  {directConnectMutation.isPending ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Saving...
                    </>
                  ) : (
                    'Save Token'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default WhatsAppConnectionScreen;
