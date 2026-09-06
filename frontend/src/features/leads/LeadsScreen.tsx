/**
 * LeadsScreen: SMB WhatsApp CRM & Pipeline Board.
 *
 * Capabilities:
 * - Visual Kanban pipeline: NEW -> CONTACTED -> QUALIFIED -> PROPOSAL -> WON -> LOST.
 * - Revenue analytics and potential pipeline value computation in INR (₹).
 * - Lead modal for editing estimated value, priority tags, and status notes.
 * - Direct 1-click jump into WhatsApp Customer Chat from any lead card.
 */
import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../api/client';
import {
  LeadDto,
  LeadAnalyticsDto,
  LeadStage,
} from '../../api/types';
import { formatE164 } from '../../lib/phoneUtils';
import { Skeleton } from '../../components/Skeleton';
import { ErrorState } from '../../components/ErrorState';
import { AlertBanner } from '../../components/AlertBanner';
import {
  IndianRupee,
  Users,
  Trophy,
  MessageCircle,
  X,
  Edit2,
  Search,
  Plus,
  GripVertical,
  MessageSquare,
} from 'lucide-react';

const STAGES: { key: LeadStage; title: string; color: string; bgLight: string; border: string }[] = [
  { key: 'NEW', title: 'New Inquiries', color: 'text-blue-700', bgLight: 'bg-blue-50/70', border: 'border-blue-200' },
  { key: 'IN_DISCUSSION', title: 'In Discussion', color: 'text-purple-700', bgLight: 'bg-purple-50/70', border: 'border-purple-200' },
  { key: 'PROPOSAL_SENT', title: 'Quotation Sent', color: 'text-amber-700', bgLight: 'bg-amber-50/70', border: 'border-amber-200' },
  { key: 'WON', title: 'Deals Won', color: 'text-emerald-700', bgLight: 'bg-emerald-50/70', border: 'border-emerald-200' },
];

function formatRelativeTime(dateStr?: string): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / (1000 * 60));
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays === 1) return 'Yesterday';
  if (diffDays < 7) return `${diffDays}d ago`;
  return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

export const LeadsScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  // Drag and Drop state
  const [draggedLeadId, setDraggedLeadId] = useState<string | null>(null);
  const [dragOverStage, setDragOverStage] = useState<LeadStage | null>(null);

  // Modals & Active Lead State
  const [selectedLead, setSelectedLead] = useState<LeadDto | null>(null);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // Search and Filter State
  const [searchQuery, setSearchQuery] = useState('');
  const [filterTab, setFilterTab] = useState<'ALL' | 'IN_DISCUSSION' | 'PROPOSAL_SENT' | 'WON'>('ALL');

  // Notification states
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Edit Lead Form State
  const [editDealValue, setEditDealValue] = useState<string>('');
  const [editNotes, setEditNotes] = useState<string>('');
  const [editStage, setEditStage] = useState<LeadStage>('NEW');

  // Create Lead Form State
  const [newLeadName, setNewLeadName] = useState('');
  const [newLeadPhone, setNewLeadPhone] = useState('');
  const [newLeadStage, setNewLeadStage] = useState<LeadStage>('NEW');
  const [newLeadDealValue, setNewLeadDealValue] = useState<string>('');
  const [newLeadNotes, setNewLeadNotes] = useState('');

  // 1. Fetch Leads
  const {
    data: leads,
    isLoading: isLeadsLoading,
    isError: isLeadsError,
    error: leadsError,
    refetch: refetchLeads,
  } = useQuery<LeadDto[]>({
    queryKey: ['leads'],
    queryFn: () => apiClient<LeadDto[]>('/api/leads'),
    refetchInterval: 6000,
  });

  // 2. Fetch Analytics
  const { data: serverAnalytics } = useQuery<LeadAnalyticsDto>({
    queryKey: ['leads-analytics'],
    queryFn: () => apiClient<LeadAnalyticsDto>('/api/leads/analytics'),
    refetchInterval: 6000,
  });

  // Calculated Real-time Analytics (Fallback to Client State for instant sync)
  const analytics = useMemo(() => {
    if (!leads) return serverAnalytics || null;

    const totalLeads = leads.length;
    const inDiscussionCount = leads.filter((l) => l.stage === 'IN_DISCUSSION').length;
    const wonLeads = leads.filter((l) => l.stage === 'WON');
    const wonCount = wonLeads.length;
    const wonValue = wonLeads.reduce((acc, l) => acc + (Number(l.dealValue) || 0), 0);
    const activePipelineValue = leads
      .filter((l) => l.stage !== 'LOST')
      .reduce((acc, l) => acc + (Number(l.dealValue) || 0), 0);

    return {
      totalLeads,
      inDiscussion: inDiscussionCount,
      won: wonCount,
      wonValue,
      totalPipelineValue: activePipelineValue,
      conversionRatePercent: totalLeads > 0 ? Math.round((wonCount / totalLeads) * 100) : 0,
      newLeads: leads.filter((l) => l.stage === 'NEW').length,
      proposalSent: leads.filter((l) => l.stage === 'PROPOSAL_SENT').length,
      lost: leads.filter((l) => l.stage === 'LOST').length,
    };
  }, [leads, serverAnalytics]);

  // 3. Update Stage Mutation
  const updateStageMutation = useMutation({
    mutationFn: ({ leadId, stage }: { leadId: string; stage: LeadStage }) =>
      apiClient<LeadDto>(`/api/leads/${leadId}/stage`, {
        method: 'PATCH',
        body: JSON.stringify({ stage }),
      }),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['leads'] });
      queryClient.invalidateQueries({ queryKey: ['leads-analytics'] });
      setSuccessMsg(`Moved to ${updated.stage.replace('_', ' ')}.`);
      setTimeout(() => setSuccessMsg(null), 2500);
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to update stage.');
    },
  });

  // 4. Update Details Mutation (Atomic: stage, dealValue, notes)
  const updateDetailsMutation = useMutation({
    mutationFn: ({
      leadId,
      stage,
      dealValue,
      notes,
    }: {
      leadId: string;
      stage?: LeadStage;
      dealValue?: number | null;
      notes: string;
    }) =>
      apiClient<LeadDto>(`/api/leads/${leadId}/details`, {
        method: 'PATCH',
        body: JSON.stringify({ stage, dealValue, notes }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leads'] });
      queryClient.invalidateQueries({ queryKey: ['leads-analytics'] });
      setShowEditModal(false);
      setSuccessMsg('Lead details updated.');
      setTimeout(() => setSuccessMsg(null), 2500);
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to update lead details.');
    },
  });

  // 5. Create Manual Lead Mutation
  const createLeadMutation = useMutation({
    mutationFn: (data: {
      name: string;
      phoneE164: string;
      stage: LeadStage;
      dealValue: number | null;
      notes: string;
    }) =>
      apiClient<LeadDto>('/api/leads', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leads'] });
      queryClient.invalidateQueries({ queryKey: ['leads-analytics'] });
      setShowCreateModal(false);
      setNewLeadName('');
      setNewLeadPhone('');
      setNewLeadDealValue('');
      setNewLeadNotes('');
      setSuccessMsg('Lead added successfully.');
      setTimeout(() => setSuccessMsg(null), 2500);
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to add lead.');
    },
  });

  // Drag and drop handlers
  const handleDragStart = (e: React.DragEvent, leadId: string) => {
    e.dataTransfer.setData('text/plain', leadId);
    setDraggedLeadId(leadId);
  };

  const handleDragOver = (e: React.DragEvent, stage: LeadStage) => {
    e.preventDefault();
    if (dragOverStage !== stage) {
      setDragOverStage(stage);
    }
  };

  const handleDrop = (e: React.DragEvent, targetStage: LeadStage) => {
    e.preventDefault();
    const leadId = e.dataTransfer.getData('text/plain') || draggedLeadId;
    setDraggedLeadId(null);
    setDragOverStage(null);

    if (leadId) {
      const currentLead = leads?.find((l) => l.id === leadId);
      if (currentLead && currentLead.stage !== targetStage) {
        updateStageMutation.mutate({ leadId, stage: targetStage });
      }
    }
  };

  const handleOpenEdit = (lead: LeadDto) => {
    setSelectedLead(lead);
    setEditDealValue(lead.dealValue ? String(lead.dealValue) : '');
    setEditNotes(lead.notes || '');
    setEditStage(lead.stage);
    setShowEditModal(true);
  };

  // Filter and search logic
  const filteredLeads = useMemo(() => {
    if (!leads) return [];
    let list = leads;

    if (filterTab === 'IN_DISCUSSION') {
      list = list.filter((l) => l.stage === 'IN_DISCUSSION');
    } else if (filterTab === 'PROPOSAL_SENT') {
      list = list.filter((l) => l.stage === 'PROPOSAL_SENT');
    } else if (filterTab === 'WON') {
      list = list.filter((l) => l.stage === 'WON');
    }

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      list = list.filter(
        (l) =>
          (l.contactName && l.contactName.toLowerCase().includes(q)) ||
          (l.phoneE164 && l.phoneE164.includes(q)) ||
          (l.requirementSummary && l.requirementSummary.toLowerCase().includes(q)) ||
          (l.notes && l.notes.toLowerCase().includes(q))
      );
    }

    return list;
  }, [leads, filterTab, searchQuery]);

  if (isLeadsLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-24 w-full rounded-2xl" />
        <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
          <Skeleton className="h-96 rounded-2xl" />
          <Skeleton className="h-96 rounded-2xl" />
          <Skeleton className="h-96 rounded-2xl" />
          <Skeleton className="h-96 rounded-2xl" />
        </div>
      </div>
    );
  }

  if (isLeadsError) {
    return (
      <ErrorState
        title="Could not load Leads CRM"
        message={(leadsError as any)?.message || 'Failed to retrieve leads pipeline.'}
        onRetry={refetchLeads}
      />
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-gray-900 tracking-tight">
            Leads Pipeline
          </h1>
          <p className="text-xs text-gray-500 mt-1">
            Drag cards or select from the stage list to track customer inquiries and sales.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-brand-600 rounded-xl hover:bg-brand-700 transition-colors shadow-xs shrink-0"
        >
          <Plus className="w-4 h-4" />
          Add Lead
        </button>
      </div>

      {/* Alert Banners */}
      {errorMsg && (
        <AlertBanner
          type="error"
          message={errorMsg}
          onClose={() => setErrorMsg(null)}
        />
      )}
      {successMsg && (
        <AlertBanner
          type="success"
          message={successMsg}
          onClose={() => setSuccessMsg(null)}
        />
      )}

      {/* Revenue & Pipeline Summary Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-4 rounded-2xl bg-white border border-gray-200 shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-500 uppercase tracking-wider">
              Active Pipeline
            </span>
            <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
              <IndianRupee className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2">
            <span className="text-2xl font-black text-gray-900">
              ₹{(analytics?.totalPipelineValue || 0).toLocaleString('en-IN')}
            </span>
          </div>
          <p className="text-[11px] text-gray-400 mt-0.5 font-medium">
            Total active deal value
          </p>
        </div>

        <div className="p-4 rounded-2xl bg-white border border-gray-200 shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-500 uppercase tracking-wider">
              Total Inquiries
            </span>
            <div className="w-8 h-8 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2">
            <span className="text-2xl font-black text-gray-900">
              {analytics?.totalLeads || 0}
            </span>
          </div>
          <p className="text-[11px] text-gray-400 mt-0.5 font-medium">
            Contacts in pipeline
          </p>
        </div>

        <div className="p-4 rounded-2xl bg-white border border-gray-200 shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-500 uppercase tracking-wider">
              In Discussion
            </span>
            <div className="w-8 h-8 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
              <MessageSquare className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2">
            <span className="text-2xl font-black text-purple-700">
              {analytics?.inDiscussion || 0}
            </span>
          </div>
          <p className="text-[11px] text-gray-400 mt-0.5 font-medium">
            Active conversations
          </p>
        </div>

        <div className="p-4 rounded-2xl bg-white border border-gray-200 shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-gray-500 uppercase tracking-wider">
              Deals Won
            </span>
            <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <Trophy className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-1.5">
            <span className="text-2xl font-black text-emerald-600">
              ₹{(analytics?.wonValue || 0).toLocaleString('en-IN')}
            </span>
            <span className="text-xs font-bold text-gray-400">
              ({analytics?.won || 0} closed)
            </span>
          </div>
          <p className="text-[11px] text-gray-400 mt-0.5 font-medium">
            {analytics?.conversionRatePercent || 0}% closed successfully
          </p>
        </div>
      </div>

      {/* Search & Filter Toolbar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3 rounded-2xl border border-gray-200 shadow-2xs">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search by name, phone, or message..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-2 text-gray-400 hover:text-gray-600"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        <div className="flex items-center gap-1.5 w-full sm:w-auto overflow-x-auto">
          <button
            type="button"
            onClick={() => setFilterTab('ALL')}
            className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-colors shrink-0 ${
              filterTab === 'ALL'
                ? 'bg-gray-900 text-white shadow-xs'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            All ({leads?.length || 0})
          </button>
          <button
            type="button"
            onClick={() => setFilterTab('IN_DISCUSSION')}
            className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-colors shrink-0 ${
              filterTab === 'IN_DISCUSSION'
                ? 'bg-purple-600 text-white shadow-xs'
                : 'bg-purple-50 text-purple-700 hover:bg-purple-100'
            }`}
          >
            In Discussion ({leads?.filter((l) => l.stage === 'IN_DISCUSSION').length || 0})
          </button>
          <button
            type="button"
            onClick={() => setFilterTab('PROPOSAL_SENT')}
            className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-colors shrink-0 ${
              filterTab === 'PROPOSAL_SENT'
                ? 'bg-amber-600 text-white shadow-xs'
                : 'bg-amber-50 text-amber-700 hover:bg-amber-100'
            }`}
          >
            Quotation Sent ({leads?.filter((l) => l.stage === 'PROPOSAL_SENT').length || 0})
          </button>
          <button
            type="button"
            onClick={() => setFilterTab('WON')}
            className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-colors shrink-0 ${
              filterTab === 'WON'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
            }`}
          >
            Won ({leads?.filter((l) => l.stage === 'WON').length || 0})
          </button>
        </div>
      </div>

      {/* 4-Stage Kanban Pipeline */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 items-start">
        {STAGES.map((col) => {
          const colLeads = filteredLeads.filter((l) => l.stage === col.key);
          const colValue = colLeads.reduce((acc, l) => acc + (Number(l.dealValue) || 0), 0);
          const isOver = dragOverStage === col.key;

          return (
            <div
              key={col.key}
              onDragOver={(e) => handleDragOver(e, col.key)}
              onDragLeave={() => {
                if (dragOverStage === col.key) setDragOverStage(null);
              }}
              onDrop={(e) => handleDrop(e, col.key)}
              className={`flex flex-col rounded-2xl bg-slate-50/80 border transition-all duration-200 min-h-[520px] ${
                isOver
                  ? 'border-brand-500 bg-brand-50/40 ring-2 ring-brand-400 ring-offset-1'
                  : 'border-slate-200/80'
              }`}
            >
              {/* Column Header */}
              <div className="p-3.5 border-b border-slate-200/80 flex items-center justify-between bg-white rounded-t-2xl">
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className={`font-bold text-xs uppercase tracking-wider ${col.color}`}>
                      {col.title}
                    </h2>
                    <span className="w-5 h-5 rounded-full bg-slate-100 text-slate-700 text-[11px] font-bold flex items-center justify-center">
                      {colLeads.length}
                    </span>
                  </div>
                  <div className="text-[11px] font-semibold text-slate-500 mt-0.5 flex items-center">
                    <IndianRupee className="w-3 h-3 text-slate-400" />
                    {colValue.toLocaleString('en-IN')}
                  </div>
                </div>

                {isOver && (
                  <span className="text-[10px] font-bold text-brand-600 bg-brand-100 px-2 py-0.5 rounded-md animate-pulse">
                    Drop here
                  </span>
                )}
              </div>

              {/* Cards Container */}
              <div className="p-3 space-y-3 flex-1">
                {colLeads.length === 0 ? (
                  <div
                    className={`h-36 rounded-xl border border-dashed flex flex-col items-center justify-center text-center p-4 transition-colors ${
                      isOver
                        ? 'border-brand-400 bg-brand-50/50 text-brand-700'
                        : 'border-slate-200 text-slate-400 bg-slate-50/40'
                    }`}
                  >
                    <p className="text-xs font-semibold">
                      {isOver ? 'Drop lead to move' : 'No leads in this stage'}
                    </p>
                    <p className="text-[10px] text-slate-400 mt-1">
                      Drag here to move
                    </p>
                  </div>
                ) : (
                  colLeads.map((lead) => (
                    <div
                      key={lead.id}
                      draggable
                      onDragStart={(e) => handleDragStart(e, lead.id)}
                      onDragEnd={() => {
                        setDraggedLeadId(null);
                        setDragOverStage(null);
                      }}
                      className={`group p-3 rounded-xl bg-white border border-slate-200/90 shadow-2xs hover:shadow-md hover:border-brand-400 transition-all cursor-grab active:cursor-grabbing relative ${
                        draggedLeadId === lead.id ? 'opacity-40 scale-95' : ''
                      }`}
                    >
                      {/* Header: Name & Deal Value */}
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-1.5 min-w-0">
                          <GripVertical className="w-3.5 h-3.5 text-slate-300 group-hover:text-slate-400 shrink-0" />
                          <h3 className="font-bold text-sm text-gray-900 truncate">
                            {lead.contactName || 'Unnamed Contact'}
                          </h3>
                        </div>

                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleOpenEdit(lead);
                          }}
                          className={`text-xs font-bold px-2 py-0.5 rounded-md transition-colors shrink-0 ${
                            lead.dealValue && lead.dealValue > 0
                              ? 'text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200'
                              : 'text-slate-400 bg-slate-50 hover:bg-slate-100 border border-slate-100'
                          }`}
                          title="Edit deal value"
                        >
                          {lead.dealValue && lead.dealValue > 0
                            ? `₹${lead.dealValue.toLocaleString('en-IN')}`
                            : '+ ₹ Value'}
                        </button>
                      </div>

                      {/* Phone Number */}
                      <p className="text-[11px] text-gray-400 font-mono pl-5 mt-0.5">
                        {formatE164(lead.phoneE164)}
                      </p>

                      {/* Internal Notes Snippet */}
                      {lead.notes && (
                        <p className="mt-2 text-xs text-slate-600 line-clamp-2 pl-5 italic">
                          "{lead.notes}"
                        </p>
                      )}

                      {/* Footer: Time & Minimal Icon Actions */}
                      <div className="mt-3 pt-2 border-t border-slate-100 flex items-center justify-between pl-5">
                        <span className="text-[10px] text-slate-400">
                          {lead.lastInteractionAt ? formatRelativeTime(lead.lastInteractionAt) : ''}
                        </span>

                        <div className="flex items-center gap-1">
                          {/* Stage Selector Dropdown */}
                          <select
                            value={lead.stage}
                            onChange={(e) => {
                              e.stopPropagation();
                              updateStageMutation.mutate({
                                leadId: lead.id,
                                stage: e.target.value as LeadStage,
                              });
                            }}
                            className="text-[10px] font-semibold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded px-1.5 py-0.5 border-0 cursor-pointer focus:outline-hidden"
                          >
                            <option value="NEW">New</option>
                            <option value="IN_DISCUSSION">Discussion</option>
                            <option value="PROPOSAL_SENT">Proposal</option>
                            <option value="WON">Won</option>
                            <option value="LOST">Lost</option>
                          </select>

                          {/* Chat in Web Inbox Icon */}
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              navigate(`/inbox?contactId=${lead.contactId}`);
                            }}
                            className="p-1 rounded-lg text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 transition-colors"
                            title="Open Chat in Inbox"
                          >
                            <MessageCircle className="w-4 h-4" />
                          </button>

                          {/* Edit Details Icon */}
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleOpenEdit(lead);
                            }}
                            className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-50 transition-colors"
                            title="Edit notes & details"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Edit Deal Value & Notes Modal */}
      {showEditModal && selectedLead && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 space-y-4 animate-in zoom-in-95 duration-150 border border-gray-200">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h3 className="text-base font-bold text-gray-900">
                  Lead Details: {selectedLead.contactName || 'Contact'}
                </h3>
                <p className="text-xs text-gray-500">{formatE164(selectedLead.phoneE164)}</p>
              </div>
              <button
                type="button"
                onClick={() => setShowEditModal(false)}
                className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                const trimmedDeal = editDealValue.trim();
                const dealVal = trimmedDeal !== '' ? Number(trimmedDeal) : null;
                updateDetailsMutation.mutate({
                  leadId: selectedLead.id,
                  stage: editStage,
                  dealValue: dealVal,
                  notes: editNotes,
                });
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Pipeline Stage
                </label>
                <select
                  value={editStage}
                  onChange={(e) => setEditStage(e.target.value as LeadStage)}
                  className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white font-semibold"
                >
                  <option value="NEW">New Inquiry</option>
                  <option value="IN_DISCUSSION">In Discussion</option>
                  <option value="PROPOSAL_SENT">Quotation Sent</option>
                  <option value="WON">Deal Won</option>
                  <option value="LOST">Mark as Lost</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Deal Value (₹)
                </label>
                <div className="relative">
                  <span className="absolute left-3 top-2.5 text-xs text-gray-500 font-bold">₹</span>
                  <input
                    type="number"
                    min="0"
                    step="any"
                    placeholder="Enter amount (e.g. 5000)"
                    value={editDealValue}
                    onChange={(e) => setEditDealValue(e.target.value)}
                    className="w-full pl-7 pr-3 py-2 text-sm bg-gray-50 border border-gray-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Notes
                </label>
                <textarea
                  rows={3}
                  value={editNotes}
                  onChange={(e) => setEditNotes(e.target.value)}
                  placeholder="e.g. Spoke on call, interested in annual plan."
                  className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="px-4 py-2 text-xs font-bold text-gray-600 hover:bg-gray-100 rounded-xl transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updateDetailsMutation.isPending}
                  className="px-4 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-xs transition-colors"
                >
                  {updateDetailsMutation.isPending ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Manual Lead Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 space-y-4 animate-in zoom-in-95 duration-150 border border-gray-200">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h3 className="text-base font-bold text-gray-900">Add New Lead</h3>
                <p className="text-xs text-gray-500">Register an offline or walk-in customer inquiry</p>
              </div>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (!newLeadPhone.trim()) {
                  setErrorMsg('Phone number is required.');
                  return;
                }
                const trimmedDeal = newLeadDealValue.trim();
                createLeadMutation.mutate({
                  name: newLeadName.trim(),
                  phoneE164: newLeadPhone.trim(),
                  stage: newLeadStage,
                  dealValue: trimmedDeal !== '' ? Number(trimmedDeal) : null,
                  notes: newLeadNotes.trim(),
                });
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Customer Name
                </label>
                <input
                  type="text"
                  placeholder="e.g. Rahul Sharma"
                  value={newLeadName}
                  onChange={(e) => setNewLeadName(e.target.value)}
                  className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  WhatsApp Phone Number (E.164) *
                </label>
                <input
                  type="text"
                  placeholder="e.g. +919876543210"
                  value={newLeadPhone}
                  onChange={(e) => setNewLeadPhone(e.target.value)}
                  required
                  className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Pipeline Stage
                  </label>
                  <select
                    value={newLeadStage}
                    onChange={(e) => setNewLeadStage(e.target.value as LeadStage)}
                    className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
                  >
                    <option value="NEW">New Inquiry</option>
                    <option value="IN_DISCUSSION">In Discussion</option>
                    <option value="PROPOSAL_SENT">Quotation Sent</option>
                    <option value="WON">Deal Won</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Deal Value (₹)
                  </label>
                  <input
                    type="number"
                    min="0"
                    step="any"
                    placeholder="e.g. 5000"
                    value={newLeadDealValue}
                    onChange={(e) => setNewLeadDealValue(e.target.value)}
                    className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Notes
                </label>
                <textarea
                  rows={2}
                  value={newLeadNotes}
                  onChange={(e) => setNewLeadNotes(e.target.value)}
                  placeholder="Initial inquiry details..."
                  className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-brand-500 focus:bg-white"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-xs font-bold text-gray-600 hover:bg-gray-100 rounded-xl transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createLeadMutation.isPending}
                  className="px-4 py-2 text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 rounded-xl shadow-xs transition-colors"
                >
                  {createLeadMutation.isPending ? 'Adding...' : 'Add Lead'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default LeadsScreen;
