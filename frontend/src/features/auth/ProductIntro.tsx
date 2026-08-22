import React from 'react';
import { MessageCircle, Sparkles } from 'lucide-react';

/** A compact product preview — not a second onboarding flow. */
export const ProductIntro: React.FC = () => (
  <section className="w-full lg:w-[29rem] xl:w-[33rem] rounded-[2rem] bg-gradient-to-br from-brand-600 via-brand-700 to-slate-900 p-7 sm:p-9 xl:p-11 text-white shadow-2xl shadow-brand-900/20 overflow-hidden relative">
    <div className="absolute -right-20 -top-20 w-56 h-56 rounded-full bg-white/10 blur-2xl" />
    <div className="relative">
      <div className="flex items-center gap-2 text-sm font-semibold text-brand-100">
        <span className="w-8 h-8 rounded-xl bg-white/15 flex items-center justify-center"><MessageCircle className="w-4 h-4" /></span>
        WhatsApp Business Helper
      </div>
      <h1 className="mt-7 text-3xl sm:text-4xl font-bold tracking-tight leading-[1.1]">Every customer gets a quick, helpful reply.</h1>
      <p className="mt-4 max-w-md text-brand-100 leading-relaxed">Answer common WhatsApp questions automatically, while you focus on running your business.</p>
      <div className="mt-8 rounded-2xl bg-white p-4 sm:p-5 text-slate-900 shadow-lg">
        <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
          <div className="w-8 h-8 rounded-full bg-emerald-500 flex items-center justify-center text-white"><MessageCircle className="w-4 h-4" /></div>
          <div><p className="text-sm font-semibold">Your business on WhatsApp</p><p className="text-xs text-emerald-600">● Ready to help customers</p></div>
        </div>
        <div className="mt-4 space-y-3 text-sm">
          <div className="max-w-[82%] rounded-2xl rounded-tl-md bg-slate-100 px-3.5 py-2.5 text-slate-600">What are your shop timings?</div>
          <div className="ml-auto max-w-[88%] rounded-2xl rounded-tr-md bg-emerald-500 px-3.5 py-2.5 text-white">We are open Monday to Saturday, 10 AM to 8 PM.</div>
        </div>
      </div>
      <p className="mt-5 flex items-center gap-2 text-xs text-brand-100"><Sparkles className="w-4 h-4" /> Simple setup. You choose every answer.</p>
    </div>
  </section>
);
