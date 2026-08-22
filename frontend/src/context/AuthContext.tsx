import React, { createContext, useContext, useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import {
  LoginRequest,
  RegistrationRequest,
  RegistrationResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  UserSession,
} from '../api/types';

interface AuthContextType {
  user: UserSession | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (req: LoginRequest) => Promise<UserSession>;
  register: (req: RegistrationRequest) => Promise<RegistrationResponse>;
  forgotPassword: (req: ForgotPasswordRequest) => Promise<{ message: string }>;
  resetPassword: (req: ResetPasswordRequest) => Promise<{ message: string }>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSession | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const refreshUser = async () => {
    try {
      const userData = await apiClient<UserSession>('/api/auth/me');
      setUser(userData);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshUser();
  }, []);

  const login = async (req: LoginRequest): Promise<UserSession> => {
    const sessionData = await apiClient<UserSession>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(req),
    });
    setUser(sessionData);
    return sessionData;
  };

  const register = async (req: RegistrationRequest): Promise<RegistrationResponse> => {
    const response = await apiClient<RegistrationResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(req),
    });
    // Automatically establish user session right after signup
    try {
      const sessionData = await apiClient<UserSession>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          email: req.email,
          password: req.password,
        }),
      });
      setUser(sessionData);
    } catch {
      await refreshUser();
    }
    return response;
  };

  const forgotPassword = async (req: ForgotPasswordRequest): Promise<{ message: string }> => {
    return await apiClient<{ message: string }>('/api/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify(req),
    });
  };

  const resetPassword = async (req: ResetPasswordRequest): Promise<{ message: string }> => {
    return await apiClient<{ message: string }>('/api/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify(req),
    });
  };

  const logout = async (): Promise<void> => {
    try {
      await apiClient('/api/auth/logout', { method: 'POST' });
    } finally {
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAuthenticated: !!user,
        login,
        register,
        forgotPassword,
        resetPassword,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
