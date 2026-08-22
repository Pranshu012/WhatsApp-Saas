import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { MessageSquare, ArrowRight, Lock, Mail, Building, User, Loader2 } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { ProductIntro } from './ProductIntro';

export const RegisterScreen: React.FC = () => {
  const [businessName, setBusinessName] = useState('');
  const [slug, setSlug] = useState('');
  const [slugManuallyEdited, setSlugManuallyEdited] = useState(false);
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleBusinessNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setBusinessName(val);
    if (!slugManuallyEdited) {
      const generatedSlug = val
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
      setSlug(generatedSlug);
    }
  };

  const handleSlugChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSlugManuallyEdited(true);
    setSlug(
      e.target.value
        .toLowerCase()
        .replace(/[^a-z0-9-]+/g, '')
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password.length < 12) {
      setError('Password must be at least 12 characters long for security.');
      return;
    }

    if (!slug || slug.trim().length === 0) {
      setError('Please provide a valid workspace slug.');
      return;
    }

    setLoading(true);

    try {
      await register({
        businessName: businessName.trim(),
        slug: slug.trim(),
        fullName: fullName.trim(),
        email: email.trim(),
        password,
      });
      navigate('/', { replace: true });
    } catch (err: any) {
      setError(err.message || 'Registration failed. Please check your details.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center py-8 px-4 sm:px-6 lg:px-8">
      <div className="w-full max-w-6xl flex flex-col lg:flex-row items-center justify-center gap-6 lg:gap-14">
        <ProductIntro />
        <div className="w-full sm:max-w-[27rem]">
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-xl bg-brand-600 flex items-center justify-center text-white shadow-lg shadow-brand-500/20">
            <MessageSquare className="w-6 h-6" />
          </div>
          <span className="font-bold text-slate-900">WhatsApp SaaS</span>
        </div>
        <h2 className="mt-8 text-3xl font-bold tracking-tight text-slate-900">
          Start automating customer replies
        </h2>
        <p className="mt-2 text-sm text-slate-600">
          Create your account, then connect WhatsApp and add the answers customers ask for most.
        </p>
      </div>

      <div className="mt-7 w-full">
        <div className="bg-white p-6 sm:p-8 shadow-xl shadow-slate-200/60 border border-slate-200 rounded-2xl">
          {error && (
            <AlertBanner
              type="error"
              message={error}
              onClose={() => setError(null)}
              className="mb-6"
            />
          )}

          <form className="space-y-4" onSubmit={handleSubmit}>
            {/* Business Name */}
            <div>
              <label htmlFor="businessName" className="block text-sm font-medium text-gray-700 mb-1">
                Business Name
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  <Building className="w-5 h-5" />
                </div>
                <input
                  id="businessName"
                  name="businessName"
                  type="text"
                  required
                  value={businessName}
                  onChange={handleBusinessNameChange}
                  className="block w-full pl-10 pr-3 py-2.5 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 text-sm min-h-[44px]"
                  placeholder="e.g. Patel Electronics"
                />
              </div>
            </div>

            {/* Workspace Slug */}
            <div>
              <label htmlFor="slug" className="block text-sm font-medium text-gray-700 mb-1">
                Business code
              </label>
              <div className="flex rounded-lg shadow-sm border border-gray-300 overflow-hidden focus-within:ring-2 focus-within:ring-brand-500 focus-within:border-brand-500">
                <span className="inline-flex items-center px-3 text-gray-400 bg-gray-50 text-xs border-r border-gray-200">
                  your-business/
                </span>
                <input
                  id="slug"
                  name="slug"
                  type="text"
                  required
                  value={slug}
                  onChange={handleSlugChange}
                  className="block w-full px-3 py-2 text-gray-900 placeholder-gray-400 focus:outline-none text-sm min-h-[44px]"
                  placeholder="patel-electronics"
                />
              </div>
            </div>

            {/* Owner Full Name */}
            <div>
              <label htmlFor="fullName" className="block text-sm font-medium text-gray-700 mb-1">
                Your Full Name
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  <User className="w-5 h-5" />
                </div>
                <input
                  id="fullName"
                  name="fullName"
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="block w-full pl-10 pr-3 py-2.5 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 text-sm min-h-[44px]"
                  placeholder="Ramesh Patel"
                />
              </div>
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                Work Email Address
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  <Mail className="w-5 h-5" />
                </div>
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="block w-full pl-10 pr-3 py-2.5 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 text-sm min-h-[44px]"
                  placeholder="ramesh@patelelectronics.com"
                />
              </div>
            </div>

            {/* Password */}
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
                Password <span className="text-xs text-gray-500 font-normal">(min. 12 characters)</span>
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  <Lock className="w-5 h-5" />
                </div>
                <input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="new-password"
                  required
                  minLength={12}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="block w-full pl-10 pr-3 py-2.5 border border-gray-300 rounded-lg text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 text-sm min-h-[44px]"
                  placeholder="••••••••••••"
                />
              </div>
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full flex justify-center items-center gap-2 py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-500 disabled:opacity-50 min-h-[44px]"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Creating workspace...
                  </>
                ) : (
                  <>
                    Create My Business Account
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>
          </form>

          <div className="mt-6 border-t border-gray-200 pt-4 text-center">
            <p className="text-xs text-gray-600">
              Already have an account?{' '}
              <Link to="/login" className="font-semibold text-brand-600 hover:text-brand-500">
                Sign in here
              </Link>
            </p>
          </div>
        </div>
        </div>
      </div>
    </div>
  );
};
