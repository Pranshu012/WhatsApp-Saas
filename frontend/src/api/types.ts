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
