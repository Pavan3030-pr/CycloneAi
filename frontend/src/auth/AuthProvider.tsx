import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { login as loginRequest, setAccessToken } from '@/api/client';
import type { Role } from '@/api/types';

/**
 * Session state for the console.
 *
 * The token lives in the API client (which attaches it to every request) and the principal lives
 * here, so a component asking "can this user edit?" never has to decode a JWT. Roles come from the
 * token response the server already computed, which keeps the client from being the authority on
 * anything.
 */

interface Principal {
  username: string;
  roles: Role[];
  expiresAt: string;
}

interface AuthContextValue {
  principal: Principal | null;
  isAuthenticated: boolean;
  canWriteAssets: boolean;
  login: (username: string, password: string) => Promise<Principal>;
  demoLogin: () => Promise<Principal>;
  logout: () => void;
}

const PRINCIPAL_STORAGE_KEY = 'cyclone.principal';

const DEMO_USERNAME = import.meta.env.VITE_DEMO_USERNAME ?? 'analyst';
const DEMO_PASSWORD = import.meta.env.VITE_DEMO_PASSWORD ?? 'cyclone-demo-analyst';

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredPrincipal(): Principal | null {
  try {
    const raw = window.localStorage.getItem(PRINCIPAL_STORAGE_KEY);
    return raw === null ? null : (JSON.parse(raw) as Principal);
  } catch {
    return null;
  }
}

function storePrincipal(principal: Principal | null): void {
  try {
    if (principal === null) {
      window.localStorage.removeItem(PRINCIPAL_STORAGE_KEY);
    } else {
      window.localStorage.setItem(PRINCIPAL_STORAGE_KEY, JSON.stringify(principal));
    }
  } catch {
    // Storage being unavailable only costs persistence across reloads.
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [principal, setPrincipal] = useState<Principal | null>(() => readStoredPrincipal());

  const authenticate = useCallback(async (username: string, password: string): Promise<Principal> => {
    const token = await loginRequest(username, password);
    setAccessToken(token.accessToken);
    const nextPrincipal: Principal = {
      username,
      roles: token.roles,
      expiresAt: token.expiresAt,
    };
    setPrincipal(nextPrincipal);
    storePrincipal(nextPrincipal);
    return nextPrincipal;
  }, []);

  const logout = useCallback(() => {
    setAccessToken(null);
    setPrincipal(null);
    storePrincipal(null);
  }, []);

  const value = useMemo<AuthContextValue>(() => {
    const roles = principal?.roles ?? [];
    return {
      principal,
      isAuthenticated: principal !== null,
      canWriteAssets: roles.includes('ADMIN') || roles.includes('ANALYST'),
      login: authenticate,
      demoLogin: () => authenticate(DEMO_USERNAME, DEMO_PASSWORD),
      logout,
    };
  }, [authenticate, logout, principal]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (context === null) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
