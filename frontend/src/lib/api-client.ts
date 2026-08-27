/**
 * Client HTTP unique vers l'api-gateway. Toutes les requêtes de features/* passent
 * par ici — jamais de fetch() brut ailleurs dans le code.
 *
 * VITE_API_BASE_URL pointe vers l'api-gateway (ex: http://localhost:8080),
 * jamais directement vers un microservice — la gateway reste le seul point
 * d'entrée, cohérent avec le contrat backend (JWT vérifié uniquement là-bas).
 *
 * Intercepteur 401 : tente un refresh silencieux (rotation du refresh token)
 * puis rejoue la requête une seule fois. Si le refresh échoue, nettoie les
 * tokens — le guard d'auth (routes/_app/route.tsx) redirige vers /connexion.
 */

import {
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  setRefreshToken,
  clearTokens,
} from './auth-tokens';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

// --- Intercepteur refresh (dédipliqué : une seule requête refresh à la fois) ---

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;

  // Déduplication : requêtes concurrentes partagent le même refresh
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });

      if (!res.ok) return null;

      const envelope = await res.json();
      if (!envelope.success || !envelope.data) return null;

      const { accessToken: newAccess, refreshToken: newRefresh } = envelope.data;
      setAccessToken(newAccess);
      setRefreshToken(newRefresh); // rotation
      return newAccess;
    } catch {
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

// --- Enveloppe API backend (common/response/ApiResponse.java) ---

interface ApiResponseError {
  code: string;
  message: string;
  details: Record<string, unknown>;
}

interface ApiResponseMeta {
  timestamp: string;
  requestId: string;
}

interface ApiResponseEnvelope<T> {
  success: boolean;
  data: T | null;
  error: ApiResponseError | null;
  meta: ApiResponseMeta;
}

export class ApiError extends Error {
  status: number;
  code: string;
  requestId?: string;
  details: Record<string, unknown>;

  constructor(status: number, error: ApiResponseError, requestId?: string) {
    super(error.message);
    this.status = status;
    this.code = error.code;
    this.details = error.details;
    this.requestId = requestId;
  }
}

// --- Requête avec intercepteur 401 ---

type RequestOptions = Omit<RequestInit, 'body'> & { body?: unknown };

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, headers, ...rest } = options;
  const isFormData = body instanceof FormData;

  const doFetch = (token: string | null) =>
    fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers: {
        ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: isFormData ? (body as FormData) : body !== undefined ? JSON.stringify(body) : undefined,
    });

  let response = await doFetch(getAccessToken());

  if (response.status === 401) {
    const newToken = await refreshAccessToken();
    if (newToken) {
      response = await doFetch(newToken);
    } else {
      clearTokens();
    }
  }

  if (response.status === 204) return undefined as T;

  const envelope = (await response.json()) as ApiResponseEnvelope<T>;

  if (!envelope.success || envelope.error) {
    throw new ApiError(response.status, envelope.error!, envelope.meta?.requestId);
  }

  return envelope.data as T;
}

// --- Client exporté ---

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'POST', body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'PATCH', body }),
  delete: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: 'DELETE' }),
};
