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
}

export interface CsrfTokenResponse {
  token: string;
  headerName: string;
  parameterName: string;
}

export interface RegistrationRequest {
  businessName: string;
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
  ruleId?: string;
  matchType?: MatchType;
  matchValue?: string;
  caseSensitive?: boolean;
  message: string;
}

export interface TestRuleResponse {
  matched: boolean;
  matchedRuleId?: string;
  matchedRuleName?: string;
  actionType?: ActionType;
  actionPayload?: string;
}

export interface FaqResponse {
  id: string;
  question: string;
  answer: string;
  category?: string;
  active: boolean;
  createdAt: string;
}

export interface CreateFaqRequest {
  question: string;
  answer: string;
  category?: string;
  active?: boolean;
}

export interface TestFaqRequest {
  query: string;
}

export interface TestFaqResponse {
  matched: boolean;
  faqId?: string;
  question?: string;
  answer?: string;
  score?: number;
  confident?: boolean;
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
