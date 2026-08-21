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
                element={
                  <FeaturePlaceholder
                    title="WhatsApp Business Account"
                    subtitle="Connect your Meta WhatsApp Business Account via Embedded Signup."
                    icon={Smartphone}
                    incrementBadge="F18"
                    emptyTitle="WhatsApp Not Connected"
                    emptyDescription="Connect your official WhatsApp number via Meta Embedded Signup popup to start automated and scheduled messaging."
                    actionLabel="Connect with Facebook"
                  />
                }
              />

              {/* Keyword Automation Rules */}
              <Route
                path="/automation"
                element={
                  <FeaturePlaceholder
                    title="Keyword Automation Rules"
                    subtitle="Configure instant automated replies based on customer messages."
                    icon={Zap}
                    incrementBadge="F19"
                    emptyTitle="No automation rules configured"
                    emptyDescription="Add rules like EXACT matching for 'PRICING' or 'MENU' to send instant automated replies 24/7."
                    actionLabel="Add First Rule"
                  />
                }
              />

              {/* FAQ Matching Bot */}
              <Route
                path="/faq"
                element={
                  <FeaturePlaceholder
                    title="FAQ Knowledge Base"
                    subtitle="Automated answers with PostgreSQL Full-Text & Trigram typo matching."
                    icon={HelpCircle}
                    incrementBadge="F19"
                    emptyTitle="No FAQ questions added"
                    emptyDescription="Add common customer questions like refund policy or store hours. The engine automatically matches typos and variations."
                    actionLabel="Add New FAQ"
                  />
                }
              />

              {/* Templates */}
              <Route
                path="/templates"
                element={
                  <FeaturePlaceholder
                    title="WhatsApp Templates"
                    subtitle="Submit and synchronize approved Meta message templates for outbound messaging."
                    icon={FileText}
                    incrementBadge="F19"
                    emptyTitle="No templates synced"
                    emptyDescription="Create or sync pre-approved message templates required for messaging customers outside the 24-hour window."
                    actionLabel="Sync Meta Templates"
                  />
                }
              />

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
              <Route
                path="/unmatched"
                element={
                  <FeaturePlaceholder
                    title="Unmatched Inquiries"
                    subtitle="Customer messages that did not trigger any rule or confident FAQ."
                    icon={Inbox}
                    incrementBadge="F19"
                    emptyTitle="All messages successfully resolved"
                    emptyDescription="Unanswered customer inquiries will be logged here so you can easily turn them into new FAQs with one click."
                  />
                }
              />

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
