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
import {
  LayoutDashboard,
  MessageCircle,
  Calendar,
  Settings,
} from 'lucide-react';

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
              <Route
                path="/"
                element={
                  <FeaturePlaceholder
                    title="Dashboard"
                    subtitle="Message volume, delivery rates, and ledger billing counts."
                    icon={LayoutDashboard}
                    incrementBadge="F20"
                    emptyTitle="No message activity yet"
                    emptyDescription="Connect your WhatsApp account to view real-time monthly message usage, delivery breakdowns, and cost estimates."
                    actionLabel="Connect WhatsApp"
                    onAction={() => window.location.assign('/whatsapp')}
                  />
                }
              />

              {/* Inbox */}
              <Route
                path="/inbox"
                element={
                  <FeaturePlaceholder
                    title="Inbox"
                    subtitle="Customer conversations with 24-hour service window tracking."
                    icon={MessageCircle}
                    incrementBadge="F20"
                    emptyTitle="Inbox is empty"
                    emptyDescription="Incoming customer WhatsApp messages will appear here for manual replies inside the 24-hour service window."
                  />
                }
              />

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
              <Route
                path="/scheduled"
                element={
                  <FeaturePlaceholder
                    title="Scheduled Messages"
                    subtitle="Plan and broadcast automated template messages at scheduled UTC times."
                    icon={Calendar}
                    incrementBadge="F20"
                    emptyTitle="No scheduled messages"
                    emptyDescription="Schedule order reminders, appointment updates, or announcements for delivery at a future date and time."
                    actionLabel="Schedule Message"
                  />
                }
              />

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
