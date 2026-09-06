import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Loader2 } from 'lucide-react';

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: any) => void;
          renderButton: (parent: HTMLElement, options: any) => void;
          prompt: (notification?: any) => void;
        };
      };
    };
  }
}

interface GoogleSignInButtonProps {
  text?: string;
  businessName?: string;
  onSuccess?: () => void;
  onError?: (error: string) => void;
  className?: string;
}

export const GoogleSignInButton: React.FC<GoogleSignInButtonProps> = ({
  text = 'Continue with Google',
  businessName,
  onSuccess,
  onError,
  className = '',
}) => {
  const { loginWithGoogle } = useAuth();
  const [loading, setLoading] = useState(false);
  const [isGsiLoaded, setIsGsiLoaded] = useState(false);

  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

  // 1. Dynamically load Google Identity Services script if client ID is configured
  useEffect(() => {
    if (!googleClientId) return;

    if (window.google?.accounts?.id) {
      setIsGsiLoaded(true);
      return;
    }

    const scriptId = 'google-gsi-client';
    if (!document.getElementById(scriptId)) {
      const script = document.createElement('script');
      script.id = scriptId;
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => setIsGsiLoaded(true);
      document.body.appendChild(script);
    } else {
      setIsGsiLoaded(true);
    }
  }, [googleClientId]);

  // 2. Initialize Google client when ready
  useEffect(() => {
    if (!isGsiLoaded || !googleClientId || !window.google?.accounts?.id) return;

    try {
      window.google.accounts.id.initialize({
        client_id: googleClientId,
        callback: async (response: { credential: string }) => {
          if (!response.credential) {
            onError?.('Google did not return credentials.');
            return;
          }

          setLoading(true);
          try {
            await loginWithGoogle({
              credential: response.credential,
              businessName: businessName?.trim() || undefined,
            });
            onSuccess?.();
          } catch (err: any) {
            onError?.(err.message || 'Google authentication failed.');
          } finally {
            setLoading(false);
          }
        },
      });
    } catch (e) {
      console.warn('Google Identity initialization notice:', e);
    }
  }, [isGsiLoaded, googleClientId, businessName, loginWithGoogle, onSuccess, onError]);

  // 3. Click Handler
  const handleClick = async () => {
    if (loading) return;

    // If real Google Client ID is configured and GIS is available, trigger Google prompt
    if (googleClientId && window.google?.accounts?.id) {
      try {
        window.google.accounts.id.prompt();
        return;
      } catch (e) {
        console.warn('Google prompt fallback:', e);
      }
    }

    // Dev / Fallback Mock Mode: allows instant 1-click testing even without Google Cloud Console setup
    setLoading(true);
    try {
      // In dev mode, use a realistic sample email or allow instant onboarding
      const demoEmail = 'user.' + Math.random().toString(36).substring(2, 7) + '@business.com';
      await loginWithGoogle({
        credential: 'mock-google-token',
        businessName: businessName?.trim() || 'Demo Enterprise',
        email: demoEmail,
        name: 'Alex Rivera',
      });
      onSuccess?.();
    } catch (err: any) {
      onError?.(err.message || 'Failed to sign in with Google.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={loading}
      className={`w-full flex items-center justify-center gap-3 px-4 py-3 border border-slate-300 rounded-2xl shadow-sm bg-white text-sm font-semibold text-slate-700 hover:bg-slate-50 hover:border-slate-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-500 transition-all min-h-[46px] group ${
        loading ? 'opacity-80 cursor-wait' : ''
      } ${className}`}
    >
      {loading ? (
        <Loader2 className="w-5 h-5 text-brand-600 animate-spin" />
      ) : (
        <svg className="w-5 h-5 shrink-0" viewBox="0 0 24 24">
          <path
            fill="#4285F4"
            d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z"
          />
          <path
            fill="#34A853"
            d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.33 24 12 24z"
          />
          <path
            fill="#FBBC05"
            d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.16 0 9.97 0 12s.45 3.84 1.25 5.42l4.03-3.15z"
          />
          <path
            fill="#EA4335"
            d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"
          />
        </svg>
      )}
      <span className="text-slate-800 font-medium">
        {loading ? 'Connecting to Google...' : text}
      </span>
    </button>
  );
};
