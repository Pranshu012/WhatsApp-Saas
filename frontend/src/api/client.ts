import { ApiErrorResponse, CsrfTokenResponse } from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export class ApiError extends Error {
  status: number;
  error: string;
  requestId?: string;

  constructor(status: number, error: string, message: string, requestId?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.error = error;
    this.requestId = requestId;
  }
}

let cachedCsrfToken: string | null = null;

function getCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(^|;\\s*)(' + name + ')=([^;]*)'));
  return match ? decodeURIComponent(match[3]) : null;
}

export async function fetchCsrfToken(): Promise<string> {
  const cookieToken = getCookie('XSRF-TOKEN');
  if (cookieToken) {
    cachedCsrfToken = cookieToken;
    return cookieToken;
  }

  try {
    const res = await fetch(`${BASE_URL}/api/auth/csrf`, {
      credentials: 'include',
    });
    if (res.ok) {
      const data: CsrfTokenResponse = await res.json();
      if (data.token) {
        cachedCsrfToken = data.token;
        return data.token;
      }
    }
  } catch (err) {
    console.warn('Failed to fetch CSRF token from endpoint', err);
  }

  return cachedCsrfToken || '';
}

export async function apiClient<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const method = (options.method || 'GET').toUpperCase();
  const isMutating = ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method);

  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (options.body && typeof options.body === 'string' && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  // Attach CSRF token on mutating requests
  if (isMutating) {
    const csrfToken = getCookie('XSRF-TOKEN') || cachedCsrfToken || (await fetchCsrfToken());
    if (csrfToken) {
      headers['X-XSRF-TOKEN'] = csrfToken;
    }
  }

  const url = endpoint.startsWith('http') ? endpoint : `${BASE_URL}${endpoint}`;

  const response = await fetch(url, {
    ...options,
    headers,
    credentials: 'include',
  });

  if (response.status === 204) {
    return {} as T;
  }

  const contentType = response.headers.get('content-type') || '';
  let data: any;

  if (contentType.includes('application/json')) {
    data = await response.json().catch(() => ({}));
  } else {
    data = await response.text().catch(() => '');
  }

  if (!response.ok) {
    const errorBody = data as Partial<ApiErrorResponse>;
    const userMessage = mapErrorToUserMessage(response.status, errorBody?.message || errorBody?.error);
    throw new ApiError(
      response.status,
      errorBody?.error || response.statusText,
      userMessage,
      errorBody?.requestId
    );
  }

  return data as T;
}

function mapErrorToUserMessage(status: number, serverMsg?: string): string {
  if (serverMsg && serverMsg.length > 0 && !serverMsg.startsWith('Validation failed')) {
    return serverMsg;
  }

  switch (status) {
    case 400:
      return 'Please check your inputs and try again.';
    case 401:
      return 'Invalid email or password. Please try again.';
    case 403:
      return 'You do not have permission to perform this action.';
    case 404:
      return 'The requested resource was not found.';
    case 409:
      return serverMsg || 'A conflicting record already exists.';
    case 429:
      return 'Too many attempts. Please slow down and wait a minute.';
    case 500:
    case 502:
    case 503:
      return 'Service is temporarily unavailable. Please try again shortly.';
    default:
      return serverMsg || 'An unexpected error occurred. Please try again.';
  }
}
