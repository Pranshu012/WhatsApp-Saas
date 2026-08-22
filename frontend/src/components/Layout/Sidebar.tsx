import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  MessageCircle,
  Smartphone,
  Zap,
  HelpCircle,
  FileText,
  Calendar,
  Inbox,
  Settings,
  X,
  MessageSquare,
  Sparkles,
} from 'lucide-react';
import { cn } from '../../lib/utils';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
}

const navItems: NavItem[] = [
  { label: 'Home', href: '/', icon: LayoutDashboard },
  { label: '🚀 Quick Setup Guide', href: '/guide', icon: Sparkles },
  { label: 'Inbox', href: '/inbox', icon: MessageCircle },
  { label: 'Connect WhatsApp', href: '/whatsapp', icon: Smartphone },
  { label: 'Auto Replies', href: '/automation', icon: Zap },
  { label: 'Common Questions', href: '/faq', icon: HelpCircle },
  { label: 'Templates', href: '/templates', icon: FileText },
  { label: 'Scheduled Messages', href: '/scheduled', icon: Calendar },
  { label: 'Needs Your Reply', href: '/unmatched', icon: Inbox },
  { label: 'Settings', href: '/settings', icon: Settings },
];

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-gray-900/50 backdrop-blur-sm md:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      {/* Sidebar container */}
      <aside
        className={cn(
          'fixed top-0 bottom-0 left-0 z-50 w-64 bg-slate-900 text-slate-100 flex flex-col transition-transform duration-200 ease-in-out md:translate-x-0 md:static md:z-0',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {/* Header / Logo */}
        <div className="h-16 flex items-center justify-between px-5 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-brand-500 flex items-center justify-center text-white shadow-md shadow-brand-500/20">
              <MessageSquare className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold tracking-tight text-white block leading-none">WhatsApp SaaS</span>
              <span className="text-[10px] text-slate-400 font-medium">SMB Automation</span>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 md:hidden min-h-[44px] min-w-[44px] flex items-center justify-center"
            aria-label="Close sidebar"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation list */}
        <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.href}
                to={item.href}
                onClick={onClose}
                end={item.href === '/'}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors min-h-[44px]',
                    isActive
                      ? 'bg-brand-600 text-white shadow-sm'
                      : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                  )
                }
              >
                <Icon className="w-5 h-5 shrink-0" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* Bottom Help / Tech Provider Badge */}
        <div className="p-4 border-t border-slate-800 text-xs text-slate-400">
          <div className="font-medium text-slate-300 mb-0.5">Meta Tech Provider</div>
          <div className="text-[11px] leading-tight text-slate-500">Official Cloud API Gateway</div>
        </div>
      </aside>
    </>
  );
};
