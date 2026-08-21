import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './components/Layout/AppLayout';
import { LoginScreen } from './features/auth/LoginScreen';
import { RegisterScreen } from './features/auth/RegisterScreen';
import { ForgotPasswordScreen } from './features/auth/ForgotPasswordScreen';
import { ResetPasswordScreen } from './features/auth/ResetPasswordScreen';
import { FeaturePlaceholder } from './features/placeholders/FeaturePlaceholder';
import { WhatsAppConnectionScreen } from './features/whatsapp/WhatsAppConnectionScreen';
import { AutomationRulesScreen } from './features/automation/AutomationRulesScreen';
import { FaqScreen } from './features/faq/FaqScreen';
import { TemplatesScreen } from './features/templates/TemplatesScreen';
import { UnmatchedMessagesScreen } from './features/unmatched/UnmatchedMessagesScreen';
import { DashboardScreen } from './features/dashboard/DashboardScreen';
import { InboxScreen } from './features/inbox/InboxScreen';
import { ScheduledMessagesScreen } from './features/scheduled/ScheduledMessagesScreen';
import { Settings } from 'lucide-react';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
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
              {/* Dashboard */}
              <Route path="/" element={<DashboardScreen />} />

              {/* Inbox */}
              <Route path="/inbox" element={<InboxScreen />} />

              {/* WhatsApp Onboarding & Connection */}
              <Route
                path="/whatsapp"
                element={<WhatsAppConnectionScreen />}
              />

              {/* Keyword Automation Rules */}
              <Route path="/automation" element={<AutomationRulesScreen />} />

              {/* FAQ Matching Bot */}
              <Route path="/faq" element={<FaqScreen />} />

              {/* Templates */}
              <Route path="/templates" element={<TemplatesScreen />} />

              {/* Scheduled Messages */}
              <Route path="/scheduled" element={<ScheduledMessagesScreen />} />

              {/* Unmatched Messages */}
              <Route path="/unmatched" element={<UnmatchedMessagesScreen />} />

              {/* Settings */}
              <Route
                path="/settings"
                element={
                  <FeaturePlaceholder
                    title="Settings"
                    subtitle="Manage business details, GSTIN, and default timezones."
                    icon={Settings}
                    incrementBadge="F19"
                    emptyTitle="Business Profile"
                    emptyDescription="Configure your official business name, GST number, billing address, and default working hours."
                  />
                }
              />
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
