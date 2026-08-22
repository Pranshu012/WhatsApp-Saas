import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  MessageSquare,
  Zap,
  HelpCircle,
  Clock,
  Send,
  ShieldCheck,
  BarChart3,
  CheckCircle2,
  ArrowRight,
  Sparkles,
  Bot,
  TrendingUp,
  Percent,
  Check,
  Smartphone,
} from 'lucide-react';

interface ChatMessage {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  time: string;
}

export const LandingPage: React.FC = () => {
  // Simulator State
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: '1',
      sender: 'user',
      text: 'What are your store timings?',
      time: '10:42 AM',
    },
    {
      id: '2',
      sender: 'bot',
      text: 'We are open Monday to Saturday from 9:30 AM to 8:30 PM! Sunday closed. Can I help you with our product catalog?',
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
    pricing: 'Our Starter plan is ₹999/mo and Growth is ₹1,999/mo with unlimited auto-replies and Meta Cloud API integration. Click here to view details: https://wasaas.app/pricing',
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

    // Simulate AI / FAQ Semantic matching response
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

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 font-sans selection:bg-brand-500 selection:text-white">
      {/* 1. STICKY NAVBAR */}
      <header className="sticky top-0 z-50 bg-white/85 backdrop-blur-md border-b border-slate-200/80 transition-all">
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
            <a href="#features" className="hover:text-brand-600 transition-colors">Features</a>
            <a href="#demo" className="hover:text-brand-600 transition-colors">Interactive Demo</a>
            <a href="#calculator" className="hover:text-brand-600 transition-colors">ROI Calculator</a>
            <a href="#pricing" className="hover:text-brand-600 transition-colors">Pricing</a>
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
              className="inline-flex items-center gap-2 px-4.5 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-sm font-bold shadow-md shadow-brand-600/20 transition-all hover:shadow-lg"
            >
              Start Free Trial
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </header>

      {/* 2. HERO SECTION */}
      <section className="relative pt-12 pb-20 md:pt-20 md:pb-28 overflow-hidden bg-gradient-to-b from-emerald-50/40 via-white to-slate-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
            {/* Left Column: Copy & Value Proposition */}
            <div className="lg:col-span-7 space-y-6 text-center lg:text-left">
              <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-brand-50 border border-brand-200/80 text-brand-700 text-xs font-bold shadow-sm">
                <Sparkles className="w-3.5 h-3.5 text-brand-600" />
                Official Meta Cloud API • Zero Markup Fees • 100% DPDP & GST Ready
              </div>

              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black text-slate-900 tracking-tight leading-[1.1]">
                Turn WhatsApp into your{' '}
                <span className="bg-gradient-to-r from-brand-600 via-emerald-600 to-teal-600 bg-clip-text text-transparent">
                  24/7 automated sales
                </span>{' '}
                & support machine.
              </h1>

              <p className="text-base sm:text-lg text-slate-600 max-w-2xl mx-auto lg:mx-0 leading-relaxed">
                Instantly answer customer inquiries in under 1 second, match typo-filled questions with semantic AI, and send broadcast marketing campaigns — without hiring night shift agents.
              </p>

              <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4 pt-2">
                <Link
                  to="/register"
                  className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-7 py-3.5 rounded-2xl bg-brand-600 hover:bg-brand-700 text-white font-black text-base shadow-xl shadow-brand-600/30 hover:scale-[1.02] active:scale-[0.98] transition-all min-h-[48px]"
                >
                  Create Business Account Free
                  <ArrowRight className="w-5 h-5" />
                </Link>
                <a
                  href="#demo"
                  className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-2xl bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold text-sm shadow-sm transition-all min-h-[48px]"
                >
                  <Smartphone className="w-4 h-4 text-brand-600" />
                  Try Live Simulator Below
                </a>
              </div>

              {/* Trust Badges */}
              <div className="pt-4 flex flex-wrap items-center justify-center lg:justify-start gap-6 text-xs font-semibold text-slate-500">
                <div className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>14-Day Free Access</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>No Ban Risk (Meta Cloud API)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Percent className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>Zero Per-Message Markups</span>
                </div>
              </div>
            </div>

            {/* Right Column: Hero Visual Card */}
            <div className="lg:col-span-5 flex justify-center">
              <div className="w-full max-w-md bg-white rounded-3xl p-6 shadow-2xl shadow-slate-300/60 border border-slate-200/80 relative">
                {/* Floating Stat Badge */}
                <div className="absolute -top-4 -right-4 bg-slate-900 text-white p-3 rounded-2xl shadow-xl flex items-center gap-3 border border-slate-800 animate-bounce-short">
                  <div className="w-9 h-9 rounded-xl bg-brand-500/20 text-brand-400 flex items-center justify-center">
                    <TrendingUp className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Avg Response Time</p>
                    <p className="text-sm font-black text-white">&lt; 0.8 Seconds</p>
                  </div>
                </div>

                <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-emerald-500 flex items-center justify-center text-white font-bold">
                      <Bot className="w-5 h-5" />
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-slate-900">Radhe Sweets & Bakers</h4>
                      <p className="text-xs text-emerald-600 flex items-center gap-1">
                        <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                        AI Auto-Reply Active 24/7
                      </p>
                    </div>
                  </div>
                  <span className="text-[11px] font-bold px-2 py-0.5 rounded bg-brand-50 text-brand-700 border border-brand-200">
                    Official WABA
                  </span>
                </div>

                <div className="mt-5 space-y-3">
                  <div className="bg-slate-50 p-3.5 rounded-2xl rounded-tl-sm text-xs text-slate-700 leading-relaxed border border-slate-100">
                    <p className="font-semibold text-slate-900 mb-0.5">Customer:</p>
                    &ldquo;bhaiya rasgulla ka 1kg box kitne ka h or delivery kitni der me hogi?&rdquo;
                  </div>

                  <div className="bg-emerald-600 p-3.5 rounded-2xl rounded-tr-sm text-xs text-white leading-relaxed ml-6 shadow-md shadow-emerald-600/20">
                    <p className="font-semibold text-emerald-100 mb-0.5">Automated Bot (0.4s):</p>
                    &ldquo;Namaste! Gulab Jamun & Rasgulla 1kg box is ₹320. Fresh batch available! Orders are delivered in 45 mins within 5km. Reply &apos;ORDER&apos; to place your order right now! 🍰&rdquo;
                  </div>
                </div>

                <div className="mt-5 pt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500 font-medium">
                  <span>⚡ 100% Automated</span>
                  <span>🔒 End-to-End Encrypted</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 3. LIVE INTERACTIVE WHATSAPP SIMULATOR */}
      <section id="demo" className="py-20 bg-slate-900 text-white relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto space-y-4 mb-14">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-400 text-xs font-bold">
              <Bot className="w-3.5 h-3.5" />
              Interactive Product Demo
            </div>
            <h2 className="text-3xl sm:text-4xl font-black tracking-tight text-white">
              Try our WhatsApp Bot live right now
            </h2>
            <p className="text-slate-300 text-sm sm:text-base">
              Click a sample question below or type your own custom inquiry to experience how our PostgreSQL Full-Text & Trigram semantic engine responds instantly.
            </p>
          </div>

          <div className="max-w-xl mx-auto bg-slate-950 rounded-[2.5rem] p-4 sm:p-6 shadow-2xl border border-slate-800">
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
            <div className="my-4 h-72 overflow-y-auto space-y-3 p-2 bg-slate-900/70 rounded-2xl border border-slate-800/80">
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

      {/* 4. KEY FEATURES GRID */}
      <section id="features" className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto space-y-4 mb-16">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-50 text-brand-700 text-xs font-bold border border-brand-200">
              <Zap className="w-3.5 h-3.5 text-brand-600" />
              Simple Tools Built For Your Business
            </div>
            <h2 className="text-3xl sm:text-4xl font-black tracking-tight text-slate-900">
              Everything you need to grow on WhatsApp
            </h2>
            <p className="text-slate-600 text-sm sm:text-base">
              No more missed customer leads, slow manual replies, or unorganized chat history.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {/* Feature 1 */}
            <div className="p-7 rounded-3xl bg-slate-50 border border-slate-200/80 hover:shadow-xl hover:border-brand-500/40 transition-all space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-brand-600 text-white flex items-center justify-center shadow-md shadow-brand-500/25">
                <Zap className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900">Auto-Reply to Common Questions</h3>
              <p className="text-sm text-slate-600 leading-relaxed">
                Set instant replies for your price list, menu, store address, or catalog. Your customers get answers in seconds — 24 hours a day, even while you sleep.
              </p>
            </div>

            {/* Feature 2 */}
            <div className="p-7 rounded-3xl bg-slate-50 border border-slate-200/80 hover:shadow-xl hover:border-brand-500/40 transition-all space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-teal-600 text-white flex items-center justify-center shadow-md shadow-teal-500/25">
                <HelpCircle className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900">Smart AI Answers (Even with Typos)</h3>
              <p className="text-sm text-slate-600 leading-relaxed">
                Customers often make spelling mistakes or write in Hinglish like <em>&ldquo;timings kya hai&rdquo;</em>. Our smart AI understands their question and sends the right answer every time.
              </p>
            </div>

            {/* Feature 3 */}
            <div className="p-7 rounded-3xl bg-slate-50 border border-slate-200/80 hover:shadow-xl hover:border-brand-500/40 transition-all space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-amber-600 text-white flex items-center justify-center shadow-md shadow-amber-500/25">
                <Clock className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900">Free 24-Hour Reply Tracker</h3>
              <p className="text-sm text-slate-600 leading-relaxed">
                WhatsApp allows free replies within 24 hours of a customer message. Our live countdown timer ensures you reply on time and save extra messaging costs.
              </p>
            </div>

            {/* Feature 4 */}
            <div className="p-7 rounded-3xl bg-slate-50 border border-slate-200/80 hover:shadow-xl hover:border-brand-500/40 transition-all space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shadow-md shadow-indigo-500/25">
                <Send className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900">Send Bulk Offers & Updates in 1-Click</h3>
              <p className="text-sm text-slate-600 leading-relaxed">
                Send festival greetings, discount offers, and payment reminders to thousands of customers at once — safely and legally without risking number bans.
              </p>
            </div>

            {/* Feature 5 */}
            <div className="p-7 rounded-3xl bg-slate-50 border border-slate-200/80 hover:shadow-xl hover:border-brand-500/40 transition-all space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-emerald-600 text-white flex items-center justify-center shadow-md shadow-emerald-500/25">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900">Quick 2-Minute WhatsApp Setup</h3>
              <p className="text-sm text-slate-600 leading-relaxed">
                Connect your existing WhatsApp Business number directly in 2 clicks. No technical knowledge, complex coding, or server management required.
              </p>
            </div>

            {/* Feature 6 */}
            <div className="p-7 rounded-3xl bg-slate-50 border border-slate-200/80 hover:shadow-xl hover:border-brand-500/40 transition-all space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-purple-600 text-white flex items-center justify-center shadow-md shadow-purple-500/25">
                <BarChart3 className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900">Clear & Honest Billing (No Hidden Fees)</h3>
              <p className="text-sm text-slate-600 leading-relaxed">
                Track every message sent, delivered, and read in real time. You pay standard WhatsApp charges directly with zero middleman markups.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 5. INTERACTIVE ROI CALCULATOR */}
      <section id="calculator" className="py-20 bg-gradient-to-br from-slate-900 via-slate-950 to-brand-950 text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            <div className="lg:col-span-6 space-y-6">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-500/20 text-brand-300 text-xs font-bold">
                <TrendingUp className="w-3.5 h-3.5" />
                Staff Time & Money Savings Calculator
              </div>
              <h2 className="text-3xl sm:text-4xl font-black tracking-tight text-white">
                See how much time and money WhatsApp SaaS saves your team
              </h2>
              <p className="text-slate-300 text-sm sm:text-base leading-relaxed">
                Repetitive questions like store timings, location, and price lists eat up 60-80% of your staff&apos;s working day. Automate them so your staff focuses on high-ticket sales.
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
              <div className="bg-slate-900/90 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6">
                <h3 className="text-base font-bold text-slate-300 border-b border-slate-800 pb-3">
                  Estimated Monthly Business Savings
                </h3>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="p-4 rounded-2xl bg-slate-800/60 border border-slate-700/60 space-y-1">
                    <p className="text-xs text-slate-400 font-medium">Automated Answers</p>
                    <p className="text-2xl font-black text-white font-mono">
                      {automatedInquiries.toLocaleString()}
                    </p>
                    <p className="text-[11px] text-emerald-400 font-semibold">78% answered instantly</p>
                  </div>

                  <div className="p-4 rounded-2xl bg-slate-800/60 border border-slate-700/60 space-y-1">
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
                    Start Saving Now
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 6. TRANSPARENT PRICING */}
      <section id="pricing" className="py-20 bg-slate-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto space-y-4 mb-16">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-50 text-brand-700 text-xs font-bold border border-brand-200">
              <Percent className="w-3.5 h-3.5 text-brand-600" />
              Honest & Transparent
            </div>
            <h2 className="text-3xl sm:text-4xl font-black tracking-tight text-slate-900">
              Simple pricing. No hidden message markups.
            </h2>
            <p className="text-slate-600 text-sm sm:text-base">
              You connect your own Meta Cloud account. You pay exact official Meta rates without 2x-3x platform surcharges.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl mx-auto">
            {/* Free Trial */}
            <div className="bg-white p-8 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-6">
              <div className="space-y-4">
                <h3 className="text-lg font-bold text-slate-900">14-Day Free Trial</h3>
                <p className="text-xs text-slate-500">Test every feature with your live WhatsApp number.</p>
                <div className="text-3xl font-black text-slate-900 font-mono">₹0</div>
                <ul className="space-y-3 text-xs text-slate-600 pt-4 border-t border-slate-100">
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600" /> 1 WhatsApp Number
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600" /> Keyword Auto-Replies
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600" /> Full-Text FAQ Bot
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600" /> 24h Window Inbox
                  </li>
                </ul>
              </div>
              <Link
                to="/register"
                className="w-full py-3 px-4 rounded-xl border border-slate-300 hover:bg-slate-50 text-slate-800 font-bold text-sm text-center block transition-colors"
              >
                Start Free Trial
              </Link>
            </div>

            {/* Growth Plan (Popular) */}
            <div className="bg-gradient-to-b from-slate-900 to-slate-950 text-white p-8 rounded-3xl border-2 border-brand-500 shadow-2xl shadow-brand-900/30 flex flex-col justify-between space-y-6 relative">
              <div className="absolute -top-3.5 left-1/2 -translate-x-1/2 bg-brand-500 text-white text-[11px] font-black uppercase tracking-wider px-3.5 py-1 rounded-full shadow-md">
                Most Popular for SMBs
              </div>

              <div className="space-y-4">
                <h3 className="text-lg font-bold text-white">Growth Plan</h3>
                <p className="text-xs text-slate-400">Complete automation engine for growing brands.</p>
                <div className="text-3xl font-black text-white font-mono">
                  ₹1,999 <span className="text-xs font-normal text-slate-400">/ month</span>
                </div>
                <ul className="space-y-3 text-xs text-slate-300 pt-4 border-t border-slate-800">
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-brand-400" /> Unlimited Auto-Replies
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-brand-400" /> Unlimited Semantic FAQs
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-brand-400" /> Scheduled Broadcasts
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-brand-400" /> 24h Countdown Guard
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-brand-400" /> Indian GST Tax Invoicing
                  </li>
                </ul>
              </div>
              <Link
                to="/register"
                className="w-full py-3 px-4 rounded-xl bg-brand-500 hover:bg-brand-600 text-white font-black text-sm text-center block shadow-lg transition-all"
              >
                Get Started with Growth
              </Link>
            </div>

            {/* Pro / Agency */}
            <div className="bg-white p-8 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-6">
              <div className="space-y-4">
                <h3 className="text-lg font-bold text-slate-900">Pro / Multi-Branch</h3>
                <p className="text-xs text-slate-500">For high-volume retail chains & franchises.</p>
                <div className="text-3xl font-black text-slate-900 font-mono">
                  ₹4,999 <span className="text-xs font-normal text-slate-500">/ month</span>
                </div>
                <ul className="space-y-3 text-xs text-slate-600 pt-4 border-t border-slate-100">
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600" /> Up to 5 WhatsApp Numbers
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600" /> High-Throughput Queue
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600" /> Dedicated Account Manager
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600" /> 99.9% Uptime SLA
                  </li>
                </ul>
              </div>
              <Link
                to="/register"
                className="w-full py-3 px-4 rounded-xl border border-slate-300 hover:bg-slate-50 text-slate-800 font-bold text-sm text-center block transition-colors"
              >
                Contact for Pro
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* 7. FINAL CTA BANNER */}
      <section className="py-16 bg-gradient-to-r from-brand-700 via-brand-600 to-emerald-600 text-white">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-6">
          <h2 className="text-3xl sm:text-4xl font-black tracking-tight">
            Ready to never miss a customer on WhatsApp again?
          </h2>
          <p className="text-brand-100 max-w-xl mx-auto text-sm sm:text-base">
            Set up your automated business bot in 3 minutes. Free 14-day trial, no credit card required.
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
