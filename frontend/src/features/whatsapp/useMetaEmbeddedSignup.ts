import { useEffect, useRef, useState } from 'react';
import { apiClient } from '../../api/client';
import { WhatsAppAccountResponse } from '../../api/types';

declare global {
  interface Window {
    FB?: any;
    fbAsyncInit?: () => void;
  }
}

interface ConnectPayload {
  code: string;
  wabaId: string;
  phoneNumberId: string;
}

interface UseMetaEmbeddedSignupProps {
  onSuccess: (account: WhatsAppAccountResponse) => void;
  onError: (error: string) => void;
}

export function useMetaEmbeddedSignup({ onSuccess, onError }: UseMetaEmbeddedSignupProps) {
  const [isConnecting, setIsConnecting] = useState(false);
  const [isSdkLoaded, setIsSdkLoaded] = useState(false);
  const sessionInfoRef = useRef<{ wabaId?: string; phoneNumberId?: string }>({});

  // 1. Initialize Meta JS SDK
  useEffect(() => {
    const appId = import.meta.env.VITE_META_APP_ID || '123456789012345';
    const version = import.meta.env.VITE_META_GRAPH_VERSION || 'v21.0';

    if (window.FB) {
      setIsSdkLoaded(true);
      return;
    }

    window.fbAsyncInit = function () {
      window.FB.init({
        appId,
        cookie: true,
        xfbml: true,
        version,
      });
      setIsSdkLoaded(true);
    };

    if (!document.getElementById('facebook-jssdk')) {
      const script = document.createElement('script');
      script.id = 'facebook-jssdk';
      script.src = 'https://connect.facebook.net/en_US/sdk.js';
      script.async = true;
      script.defer = true;
      document.body.appendChild(script);
    }
  }, []);

  // 2. Listen for window message from Embedded Signup session
  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      // Validate origin if coming from facebook
      if (
        event.origin.includes('facebook.com') ||
        event.origin.includes('facebook.net') ||
        event.origin.includes('whatsapp.com')
      ) {
        try {
          const data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
          if (data && data.type === 'WA_EMBEDDED_SIGNUP') {
            if (data.event === 'FINISH' || data.event === 'SESSION_INFO') {
              const { waba_id, phone_number_id } = data.data || {};
              if (waba_id) sessionInfoRef.current.wabaId = waba_id;
              if (phone_number_id) sessionInfoRef.current.phoneNumberId = phone_number_id;
            } else if (data.event === 'CANCEL') {
              setIsConnecting(false);
            }
          }
        } catch {
          // Non-JSON message from other extensions or iframes, ignore
        }
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, []);

  // 3. Launch Embedded Signup Popup
  const launchSignup = async () => {
    const configId = import.meta.env.VITE_META_CONFIG_ID;
    sessionInfoRef.current = {};
    setIsConnecting(true);

    if (!window.FB) {
      setIsConnecting(false);
      onError('Meta Facebook SDK is not yet loaded. Please refresh and try again.');
      return;
    }

    try {
      window.FB.login(
        async (response: any) => {
          // Check if popup was abandoned / closed without approval
          if (!response || !response.authResponse || !response.authResponse.code) {
            // User closed the popup or cancelled: Return to ready state gracefully
            setIsConnecting(false);
            return;
          }

          const code = response.authResponse.code;
          const wabaId = sessionInfoRef.current.wabaId || '';
          const phoneNumberId = sessionInfoRef.current.phoneNumberId || '';

          try {
            const payload: ConnectPayload = {
              code,
              wabaId,
              phoneNumberId,
            };

            const connectedAccount = await apiClient<WhatsAppAccountResponse>('/api/whatsapp/connect', {
              method: 'POST',
              body: JSON.stringify(payload),
            });

            setIsConnecting(false);
            onSuccess(connectedAccount);
          } catch (err: any) {
            setIsConnecting(false);
            onError(err.message || 'Failed to complete WhatsApp account connection.');
          }
        },
        {
          config_id: configId,
          response_type: 'code',
          override_default_response_type: true,
          extras: {
            setup: {},
            featureType: '',
            sessionInfoVersion: '3',
          },
        }
      );
    } catch (err: any) {
      setIsConnecting(false);
      onError(err.message || 'Failed to open Meta Embedded Signup popup.');
    }
  };

  const cancelSignup = () => {
    setIsConnecting(false);
  };

  return {
    isConnecting,
    isSdkLoaded,
    launchSignup,
    cancelSignup,
  };
}
