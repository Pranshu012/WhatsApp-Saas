export interface ApiErrorResponse {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  requestId?: string;
}

export interface UserSession {
  userId: string;
  tenantId: string;
  email: string;
  fullName: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
  isSuperAdmin?: boolean;
}

export interface CsrfTokenResponse {
  token: string;
  headerName: string;
  parameterName: string;
}

export interface RegistrationRequest {
  businessName: string;
  /** Internal workspace ID; generated from the business name and never shown in the form. */
  slug: string;
  fullName: string;
  email: string;
  password: string;
}

export interface RegistrationResponse {
  businessName: string;
  slug: string;
  ownerName: string;
  ownerEmail: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  tenantSlug?: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface WhatsAppAccountResponse {
  id: string;
  wabaId: string;
  phoneNumberId: string;
  displayPhoneNumber: string;
  verifiedName: string;
  qualityRating: string;
  messagingLimitTier: string;
  status: string;
  connectedAt?: string;
}

export type MatchType = 'EXACT' | 'CONTAINS' | 'STARTS_WITH' | 'REGEX';
export type ActionType = 'REPLY_TEXT' | 'HANDOVER' | 'SEND_TEMPLATE' | 'SEND_BUTTONS';

export interface AutomationRuleResponse {
  id: string;
  name: string;
  matchType: MatchType;
  matchValue: string;
  caseSensitive: boolean;
  actionType: ActionType;
  actionPayload: string;
  priority: number;
  enabled: boolean;
  createdAt: string;
}

export interface CreateAutomationRuleRequest {
  name: string;
  matchType: MatchType;
  matchValue: string;
  caseSensitive?: boolean;
  actionType: ActionType;
  actionPayload: string;
  priority?: number;
  enabled?: boolean;
}

export interface TestRuleRequest {
  messageText: string;
}

export interface TestRuleResponse {
  matched: boolean;
  ruleId?: string;
  ruleName?: string;
}

export interface FaqResponse {
  id: string;
  question: string;
  answer: string;
  enabled: boolean;
  createdAt: string;
}

export interface CreateFaqRequest {
  question: string;
  answer: string;
}

export interface TestFaqRequest {
  question: string;
}

export interface TestFaqResponse {
  id?: string;
  question?: string;
  answer?: string;
  confidenceScore: number;
  isConfident: boolean;
}

export interface WhatsAppTemplateResponse {
  id: string;
  metaTemplateId: string;
  name: string;
  language: string;
  category: 'MARKETING' | 'UTILITY' | 'AUTHENTICATION';
  metaCategory: 'MARKETING' | 'UTILITY' | 'AUTHENTICATION';
  status: 'APPROVED' | 'PENDING' | 'REJECTED' | 'PAUSED' | 'DISABLED';
  bodyText: string;
  categoryConflict: boolean;
  rejectionReason?: string;
  syncedAt?: string;
}

export interface CreateTemplateRequest {
  name: string;
  category: 'MARKETING' | 'UTILITY' | 'AUTHENTICATION';
  language: string;
  bodyText: string;
}

export interface UnmatchedMessageResponse {
  id: string;
  whatsappAccountId: string;
  contactId: string;
  senderPhone: string;
  messageText: string;
  wamid: string;
  receivedAt: string;
}

export interface DashboardStatsResponse {
  currentMonth: string;
  totalMessages: number;
  categoryCounts: {
    MARKETING: number;
    UTILITY: number;
    AUTHENTICATION: number;
    SERVICE: number;
    INBOUND_FREE: number;
  };
  deliveryOutcomes: {
    INTENT: number;
    SENT: number;
    DELIVERED: number;
    READ: number;
    FAILED: number;
  };
  deliveryRatePercent: number;
  note: string;
}

export interface ConversationSummaryDto {
  id: string;
  contactId: string;
  contactName?: string;
  phoneE164: string;
  lastInboundAt?: string;
  lastOutboundAt?: string;
  status: string;
  serviceWindowActive: boolean;
  serviceWindowExpiresAt?: string;
}

export interface MessageLedgerDto {
  id: string;
  whatsappAccountId: string;
  direction: 'INBOUND' | 'OUTBOUND';
  wamid: string;
  recipientPhoneHash: string;
  recipientPhoneLast4: string;
  billingCategory: 'MARKETING' | 'UTILITY' | 'AUTHENTICATION' | 'SERVICE' | 'INBOUND_FREE';
  templateName?: string;
  conversationWindow?: string;
  status: 'INTENT' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED';
  statusAt: string;
  createdAt: string;
  errorCode?: string;
  errorMessage?: string;
}

export interface ScheduledMessageResponse {
  id: string;
  contactId: string;
  templateId: string;
  whatsappAccountId: string;
  scheduledFor: string;
  timezone?: string;
  status: 'PENDING' | 'CLAIMED' | 'DISPATCHED' | 'CANCELLED' | 'FAILED';
  claimedAt?: string;
  createdAt: string;
}

export interface CreateScheduleRequest {
  contactId: string;
  templateId: string;
  whatsappAccountId: string;
  scheduledFor: string;
  timezone?: string;
}

export interface BusinessSettingsDto {
  id: string;
  businessName: string;
  slug: string;
  timezone: string;
  gstin?: string;
  legalName?: string;
  billingAddress?: string;
}

export interface UpdateBusinessSettingsRequest {
  businessName: string;
  timezone?: string;
  gstin?: string;
  legalName?: string;
  billingAddress?: string;
}

export interface SubscriptionDto {
  id: string;
  tenantId: string;
  planType: 'FREE_TRIAL' | 'BUSINESS_499' | 'CUSTOM';
  status: 'TRIALING' | 'ACTIVE' | 'PAST_DUE' | 'EXPIRED' | 'SUSPENDED' | 'CANCELLED';
  trialStartDate?: string;
  trialExpiresAt?: string;
  currentPeriodStart?: string;
  currentPeriodEnd?: string;
  monthlyPricePaise: number;
  currency: string;
  daysRemaining: number;
  isCurrentlyValid: boolean;
  notes?: string;
}

export interface AdminTenantDto {
  tenantId: string;
  businessName: string;
  slug: string;
  status: 'ACTIVE' | 'SUSPENDED';
  timezone: string;
  gstin?: string;
  legalName?: string;
  billingAddress?: string;
  createdAt: string;
  ownerId?: string;
  ownerName?: string;
  ownerEmail?: string;
  whatsAppConnected: boolean;
  displayPhoneNumber?: string;
  qualityRating?: string;
  messagingLimitTier?: string;
  subscriptionId?: string;
  planType: 'FREE_TRIAL' | 'BUSINESS_499' | 'CUSTOM';
  subscriptionStatus: 'TRIALING' | 'ACTIVE' | 'PAST_DUE' | 'EXPIRED' | 'SUSPENDED' | 'CANCELLED';
  trialStartDate?: string;
  trialExpiresAt?: string;
  currentPeriodStart?: string;
  currentPeriodEnd?: string;
  daysRemaining: number;
  isSubscriptionValid: boolean;
  monthlyPricePaise: number;
  notes?: string;
  totalMessagesThisMonth: number;
  totalFaqs: number;
  totalAutomationRules: number;
}

export interface AdminPlatformStatsDto {
  totalTenants: number;
  activeTenants: number;
  trialingTenants: number;
  suspendedTenants: number;
  totalUsers: number;
  totalMessagesThisMonth: number;
  totalActiveWhatsAppAccounts: number;
  estimatedMonthlyRevenueInr: number;
  planDistribution: Record<string, number>;
}

export interface ActivateTenantRequest {
  planType: string;
  durationDays: number;
  notes?: string;
}

export interface ExtendSubscriptionRequest {
  extraDays: number;
  notes?: string;
}

export interface SuspendTenantRequest {
  reason?: string;
}
