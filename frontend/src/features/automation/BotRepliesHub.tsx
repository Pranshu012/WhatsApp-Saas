/**
 * BotRepliesHub: Unified WhatsApp Automation & AI Assistant Center.
 *
 * Capabilities:
 * - Keyword Automation Rules: exact, contains, and regex pattern auto-responders.
 * - Knowledge Base (FAQs): Business answers and questions with interactive search.
 * - Gemini AI Fallback: Automated contextual customer support powered by Google Gemini.
 * - Unanswered Inquiries Queue: Triage incoming customer questions that didn't match rules.
 * - Live WhatsApp Chatbot Simulator: Test replies in real time with interactive preview.
 */
import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import {
  AutomationRuleResponse,
  CreateAutomationRuleRequest,
  MatchType,
  TestRuleResponse,
  FaqResponse,
  CreateFaqRequest,
  TestFaqResponse,
  UnmatchedMessageResponse,
} from '../../api/types';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Skeleton } from '../../components/Skeleton';
import { AlertBanner } from '../../components/AlertBanner';
import { formatE164 } from '../../lib/phoneUtils';
import {
  Zap,
  HelpCircle,
  Inbox,
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
  Sparkles,
  Save,
  ShieldCheck,
} from 'lucide-react';

export const BotRepliesHub: React.FC = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'keywords' | 'faqs' | 'ai' | 'unanswered'>('keywords');
  const [showSimulator, setShowSimulator] = useState(false);

  // Common notification state
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // ==========================================
  // TAB 1: KEYWORD RULES STATE & QUERIES
  // ==========================================
  const [showRuleModal, setShowRuleModal] = useState(false);
  const [editingRuleId, setEditingRuleId] = useState<string | null>(null);
  const [ruleName, setRuleName] = useState('');
  const [ruleMatchType, setRuleMatchType] = useState<MatchType>('CONTAINS');
  const [ruleMatchValue, setRuleMatchValue] = useState('');
  const [ruleActionPayload, setRuleActionPayload] = useState('');

  const {
    data: rules,
    isLoading: isLoadingRules,
    isError: isErrorRules,
    error: errorRules,
  } = useQuery<AutomationRuleResponse[]>({
    queryKey: ['automation-rules'],
    queryFn: () => apiClient<AutomationRuleResponse[]>('/api/automation-rules'),
  });

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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['automation-rules'] });
      setShowRuleModal(false);
      resetRuleForm();
      setSuccessMsg('Keyword trigger rule saved successfully.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to save keyword rule.');
    },
  });

  const toggleRuleMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      apiClient<AutomationRuleResponse>(`/api/automation-rules/${id}/toggle`, {
        method: 'POST',
        body: JSON.stringify({ enabled }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['automation-rules'] });
    },
  });

  const deleteRuleMutation = useMutation({
    mutationFn: (id: string) =>
      apiClient(`/api/automation-rules/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['automation-rules'] });
      setSuccessMsg('Keyword rule deleted.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to delete rule.');
    },
  });

  const resetRuleForm = () => {
    setEditingRuleId(null);
    setRuleName('');
    setRuleMatchType('CONTAINS');
    setRuleMatchValue('');
    setRuleActionPayload('');
  };

  const openEditRule = (rule: AutomationRuleResponse) => {
    setEditingRuleId(rule.id);
    setRuleName(rule.name);
    setRuleMatchType(rule.matchType);
    setRuleMatchValue(rule.matchValue);
    let displayText = rule.actionPayload || '';
    try {
      const parsed = JSON.parse(rule.actionPayload);
      if (parsed && parsed.text) displayText = parsed.text;
    } catch {}
    setRuleActionPayload(displayText);
    setShowRuleModal(true);
  };

  // ==========================================
  // TAB 2: SMART FAQS STATE & QUERIES
  // ==========================================
  const [showFaqModal, setShowFaqModal] = useState(false);
  const [editingFaqId, setEditingFaqId] = useState<string | null>(null);
  const [faqQuestion, setFaqQuestion] = useState('');
  const [faqAnswer, setFaqAnswer] = useState('');

  const {
    data: faqs,
    isLoading: isLoadingFaqs,
    isError: isErrorFaqs,
    error: errorFaqs,
  } = useQuery<FaqResponse[]>({
    queryKey: ['faqs'],
    queryFn: () => apiClient<FaqResponse[]>('/api/faqs'),
  });

  const saveFaqMutation = useMutation({
    mutationFn: (req: CreateFaqRequest) => {
      if (editingFaqId) {
        return apiClient<FaqResponse>(`/api/faqs/${editingFaqId}`, {
          method: 'PUT',
          body: JSON.stringify(req),
        });
      }
      return apiClient<FaqResponse>('/api/faqs', {
        method: 'POST',
        body: JSON.stringify(req),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['faqs'] });
      setShowFaqModal(false);
      resetFaqForm();
      setSuccessMsg('Smart FAQ saved successfully.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to save FAQ.');
    },
  });

  const toggleFaqMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      apiClient<FaqResponse>(`/api/faqs/${id}/toggle`, {
        method: 'POST',
        body: JSON.stringify({ enabled }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['faqs'] });
    },
  });

  const deleteFaqMutation = useMutation({
    mutationFn: (id: string) => apiClient(`/api/faqs/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['faqs'] });
      setSuccessMsg('FAQ deleted.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to delete FAQ.');
    },
  });

  const resetFaqForm = () => {
    setEditingFaqId(null);
    setFaqQuestion('');
    setFaqAnswer('');
  };

  const openEditFaq = (faq: FaqResponse) => {
    setEditingFaqId(faq.id);
    setFaqQuestion(faq.question);
    setFaqAnswer(faq.answer);
    setShowFaqModal(true);
  };

  // ==========================================
  // TAB 3: UNANSWERED QUERIES
  // ==========================================
  const {
    data: unmatchedMessages,
    isLoading: isLoadingUnmatched,
    isError: isErrorUnmatched,
    error: errorUnmatched,
  } = useQuery<UnmatchedMessageResponse[]>({
    queryKey: ['unmatched-messages'],
    queryFn: () => apiClient<UnmatchedMessageResponse[]>('/api/unmatched-messages'),
  });

  const deleteUnmatchedMutation = useMutation({
    mutationFn: (id: string) => apiClient(`/api/unmatched-messages/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['unmatched-messages'] });
      setSuccessMsg('Inquiry dismissed.');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to dismiss inquiry.');
    },
  });

  const convertToFaq = (text: string, id: string) => {
    setEditingFaqId(null);
    setFaqQuestion(text);
    setFaqAnswer('');
    setShowFaqModal(true);
    deleteUnmatchedMutation.mutate(id);
  };

  // ==========================================
  // TAB 4: AI RECEPTIONIST (GEMINI) STATE
  // ==========================================
  interface AiConfigDto {
    enabled: boolean;
    apiKey: string;
    model: string;
    businessContext: string;
    systemPrompt: string;
    monthlyLimit: number;
    usedThisMonth: number;
  }

  const { data: aiConfig } = useQuery<AiConfigDto>({
    queryKey: ['ai-config'],
    queryFn: () => apiClient<AiConfigDto>('/api/ai/config'),
  });

  const [aiEnabled, setAiEnabled] = useState(true);
  const [aiApiKey, setAiApiKey] = useState('');
  const [aiModel, setAiModel] = useState('gemini-flash-latest');
  const [aiBusinessContext, setAiBusinessContext] = useState('');
  const [aiSystemPrompt, setAiSystemPrompt] = useState('');
  const [aiMonthlyLimit, setAiMonthlyLimit] = useState(500);
  const aiUsedThisMonth = aiConfig?.usedThisMonth || 0;

  useEffect(() => {
    if (aiConfig) {
      setAiEnabled(aiConfig.enabled);
      setAiApiKey(aiConfig.apiKey || '');
      setAiModel(aiConfig.model || 'gemini-flash-latest');
      setAiBusinessContext(aiConfig.businessContext || '');
      setAiSystemPrompt(aiConfig.systemPrompt || '');
      setAiMonthlyLimit(aiConfig.monthlyLimit || 500);
    }
  }, [aiConfig]);

  const saveAiMutation = useMutation({
    mutationFn: (req: {
      enabled: boolean;
      apiKey: string;
      model: string;
      businessContext: string;
      systemPrompt: string;
      monthlyLimit?: number;
    }) =>
      apiClient<AiConfigDto>('/api/ai/config', {
        method: 'PUT',
        body: JSON.stringify(req),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ai-config'] });
      setSuccessMsg('✨ AI Receptionist configuration saved successfully!');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to save AI configuration.');
    },
  });


  // ==========================================
  // LIVE SIMULATOR STATE
  // ==========================================
  const [simQuery, setSimQuery] = useState('');
  const [simResult, setSimResult] = useState<{
    matched: boolean;
    source: 'keyword' | 'faq' | 'ai' | 'none';
    title?: string;
    reply?: string;
    confidence?: number;
  } | null>(null);
  const [isSimulating, setIsSimulating] = useState(false);

  const runSimulation = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!simQuery.trim()) return;
    setIsSimulating(true);
    setSimResult(null);

    try {
      // 1. Test keyword rule match
      const ruleRes = await apiClient<TestRuleResponse>('/api/automation-rules/test', {
        method: 'POST',
        body: JSON.stringify({ messageText: simQuery }),
      });

      if (ruleRes.matched && ruleRes.ruleName) {
        const found = rules?.find((r) => r.id === ruleRes.ruleId);
        let replyText = found?.actionPayload || 'Auto-reply matched';
        try {
          const parsed = JSON.parse(replyText);
          if (parsed && parsed.text) replyText = parsed.text;
        } catch {}

        setSimResult({
          matched: true,
          source: 'keyword',
          title: `Keyword Trigger: "${ruleRes.ruleName}"`,
          reply: replyText,
        });
        setIsSimulating(false);
        return;
      }

      // 2. Test FAQ match
      const faqRes = await apiClient<TestFaqResponse>('/api/faqs/test', {
        method: 'POST',
        body: JSON.stringify({ question: simQuery }),
      });

      if (faqRes.isConfident && faqRes.question && faqRes.answer) {
        setSimResult({
          matched: true,
          source: 'faq',
          title: `Smart FAQ: "${faqRes.question}"`,
          reply: faqRes.answer,
          confidence: faqRes.confidenceScore,
        });
        setIsSimulating(false);
        return;
      }

      // 3. Test Gemini AI Assistant
      try {
        const aiRes = await apiClient<{ success: boolean; reply?: string; message?: string }>('/api/ai/test', {
          method: 'POST',
          body: JSON.stringify({ message: simQuery }),
        });

        if (aiRes.success && aiRes.reply) {
          setSimResult({
            matched: true,
            source: 'ai',
            title: '✨ Answered by Gemini AI Receptionist',
            reply: aiRes.reply,
          });
          setIsSimulating(false);
          return;
        }
      } catch {}

      // Fallback: Unmatched
      setSimResult({
        matched: false,
        source: 'none',
        reply: "No rule, FAQ, or AI response matched. This inquiry would appear in 'Unanswered Inquiries' for human review.",
      });
    } catch (err: any) {
      setSimResult({
        matched: false,
        source: 'none',
        reply: `Test error: ${err.message || 'Could not run test'}`,
      });
    } finally {
      setIsSimulating(false);
    }
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-gray-200">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-brand-500 text-white flex items-center justify-center shadow-md shadow-brand-500/20">
              <Zap className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-xl sm:text-2xl font-bold text-gray-900">Bot Auto-Replies</h1>
              <p className="text-xs sm:text-sm text-gray-500">
                Manage your bot's answers, questions, and automated WhatsApp responses.
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setShowSimulator(!showSimulator)}
            className={`inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold rounded-xl border transition-colors min-h-[44px] ${
              showSimulator
                ? 'bg-brand-600 text-white border-brand-600 shadow-md shadow-brand-500/20'
                : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50'
            }`}
          >
            <Sparkles className="w-4 h-4 text-brand-400" />
            {showSimulator ? 'Close Live Tester' : 'Test Bot Live'}
          </button>

          {activeTab === 'keywords' && (
            <button
              type="button"
              onClick={() => {
                resetRuleForm();
                setShowRuleModal(true);
              }}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-md shadow-brand-500/20 transition-colors min-h-[44px]"
            >
              <Plus className="w-4 h-4" />
              Add Keyword Rule
            </button>
          )}

          {activeTab === 'faqs' && (
            <button
              type="button"
              onClick={() => {
                resetFaqForm();
                setShowFaqModal(true);
              }}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-md shadow-brand-500/20 transition-colors min-h-[44px]"
            >
              <Plus className="w-4 h-4" />
              Add New FAQ
            </button>
          )}
        </div>
      </div>

      {/* Notifications */}
      {successMsg && (
        <AlertBanner type="success" message={successMsg} onClose={() => setSuccessMsg(null)} />
      )}
      {errorMsg && (
        <AlertBanner type="error" message={errorMsg} onClose={() => setErrorMsg(null)} />
      )}

      {/* Live Interactive Simulator Drawer (Collapsible) */}
      {showSimulator && (
        <div className="p-6 bg-gradient-to-r from-slate-900 to-indigo-950 text-white rounded-2xl shadow-xl border border-slate-800 space-y-4 animate-in fade-in">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <Sparkles className="w-5 h-5 text-amber-400" />
              <h3 className="font-bold text-base text-white">Live Bot Simulator</h3>
              <span className="text-[11px] bg-brand-500/20 text-brand-300 px-2 py-0.5 rounded-full border border-brand-500/30">
                Instant Local Test
              </span>
            </div>
            <button
              onClick={() => setShowSimulator(false)}
              className="text-slate-400 hover:text-white text-sm"
            >
              ✕
            </button>
          </div>
          <p className="text-xs text-slate-300">
            Type anything a customer might say (e.g. <span className="font-semibold text-amber-300">"Price"</span>, <span className="font-semibold text-amber-300">"Wat r your timins?"</span>). The engine checks both Keyword triggers and Typo-Tolerant FAQs.
          </p>
          <form onSubmit={runSimulation} className="flex gap-2">
            <input
              type="text"
              value={simQuery}
              onChange={(e) => setSimQuery(e.target.value)}
              placeholder="Type a test customer message..."
              className="flex-1 px-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-sm text-white placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <button
              type="submit"
              disabled={isSimulating || !simQuery.trim()}
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-brand-500 hover:bg-brand-600 disabled:opacity-50 text-white text-sm font-semibold rounded-xl transition-colors min-h-[44px]"
            >
              {isSimulating ? <Loader2 className="w-4 h-4 animate-spin" /> : <TestTube2 className="w-4 h-4" />}
              Test
            </button>
          </form>

          {simResult && (
            <div
              className={`p-4 rounded-xl text-xs space-y-2 border ${
                simResult.matched
                  ? 'bg-emerald-950/60 border-emerald-500/30 text-emerald-100'
                  : 'bg-amber-950/60 border-amber-500/30 text-amber-100'
              }`}
            >
              <div className="flex items-center justify-between font-bold text-sm">
                <span className="flex items-center gap-1.5">
                  {simResult.matched ? (
                    <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  ) : (
                    <XCircle className="w-4 h-4 text-amber-400" />
                  )}
                  {simResult.title || (simResult.matched ? 'Matched' : 'Unmatched')}
                </span>
                {simResult.confidence !== undefined && (
                  <span className="text-[11px] bg-slate-800 text-slate-300 px-2 py-0.5 rounded-full">
                    Match Confidence: {Math.round(simResult.confidence * 100)}%
                  </span>
                )}
              </div>
              <div className="p-3 bg-slate-900/80 rounded-lg border border-slate-800 font-sans text-xs whitespace-pre-wrap text-white">
                {simResult.reply}
              </div>
            </div>
          )}
        </div>
      )}

      {/* 3 Core Tabs Switcher */}
      <div className="flex items-center gap-2 p-1.5 bg-gray-100/80 rounded-2xl border border-gray-200 flex-wrap sm:flex-nowrap">
        <button
          type="button"
          onClick={() => setActiveTab('keywords')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs sm:text-sm font-bold transition-all min-w-[130px] ${
            activeTab === 'keywords'
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          <Zap className="w-4 h-4 text-amber-500" />
          <span>Keywords</span>
          <span className="px-2 py-0.5 rounded-full text-[11px] bg-gray-100 text-gray-700">
            {rules?.length || 0}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('faqs')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs sm:text-sm font-bold transition-all min-w-[130px] ${
            activeTab === 'faqs'
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          <HelpCircle className="w-4 h-4 text-blue-500" />
          <span>Smart FAQs</span>
          <span className="px-2 py-0.5 rounded-full text-[11px] bg-gray-100 text-gray-700">
            {faqs?.length || 0}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('ai')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs sm:text-sm font-bold transition-all min-w-[150px] ${
            activeTab === 'ai'
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          <Sparkles className="w-4 h-4 text-purple-600" />
          <span>AI Receptionist</span>
          <span className="px-2 py-0.5 rounded-full text-[10px] bg-purple-100 text-purple-800 font-bold uppercase tracking-wider">
            Gemini
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('unanswered')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs sm:text-sm font-bold transition-all min-w-[130px] ${
            activeTab === 'unanswered'
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          <Inbox className="w-4 h-4 text-purple-500" />
          <span>Unanswered</span>
          <span
            className={`px-2 py-0.5 rounded-full text-[11px] font-bold ${
              (unmatchedMessages?.length || 0) > 0
                ? 'bg-amber-100 text-amber-800'
                : 'bg-gray-100 text-gray-700'
            }`}
          >
            {unmatchedMessages?.length || 0}
          </span>
        </button>
      </div>

      {/* ======================================================== */}
      {/* TAB 1 CONTENT: KEYWORD TRIGGERS                          */}
      {/* ======================================================== */}
      {activeTab === 'keywords' && (
        <div className="space-y-4">
          <div className="p-4 bg-brand-50/60 rounded-2xl border border-brand-100 text-xs text-brand-900 flex items-start gap-3">
            <Zap className="w-5 h-5 text-brand-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-brand-950">How Keywords Work: </span>
              Whenever an incoming WhatsApp message contains a specific trigger word (like <code className="bg-white px-1.5 py-0.5 rounded font-bold border border-brand-200">PRICE</code> or <code className="bg-white px-1.5 py-0.5 rounded font-bold border border-brand-200">MENU</code>), your bot automatically delivers the response below in under 1 second.
            </div>
          </div>

          {isLoadingRules ? (
            <div className="space-y-3">
              <Skeleton className="h-20 rounded-2xl" />
              <Skeleton className="h-20 rounded-2xl" />
            </div>
          ) : isErrorRules ? (
            <ErrorState title="Failed to load keyword rules" message={errorRules?.message} />
          ) : rules && rules.length > 0 ? (
            <div className="grid grid-cols-1 gap-3">
              {rules.map((rule) => {
                let cleanReply = rule.actionPayload;
                try {
                  const p = JSON.parse(rule.actionPayload);
                  if (p && p.text) cleanReply = p.text;
                } catch {}

                return (
                  <div
                    key={rule.id}
                    className="p-5 bg-white rounded-2xl border border-gray-200 shadow-sm flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 hover:border-gray-300 transition-colors"
                  >
                    <div className="space-y-1.5 max-w-2xl">
                      <div className="flex items-center gap-2.5">
                        <span className="font-bold text-gray-900 text-base">{rule.name}</span>
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono bg-gray-100 text-gray-700 border border-gray-200">
                          keyword: <strong>"{rule.matchValue}"</strong>
                        </span>
                        {!rule.enabled && (
                          <span className="text-[10px] bg-red-50 text-red-700 font-semibold px-2 py-0.5 rounded-full border border-red-200">
                            Disabled
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-gray-600 bg-gray-50/80 p-2.5 rounded-xl border border-gray-100 font-sans whitespace-pre-wrap">
                        {cleanReply}
                      </p>
                    </div>

                    <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                      <button
                        type="button"
                        onClick={() =>
                          toggleRuleMutation.mutate({ id: rule.id, enabled: !rule.enabled })
                        }
                        className="p-2 text-gray-400 hover:text-gray-700 rounded-lg hover:bg-gray-100 transition-colors"
                        title={rule.enabled ? 'Disable' : 'Enable'}
                      >
                        {rule.enabled ? (
                          <ToggleRight className="w-6 h-6 text-brand-600" />
                        ) : (
                          <ToggleLeft className="w-6 h-6 text-gray-400" />
                        )}
                      </button>
                      <button
                        type="button"
                        onClick={() => openEditRule(rule)}
                        className="p-2 text-gray-500 hover:text-gray-900 rounded-lg hover:bg-gray-100 transition-colors"
                        title="Edit"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          if (confirm('Delete this rule?')) deleteRuleMutation.mutate(rule.id);
                        }}
                        className="p-2 text-red-500 hover:text-red-700 rounded-lg hover:bg-red-50 transition-colors"
                        title="Delete"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <EmptyState
              icon={Zap}
              title="No Keyword Triggers Yet"
              description="Create simple instant triggers like 'PRICE', 'MENU', or 'LOCATION'."
              actionLabel="Add First Rule"
              onAction={() => {
                resetRuleForm();
                setShowRuleModal(true);
              }}
            />
          )}
        </div>
      )}

      {/* ======================================================== */}
      {/* TAB 2 CONTENT: SMART FAQS                                */}
      {/* ======================================================== */}
      {activeTab === 'faqs' && (
        <div className="space-y-4">
          <div className="p-4 bg-blue-50/60 rounded-2xl border border-blue-100 text-xs text-blue-900 flex items-start gap-3">
            <HelpCircle className="w-5 h-5 text-blue-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-blue-950">How Smart FAQs Work: </span>
              Customers can ask in their own words (e.g. <em>"what are your consultation hours?"</em> or <em>"kab khula hai"</em>). The bot handles typos and matches with confidence.
            </div>
          </div>

          {isLoadingFaqs ? (
            <div className="space-y-3">
              <Skeleton className="h-20 rounded-2xl" />
              <Skeleton className="h-20 rounded-2xl" />
            </div>
          ) : isErrorFaqs ? (
            <ErrorState title="Failed to load FAQs" message={errorFaqs?.message} />
          ) : faqs && faqs.length > 0 ? (
            <div className="grid grid-cols-1 gap-3">
              {faqs.map((faq) => (
                <div
                  key={faq.id}
                  className="p-5 bg-white rounded-2xl border border-gray-200 shadow-sm flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 hover:border-gray-300 transition-colors"
                >
                  <div className="space-y-1.5 max-w-2xl">
                    <div className="flex items-center gap-2">
                      <h4 className="font-bold text-gray-900 text-sm sm:text-base">
                        Q: {faq.question}
                      </h4>
                      {!faq.enabled && (
                        <span className="text-[10px] bg-red-50 text-red-700 font-semibold px-2 py-0.5 rounded-full border border-red-200">
                          Disabled
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-gray-600 bg-gray-50/80 p-2.5 rounded-xl border border-gray-100 whitespace-pre-wrap">
                      A: {faq.answer}
                    </p>
                  </div>

                  <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                    <button
                      type="button"
                      onClick={() =>
                        toggleFaqMutation.mutate({ id: faq.id, enabled: !faq.enabled })
                      }
                      className="p-2 text-gray-400 hover:text-gray-700 rounded-lg hover:bg-gray-100 transition-colors"
                      title={faq.enabled ? 'Disable' : 'Enable'}
                    >
                      {faq.enabled ? (
                        <ToggleRight className="w-6 h-6 text-brand-600" />
                      ) : (
                        <ToggleLeft className="w-6 h-6 text-gray-400" />
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={() => openEditFaq(faq)}
                      className="p-2 text-gray-500 hover:text-gray-900 rounded-lg hover:bg-gray-100 transition-colors"
                      title="Edit"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        if (confirm('Delete this FAQ?')) deleteFaqMutation.mutate(faq.id);
                      }}
                      className="p-2 text-red-500 hover:text-red-700 rounded-lg hover:bg-red-50 transition-colors"
                      title="Delete"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={HelpCircle}
              title="No FAQs Yet"
              description="Add your business's top questions like timings, address, or cancellation policies."
              actionLabel="Add First FAQ"
              onAction={() => {
                resetFaqForm();
                setShowFaqModal(true);
              }}
            />
          )}
        </div>
      )}

      {/* ======================================================== */}
      {/* TAB: AI RECEPTIONIST (GEMINI)                           */}
      {/* ======================================================== */}
      {activeTab === 'ai' && (
        <div className="space-y-6">
          {/* Header Banner */}
          <div className="p-5 bg-gradient-to-r from-purple-50 via-indigo-50 to-purple-50 rounded-2xl border border-purple-200/70 text-purple-950 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-start gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-purple-600 text-white flex items-center justify-center shadow-md shadow-purple-500/20 shrink-0">
                <Sparkles className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-bold text-base text-purple-950 flex items-center gap-2">
                  Gemini AI Receptionist
                  <span className="text-[11px] font-semibold bg-purple-200 text-purple-800 px-2 py-0.5 rounded-full">
                    Powered by Google
                  </span>
                </h3>
                <p className="text-xs text-purple-800/80 mt-0.5">
                  Reads your business knowledge and replies to customers naturally in Hindi, English, or Hinglish.
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3 shrink-0">
              <span className="text-xs font-bold text-gray-700">
                AI Status:
              </span>
              <button
                type="button"
                onClick={() => setAiEnabled(!aiEnabled)}
                className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all min-h-[40px] ${
                  aiEnabled
                    ? 'bg-purple-600 text-white shadow-md shadow-purple-500/20'
                    : 'bg-gray-200 text-gray-700'
                }`}
              >
                {aiEnabled ? <ToggleRight className="w-5 h-5 text-white" /> : <ToggleLeft className="w-5 h-5 text-gray-500" />}
                {aiEnabled ? 'Enabled (Active)' : 'Disabled'}
              </button>
            </div>
          </div>

          {/* Configuration Form */}
          <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm space-y-6">
            <div>
              <div className="mb-2">
                <label className="block text-sm font-bold text-gray-900">
                  Business Knowledge Base & Facts
                </label>
              </div>
              <p className="text-xs text-gray-500 mb-2">
                Enter your business facts (timing, address, doctor/stylist names, consultation fees, services). The AI will answer any customer inquiry strictly based on these facts.
              </p>
              <textarea
                rows={7}
                value={aiBusinessContext}
                onChange={(e) => setAiBusinessContext(e.target.value)}
                placeholder="Business Name: Sharma Dental Clinic&#10;Location: 123 MG Road, Indiranagar, Bengaluru&#10;Timings: Mon-Sat 10:00 AM - 8:00 PM (Sunday Closed)&#10;Consultation Fee: ₹400&#10;Doctors: Dr. Sharma (Dentist), Dr. Ananya (Braces Specialist)"
                className="w-full px-4 py-3 bg-gray-50 border border-gray-300 rounded-xl text-sm font-normal text-gray-900 focus:bg-white focus:ring-2 focus:ring-purple-500 focus:outline-none"
              />
            </div>

            {/* Model & API Key Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t border-gray-100">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  AI Model
                </label>
                <select
                  value={aiModel}
                  onChange={(e) => setAiModel(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-sm font-medium focus:bg-white focus:ring-2 focus:ring-purple-500 focus:outline-none"
                >
                  <option value="gemini-flash-latest">Gemini Flash (Super Fast & Smart - Recommended)</option>
                  <option value="gemini-pro-latest">Gemini Pro (Deep Reasoning & Complex Queries)</option>
                </select>
                <p className="text-[11px] text-gray-400 mt-1">
                  Flash is recommended for instant 0.5s WhatsApp responses.
                </p>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Google Gemini API Key
                </label>
                <input
                  type="password"
                  value={aiApiKey}
                  onChange={(e) => setAiApiKey(e.target.value)}
                  placeholder="Paste your AI Studio key (or leave blank to use system key)"
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-sm font-normal focus:bg-white focus:ring-2 focus:ring-purple-500 focus:outline-none"
                />
                <p className="text-[11px] text-gray-400 mt-1">
                  Get a free key from <a href="https://aistudio.google.com" target="_blank" rel="noreferrer" className="text-purple-600 underline">aistudio.google.com</a> (1,500 queries/day free).
                </p>
              </div>
            </div>

            {/* Token Shield & Monthly Quota Guard */}
            <div className="p-4 bg-gradient-to-r from-amber-50 to-orange-50 rounded-xl border border-amber-200">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-lg bg-amber-600 text-white flex items-center justify-center shrink-0">
                    <ShieldCheck className="w-4 h-4" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-amber-950 flex items-center gap-2">
                      Token Shield & Quota Guard
                      <span className="text-[10px] font-semibold bg-amber-200 text-amber-900 px-2 py-0.5 rounded-full">
                        Cost Protection
                      </span>
                    </h4>
                    <p className="text-xs text-amber-800/80">
                      Limits AI calls per month so customers cannot drain your Google Gemini quota.
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-lg border border-amber-200 text-xs font-semibold text-amber-900">
                  <span>Usage this month:</span>
                  <span className="font-bold text-amber-700">{aiUsedThisMonth} / {aiMonthlyLimit}</span>
                </div>
              </div>

              {/* Progress Bar */}
              <div className="w-full bg-amber-200/60 rounded-full h-2.5 mb-3 overflow-hidden">
                <div
                  className={`h-2.5 rounded-full transition-all duration-500 ${
                    aiUsedThisMonth >= aiMonthlyLimit
                      ? 'bg-red-600'
                      : aiUsedThisMonth / (aiMonthlyLimit || 1) > 0.8
                      ? 'bg-amber-600'
                      : 'bg-emerald-600'
                  }`}
                  style={{
                    width: `${Math.min(100, Math.round((aiUsedThisMonth / (aiMonthlyLimit || 1)) * 100))}%`,
                  }}
                />
              </div>

              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-gray-700 whitespace-nowrap">
                    Plan Quota:
                  </span>
                  <span className="px-2.5 py-1 bg-white border border-amber-300 rounded-lg text-xs font-extrabold text-amber-950 shadow-sm">
                    {aiMonthlyLimit} messages / month
                  </span>
                </div>
                <p className="text-[11px] text-amber-900/80">
                  {aiUsedThisMonth >= aiMonthlyLimit
                    ? '⚠️ Monthly limit reached! AI replies paused until next month. Contact admin to upgrade.'
                    : `Remaining: ${Math.max(0, aiMonthlyLimit - aiUsedThisMonth)} AI replies available.`}
                </p>
              </div>
            </div>

            {/* Save Button */}
            <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
              <button
                type="button"
                onClick={() => {
                  saveAiMutation.mutate({
                    enabled: aiEnabled,
                    apiKey: aiApiKey,
                    model: aiModel,
                    businessContext: aiBusinessContext,
                    systemPrompt: aiSystemPrompt,
                  });
                }}
                disabled={saveAiMutation.isPending}
                className="inline-flex items-center gap-2 px-6 py-2.5 bg-purple-600 hover:bg-purple-700 text-white font-bold text-sm rounded-xl shadow-md shadow-purple-500/20 transition-all min-h-[44px] disabled:opacity-50"
              >
                {saveAiMutation.isPending ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Save className="w-4 h-4" />
                )}
                Save AI Settings
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================================================== */}
      {/* TAB 3 CONTENT: UNANSWERED INQUIRIES                      */}
      {/* ======================================================== */}
      {activeTab === 'unanswered' && (
        <div className="space-y-4">
          <div className="p-4 bg-purple-50/60 rounded-2xl border border-purple-100 text-xs text-purple-900 flex items-start gap-3">
            <Inbox className="w-5 h-5 text-purple-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-purple-950">Questions the Bot Missed: </span>
              These messages came from real customers on WhatsApp but didn't match any of your FAQs. Turn them into new answers with 1 click so your bot gets smarter!
            </div>
          </div>

          {isLoadingUnmatched ? (
            <div className="space-y-3">
              <Skeleton className="h-20 rounded-2xl" />
              <Skeleton className="h-20 rounded-2xl" />
            </div>
          ) : isErrorUnmatched ? (
            <ErrorState title="Failed to load unmatched inquiries" message={errorUnmatched?.message} />
          ) : unmatchedMessages && unmatchedMessages.length > 0 ? (
            <div className="grid grid-cols-1 gap-3">
              {unmatchedMessages.map((msg) => (
                <div
                  key={msg.id}
                  className="p-5 bg-white rounded-2xl border border-purple-100 shadow-sm flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 hover:border-purple-200 transition-colors"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 text-xs font-semibold text-gray-500">
                      <span>Customer: {formatE164(msg.senderPhone)}</span>
                      <span>•</span>
                      <span>{new Date(msg.receivedAt).toLocaleString('en-IN')}</span>
                    </div>
                    <p className="text-sm font-bold text-gray-900">"{msg.messageText}"</p>
                  </div>

                  <div className="flex items-center gap-2.5 shrink-0 self-end sm:self-center">
                    <button
                      type="button"
                      onClick={() => convertToFaq(msg.messageText, msg.id)}
                      className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-sm transition-colors min-h-[40px]"
                    >
                      <Plus className="w-3.5 h-3.5" />
                      Add as FAQ
                    </button>
                    <button
                      type="button"
                      onClick={() => deleteUnmatchedMutation.mutate(msg.id)}
                      className="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors"
                      title="Dismiss"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={CheckCircle2}
              title="All Questions Answered!"
              description="Great job! There are no missed or unanswered customer queries at this time."
            />
          )}
        </div>
      )}

      {/* ======================================================== */}
      {/* MODAL: ADD / EDIT KEYWORD RULE                           */}
      {/* ======================================================== */}
      {showRuleModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-gray-100 space-y-4 animate-in fade-in">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <h3 className="text-lg font-bold text-gray-900">
                {editingRuleId ? 'Edit Keyword Trigger' : 'Add Keyword Trigger'}
              </h3>
              <button
                type="button"
                onClick={() => setShowRuleModal(false)}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100"
              >
                ✕
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (!ruleName.trim() || !ruleMatchValue.trim() || !ruleActionPayload.trim()) return;
                saveRuleMutation.mutate({
                  name: ruleName.trim(),
                  matchType: ruleMatchType,
                  matchValue: ruleMatchValue.trim(),
                  caseSensitive: false,
                  actionType: 'SEND_TEXT',
                  actionPayload: JSON.stringify({ text: ruleActionPayload.trim() }),
                  priority: (rules?.length || 0) + 1,
                  enabled: true,
                });
              }}
              className="space-y-4 text-xs font-semibold text-gray-700"
            >
              <div>
                <label className="block mb-1 text-gray-700">Rule Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Pricing Menu"
                  value={ruleName}
                  onChange={(e) => setRuleName(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-sm font-normal focus:bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block mb-1 text-gray-700">Trigger Keyword</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. price, menu, offer"
                  value={ruleMatchValue}
                  onChange={(e) => setRuleMatchValue(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-sm font-normal focus:bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none"
                />
                <p className="text-[11px] text-gray-400 font-normal mt-1">
                  Whenever customer message includes this word, bot replies automatically.
                </p>
              </div>

              <div>
                <label className="block mb-1 text-gray-700">Bot Reply Message</label>
                <textarea
                  required
                  rows={4}
                  placeholder="e.g. Hello! Our general consultation fee is ₹500. Clear aligners start at ₹45,000."
                  value={ruleActionPayload}
                  onChange={(e) => setRuleActionPayload(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-sm font-normal focus:bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowRuleModal(false)}
                  className="px-4 py-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold min-h-[44px]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saveRuleMutation.isPending}
                  className="inline-flex items-center gap-2 px-6 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-md shadow-brand-500/20 min-h-[44px]"
                >
                  {saveRuleMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Save Rule'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ======================================================== */}
      {/* MODAL: ADD / EDIT FAQ                                    */}
      {/* ======================================================== */}
      {showFaqModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-gray-100 space-y-4 animate-in fade-in">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <h3 className="text-lg font-bold text-gray-900">
                {editingFaqId ? 'Edit Smart FAQ' : 'Add New Smart FAQ'}
              </h3>
              <button
                type="button"
                onClick={() => setShowFaqModal(false)}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100"
              >
                ✕
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (!faqQuestion.trim() || !faqAnswer.trim()) return;
                saveFaqMutation.mutate({
                  question: faqQuestion.trim(),
                  answer: faqAnswer.trim(),
                });
              }}
              className="space-y-4 text-xs font-semibold text-gray-700"
            >
              <div>
                <label className="block mb-1 text-gray-700">Common Question</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. What are your clinic timings?"
                  value={faqQuestion}
                  onChange={(e) => setFaqQuestion(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-sm font-normal focus:bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block mb-1 text-gray-700">Answer to Send</label>
                <textarea
                  required
                  rows={4}
                  placeholder="e.g. We are open Monday to Saturday from 10:00 AM to 8:00 PM."
                  value={faqAnswer}
                  onChange={(e) => setFaqAnswer(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-300 rounded-xl text-sm font-normal focus:bg-white focus:ring-2 focus:ring-brand-500 focus:outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowFaqModal(false)}
                  className="px-4 py-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold min-h-[44px]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saveFaqMutation.isPending}
                  className="inline-flex items-center gap-2 px-6 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-md shadow-brand-500/20 min-h-[44px]"
                >
                  {saveFaqMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Save FAQ'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
