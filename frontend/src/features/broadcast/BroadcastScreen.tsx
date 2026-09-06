import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import {
  BroadcastCampaignDto,
  BroadcastCampaignDetailDto,
  CreateBroadcastRequest,
  TargetType,
  WhatsAppTemplateResponse,
  LeadDto,
} from '../../api/types';
import {
  Megaphone,
  Plus,
  Send,
  Calendar,
  Clock,
  CheckCircle2,
  AlertCircle,
  XCircle,
  Users,
  Target,
  FileSpreadsheet,
  FileText,
  Upload,
  X,
  Eye,
  Trash2,
  RefreshCw,
  Search,
  ShieldCheck,
} from 'lucide-react';

export const BroadcastScreen: React.FC = () => {
  const queryClient = useQueryClient();

  // Modals
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedCampaignId, setSelectedCampaignId] = useState<string | null>(null);
  const [step, setStep] = useState<1 | 2 | 3>(1);

  // Notifications
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Search
  const [searchQuery, setSearchQuery] = useState('');

  // Form State
  const [campaignName, setCampaignName] = useState('');
  const [targetType, setTargetType] = useState<TargetType>('ALL_CONTACTS');
  const [targetStage, setTargetStage] = useState<string>('ALL');
  const [selectedTemplateName, setSelectedTemplateName] = useState<string>('');
  const [templateParams, setTemplateParams] = useState<Record<string, string>>({});
  const [dispatchTiming, setDispatchTiming] = useState<'NOW' | 'SCHEDULED'>('NOW');
  const [scheduledDate, setScheduledDate] = useState('');
  const [scheduledTime, setScheduledTime] = useState('');

  // CSV and Manual Lists
  const [customRecipients, setCustomRecipients] = useState<{ phoneE164: string; name?: string }[]>([]);
  const [pasteInput, setPasteInput] = useState('');
  const [csvFileName, setCsvFileName] = useState('');

  // 1. Fetch Campaigns
  const {
    data: campaigns,
    isLoading: isCampaignsLoading,
    refetch: refetchCampaigns,
  } = useQuery<BroadcastCampaignDto[]>({
    queryKey: ['broadcasts'],
    queryFn: () => apiClient<BroadcastCampaignDto[]>('/api/broadcasts'),
    refetchInterval: 5000,
  });

  // 2. Fetch Campaign Detail (when detail modal is open)
  const { data: campaignDetail } = useQuery<BroadcastCampaignDetailDto>({
    queryKey: ['broadcast-detail', selectedCampaignId],
    queryFn: () => apiClient<BroadcastCampaignDetailDto>(`/api/broadcasts/${selectedCampaignId}`),
    enabled: !!selectedCampaignId,
    refetchInterval: selectedCampaignId ? 4000 : false,
  });

  // 3. Fetch Templates for Dropdown
  const { data: templates } = useQuery<WhatsAppTemplateResponse[]>({
    queryKey: ['templates'],
    queryFn: () => apiClient<WhatsAppTemplateResponse[]>('/api/templates'),
  });

  // 4. Fetch Leads count for audience estimate
  const { data: leads } = useQuery<LeadDto[]>({
    queryKey: ['leads'],
    queryFn: () => apiClient<LeadDto[]>('/api/leads'),
  });

  // 5. Create Broadcast Mutation
  const createMutation = useMutation({
    mutationFn: (data: CreateBroadcastRequest) =>
      apiClient<BroadcastCampaignDto>('/api/broadcasts', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['broadcasts'] });
      setShowCreateModal(false);
      resetForm();
      setSuccessMsg('Broadcast campaign created and queued for delivery.');
      setTimeout(() => setSuccessMsg(null), 3000);
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to create broadcast campaign.');
    },
  });

  // 6. Cancel Broadcast Mutation
  const cancelMutation = useMutation({
    mutationFn: (id: string) =>
      apiClient<void>(`/api/broadcasts/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['broadcasts'] });
      setSuccessMsg('Broadcast campaign cancelled.');
      setTimeout(() => setSuccessMsg(null), 2500);
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to cancel campaign.');
    },
  });

  const resetForm = () => {
    setStep(1);
    setCampaignName('');
    setTargetType('ALL_CONTACTS');
    setTargetStage('ALL');
    setSelectedTemplateName('');
    setTemplateParams({});
    setDispatchTiming('NOW');
    setScheduledDate('');
    setScheduledTime('');
    setCustomRecipients([]);
    setPasteInput('');
    setCsvFileName('');
  };

  // Selected Template Object
  const currentTemplate = useMemo(() => {
    return templates?.find((t) => t.name === selectedTemplateName) || null;
  }, [templates, selectedTemplateName]);

  // Detected variable count in template body
  const detectedVariables = useMemo(() => {
    if (!currentTemplate || !currentTemplate.bodyText) return [];
    const matches = currentTemplate.bodyText.match(/\{\{(\d+)\}\}/g);
    if (!matches) return [];
    const nums = Array.from(new Set(matches.map((m) => m.replace(/\D/g, ''))));
    nums.sort((a, b) => Number(a) - Number(b));
    return nums;
  }, [currentTemplate]);

  // Live preview text with replaced parameters
  const previewBodyText = useMemo(() => {
    if (!currentTemplate) return '';
    let text = currentTemplate.bodyText || '';
    detectedVariables.forEach((num) => {
      const val = templateParams[num] || `{{${num}}}`;
      text = text.split(`{{${num}}}`).join(val);
    });
    return text;
  }, [currentTemplate, detectedVariables, templateParams]);

  // Handle CSV File Upload
  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setCsvFileName(file.name);
    const reader = new FileReader();
    reader.onload = (evt) => {
      const text = evt.target?.result as string;
      if (!text) return;

      const lines = text.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
      if (lines.length === 0) return;

      const parsed: { phoneE164: string; name?: string }[] = [];
      const hasHeader = lines[0].toLowerCase().includes('phone') || lines[0].toLowerCase().includes('mobile');
      const startIdx = hasHeader ? 1 : 0;

      for (let i = startIdx; i < lines.length; i++) {
        const parts = lines[i].split(',').map((p) => p.trim().replace(/^["']|["']$/g, ''));
        if (parts.length >= 1 && parts[0]) {
          const phone = parts[0];
          const name = parts.length > 1 ? parts[1] : undefined;
          parsed.push({ phoneE164: phone, name });
        }
      }

      setCustomRecipients(parsed);
    };
    reader.readAsText(file);
  };

  // Handle Paste Input
  const handlePasteChange = (text: string) => {
    setPasteInput(text);
    const lines = text.split(/[\n,]+/).map((l) => l.trim()).filter(Boolean);
    const parsed = lines.map((line) => {
      const parts = line.split(':');
      if (parts.length >= 2) {
        return { name: parts[0].trim(), phoneE164: parts[1].trim() };
      }
      return { phoneE164: line };
    });
    setCustomRecipients(parsed);
  };

  // Audience Count Estimate
  const estimatedAudienceCount = useMemo(() => {
    if (targetType === 'ALL_CONTACTS') {
      return leads ? leads.length : 0;
    }
    if (targetType === 'LEADS_BY_STAGE') {
      if (!leads) return 0;
      if (targetStage === 'ALL') return leads.length;
      return leads.filter((l) => l.stage === targetStage).length;
    }
    return customRecipients.length;
  }, [targetType, targetStage, leads, customRecipients]);

  // Submit Handler
  const handleFinalSubmit = () => {
    if (!campaignName.trim()) {
      setErrorMsg('Please enter a campaign name.');
      return;
    }
    if (!selectedTemplateName) {
      setErrorMsg('Please choose an approved WhatsApp template.');
      return;
    }
    if (estimatedAudienceCount === 0) {
      setErrorMsg('Target audience has 0 recipients. Please select a valid audience.');
      return;
    }

    let scheduledForIso: string | undefined = undefined;
    if (dispatchTiming === 'SCHEDULED') {
      if (!scheduledDate || !scheduledTime) {
        setErrorMsg('Please select a valid scheduled date and time.');
        return;
      }
      const dt = new Date(`${scheduledDate}T${scheduledTime}:00`);
      if (dt.getTime() <= Date.now()) {
        setErrorMsg('Scheduled time must be in the future.');
        return;
      }
      scheduledForIso = dt.toISOString();
    }

    createMutation.mutate({
      name: campaignName.trim(),
      targetType,
      targetStage: targetType === 'LEADS_BY_STAGE' ? targetStage : undefined,
      templateName: selectedTemplateName,
      templateLanguage: currentTemplate?.language || 'en_US',
      templateId: currentTemplate?.id,
      templateParams,
      messagePreview: previewBodyText,
      scheduledFor: scheduledForIso,
      customRecipients: targetType === 'CSV_UPLOAD' || targetType === 'PASTE_NUMBERS' ? customRecipients : undefined,
    });
  };

  // KPIs
  const stats = useMemo(() => {
    if (!campaigns) return { total: 0, sent: 0, active: 0, rate: 100 };
    const total = campaigns.length;
    const sent = campaigns.reduce((acc, c) => acc + c.sentCount, 0);
    const active = campaigns.filter((c) => c.status === 'RUNNING' || c.status === 'SCHEDULED').length;
    const totalRecipients = campaigns.reduce((acc, c) => acc + c.totalRecipients, 0);
    const rate = totalRecipients > 0 ? Math.round((sent / totalRecipients) * 100) : 100;
    return { total, sent, active, rate };
  }, [campaigns]);

  // Filtered campaigns
  const filteredCampaigns = useMemo(() => {
    if (!campaigns) return [];
    if (!searchQuery.trim()) return campaigns;
    const q = searchQuery.toLowerCase().trim();
    return campaigns.filter(
      (c) =>
        c.name.toLowerCase().includes(q) ||
        (c.templateName && c.templateName.toLowerCase().includes(q))
    );
  }, [campaigns, searchQuery]);

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-gray-200">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 flex items-center gap-2.5">
            <Megaphone className="w-6 h-6 text-brand-600" />
            Bulk Broadcast & Campaigns
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-1">
            Send targeted WhatsApp marketing offers, festival discounts, and client updates safely with Meta template compliance.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => refetchCampaigns()}
            className="p-2.5 rounded-xl border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 hover:text-gray-900 transition-colors shadow-xs"
            title="Refresh campaigns"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
          <button
            type="button"
            onClick={() => {
              resetForm();
              setShowCreateModal(true);
            }}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs sm:text-sm font-bold shadow-sm transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>New Broadcast</span>
          </button>
        </div>
      </div>

      {/* Alert Banners */}
      {successMsg && (
        <div className="p-3.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs font-semibold flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600" />
          <span>{successMsg}</span>
        </div>
      )}
      {errorMsg && (
        <div className="p-3.5 bg-red-50 border border-red-200 text-red-800 rounded-xl text-xs font-semibold flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
            <span>{errorMsg}</span>
          </div>
          <button type="button" onClick={() => setErrorMsg(null)} className="text-red-500 hover:text-red-700">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="p-4 bg-white rounded-2xl border border-gray-200 shadow-xs">
          <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400">Total Campaigns</span>
          <div className="text-2xl font-bold text-gray-900 mt-1">{stats.total}</div>
        </div>
        <div className="p-4 bg-white rounded-2xl border border-gray-200 shadow-xs">
          <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400">Messages Sent</span>
          <div className="text-2xl font-bold text-brand-600 mt-1">{stats.sent.toLocaleString('en-IN')}</div>
        </div>
        <div className="p-4 bg-white rounded-2xl border border-gray-200 shadow-xs">
          <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400">Delivery Rate</span>
          <div className="text-2xl font-bold text-emerald-600 mt-1">{stats.rate}%</div>
        </div>
        <div className="p-4 bg-white rounded-2xl border border-gray-200 shadow-xs">
          <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400">Active / Scheduled</span>
          <div className="text-2xl font-bold text-purple-600 mt-1">{stats.active}</div>
        </div>
      </div>

      {/* Search & Campaign Table */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-xs overflow-hidden">
        <div className="p-4 border-b border-gray-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="relative w-full sm:w-72">
            <Search className="w-4 h-4 absolute left-3 top-2.5 text-gray-400" />
            <input
              type="text"
              placeholder="Search campaigns..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
            />
          </div>
          <span className="text-xs text-gray-500 font-medium">
            Showing {filteredCampaigns.length} campaigns
          </span>
        </div>

        {isCampaignsLoading ? (
          <div className="p-8 text-center text-xs text-gray-500">Loading broadcasts...</div>
        ) : filteredCampaigns.length === 0 ? (
          <div className="p-12 text-center">
            <div className="w-12 h-12 rounded-2xl bg-brand-50 text-brand-600 flex items-center justify-center mx-auto mb-3">
              <Megaphone className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-bold text-gray-900">No broadcast campaigns yet</h3>
            <p className="text-xs text-gray-500 max-w-sm mx-auto mt-1 mb-4">
              Reach hundreds of clients at once with customized WhatsApp announcements or festival promotions.
            </p>
            <button
              type="button"
              onClick={() => {
                resetForm();
                setShowCreateModal(true);
              }}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors"
            >
              <Plus className="w-4 h-4" />
              <span>Create First Broadcast</span>
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-gray-600">
              <thead className="bg-gray-50/75 text-gray-400 uppercase tracking-wider text-[10px] font-bold border-b border-gray-100">
                <tr>
                  <th className="py-3 px-4">Campaign Name</th>
                  <th className="py-3 px-4">Audience</th>
                  <th className="py-3 px-4">Template</th>
                  <th className="py-3 px-4">Progress</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Date</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredCampaigns.map((c) => {
                  const percent = c.totalRecipients > 0 ? Math.round((c.sentCount / c.totalRecipients) * 100) : 0;
                  return (
                    <tr key={c.id} className="hover:bg-gray-50/50 transition-colors">
                      <td className="py-3.5 px-4">
                        <span className="font-bold text-gray-900 block">{c.name}</span>
                        {c.messagePreview && (
                          <span className="text-[11px] text-gray-400 line-clamp-1 max-w-xs">
                            {c.messagePreview}
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 text-[10px] font-bold">
                          {c.targetType === 'ALL_CONTACTS' && 'All Contacts'}
                          {c.targetType === 'LEADS_BY_STAGE' && `Leads: ${c.targetStage || 'ALL'}`}
                          {c.targetType === 'CSV_UPLOAD' && 'CSV Upload'}
                          {c.targetType === 'PASTE_NUMBERS' && 'Direct List'}
                        </span>
                        <span className="text-[11px] text-gray-400 block mt-0.5">
                          {c.totalRecipients} recipients
                        </span>
                      </td>
                      <td className="py-3.5 px-4 font-mono text-[11px] text-gray-600">
                        {c.templateName || '—'}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="w-32">
                          <div className="flex items-center justify-between text-[10px] mb-1">
                            <span className="font-semibold text-gray-700">{c.sentCount} sent</span>
                            <span className="text-gray-400">{percent}%</span>
                          </div>
                          <div className="w-full bg-gray-100 rounded-full h-1.5 overflow-hidden">
                            <div
                              className="bg-brand-600 h-1.5 rounded-full transition-all"
                              style={{ width: `${percent}%` }}
                            />
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-4">
                        {c.status === 'COMPLETED' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <CheckCircle2 className="w-3 h-3" /> Completed
                          </span>
                        )}
                        {c.status === 'RUNNING' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                            <RefreshCw className="w-3 h-3 animate-spin" /> Sending...
                          </span>
                        )}
                        {c.status === 'SCHEDULED' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200">
                            <Clock className="w-3 h-3" /> Scheduled
                          </span>
                        )}
                        {c.status === 'CANCELLED' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-gray-100 text-gray-600 border border-gray-200">
                            <XCircle className="w-3 h-3" /> Cancelled
                          </span>
                        )}
                        {c.status === 'FAILED' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-red-50 text-red-700 border border-red-200">
                            <AlertCircle className="w-3 h-3" /> Failed
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-gray-500 text-[11px]">
                        {new Date(c.createdAt).toLocaleDateString([], { month: 'short', day: 'numeric' })}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => setSelectedCampaignId(c.id)}
                            className="p-1.5 rounded-lg text-gray-500 hover:text-gray-900 hover:bg-gray-100 transition-colors"
                            title="View delivery logs"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          {(c.status === 'SCHEDULED' || c.status === 'RUNNING') && (
                            <button
                              type="button"
                              onClick={() => {
                                if (window.confirm('Are you sure you want to cancel this campaign?')) {
                                  cancelMutation.mutate(c.id);
                                }
                              }}
                              className="p-1.5 rounded-lg text-red-500 hover:text-red-700 hover:bg-red-50 transition-colors"
                              title="Cancel campaign"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Broadcast 3-Step Wizard Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl shadow-xl max-w-2xl w-full p-6 space-y-5 animate-in zoom-in-95 duration-150 border border-gray-200 max-h-[90vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h3 className="text-base font-bold text-gray-900">New Broadcast Campaign</h3>
                <p className="text-xs text-gray-500">
                  Step {step} of 3: {step === 1 ? 'Campaign & Audience' : step === 2 ? 'Choose Template & Preview' : 'Review & Schedule'}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* STEP 1: Campaign Name & Audience */}
            {step === 1 && (
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Campaign Name *
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Diwali Festive Offer 2026"
                    value={campaignName}
                    onChange={(e) => setCampaignName(e.target.value)}
                    className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-2">
                    Target Audience
                  </label>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                    {/* All Contacts */}
                    <div
                      onClick={() => setTargetType('ALL_CONTACTS')}
                      className={`p-3 rounded-xl border cursor-pointer transition-all ${
                        targetType === 'ALL_CONTACTS'
                          ? 'border-brand-500 bg-brand-50/50 ring-1 ring-brand-500'
                          : 'border-gray-200 hover:border-gray-300 bg-white'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <Users className="w-4 h-4 text-brand-600" />
                        <span className="text-xs font-bold text-gray-900">All Contacts</span>
                      </div>
                      <p className="text-[11px] text-gray-500 mt-1">
                        Send to every contact in your WhatsApp inbox ({leads?.length || 0} contacts).
                      </p>
                    </div>

                    {/* CRM Leads by Stage */}
                    <div
                      onClick={() => setTargetType('LEADS_BY_STAGE')}
                      className={`p-3 rounded-xl border cursor-pointer transition-all ${
                        targetType === 'LEADS_BY_STAGE'
                          ? 'border-brand-500 bg-brand-50/50 ring-1 ring-brand-500'
                          : 'border-gray-200 hover:border-gray-300 bg-white'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <Target className="w-4 h-4 text-purple-600" />
                        <span className="text-xs font-bold text-gray-900">CRM Leads by Stage</span>
                      </div>
                      <p className="text-[11px] text-gray-500 mt-1">
                        Filter by stage (New Inquiries, In Discussion, Deals Won).
                      </p>
                    </div>

                    {/* CSV Upload */}
                    <div
                      onClick={() => setTargetType('CSV_UPLOAD')}
                      className={`p-3 rounded-xl border cursor-pointer transition-all ${
                        targetType === 'CSV_UPLOAD'
                          ? 'border-brand-500 bg-brand-50/50 ring-1 ring-brand-500'
                          : 'border-gray-200 hover:border-gray-300 bg-white'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
                        <span className="text-xs font-bold text-gray-900">Upload CSV / Excel</span>
                      </div>
                      <p className="text-[11px] text-gray-500 mt-1">
                        Import an external spreadsheet with Phone and Name columns.
                      </p>
                    </div>

                    {/* Direct Paste Numbers */}
                    <div
                      onClick={() => setTargetType('PASTE_NUMBERS')}
                      className={`p-3 rounded-xl border cursor-pointer transition-all ${
                        targetType === 'PASTE_NUMBERS'
                          ? 'border-brand-500 bg-brand-50/50 ring-1 ring-brand-500'
                          : 'border-gray-200 hover:border-gray-300 bg-white'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <FileText className="w-4 h-4 text-amber-600" />
                        <span className="text-xs font-bold text-gray-900">Paste Numbers</span>
                      </div>
                      <p className="text-[11px] text-gray-500 mt-1">
                        Quickly enter comma or line-separated phone numbers.
                      </p>
                    </div>
                  </div>
                </div>

                {/* Sub-inputs based on targetType */}
                {targetType === 'LEADS_BY_STAGE' && (
                  <div className="p-3 bg-gray-50 rounded-xl border border-gray-200 space-y-2">
                    <label className="block text-xs font-bold text-gray-700">
                      Select Pipeline Stage
                    </label>
                    <select
                      value={targetStage}
                      onChange={(e) => setTargetStage(e.target.value)}
                      className="w-full px-3 py-2 text-xs bg-white border border-gray-200 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-brand-500"
                    >
                      <option value="ALL">All CRM Leads</option>
                      <option value="NEW">New Inquiries</option>
                      <option value="IN_DISCUSSION">In Discussion</option>
                      <option value="PROPOSAL_SENT">Quotation Sent</option>
                      <option value="WON">Deals Won</option>
                    </select>
                  </div>
                )}

                {targetType === 'CSV_UPLOAD' && (
                  <div className="p-4 bg-gray-50 rounded-xl border border-dashed border-gray-300 text-center">
                    <Upload className="w-6 h-6 text-gray-400 mx-auto mb-2" />
                    <span className="text-xs font-bold text-gray-700 block">
                      {csvFileName || 'Upload CSV (phone, name)'}
                    </span>
                    <p className="text-[11px] text-gray-400 mb-3">
                      First column must contain phone numbers (e.g. +919876543210).
                    </p>
                    <input
                      type="file"
                      accept=".csv,.txt"
                      onChange={handleFileUpload}
                      className="text-xs text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-brand-50 file:text-brand-700 hover:file:bg-brand-100 cursor-pointer"
                    />
                    {customRecipients.length > 0 && (
                      <span className="block text-xs font-bold text-emerald-600 mt-2">
                        {customRecipients.length} valid contacts loaded from CSV
                      </span>
                    )}
                  </div>
                )}

                {targetType === 'PASTE_NUMBERS' && (
                  <div className="space-y-1">
                    <label className="block text-xs font-bold text-gray-700">
                      Enter Phone Numbers (one per line or comma-separated)
                    </label>
                    <textarea
                      rows={4}
                      value={pasteInput}
                      onChange={(e) => handlePasteChange(e.target.value)}
                      placeholder="+919876543210&#10;Rahul: +919123456789&#10;+919988776655"
                      className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl font-mono focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
                    />
                    <span className="text-[11px] text-gray-500">
                      Parsed {customRecipients.length} recipients
                    </span>
                  </div>
                )}

                {/* Estimate Pill */}
                <div className="p-3 bg-blue-50/70 border border-blue-200 rounded-xl flex items-center justify-between text-xs">
                  <span className="text-blue-800 font-medium">Estimated Audience Size:</span>
                  <span className="font-bold text-blue-900 bg-white px-2.5 py-0.5 rounded-lg border border-blue-200 shadow-2xs">
                    {estimatedAudienceCount} recipients
                  </span>
                </div>
              </div>
            )}

            {/* STEP 2: Choose Template & Variables */}
            {step === 2 && (
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Select Approved WhatsApp Template *
                  </label>
                  {(!templates || templates.length === 0) ? (
                    <div className="p-4 bg-amber-50 border border-amber-200 text-amber-800 rounded-xl text-xs">
                      No approved WhatsApp templates found. Please create or sync templates in Settings.
                    </div>
                  ) : (
                    <select
                      value={selectedTemplateName}
                      onChange={(e) => {
                        setSelectedTemplateName(e.target.value);
                        setTemplateParams({});
                      }}
                      className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl font-semibold text-gray-800 focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
                    >
                      <option value="">-- Choose a template --</option>
                      {templates.map((t) => (
                        <option key={t.id} value={t.name}>
                          {t.name} ({t.category} - {t.language})
                        </option>
                      ))}
                    </select>
                  )}
                </div>

                {/* Dynamic Variable Inputs */}
                {detectedVariables.length > 0 && (
                  <div className="p-3 bg-gray-50 rounded-xl border border-gray-200 space-y-3">
                    <span className="text-xs font-bold text-gray-800 block">
                      Template Variables Mapping
                    </span>
                    <p className="text-[11px] text-gray-500">
                      Use <span className="font-mono text-brand-600">{"{{name}}"}</span> to automatically insert recipient name.
                    </p>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {detectedVariables.map((num) => (
                        <div key={num}>
                          <label className="block text-[11px] font-bold text-gray-600 mb-1">
                            Variable {"{{" + num + "}}"}
                          </label>
                          <input
                            type="text"
                            placeholder={num === '1' ? 'e.g. {{name}}' : 'e.g. 20% OFF'}
                            value={templateParams[num] || ''}
                            onChange={(e) =>
                              setTemplateParams({ ...templateParams, [num]: e.target.value })
                            }
                            className="w-full px-3 py-1.5 text-xs bg-white border border-gray-200 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-brand-500"
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* WhatsApp Chat Preview Bubble */}
                {currentTemplate && (
                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1.5">
                      WhatsApp Message Preview
                    </label>
                    <div className="p-4 bg-slate-900 rounded-2xl max-w-sm mx-auto shadow-inner">
                      <div className="bg-[#dcf8c6] text-slate-800 rounded-2xl rounded-tr-xs p-3.5 shadow-sm text-xs space-y-2">
                        <p className="whitespace-pre-wrap leading-relaxed font-sans">
                          {previewBodyText}
                        </p>
                        <div className="flex items-center justify-end gap-1 text-[9px] text-slate-500 pt-1">
                          <span>12:00 PM</span>
                          <span>✓✓</span>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* STEP 3: Review & Schedule */}
            {step === 3 && (
              <div className="space-y-4">
                <div className="p-4 bg-gray-50 rounded-xl border border-gray-200 space-y-2 text-xs">
                  <div className="flex justify-between py-1 border-b border-gray-200">
                    <span className="text-gray-500 font-medium">Campaign Name</span>
                    <span className="font-bold text-gray-900">{campaignName}</span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-gray-200">
                    <span className="text-gray-500 font-medium">Template</span>
                    <span className="font-bold text-gray-900 font-mono">{selectedTemplateName}</span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-gray-200">
                    <span className="text-gray-500 font-medium">Total Audience</span>
                    <span className="font-bold text-brand-600">{estimatedAudienceCount} recipients</span>
                  </div>
                  <div className="flex justify-between py-1">
                    <span className="text-gray-500 font-medium">Target Group</span>
                    <span className="font-semibold text-gray-800">{targetType.replace('_', ' ')}</span>
                  </div>
                </div>

                {/* Dispatch Timing Choice */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-2">
                    Dispatch Schedule
                  </label>
                  <div className="grid grid-cols-2 gap-3">
                    <div
                      onClick={() => setDispatchTiming('NOW')}
                      className={`p-3 rounded-xl border cursor-pointer transition-all ${
                        dispatchTiming === 'NOW'
                          ? 'border-brand-500 bg-brand-50/50 ring-1 ring-brand-500'
                          : 'border-gray-200 hover:border-gray-300 bg-white'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <Send className="w-4 h-4 text-brand-600" />
                        <span className="text-xs font-bold text-gray-900">Send Now</span>
                      </div>
                      <p className="text-[11px] text-gray-500 mt-1">
                        Dispatch immediately with safe background queue.
                      </p>
                    </div>

                    <div
                      onClick={() => setDispatchTiming('SCHEDULED')}
                      className={`p-3 rounded-xl border cursor-pointer transition-all ${
                        dispatchTiming === 'SCHEDULED'
                          ? 'border-brand-500 bg-brand-50/50 ring-1 ring-brand-500'
                          : 'border-gray-200 hover:border-gray-300 bg-white'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <Calendar className="w-4 h-4 text-purple-600" />
                        <span className="text-xs font-bold text-gray-900">Schedule for Later</span>
                      </div>
                      <p className="text-[11px] text-gray-500 mt-1">
                        Pick a date and time to broadcast automatically.
                      </p>
                    </div>
                  </div>
                </div>

                {dispatchTiming === 'SCHEDULED' && (
                  <div className="grid grid-cols-2 gap-3 p-3 bg-gray-50 rounded-xl border border-gray-200">
                    <div>
                      <label className="block text-[11px] font-bold text-gray-700 mb-1">
                        Date
                      </label>
                      <input
                        type="date"
                        value={scheduledDate}
                        onChange={(e) => setScheduledDate(e.target.value)}
                        className="w-full px-3 py-1.5 text-xs bg-white border border-gray-200 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-brand-500"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-bold text-gray-700 mb-1">
                        Time
                      </label>
                      <input
                        type="time"
                        value={scheduledTime}
                        onChange={(e) => setScheduledTime(e.target.value)}
                        className="w-full px-3 py-1.5 text-xs bg-white border border-gray-200 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-brand-500"
                      />
                    </div>
                  </div>
                )}

                {/* Safety & Compliance Badge */}
                <div className="p-3 bg-emerald-50/70 border border-emerald-200 rounded-xl flex items-center gap-2.5 text-xs text-emerald-900">
                  <ShieldCheck className="w-5 h-5 text-emerald-600 shrink-0" />
                  <span>
                    <strong>Meta Anti-Ban Throttling Active:</strong> Messages are safely spaced to protect your WhatsApp account from rate limit blocks.
                  </span>
                </div>
              </div>
            )}

            {/* Modal Footer Controls */}
            <div className="flex items-center justify-between pt-3 border-t border-gray-100">
              {step > 1 ? (
                <button
                  type="button"
                  onClick={() => setStep((step - 1) as 1 | 2)}
                  className="px-4 py-2 text-xs font-bold text-gray-600 hover:bg-gray-100 rounded-xl transition-colors"
                >
                  Back
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-xs font-bold text-gray-600 hover:bg-gray-100 rounded-xl transition-colors"
                >
                  Cancel
                </button>
              )}

              {step < 3 ? (
                <button
                  type="button"
                  onClick={() => {
                    if (step === 1 && !campaignName.trim()) {
                      setErrorMsg('Please enter a campaign name.');
                      return;
                    }
                    if (step === 1 && estimatedAudienceCount === 0) {
                      setErrorMsg('Audience has 0 contacts. Please select a valid audience.');
                      return;
                    }
                    if (step === 2 && !selectedTemplateName) {
                      setErrorMsg('Please select an approved template.');
                      return;
                    }
                    setErrorMsg(null);
                    setStep((step + 1) as 2 | 3);
                  }}
                  className="px-5 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-xs transition-colors"
                >
                  Next Step
                </button>
              ) : (
                <button
                  type="button"
                  onClick={handleFinalSubmit}
                  disabled={createMutation.isPending}
                  className="px-5 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-xs transition-colors flex items-center gap-1.5"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>{createMutation.isPending ? 'Queuing...' : dispatchTiming === 'NOW' ? 'Send Broadcast Now' : 'Schedule Broadcast'}</span>
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* View Recipients Modal */}
      {selectedCampaignId && campaignDetail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl shadow-xl max-w-2xl w-full p-6 space-y-4 animate-in zoom-in-95 duration-150 border border-gray-200 max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h3 className="text-base font-bold text-gray-900">
                  {campaignDetail.campaign.name} - Delivery Logs
                </h3>
                <p className="text-xs text-gray-500">
                  {campaignDetail.recipients.length} recipients | {campaignDetail.campaign.sentCount} delivered | {campaignDetail.campaign.failedCount} failed
                </p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedCampaignId(null)}
                className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto">
              <table className="w-full text-left text-xs text-gray-600">
                <thead className="bg-gray-50 text-[10px] text-gray-400 uppercase font-bold sticky top-0">
                  <tr>
                    <th className="py-2.5 px-3">Recipient</th>
                    <th className="py-2.5 px-3">Phone</th>
                    <th className="py-2.5 px-3">Status</th>
                    <th className="py-2.5 px-3">Sent Time</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {campaignDetail.recipients.map((r) => (
                    <tr key={r.id}>
                      <td className="py-2.5 px-3 font-semibold text-gray-900">
                        {r.contactName || '—'}
                      </td>
                      <td className="py-2.5 px-3 font-mono text-[11px] text-gray-500">
                        {r.phoneE164}
                      </td>
                      <td className="py-2.5 px-3">
                        {r.status === 'SENT' && (
                          <span className="inline-flex items-center gap-1 text-emerald-700 font-bold text-[11px]">
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> Sent
                          </span>
                        )}
                        {r.status === 'PENDING' && (
                          <span className="inline-flex items-center gap-1 text-blue-700 font-bold text-[11px]">
                            <Clock className="w-3.5 h-3.5 text-blue-600" /> Queued
                          </span>
                        )}
                        {r.status === 'FAILED' && (
                          <span className="inline-flex items-center gap-1 text-red-700 font-bold text-[11px]" title={r.errorMessage}>
                            <AlertCircle className="w-3.5 h-3.5 text-red-600" /> Failed
                          </span>
                        )}
                      </td>
                      <td className="py-2.5 px-3 text-gray-400 text-[11px]">
                        {r.sentAt ? new Date(r.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="pt-3 border-t border-gray-100 text-right">
              <button
                type="button"
                onClick={() => setSelectedCampaignId(null)}
                className="px-4 py-2 text-xs font-bold text-gray-700 hover:bg-gray-100 rounded-xl transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
