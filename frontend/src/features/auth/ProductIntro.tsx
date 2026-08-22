import React from 'react';
import { CheckCircle2, MessageCircleMore, Smartphone, Sparkles } from 'lucide-react';

/** Explains the product before asking a new business owner to create an account. */
export const ProductIntro: React.FC = () => (
  <section className="w-full sm:max-w-md lg:w-[28rem] lg:max-w-none xl:w-[32rem] bg-slate-900 text-white p-7 sm:p-10 xl:p-12 flex flex-col justify-between rounded-3xl shadow-xl shadow-slate-900/15">
    <div>
      <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-brand-500/20 text-brand-100 text-xs font-semibold">
        <Sparkles className="w-4 h-4" />
        Made for small businesses
      </div>
      <h1 className="mt-5 text-2xl sm:text-3xl xl:text-4xl font-bold tracking-tight leading-tight">
        Reply to customers on WhatsApp, even when you are busy.
      </h1>
      <p className="mt-4 text-slate-300 leading-relaxed">
        Set up simple answers for common customer questions like price, location, timings, and orders. You stay in control of every reply.
      </p>
    </div>

    <div className="space-y-4 my-7 sm:my-10">
      <p className="text-sm font-semibold text-white">Get started in 3 simple steps</p>
      <Step icon={Smartphone} number="1" title="Connect WhatsApp" text="Connect your business number securely through Meta." />
      <Step icon={MessageCircleMore} number="2" title="Add common questions" text="Write the answers you already give customers every day." />
      <Step icon={CheckCircle2} number="3" title="Turn on instant replies" text="Customers get quick help while you focus on your work." />
    </div>

    <p className="text-xs leading-relaxed text-slate-400 border-t border-slate-700 pt-5">
      Your WhatsApp account and Meta message charges remain under your control. We never ask for your Meta password or payment details.
    </p>
  </section>
);

const Step: React.FC<{
  icon: React.ComponentType<{ className?: string }>;
  number: string;
  title: string;
  text: string;
}> = ({ icon: Icon, number, title, text }) => (
  <div className="flex gap-3">
    <div className="w-8 h-8 rounded-full bg-brand-500 text-white shrink-0 flex items-center justify-center text-sm font-bold">{number}</div>
    <div>
      <div className="flex items-center gap-2 text-sm font-semibold"><Icon className="w-4 h-4 text-brand-300" />{title}</div>
      <p className="mt-1 text-sm text-slate-300 leading-relaxed">{text}</p>
    </div>
  </div>
);
