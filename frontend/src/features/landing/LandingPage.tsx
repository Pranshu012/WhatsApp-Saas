import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  MessageSquare,
  Zap,
  HelpCircle,
  Send,
  CheckCircle2,
  ArrowRight,
  Sparkles,
  Bot,
  TrendingUp,
  Percent,
  Check,
  Smartphone,
  ChevronRight,
  Flame,
  Award,
  Compass,
} from 'lucide-react';

interface ChatMessage {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  time: string;
}

export const LandingPage: React.FC = () => {
  // Active Stage in Winding Path
  const [activeStage, setActiveStage] = useState<number>(2);
  const [simulatedConnected, setSimulatedConnected] = useState(false);

  // Live Simulator State
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: '1',
      sender: 'user',
      text: 'What are your store timings today?',
      time: '10:42 AM',
    },
    {
      id: '2',
      sender: 'bot',
      text: 'We are open Monday to Saturday from 9:30 AM to 8:30 PM! Sunday closed. Reply MENU to see our full catalog! 🍰',
      time: '10:42 AM',
    },
  ]);
  const [inputText, setInputText] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  // ROI Calculator State
  const [monthlyInquiries, setMonthlyInquiries] = useState<number>(2500);

  const samplePrompts = [
    { label: '💰 Pricing & Catalog', text: 'Send me your pricing and product catalog' },
    { label: '⏰ Shop Timings', text: 'wat r ur timings today?' },
    { label: '🚚 Delivery Status', text: 'Where is my order #4589?' },
    { label: '🔄 Refund Policy', text: 'do u give refund within 7 days?' },
  ];

  const botResponses: Record<string, string> = {
    pricing: 'Our all-in-one Business plan is just ₹499/month (~₹16/day) with unlimited auto-replies, smart FAQ bot, and official Meta Cloud API integration. Start your 14-day free trial here: https://wasaas.app/register',
    timing: 'We are open Monday to Saturday from 9:30 AM to 8:30 PM! Sunday closed.',
    timings: 'We are open Monday to Saturday from 9:30 AM to 8:30 PM! Sunday closed.',
    order: 'Your order #4589 has been dispatched via BlueDart! Live tracking: https://track.example.com/4589 (Estimated delivery: Tomorrow by 4 PM)',
    refund: 'Yes! We offer 100% money-back guarantee within 7 days of purchase. No questions asked.',
    catalog: 'Here is our 2026 digital product catalog: https://catalog.example.com/pdf. Replying "BUY" will connect you to our billing desk.',
  };

  const handleSendMessage = (textToSend?: string) => {
    const text = textToSend || inputText;
    if (!text.trim()) return;

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      sender: 'user',
      text: text.trim(),
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInputText('');
    setIsTyping(true);

    setTimeout(() => {
      const lower = text.toLowerCase();
      let reply = "Thanks for reaching out! A support agent will assist you shortly, or reply with 'MENU' to see automated options.";

      for (const [key, val] of Object.entries(botResponses)) {
        if (lower.includes(key)) {
          reply = val;
          break;
        }
      }

      const botMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'bot',
        text: reply,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };

      setMessages((prev) => [...prev, botMsg]);
      setIsTyping(false);
    }, 600);
  };

  // Calculations for ROI
  const automatedInquiries = Math.round(monthlyInquiries * 0.78);
  const hoursSaved = Math.round((automatedInquiries * 3.5) / 60);
  const rupeesSaved = Math.round(hoursSaved * 250);

  const stages = [
    {
      stageNumber: 1,
      title: 'Stage 1: The Daily WhatsApp Chaos',
      subtitle: 'Where most businesses start',
      badge: 'The Problem',
      badgeColor: 'bg-rose-100 text-rose-800 border-rose-200',
      icon: Flame,
      iconBg: 'bg-rose-500 text-white',
      problemList: [
        'Drowning in 100+ repetitive customer chats every single day',
        'Losing high-value leads after 8 PM when staff goes home',
        'Manually copying and pasting the same price list 50 times',
        'Customer waits 30+ minutes for a simple opening hours reply',
      ],
      result: 'Lost sales, frustrated customers, and wasted staff hours.',
      actionText: 'See The Way Out ↓',
    },
    {
      stageNumber: 2,
      title: 'Stage 2: 2-Minute Official WhatsApp Connect',
      subtitle: 'Official Meta Cloud API • Zero Ban Risk',
      badge: 'Step 1 • 2 Mins',
      badgeColor: 'bg-blue-100 text-blue-800 border-blue-200',
      icon: Smartphone,
      iconBg: 'bg-blue-600 text-white',
      problemList: [
        'Link your existing WhatsApp Business number in 2 clicks',
        'Official Meta Cloud API certification (100% ban-proof)',
        'No QR code scanning or keeping your personal phone switched on',
        'Runs 24/7 on high-speed cloud servers even while you sleep',
      ],
      interactiveType: 'connect_sim',
      result: 'Your official WhatsApp number is live on the cloud and ready to automate!',
      actionText: 'Next: Train Your Bot Brain →',
    },
    {
      stageNumber: 3,
      title: 'Stage 3: Load Your Bot Brain (FAQs & Typos)',
      subtitle: 'Smart AI understands typos & Hinglish',
      badge: 'Step 2 • 3 Mins',
      badgeColor: 'bg-teal-100 text-teal-800 border-teal-200',
      icon: HelpCircle,
      iconBg: 'bg-teal-600 text-white',
      problemList: [
        'Add common Q&As for shop timings, pricing, location & returns',
        'Understands spelling mistakes like "wat r ur timngs" or "price btao"',
        'Replies instantly in <1 second with formatted menus & links',
        'Never forgets, never takes a day off, never gives wrong answers',
      ],
      interactiveType: 'typo_demo',
      result: '78% of all customer inquiries are answered automatically in 0.8 seconds!',
      actionText: 'Next: Set Keyword Triggers →',
    },
    {
      stageNumber: 4,
      title: 'Stage 4: Set Keyword Trigger Words',
      subtitle: 'Instant menus on words like "MENU" or "PRICE"',
      badge: 'Step 3 • Instant',
      badgeColor: 'bg-amber-100 text-amber-800 border-amber-200',
      icon: Zap,
      iconBg: 'bg-amber-500 text-white',
      problemList: [
        'Customer sends "MENU" → Gets instant PDF or text catalog',
        'Customer sends "PRICE" → Gets instant price list & payment link',
        'Customer sends "OFFER" → Gets latest festive discount coupon',
        'Customer sends "LOCATION" → Gets exact Google Maps pin',
      ],
      result: 'Instant digital self-service for all your customers!',
      actionText: 'Next: Bulk Broadcasts →',
    },
    {
      stageNumber: 5,
      title: 'Stage 5: Send 1-Click Broadcast Offers',
      subtitle: 'Festival sales & discount campaigns',
      badge: 'Step 4 • Revenue Booster',
      badgeColor: 'bg-purple-100 text-purple-800 border-purple-200',
      icon: Send,
      iconBg: 'bg-purple-600 text-white',
      problemList: [
        'Send festive offers to 10,000+ opted-in customers in 1 click',
        'Personalize every message with customer name & custom discount code',
        'High 98% open rate compared to 15% on normal email/SMS',
        'Pre-approved Meta templates prevent spam complaints',
      ],
      result: 'Generate 3x-5x repeat sales on every festival and weekend!',
      actionText: 'Reach The Summit →',
    },
    {
      stageNumber: 6,
      title: 'Stage 6: The Summit — 100% Automated 24/7 Growth',
      subtitle: 'Your business runs on autopilot',
      badge: 'Goal Achieved! 🏆',
      badgeColor: 'bg-emerald-100 text-emerald-800 border-emerald-300 font-black',
      icon: Award,
      iconBg: 'bg-emerald-500 text-white',
      problemList: [
        'Save ~45 hours of manual staff typing every month',
        'Never lose a customer lead after closing hours',
        'Answer 100% of customer inquiries in under 1 second',
        'All for just ₹499/month (~₹16/day) with zero hidden markups',
      ],
      result: 'Freedom to focus on growing your business while WhatsApp handles the rest!',
      actionText: 'Start 14-Day Free Trial (₹0) →',
      isSummit: true,
    },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-sky-50 via-slate-50 to-emerald-50/40 text-slate-900 font-sans selection:bg-brand-500 selection:text-white">
      {/* 1. STICKY NAVBAR */}
      <header className="sticky top-0 z-50 bg-white/90 backdrop-blur-md border-b border-slate-200/80 transition-all">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-18 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2.5 group">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-700 to-brand-500 flex items-center justify-center text-white shadow-md shadow-brand-500/25 group-hover:scale-105 transition-transform">
              <MessageSquare className="w-5 h-5" />
            </div>
            <span className="text-xl font-black tracking-tight text-slate-900">
              WhatsApp<span className="text-brand-600">SaaS</span>
            </span>
          </Link>

          <nav className="hidden md:flex items-center gap-8 text-sm font-semibold text-slate-600">
            <a href="#roadmap" className="hover:text-brand-600 transition-colors flex items-center gap-1.5">
              <Compass className="w-4 h-4 text-brand-600" />
              Interactive Roadmap
            </a>
            <a href="#simulator" className="hover:text-brand-600 transition-colors">Live Bot Simulator</a>
            <a href="#calculator" className="hover:text-brand-600 transition-colors">Savings Calculator</a>
            <a href="#pricing" className="hover:text-brand-600 transition-colors">Pricing (₹499/mo)</a>
          </nav>

          <div className="flex items-center gap-3">
            <Link
              to="/login"
              className="px-4 py-2 text-sm font-bold text-slate-700 hover:text-brand-600 transition-colors"
            >
              Sign In
            </Link>
            <Link
              to="/register"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-sm font-bold shadow-md shadow-brand-600/20 transition-all hover:shadow-lg hover:scale-105"
            >
              Start Free Trial (₹0)
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </header>

      {/* 2. HERO SECTION WITH JOURNEY THEME */}
      <section className="relative pt-12 pb-16 md:pt-20 md:pb-24 overflow-hidden bg-gradient-to-b from-sky-100/60 via-white to-slate-50">
        {/* Subtle Background Clouds / Mountain Art */}
        <div className="absolute top-0 right-0 w-96 h-96 rounded-full bg-emerald-200/30 blur-3xl -z-10" />
        <div className="absolute top-20 left-10 w-80 h-80 rounded-full bg-sky-200/40 blur-3xl -z-10" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-6">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-emerald-100 border border-emerald-300 text-emerald-800 text-xs font-black shadow-sm uppercase tracking-wider">
            <Sparkles className="w-4 h-4 text-emerald-600" />
            Your Path from WhatsApp Chaos to 100% Automated 24/7 Sales
          </div>

          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black text-slate-900 tracking-tight leading-[1.1] max-w-4xl mx-auto">
            Take your business from{' '}
            <span className="text-rose-600 line-through decoration-rose-400">manual chat chaos</span>{' '}
            to a{' '}
            <span className="bg-gradient-to-r from-brand-600 via-emerald-600 to-teal-600 bg-clip-text text-transparent">
              24/7 automated sales machine
            </span>
            .
          </h1>

          <p className="text-base sm:text-lg text-slate-600 max-w-2xl mx-auto leading-relaxed">
            Follow the interactive roadmap below to see how our AI bot answers customer questions in &lt;1 second, handles typos & Hinglish, and helps you never miss a sale again.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-2">
            <Link
              to="/register"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-8 py-4 rounded-2xl bg-brand-600 hover:bg-brand-700 text-white font-black text-base shadow-xl shadow-brand-600/30 hover:scale-105 active:scale-[0.98] transition-all min-h-[50px]"
            >
              Start Free 14-Day Journey (₹0 to start)
              <ArrowRight className="w-5 h-5" />
            </Link>
            <a
              href="#roadmap"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-7 py-4 rounded-2xl bg-white border border-slate-300 hover:bg-slate-50 text-slate-800 font-bold text-sm shadow-sm transition-all min-h-[50px]"
            >
              <Compass className="w-4 h-4 text-brand-600" />
              Explore The 6-Stage Roadmap Below ↓
            </a>
          </div>

          {/* Quick Metrics Bar */}
          <div className="pt-8 grid grid-cols-2 sm:grid-cols-4 gap-4 max-w-4xl mx-auto text-left">
            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
              <p className="text-xs text-slate-500 font-bold uppercase">Setup Time</p>
              <p className="text-xl font-black text-slate-900 mt-0.5 font-mono">Under 3 Mins</p>
            </div>
            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
              <p className="text-xs text-slate-500 font-bold uppercase">Response Speed</p>
              <p className="text-xl font-black text-emerald-600 mt-0.5 font-mono">&lt; 0.8 Seconds</p>
            </div>
            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
              <p className="text-xs text-slate-500 font-bold uppercase">WhatsApp Ban Risk</p>
              <p className="text-xl font-black text-blue-600 mt-0.5 font-mono">0% (Meta Official)</p>
            </div>
            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
              <p className="text-xs text-slate-500 font-bold uppercase">Monthly Price</p>
              <p className="text-xl font-black text-purple-600 mt-0.5 font-mono">₹499 / mo (~₹16/d)</p>
            </div>
          </div>
        </div>
      </section>

      {/* 3. THE INTERACTIVE WINDING ROADMAP (THE STAGES & PATHWAY) */}
      <section id="roadmap" className="py-20 bg-slate-900 text-white relative overflow-hidden">
        {/* Mountain Path Background Glows */}
        <div className="absolute top-10 left-1/2 -translate-x-1/2 w-[800px] h-[500px] bg-emerald-500/10 rounded-full blur-[140px] pointer-events-none" />

        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto space-y-4 mb-16">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-emerald-500/20 text-emerald-400 text-xs font-black border border-emerald-500/30 uppercase tracking-wider">
              <Compass className="w-4 h-4" />
              Interactive Success Pathway
            </div>
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight text-white">
              The 6 Stages to 100% WhatsApp Automation
            </h2>
            <p className="text-slate-300 text-sm sm:text-base">
              Click on each milestone stage along the path to see how your business evolves from manual stress to complete automation.
            </p>

            {/* Stage Selector Pills */}
            <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
              {stages.map((stg) => (
                <button
                  key={stg.stageNumber}
                  type="button"
                  onClick={() => setActiveStage(stg.stageNumber)}
                  className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 ${
                    activeStage === stg.stageNumber
                      ? 'bg-emerald-500 text-slate-950 shadow-lg shadow-emerald-500/30 scale-105'
                      : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700'
                  }`}
                >
                  <span className="w-4 h-4 rounded-full bg-black/20 flex items-center justify-center text-[10px]">
                    {stg.stageNumber}
                  </span>
                  <span>{stg.title.split(':')[0]}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Active Stage Spotlight Card */}
          {(() => {
            const current = stages.find((s) => s.stageNumber === activeStage) || stages[0];
            const Icon = current.icon;

            return (
              <div className="bg-slate-950 rounded-3xl p-6 sm:p-10 border-2 border-emerald-500/40 shadow-2xl space-y-8 relative overflow-hidden">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-800">
                  <div className="flex items-center gap-4">
                    <div className={`w-14 h-14 rounded-2xl ${current.iconBg} flex items-center justify-center shadow-lg`}>
                      <Icon className="w-7 h-7" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className={`text-xs font-black uppercase px-2.5 py-0.5 rounded-full border ${current.badgeColor}`}>
                          {current.badge}
                        </span>
                        <span className="text-xs text-slate-400 font-mono">Stage {current.stageNumber} of 6</span>
                      </div>
                      <h3 className="text-xl sm:text-2xl font-black text-white mt-1">
                        {current.title}
                      </h3>
                      <p className="text-xs sm:text-sm text-slate-400 mt-0.5">{current.subtitle}</p>
                    </div>
                  </div>

                  {/* Stage Jump Next Button */}
                  <div className="flex items-center gap-2">
                    {current.stageNumber < 6 ? (
                      <button
                        type="button"
                        onClick={() => setActiveStage((prev) => Math.min(6, prev + 1))}
                        className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-emerald-400 font-bold rounded-xl text-xs border border-slate-700 transition-colors"
                      >
                        <span>Next Milestone</span>
                        <ChevronRight className="w-4 h-4" />
                      </button>
                    ) : (
                      <Link
                        to="/register"
                        className="inline-flex items-center gap-1.5 px-5 py-2.5 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black rounded-xl text-xs shadow-lg transition-all"
                      >
                        <span>Start Free Trial (₹0)</span>
                        <ArrowRight className="w-4 h-4" />
                      </Link>
                    )}
                  </div>
                </div>

                {/* Stage Body Grid */}
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
                  {/* Left Column: What Happens in this stage */}
                  <div className="lg:col-span-7 space-y-4">
                    <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                      Key Highlights of this Stage:
                    </h4>
                    <div className="space-y-3">
                      {current.problemList.map((item, idx) => (
                        <div key={idx} className="flex items-start gap-3 p-3 rounded-2xl bg-slate-900/90 border border-slate-800/90 text-xs sm:text-sm text-slate-200">
                          {current.isSummit ? (
                            <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                          ) : current.stageNumber === 1 ? (
                            <Flame className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                          ) : (
                            <Check className="w-5 h-5 text-brand-400 shrink-0 mt-0.5" />
                          )}
                          <span className="leading-relaxed">{item}</span>
                        </div>
                      ))}
                    </div>

                    <div className="p-4 rounded-2xl bg-emerald-950/40 border border-emerald-500/30 text-xs text-emerald-200 font-medium">
                      <strong>🎯 Result at this milestone:</strong> {current.result}
                    </div>
                  </div>

                  {/* Right Column: Interactive Stage Widget */}
                  <div className="lg:col-span-5 bg-slate-900 rounded-2xl p-5 border border-slate-800 space-y-4">
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                      <span className="text-xs font-bold text-slate-300">Live Stage Interactive Preview</span>
                      <span className="text-[10px] font-mono text-emerald-400">● Interactive</span>
                    </div>

                    {current.interactiveType === 'connect_sim' ? (
                      <div className="space-y-4 text-center py-4">
                        <div className="w-16 h-16 rounded-2xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center mx-auto">
                          <Smartphone className="w-8 h-8" />
                        </div>
                        <div>
                          <p className="text-sm font-bold text-white">
                            {simulatedConnected ? '✅ WhatsApp Number Linked!' : 'Simulate 2-Click Meta Link'}
                          </p>
                          <p className="text-xs text-slate-400 mt-1">
                            {simulatedConnected
                              ? 'Number +91 98765 43210 is now verified on Meta Cloud API.'
                              : 'Click below to see how fast official connection happens.'}
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => setSimulatedConnected(!simulatedConnected)}
                          className={`w-full py-2.5 px-4 rounded-xl text-xs font-bold transition-all ${
                            simulatedConnected
                              ? 'bg-slate-800 text-emerald-300 border border-emerald-500/30'
                              : 'bg-emerald-500 hover:bg-emerald-400 text-slate-950 shadow-lg'
                          }`}
                        >
                          {simulatedConnected ? 'Reset Simulation' : '⚡ Simulate WhatsApp Connect'}
                        </button>
                      </div>
                    ) : (
                      <div className="space-y-3">
                        <p className="text-xs text-slate-400">
                          Try typing a question below to test how our bot answers typo-filled questions:
                        </p>
                        <div className="flex gap-2">
                          <input
                            type="text"
                            value={inputText}
                            onChange={(e) => setInputText(e.target.value)}
                            placeholder="e.g. 'wat r ur timngs'"
                            className="flex-1 bg-slate-800 text-white placeholder-slate-500 text-xs px-3 py-2 rounded-xl border border-slate-700 focus:outline-none focus:ring-1 focus:ring-emerald-400"
                          />
                          <button
                            type="button"
                            onClick={() => handleSendMessage()}
                            className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs rounded-xl"
                          >
                            Send
                          </button>
                        </div>
                        <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 text-xs space-y-1">
                          <p className="text-slate-500 text-[10px] font-bold uppercase">Latest Bot Reply:</p>
                          <p className="text-emerald-300 leading-relaxed">
                            {messages[messages.length - 1]?.text || 'Type a question above to test!'}
                          </p>
                        </div>
                      </div>
                    )}

                    <div className="pt-2 text-center">
                      <Link
                        to="/register"
                        className="inline-flex items-center gap-1.5 text-xs font-bold text-emerald-400 hover:text-emerald-300 underline"
                      >
                        Enroll Now & Automate Your Number →
                      </Link>
                    </div>
                  </div>
                </div>
              </div>
            );
          })()}
        </div>
      </section>

      {/* 4. LIVE WHATSAPP PHONE SIMULATOR */}
      <section id="simulator" className="py-20 bg-slate-950 text-white relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto space-y-4 mb-14">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-400 text-xs font-bold">
              <Bot className="w-3.5 h-3.5" />
              Live Bot Simulator
            </div>
            <h2 className="text-3xl sm:text-4xl font-black tracking-tight text-white">
              Try our WhatsApp Bot right now
            </h2>
            <p className="text-slate-300 text-sm sm:text-base">
              Click a sample question below or type your own custom inquiry to experience how our AI responds in under 1 second.
            </p>
          </div>

          <div className="max-w-xl mx-auto bg-slate-900 rounded-[2.5rem] p-4 sm:p-6 shadow-2xl border border-slate-800">
            {/* Phone Header */}
            <div className="bg-emerald-800 rounded-2xl p-3.5 flex items-center justify-between shadow-md">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-full bg-white/20 flex items-center justify-center text-white font-bold text-sm">
                  WS
                </div>
                <div>
                  <h3 className="text-sm font-bold text-white leading-tight">Your Brand Store</h3>
                  <p className="text-[11px] text-emerald-200">
                    {isTyping ? 'typing...' : 'online • official business account'}
                  </p>
                </div>
              </div>
              <span className="text-xs bg-emerald-900/60 text-emerald-200 px-2 py-0.5 rounded font-mono">
                24h Window
              </span>
            </div>

            {/* Chat Body */}
            <div className="my-4 h-72 overflow-y-auto space-y-3 p-2 bg-slate-950/70 rounded-2xl border border-slate-800/80">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex flex-col ${msg.sender === 'user' ? 'items-end' : 'items-start'}`}
                >
                  <div
                    className={`max-w-[85%] rounded-2xl px-4 py-2.5 text-xs sm:text-sm leading-relaxed shadow-sm ${
                      msg.sender === 'user'
                        ? 'bg-emerald-600 text-white rounded-tr-none'
                        : 'bg-slate-800 text-slate-100 rounded-tl-none border border-slate-700'
                    }`}
                  >
                    {msg.text}
                  </div>
                  <span className="text-[10px] text-slate-500 mt-1 px-1">{msg.time}</span>
                </div>
              ))}

              {isTyping && (
                <div className="flex items-center gap-1.5 bg-slate-800 text-slate-400 px-3.5 py-2 rounded-2xl rounded-tl-none w-fit text-xs border border-slate-700 animate-pulse">
                  <span className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-bounce" />
                  <span className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-bounce [animation-delay:0.2s]" />
                  <span className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-bounce [animation-delay:0.4s]" />
                  <span className="text-[11px] text-slate-400 ml-1">AI bot is typing reply...</span>
                </div>
              )}
            </div>

            {/* Sample Clickable Prompts */}
            <div className="space-y-2 mb-4">
              <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                Click a sample customer question:
              </p>
              <div className="flex flex-wrap gap-1.5">
                {samplePrompts.map((p, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => handleSendMessage(p.text)}
                    className="text-xs px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-emerald-300 border border-slate-700 transition-colors"
                  >
                    {p.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Input Bar */}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSendMessage();
              }}
              className="flex items-center gap-2"
            >
              <input
                type="text"
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                placeholder="Type a message or question..."
                className="flex-1 bg-slate-800 text-white placeholder-slate-400 px-4 py-2.5 rounded-xl text-xs sm:text-sm border border-slate-700 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              />
              <button
                type="submit"
                disabled={!inputText.trim()}
                className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-40 text-white rounded-xl text-sm font-bold flex items-center justify-center transition-colors min-h-[40px]"
              >
                <Send className="w-4 h-4" />
              </button>
            </form>
          </div>
        </div>
      </section>

      {/* 5. INTERACTIVE ROI CALCULATOR */}
      <section id="calculator" className="py-20 bg-slate-900 text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            <div className="lg:col-span-6 space-y-6">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-500/20 text-brand-300 text-xs font-bold">
                <TrendingUp className="w-3.5 h-3.5" />
                Staff Time & Money Savings Calculator
              </div>
              <h2 className="text-3xl sm:text-4xl font-black tracking-tight text-white">
                See how much time and money WhatsApp SaaS saves your business
              </h2>
              <p className="text-slate-300 text-sm sm:text-base leading-relaxed">
                Repetitive questions like store timings, location, and price lists eat up 60-80% of your staff&apos;s working day. Automate them so your team focuses on high-ticket sales.
              </p>

              <div className="space-y-4 pt-2">
                <div className="flex justify-between items-center text-sm font-bold">
                  <span className="text-slate-300">Your Monthly WhatsApp Inquiries:</span>
                  <span className="text-xl font-black text-brand-400 font-mono">
                    {monthlyInquiries.toLocaleString()} messages
                  </span>
                </div>
                <input
                  type="range"
                  min="500"
                  max="30000"
                  step="500"
                  value={monthlyInquiries}
                  onChange={(e) => setMonthlyInquiries(Number(e.target.value))}
                  className="w-full h-3 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-brand-500"
                />
                <div className="flex justify-between text-[11px] text-slate-500 font-mono">
                  <span>500 msgs/mo</span>
                  <span>15,000 msgs/mo</span>
                  <span>30,000+ msgs/mo</span>
                </div>
              </div>
            </div>

            {/* Calculator Results Card */}
            <div className="lg:col-span-6">
              <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6">
                <h3 className="text-base font-bold text-slate-300 border-b border-slate-800 pb-3">
                  Estimated Monthly Business Savings
                </h3>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 space-y-1">
                    <p className="text-xs text-slate-400 font-medium">Automated Answers</p>
                    <p className="text-2xl font-black text-white font-mono">
                      {automatedInquiries.toLocaleString()}
                    </p>
                    <p className="text-[11px] text-emerald-400 font-semibold">78% answered instantly</p>
                  </div>

                  <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 space-y-1">
                    <p className="text-xs text-slate-400 font-medium">Staff Hours Saved</p>
                    <p className="text-2xl font-black text-brand-400 font-mono">
                      ~{hoursSaved} hrs/mo
                    </p>
                    <p className="text-[11px] text-brand-300 font-semibold">~{(hoursSaved / 4).toFixed(1)} hrs saved / week</p>
                  </div>
                </div>

                <div className="p-5 rounded-2xl bg-gradient-to-r from-emerald-950/80 to-teal-950/80 border border-emerald-500/30 flex items-center justify-between">
                  <div>
                    <p className="text-xs text-emerald-300 font-bold uppercase tracking-wider">Estimated Cost Savings</p>
                    <p className="text-3xl font-black text-white mt-0.5 font-mono">
                      ₹{rupeesSaved.toLocaleString()} <span className="text-sm font-normal text-emerald-200">/ month</span>
                    </p>
                  </div>
                  <Link
                    to="/register"
                    className="px-5 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs shadow-lg transition-all"
                  >
                    Enroll & Save Now
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 6. TRANSPARENT PRICING (₹499/mo) */}
      <section id="pricing" className="py-20 bg-slate-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto space-y-4 mb-16">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-50 text-brand-700 text-xs font-bold border border-brand-200">
              <Percent className="w-3.5 h-3.5 text-brand-600" />
              Affordable & Transparent
            </div>
            <h2 className="text-3xl sm:text-4xl font-black tracking-tight text-slate-900">
              Simple, affordable pricing for every business
            </h2>
            <p className="text-slate-600 text-sm sm:text-base">
              Start with a 14-day free trial. Upgrade anytime for just ₹499/month (~₹16/day) with zero hidden platform charges.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-3xl mx-auto items-stretch">
            {/* Free Trial */}
            <div className="bg-white p-8 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-6">
              <div className="space-y-4">
                <h3 className="text-lg font-bold text-slate-900">14-Day Free Trial</h3>
                <p className="text-xs text-slate-500">Test every feature with your live WhatsApp business number.</p>
                <div className="text-3xl font-black text-slate-900 font-mono">₹0</div>
                <ul className="space-y-3 text-xs text-slate-600 pt-4 border-t border-slate-100">
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>1 WhatsApp Business Number</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Keyword Auto-Replies</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Typo-Tolerant FAQ Knowledge Base</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>24-Hour Free Service Window Inbox</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>No Credit Card Required to Start</span>
                  </li>
                </ul>
              </div>
              <Link
                to="/register"
                className="w-full py-3.5 px-4 rounded-2xl border border-slate-300 hover:bg-slate-50 text-slate-800 font-bold text-sm text-center block transition-colors shadow-sm"
              >
                Start Free Trial
              </Link>
            </div>

            {/* All-in-One Business Plan (₹499/mo) */}
            <div className="bg-gradient-to-b from-slate-900 via-slate-900 to-slate-950 text-white p-8 rounded-3xl border-2 border-brand-500 shadow-2xl shadow-brand-900/30 flex flex-col justify-between space-y-6 relative">
              <div className="absolute -top-3.5 left-1/2 -translate-x-1/2 bg-brand-500 text-white text-[11px] font-black uppercase tracking-wider px-4 py-1 rounded-full shadow-md whitespace-nowrap">
                ⭐ Best Value for SMBs
              </div>

              <div className="space-y-4">
                <h3 className="text-lg font-bold text-white">All-in-One Business Plan</h3>
                <p className="text-xs text-slate-300">Complete 24/7 WhatsApp automation engine for your brand.</p>
                <div className="text-3xl font-black text-white font-mono">
                  ₹499 <span className="text-xs font-normal text-slate-400">/ month (~₹16 / day)</span>
                </div>
                <ul className="space-y-3 text-xs text-slate-200 pt-4 border-t border-slate-800">
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-brand-400 shrink-0" />
                    <span><strong>Unlimited</strong> Automated Bot Replies</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-brand-400 shrink-0" />
                    <span><strong>Unlimited</strong> Typo-Tolerant FAQs</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-brand-400 shrink-0" />
                    <span>Official Meta Cloud API (<strong>0% Ban Risk</strong>)</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-brand-400 shrink-0" />
                    <span>1-Click Bulk Broadcast Campaigns</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-brand-400 shrink-0" />
                    <span>Live 24h Service Window Tracker</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-brand-400 shrink-0" />
                    <span>Indian GST Tax Invoices & Input Credit</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="w-4 h-4 text-brand-400 shrink-0" />
                    <span>Zero Per-Message Middleman Surcharges</span>
                  </li>
                </ul>
              </div>
              <Link
                to="/register"
                className="w-full py-3.5 px-4 rounded-2xl bg-brand-500 hover:bg-brand-600 text-white font-black text-sm text-center block shadow-lg transition-all hover:scale-[1.02]"
              >
                Get Started for ₹499/mo
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* 7. FINAL CTA BANNER */}
      <section className="py-16 bg-gradient-to-r from-brand-700 via-brand-600 to-emerald-600 text-white">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-6">
          <h2 className="text-3xl sm:text-4xl font-black tracking-tight">
            Ready to start your journey to 100% WhatsApp automation?
          </h2>
          <p className="text-brand-100 max-w-xl mx-auto text-sm sm:text-base">
            Set up your automated business bot in 3 minutes. 14-day free trial, no credit card required.
          </p>
          <div className="pt-2">
            <Link
              to="/register"
              className="inline-flex items-center gap-2 px-8 py-4 rounded-2xl bg-white text-brand-700 hover:bg-brand-50 font-black text-base shadow-2xl transition-all hover:scale-105"
            >
              Start Free Trial in 60 Seconds
              <ArrowRight className="w-5 h-5" />
            </Link>
          </div>
        </div>
      </section>

      {/* 8. FOOTER */}
      <footer className="bg-slate-950 text-slate-400 py-12 border-t border-slate-900 text-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-brand-600 flex items-center justify-center text-white font-bold">
              <MessageSquare className="w-4 h-4" />
            </div>
            <span className="text-base font-black text-white">
              WhatsApp<span className="text-brand-500">SaaS</span>
            </span>
          </div>

          <p className="text-slate-500 text-center sm:text-left">
            © 2026 WhatsApp SaaS. Built with Official Meta Cloud API. 100% DPDP & Indian GST Compliant.
          </p>

          <div className="flex items-center gap-6 font-semibold">
            <Link to="/login" className="hover:text-white transition-colors">Sign In</Link>
            <Link to="/register" className="hover:text-white transition-colors">Create Account</Link>
          </div>
        </div>
      </footer>
    </div>
  );
};
export default LandingPage;
