/**
 * Client HTTP unique vers l'api-gateway. Toutes les requêtes de features/*\/api
 * passent par ici — jamais de fetch() brut ailleurs dans le code, pour garder
 * un seul endroit qui gère : l'URL de base, le token, les erreurs, le refresh.
 *
 * VITE_API_BASE_URL pointe vers l'api-gateway (ex: http://localhost:8080),
 * jamais directement vers un microservice — la gateway reste le seul point
 * d'entrée, cohérent avec le contrat backend (JWT vérifié uniquement là-bas).
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

// --- Stockage du token en mémoire (pas de localStorage : évite l'exposition
// du token JWT aux attaques XSS). Se vide au rechargement de page — c'est le
// refresh token (géré par AuthService côté back) qui permet de récupérer
// un nouvel access token silencieusement au démarrage. Voir features/auth/api/.
let accessToken: string | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function getAccessToken() {
  return accessToken;
}

/**
 * Enveloppe RÉELLE renvoyée par tous les services (vérifiée dans
 * common/response/ApiResponse.java — contrat non négociable côté back) :
 *   succès : { success: true,  data: T,        error: null, meta }
 *   erreur : { success: false, data: null,     error: {...}, meta }
 * Chaque réponse porte un requestId traçable — utile à logger côté front
 * en cas de bug pour le retrouver dans les logs backend.
 */
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

type RequestOptions = Omit<RequestInit, 'body'> & { body?: unknown };

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, headers, ...rest } = options;
  const isFormData = body instanceof FormData;

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: {
      // FormData (upload de fichier) : on laisse le navigateur poser le
      // Content-Type avec sa boundary — jamais 'application/json' dans ce cas.
      ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...headers,
    },
    body: isFormData ? (body as FormData) : body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401) {
    // TODO : brancher ici la logique de refresh silencieux
    // (POST /api/v1/auth/refresh via features/auth/api/use-login.ts),
    // puis rejouer la requête une fois. Si le refresh échoue aussi,
    // rediriger vers /connexion (voir routes/_app/route.tsx pour le guard).
  }

  // 204 No Content n'a pas de corps à parser (ex: DELETE réussi)
  if (response.status === 204) return undefined as T;

  const envelope = (await response.json()) as ApiResponseEnvelope<T>;

  if (!envelope.success || envelope.error) {
    throw new ApiError(response.status, envelope.error!, envelope.meta?.requestId);
  }

  return envelope.data as T;
}

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
