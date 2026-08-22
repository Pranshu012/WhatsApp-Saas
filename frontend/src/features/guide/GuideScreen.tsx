import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../api/client';
import { WhatsAppAccountResponse, FaqResponse, AutomationRuleResponse } from '../../api/types';
import {
  Sparkles,
  Smartphone,
  HelpCircle,
  Zap,
  MessageSquare,
  FileText,
  Calendar,
  Inbox,
  Settings,
  LayoutDashboard,
  ArrowRight,
  Bot,
  PlayCircle,
  HelpCircle as QuestionIcon,
} from 'lucide-react';

export const GuideScreen: React.FC = () => {
  const navigate = useNavigate();

  const { data: account } = useQuery<WhatsAppAccountResponse>({
    queryKey: ['whatsapp-account'],
    queryFn: () => apiClient<WhatsAppAccountResponse>('/api/whatsapp/account'),
    retry: false,
  });

  const { data: faqs } = useQuery<FaqResponse[]>({
    queryKey: ['faqs'],
    queryFn: () => apiClient<FaqResponse[]>('/api/faqs'),
  });

  const { data: rules } = useQuery<AutomationRuleResponse[]>({
    queryKey: ['automation-rules'],
    queryFn: () => apiClient<AutomationRuleResponse[]>('/api/automation-rules'),
  });

  const isConnected = account?.status === 'CONNECTED';
  const faqCount = faqs?.filter((f) => f.enabled).length || 0;
  const ruleCount = rules?.filter((r) => r.enabled).length || 0;

  const appSections = [
    {
      icon: LayoutDashboard,
      title: 'Home (Business Control Center)',
      route: '/',
      tag: 'Daily Dashboard',
      tagColor: 'bg-emerald-100 text-emerald-800',
      description:
        'Your daily cockpit. Shows whether your WhatsApp bot is active, how many hours and ₹ you have saved, and lets you test bot replies directly on the screen.',
    },
    {
      icon: Smartphone,
      title: 'Connect WhatsApp',
      route: '/whatsapp',
      tag: 'Step 1 • Setup',
      tagColor: 'bg-blue-100 text-blue-800',
      description:
        'Connect your official WhatsApp Business number in 2 clicks. Powered by official Meta Cloud API with 0% risk of account bans.',
    },
    {
      icon: HelpCircle,
      title: 'Common Questions (FAQ Brain)',
      route: '/faq',
      tag: 'Step 2 • Bot Brain',
      tagColor: 'bg-teal-100 text-teal-800',
      description:
        'Teach your bot how to answer frequently asked customer questions about shop timings, prices, address, and returns — even if customers make spelling mistakes.',
    },
    {
      icon: Zap,
      title: 'Auto Replies (Keyword Rules)',
      route: '/automation',
      tag: 'Step 3 • Instant Triggers',
      tagColor: 'bg-amber-100 text-amber-800',
      description:
        'Set instant automated responses triggered when a customer sends specific trigger words like "MENU", "PRICE", "OFFER", or "LOCATION".',
    },
    {
      icon: MessageSquare,
      title: 'Live Inbox',
      route: '/inbox',
      tag: '1-on-1 Chats',
      tagColor: 'bg-brand-100 text-brand-800',
      description:
        'View live customer conversations and chat manually within the free 24-hour service window. Live timer prevents extra messaging costs.',
    },
    {
      icon: FileText,
      title: 'Message Templates',
      route: '/templates',
      tag: 'Approved Messages',
      tagColor: 'bg-purple-100 text-purple-800',
      description:
        'Pre-approved message formats required by WhatsApp to send bulk festival offers, discount coupons, and automated order confirmations.',
    },
    {
      icon: Calendar,
      title: 'Scheduled Messages',
      route: '/scheduled',
      tag: 'Timed Campaigns',
      tagColor: 'bg-indigo-100 text-indigo-800',
      description:
        'Schedule marketing announcements and festive greetings for a future date and time (e.g. Diwali offer on Sunday at 10 AM).',
    },
    {
      icon: Inbox,
      title: 'Needs Your Reply (Unanswered)',
      route: '/unmatched',
      tag: 'Safety Net',
      tagColor: 'bg-rose-100 text-rose-800',
      description:
        'Customer inquiries that your bot was not 100% sure about. You can reply manually with 1 click or turn them into new automated FAQ answers.',
    },
    {
      icon: Settings,
      title: 'Settings & Business Profile',
      route: '/settings',
      tag: 'Account & Billing',
      tagColor: 'bg-slate-100 text-slate-800',
      description:
        'Update your business legal name, GSTIN number for tax invoices, and default time zone.',
    },
  ];

  return (
    <div className="space-y-10 max-w-6xl mx-auto pb-12">
      {/* 1. Header & Introduction */}
      <div className="bg-gradient-to-r from-brand-700 via-brand-600 to-emerald-600 rounded-3xl p-6 sm:p-10 text-white shadow-xl space-y-4">
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/15 backdrop-blur-md text-emerald-100 text-xs font-bold border border-white/20">
          <Sparkles className="w-4 h-4 text-emerald-300" />
          Easy Non-Tech Setup Guide
        </div>
        <h1 className="text-3xl sm:text-4xl font-black tracking-tight leading-tight">
          How WhatsApp SaaS Works For Your Business
        </h1>
        <p className="text-sm sm:text-base text-emerald-100 max-w-3xl leading-relaxed">
          No coding, no technical knowledge, and no server management needed. Follow these 4 simple steps to automate 80% of your customer WhatsApp chats in under 5 minutes.
        </p>
      </div>

      {/* 2. 4-Step Quick Setup Roadmap */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              <PlayCircle className="w-6 h-6 text-brand-600" />
              4 Steps to Get Your WhatsApp Bot Live
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Complete these steps in order to start answering customer inquiries automatically.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Step 1 */}
          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4 hover:border-brand-300 transition-all">
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="w-8 h-8 rounded-xl bg-blue-100 text-blue-800 font-black text-sm flex items-center justify-center">
                  1
                </span>
                <span
                  className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                    isConnected ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                  }`}
                >
                  {isConnected ? '✓ Completed' : 'Pending'}
                </span>
              </div>
              <h3 className="font-bold text-base text-slate-900">Connect Your WhatsApp Number</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Connect your business phone number using official Meta Cloud API. 100% official and zero risk of WhatsApp bans.
              </p>
            </div>
            <button
              type="button"
              onClick={() => navigate('/whatsapp')}
              className="w-full inline-flex items-center justify-center gap-2 py-2.5 px-4 bg-brand-50 hover:bg-brand-100 text-brand-700 font-bold rounded-xl text-xs transition-colors"
            >
              <span>{isConnected ? 'View Connected Number' : 'Connect WhatsApp Number'}</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Step 2 */}
          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4 hover:border-brand-300 transition-all">
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="w-8 h-8 rounded-xl bg-teal-100 text-teal-800 font-black text-sm flex items-center justify-center">
                  2
                </span>
                <span
                  className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                    faqCount >= 3 ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-700'
                  }`}
                >
                  {faqCount}/3 Added
                </span>
              </div>
              <h3 className="font-bold text-base text-slate-900">Add 3 Common Store Questions (FAQ)</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Save answers for your shop timings, price catalog, and address. The AI bot will reply even if customers make spelling mistakes!
              </p>
            </div>
            <button
              type="button"
              onClick={() => navigate('/faq')}
              className="w-full inline-flex items-center justify-center gap-2 py-2.5 px-4 bg-teal-50 hover:bg-teal-100 text-teal-800 font-bold rounded-xl text-xs transition-colors"
            >
              <span>Add FAQ Questions</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Step 3 */}
          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4 hover:border-brand-300 transition-all">
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="w-8 h-8 rounded-xl bg-amber-100 text-amber-800 font-black text-sm flex items-center justify-center">
                  3
                </span>
                <span
                  className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                    ruleCount > 0 ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-700'
                  }`}
                >
                  {ruleCount} Rules Active
                </span>
              </div>
              <h3 className="font-bold text-base text-slate-900">Create Keyword Auto-Replies</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Set instant trigger words like &ldquo;MENU&rdquo;, &ldquo;OFFER&rdquo;, or &ldquo;PRICE&rdquo; so customers get fast automated menus.
              </p>
            </div>
            <button
              type="button"
              onClick={() => navigate('/automation')}
              className="w-full inline-flex items-center justify-center gap-2 py-2.5 px-4 bg-amber-50 hover:bg-amber-100 text-amber-800 font-bold rounded-xl text-xs transition-colors"
            >
              <span>Set Keyword Triggers</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Step 4 */}
          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4 hover:border-brand-300 transition-all">
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-800 font-black text-sm flex items-center justify-center">
                  4
                </span>
                <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                  Ready to Test
                </span>
              </div>
              <h3 className="font-bold text-base text-slate-900">Test In Live Bot Simulator</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Type sample customer questions in the simulator on the Home screen to see exactly how your bot will reply to customers.
              </p>
            </div>
            <button
              type="button"
              onClick={() => navigate('/')}
              className="w-full inline-flex items-center justify-center gap-2 py-2.5 px-4 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 font-bold rounded-xl text-xs transition-colors"
            >
              <span>Go to Home & Test Simulator</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* 3. Visual Sidebar Page Guide */}
      <div className="space-y-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
            <Bot className="w-6 h-6 text-brand-600" />
            What Every Screen in the App Does
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Click on any section to jump directly to that tool.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {appSections.map((sec, idx) => {
            const Icon = sec.icon;
            return (
              <div
                key={idx}
                onClick={() => navigate(sec.route)}
                className="bg-white p-5 rounded-3xl border border-slate-200 hover:border-brand-400 hover:shadow-lg transition-all cursor-pointer flex flex-col justify-between space-y-3 group"
              >
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="w-10 h-10 rounded-2xl bg-slate-50 group-hover:bg-brand-50 text-slate-700 group-hover:text-brand-600 flex items-center justify-center transition-colors">
                      <Icon className="w-5 h-5" />
                    </div>
                    <span
                      className={`text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded-full ${sec.tagColor}`}
                    >
                      {sec.tag}
                    </span>
                  </div>
                  <h3 className="font-bold text-sm text-slate-900 group-hover:text-brand-600 transition-colors">
                    {sec.title}
                  </h3>
                  <p className="text-xs text-slate-600 leading-relaxed">{sec.description}</p>
                </div>
                <div className="flex items-center gap-1 text-xs font-bold text-brand-600 pt-2 border-t border-slate-100 group-hover:translate-x-1 transition-transform">
                  <span>Open {sec.title.split(' ')[0]}</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 4. Non-Tech Frequently Asked Questions */}
      <div className="bg-slate-900 text-white p-6 sm:p-8 rounded-3xl shadow-xl space-y-6">
        <div className="flex items-center gap-2">
          <QuestionIcon className="w-6 h-6 text-emerald-400" />
          <h2 className="text-lg font-bold text-white">Common Questions by Business Owners</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
          <div className="bg-slate-800/80 p-4 rounded-2xl border border-slate-700/80 space-y-2">
            <h4 className="font-bold text-sm text-white">Do I need to keep my laptop open 24/7?</h4>
            <p className="text-slate-300 leading-relaxed">
              No! WhatsApp SaaS runs 24/7 on our cloud servers. It will answer your customer inquiries automatically even when your laptop and phone are switched off.
            </p>
          </div>

          <div className="bg-slate-800/80 p-4 rounded-2xl border border-slate-700/80 space-y-2">
            <h4 className="font-bold text-sm text-white">Will my WhatsApp number get banned?</h4>
            <p className="text-slate-300 leading-relaxed">
              Never. We use the Official Meta Cloud API. Unlike third-party unauthorized QR code web scrapers, Meta officially approves your account with 0% ban risk.
            </p>
          </div>

          <div className="bg-slate-800/80 p-4 rounded-2xl border border-slate-700/80 space-y-2">
            <h4 className="font-bold text-sm text-white">Can I still reply manually to customers?</h4>
            <p className="text-slate-300 leading-relaxed">
              Yes! You can view and reply to any customer conversation anytime from the <strong>Inbox</strong> tab. The bot never blocks your human agents.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
export default GuideScreen;
