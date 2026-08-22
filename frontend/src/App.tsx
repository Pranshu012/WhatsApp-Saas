import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './components/Layout/AppLayout';
import { LandingPage } from './features/landing/LandingPage';
import { LoginScreen } from './features/auth/LoginScreen';
import { RegisterScreen } from './features/auth/RegisterScreen';
import { ForgotPasswordScreen } from './features/auth/ForgotPasswordScreen';
import { ResetPasswordScreen } from './features/auth/ResetPasswordScreen';
import { WhatsAppConnectionScreen } from './features/whatsapp/WhatsAppConnectionScreen';
import { AutomationRulesScreen } from './features/automation/AutomationRulesScreen';
import { FaqScreen } from './features/faq/FaqScreen';
import { TemplatesScreen } from './features/templates/TemplatesScreen';
import { UnmatchedMessagesScreen } from './features/unmatched/UnmatchedMessagesScreen';
import { DashboardScreen } from './features/dashboard/DashboardScreen';
import { InboxScreen } from './features/inbox/InboxScreen';
import { ScheduledMessagesScreen } from './features/scheduled/ScheduledMessagesScreen';
import { SettingsScreen } from './features/settings/SettingsScreen';
import { GuideScreen } from './features/guide/GuideScreen';
import { AdminDashboardScreen } from './features/admin/AdminDashboardScreen';
import { MessageSquare } from 'lucide-react';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

const RootRoute: React.FC = () => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4">
        <div className="flex flex-col items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-brand-600 flex items-center justify-center text-white shadow-lg animate-bounce">
            <MessageSquare className="w-6 h-6" />
          </div>
          <div className="text-sm font-medium text-slate-500">Loading WhatsApp SaaS...</div>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LandingPage />;
  }

  return (
    <AppLayout>
      <DashboardScreen />
    </AppLayout>
  );
};

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Smart Root Route: Landing Page for Visitors, Dashboard for Logged-In Users */}
            <Route path="/" element={<RootRoute />} />

            {/* Public Auth Routes */}
            <Route path="/login" element={<LoginScreen />} />
            <Route path="/register" element={<RegisterScreen />} />
            <Route path="/forgot-password" element={<ForgotPasswordScreen />} />
            <Route path="/reset-password" element={<ResetPasswordScreen />} />

            {/* Authenticated Workspace App Shell */}
            <Route
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route path="/dashboard" element={<DashboardScreen />} />
              <Route path="/guide" element={<GuideScreen />} />
              <Route path="/inbox" element={<InboxScreen />} />
              <Route path="/whatsapp" element={<WhatsAppConnectionScreen />} />
              <Route path="/automation" element={<AutomationRulesScreen />} />
              <Route path="/faq" element={<FaqScreen />} />
              <Route path="/templates" element={<TemplatesScreen />} />
              <Route path="/scheduled" element={<ScheduledMessagesScreen />} />
              <Route path="/unmatched" element={<UnmatchedMessagesScreen />} />
              <Route path="/settings" element={<SettingsScreen />} />
              <Route path="/admin" element={<AdminDashboardScreen />} />
            </Route>

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};
export default App;
