import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import {
  AutomationRuleResponse,
  CreateAutomationRuleRequest,
  MatchType,
  ActionType,
  TestRuleResponse,
} from '../../api/types';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Skeleton } from '../../components/Skeleton';
import { AlertBanner } from '../../components/AlertBanner';
import {
  Zap,
  Plus,
  Trash2,
  Edit2,
  TestTube2,
  CheckCircle2,
  XCircle,
  Loader2,
  X,
  ToggleLeft,
  ToggleRight,
} from 'lucide-react';

export const AutomationRulesScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const [editingRuleId, setEditingRuleId] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Form State
  const [name, setName] = useState('');
  const [matchType, setMatchType] = useState<MatchType>('CONTAINS');
  const [matchValue, setMatchValue] = useState('');
  const [caseSensitive, setCaseSensitive] = useState(false);
  const [showRegexOption, setShowRegexOption] = useState(false);
  const [actionType, setActionType] = useState<ActionType>('REPLY_TEXT');
  const [actionPayload, setActionPayload] = useState('');

  // Live Tester State
  const [testMessage, setTestMessage] = useState('');
  const [testResult, setTestResult] = useState<TestRuleResponse | null>(null);
  const [isTesting, setIsTesting] = useState(false);

  // 1. Fetch rules
  const {
    data: rules,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<AutomationRuleResponse[]>({
    queryKey: ['automation-rules'],
    queryFn: () => apiClient<AutomationRuleResponse[]>('/api/automation-rules'),
  });

  // 2. Save rule mutation (Create or Update)
  const saveRuleMutation = useMutation({
    mutationFn: (req: CreateAutomationRuleRequest) => {
      if (editingRuleId) {
        return apiClient<AutomationRuleResponse>(`/api/automation-rules/${editingRuleId}`, {
          method: 'PUT',
          body: JSON.stringify(req),
        });
      }
      return apiClient<AutomationRuleResponse>('/api/automation-rules', {
        method: 'POST',
        body: JSON.stringify(req),
      });
    },
    onSuccess: (savedRule) => {
      queryClient.invalidateQueries({ queryKey: ['automation-rules'] });
      setShowModal(false);
      const isEdit = !!editingRuleId;
      resetForm();
      setSuccessMsg(isEdit ? `Rule "${savedRule.name}" updated successfully.` : `Rule "${savedRule.name}" created successfully.`);
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to save rule.');
    },
  });

  // 3. Toggle rule active state
  const toggleRuleMutation = useMutation({
    mutationFn: (id: string) =>
      apiClient<AutomationRuleResponse>(`/api/automation-rules/${id}/toggle`, { method: 'PATCH' }),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['automation-rules'] });
      setSuccessMsg(`Rule "${updated.name}" is now ${updated.enabled ? 'active' : 'disabled'}.`);
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to update rule status.');
    },
  });

  // 4. Delete rule mutation
  const deleteRuleMutation = useMutation({
    mutationFn: (id: string) =>
      apiClient(`/api/automation-rules/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['automation-rules'] });
      setSuccessMsg('Rule deleted successfully.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to delete rule.');
    },
  });

  const openEditModal = (rule: AutomationRuleResponse) => {
    setEditingRuleId(rule.id);
    setName(rule.name);
    setMatchType(rule.matchType);
    setMatchValue(rule.matchValue);
    setCaseSensitive(rule.caseSensitive);
    setShowRegexOption(rule.matchType === 'REGEX');
    setActionType(rule.actionType);
    setActionPayload(rule.actionPayload);
    setShowModal(true);
  };

  const resetForm = () => {
    setEditingRuleId(null);
    setName('');
    setMatchType('CONTAINS');
    setMatchValue('');
    setCaseSensitive(false);
    setShowRegexOption(false);
    setActionType('REPLY_TEXT');
    setActionPayload('');
    setTestMessage('');
    setTestResult(null);
  };

  const handleTestRule = async () => {
    if (!testMessage.trim() || !matchValue.trim()) return;
    setIsTesting(true);
    try {
      const res = await apiClient<TestRuleResponse>('/api/automation-rules/test', {
        method: 'POST',
        body: JSON.stringify({
          messageText: testMessage.trim(),
        }),
      });
      setTestResult(res);
    } catch (err: any) {
      setTestResult({ matched: false });
    } finally {
      setIsTesting(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !matchValue.trim()) return;

    saveRuleMutation.mutate({
      name: name.trim(),
      matchType,
      matchValue: matchValue.trim(),
      caseSensitive,
      actionType,
      actionPayload: actionPayload.trim(),
      priority: (rules?.length || 0) + 1,
      enabled: true,
    });
  };

  const formatMatchSummary = (rule: AutomationRuleResponse) => {
    switch (rule.matchType) {
      case 'EXACT':
        return `When the message is exactly "${rule.matchValue}"`;
      case 'CONTAINS':
        return `When the message contains "${rule.matchValue}"`;
      case 'STARTS_WITH':
        return `When the message starts with "${rule.matchValue}"`;
      case 'REGEX':
        return `When the message matches regex /${rule.matchValue}/`;
      default:
        return `Matches "${rule.matchValue}"`;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Zap className="w-6 h-6 text-brand-600" />
            Keyword Automation Rules
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Instant automated replies triggered when inbound WhatsApp messages match specific keywords.
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-brand-600 text-white font-medium rounded-xl hover:bg-brand-700 min-h-[44px] transition-colors shadow-sm self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          Create Rule
        </button>
      </div>

      {/* Notifications */}
      {successMsg && (
        <AlertBanner type="success" message={successMsg} onClose={() => setSuccessMsg(null)} />
      )}
      {errorMsg && (
        <AlertBanner type="error" message={errorMsg} onClose={() => setErrorMsg(null)} />
      )}

      {/* Rules List */}
      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full rounded-xl" />
          <Skeleton className="h-20 w-full rounded-xl" />
          <Skeleton className="h-20 w-full rounded-xl" />
        </div>
      ) : isError ? (
        <ErrorState
          title="Could not load automation rules"
          message={(error as any)?.message || 'Something went wrong.'}
          onRetry={refetch}
        />
      ) : !rules || rules.length === 0 ? (
        <EmptyState
          icon={Zap}
          title="No auto-reply rules configured"
          description="Create keyword-based triggers to automatically respond to frequently asked inquiries 24/7."
          actionLabel="Create Your First Auto-Reply"
          onAction={() => {
            resetForm();
            setShowModal(true);
          }}
        />
      ) : (
        <div className="space-y-3">
          {rules.map((rule, idx) => (
            <div
              key={rule.id}
              className={`bg-white p-4 sm:p-5 rounded-xl border transition-all ${
                rule.enabled ? 'border-gray-200 shadow-sm' : 'border-gray-200/60 bg-gray-50/50 opacity-75'
              } flex flex-col sm:flex-row sm:items-center justify-between gap-4`}
            >
              <div className="flex items-start gap-3">
                <span className="w-7 h-7 rounded-lg bg-gray-100 flex items-center justify-center text-xs font-bold text-gray-700 shrink-0 mt-0.5">
                  #{idx + 1}
                </span>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-gray-900 text-sm sm:text-base">{rule.name}</h3>
                    <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-brand-50 text-brand-700 border border-brand-200">
                      {rule.matchType}
                    </span>
                    {!rule.enabled && (
                      <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-gray-100 text-gray-500">
                        Disabled
                      </span>
                    )}
                  </div>
                  <p className="text-xs sm:text-sm text-gray-600 mt-1">
                    {formatMatchSummary(rule)}
                  </p>
                  <div className="text-xs text-slate-500 mt-1.5 line-clamp-1">
                    ↳ Reply: {rule.actionPayload || '(Hand over to human)'}
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-2 self-end sm:self-center">
                {/* Active Toggle Button */}
                <button
                  type="button"
                  onClick={() => toggleRuleMutation.mutate(rule.id)}
                  disabled={toggleRuleMutation.isPending}
                  className={`p-2 rounded-lg min-h-[44px] min-w-[44px] flex items-center justify-center transition-colors ${
                    rule.enabled
                      ? 'text-emerald-600 hover:bg-emerald-50'
                      : 'text-gray-400 hover:bg-gray-100'
                  }`}
                  title={rule.enabled ? 'Disable rule' : 'Enable rule'}
                >
                  {rule.enabled ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6" />}
                </button>

                {/* Edit Button */}
                <button
                  type="button"
                  onClick={() => openEditModal(rule)}
                  className="p-2 text-gray-500 hover:text-brand-600 hover:bg-brand-50 rounded-lg min-h-[44px] min-w-[44px] flex items-center justify-center transition-colors"
                  title="Edit rule"
                >
                  <Edit2 className="w-4 h-4" />
                </button>

                {/* Delete Button */}
                <button
                  type="button"
                  onClick={() => deleteRuleMutation.mutate(rule.id)}
                  className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg min-h-[44px] min-w-[44px] flex items-center justify-center transition-colors"
                  title="Delete rule"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create / Edit Rule Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-gray-100 space-y-5 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <h2 className="text-lg font-bold text-gray-900">
                {editingRuleId ? 'Edit Automation Rule' : 'New Automation Rule'}
              </h2>
              <button
                type="button"
                onClick={() => setShowModal(false)}
                className="text-gray-400 hover:text-gray-600 p-1 rounded-lg min-h-[44px] min-w-[44px] flex items-center justify-center"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Rule Name */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Rule Name
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  placeholder="e.g. Price Enquiry"
                />
              </div>

              {/* Match Type */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    When message...
                  </label>
                  <select
                    value={matchType}
                    onChange={(e) => setMatchType(e.target.value as MatchType)}
                    className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 bg-white"
                  >
                    <option value="CONTAINS">Contains keyword</option>
                    <option value="EXACT">Is exactly</option>
                    <option value="STARTS_WITH">Starts with</option>
                    {showRegexOption && <option value="REGEX">Matches regex (Advanced)</option>}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Keyword / Phrase
                  </label>
                  <input
                    type="text"
                    required
                    value={matchValue}
                    onChange={(e) => setMatchValue(e.target.value)}
                    className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm min-h-[44px] focus:ring-2 focus:ring-brand-500 focus:outline-none"
                    placeholder="e.g. price, menu, timing"
                  />
                </div>
              </div>

              {/* Advanced Regex toggle */}
              {!showRegexOption && (
                <div>
                  <button
                    type="button"
                    onClick={() => setShowRegexOption(true)}
                    className="text-xs text-brand-600 hover:text-brand-700 font-medium"
                  >
                    + Show advanced Regex option
                  </button>
                </div>
              )}

              {/* Action Response */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Automatic Response Message
                </label>
                <textarea
                  rows={3}
                  required
                  value={actionPayload}
                  onChange={(e) => setActionPayload(e.target.value)}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  placeholder="Hi! Here is our current pricing catalog: ..."
                />
              </div>

              {/* LIVE RULE TESTER BOX */}
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-3">
                <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700 uppercase tracking-wider">
                  <TestTube2 className="w-4 h-4 text-brand-600" />
                  Test It Before Saving
                </div>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={testMessage}
                    onChange={(e) => setTestMessage(e.target.value)}
                    placeholder="Type sample message (e.g. what is your price?)"
                    className="flex-1 px-3 py-2 border border-slate-300 rounded-lg text-xs min-h-[40px] focus:ring-1 focus:ring-brand-500 bg-white"
                  />
                  <button
                    type="button"
                    onClick={handleTestRule}
                    disabled={isTesting || !testMessage.trim()}
                    className="px-3 py-2 text-xs font-semibold text-white bg-slate-800 hover:bg-slate-900 rounded-lg min-h-[40px] flex items-center gap-1 shrink-0"
                  >
                    {isTesting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : 'Test'}
                  </button>
                </div>

                {testResult !== null && (
                  <div
                    className={`p-2.5 rounded-lg text-xs font-medium flex items-center gap-2 ${
                      testResult.matched
                        ? 'bg-green-100 text-green-800 border border-green-200'
                        : 'bg-red-100 text-red-800 border border-red-200'
                    }`}
                  >
                    {testResult.matched ? (
                      <>
                        <CheckCircle2 className="w-4 h-4 text-green-600 shrink-0" />
                        <span>✅ This rule would match this message!</span>
                      </>
                    ) : (
                      <>
                        <XCircle className="w-4 h-4 text-red-600 shrink-0" />
                        <span>❌ No match. Check spelling or match type.</span>
                      </>
                    )}
                  </div>
                )}
              </div>

              {/* Submit Buttons */}
              <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg min-h-[44px]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saveRuleMutation.isPending}
                  className="px-5 py-2 text-xs font-semibold text-white bg-brand-600 hover:bg-brand-700 rounded-lg min-h-[44px] shadow-sm flex items-center gap-2"
                >
                  {saveRuleMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  {editingRuleId ? 'Update Rule' : 'Save Rule'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
export default AutomationRulesScreen;
