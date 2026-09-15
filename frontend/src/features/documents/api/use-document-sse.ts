import { useEffect, useRef } from 'react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { useQueryClient } from '@tanstack/react-query';
import { getAccessToken } from '@/lib/auth-tokens';
import { refreshAccessToken } from '@/lib/api-client';
import { documentKeys, documentsKeys } from './keys';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

interface DocumentStatusEvent {
  documentId: string;
  status: string;
  chunkCount: number;
  reason: string;
  timestamp: string;
}

/**
 * Registre global de connexions SSE — un seul socket par spaceId, partagé
 * entre tous les composants qui appellent useDocumentSse(spaceId).
 * Ref-counting : le socket reste ouvert tant qu'au moins 1 composant est
 * monté, et se ferme proprement quand le dernier se démonte.
 */
const activeConnections = new Map<string, { refCount: number; close: () => void }>();

/**
 * Hook SSE : ouvre une connexion Server-Sent Events vers l'ingestion-service
 * pour recevoir en temps réel les changements de statut des documents d'un espace.
 *
 * Singleton par spaceId — appelé depuis n'importe quel composant, il ne crée
 * qu'une seule connexion partagée (ref-counting).
 *
 * Gère :
 *  - Token garanti avant connexion : refresh silencieux si l'access token
 *    (mémoire, vidé au F5) est absent — sans quoi la connexion serait
 *    abandonnée et le statut des documents resterait figé jusqu'au refresh page
 *  - Refresh + reconnexion sur 401 à l'ouverture (token expiré entre-temps)
 *  - Retry avec backoff (3s → 10s → 30s) au lieu d'abandon définitif
 *  - Reconnexion automatique après fermeture explicite du serveur (timeout 30min)
 *  - Nettoyage automatique au démontage du dernier consommateur
 */
export function useDocumentSse(spaceId: string | null) {
  const queryClient = useQueryClient();
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (!spaceId) return;

    // --- Singleton par spaceId ---
    const existing = activeConnections.get(spaceId);
    if (existing) {
      existing.refCount++;
      return () => {
        existing.refCount--;
        if (existing.refCount <= 0) {
          existing.close();
          activeConnections.delete(spaceId);
        }
      };
    }

    // --- Nouvelle connexion ---
    const controller = new AbortController();
    abortRef.current = controller;
    let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
    let retryCount = 0;
    const RETRY_DELAYS = [3000, 10000, 30000];

    function scheduleReconnect() {
      if (controller.signal.aborted) return;
      const delay = RETRY_DELAYS[Math.min(retryCount, RETRY_DELAYS.length - 1)];
      retryCount++;
      reconnectTimeout = setTimeout(() => {
        if (!controller.signal.aborted) {
          connect();
        }
      }, delay);
    }

    /** Token garanti : lecture fraîche, sinon refresh silencieux (cas du F5). */
    async function ensureToken(): Promise<string | null> {
      const token = getAccessToken();
      if (token) return token;
      try {
        return await refreshAccessToken();
      } catch {
        return null;
      }
    }

    async function connect() {
      const token = await ensureToken();
      if (!token) {
        // Pas de session (ou refresh échoué) : on réessaie en backoff plutôt
        // que d'abandonner — la session peut arriver après (login différé).
        console.warn(`[SSE] No access token for space ${spaceId}, retry scheduled`);
        scheduleReconnect();
        return;
      }

      try {
        await fetchEventSource(
          `${API_BASE_URL}/api/v1/documents/stream?spaceId=${spaceId}`,
          {
            method: 'GET',
            headers: { Authorization: `Bearer ${token}` },
            signal: controller.signal,
            openWhenHidden: true,

            async onopen(response) {
              if (response.status === 401) {
                // Token expiré entre le montage et l'ouverture : refresh + retry.
                const fresh = await refreshAccessToken();
                if (fresh && !controller.signal.aborted) {
                  throw new Error('SSE 401, token refreshed — retry');
                }
                throw new Error(`SSE connection failed: ${response.status}`);
              }
              if (!response.ok) {
                throw new Error(`SSE connection failed: ${response.status}`);
              }
              // Connexion établie : reset du backoff.
              retryCount = 0;
            },

            onmessage(msg) {
              if (msg.event === 'document_status') {
                try {
                  const data = JSON.parse(msg.data) as DocumentStatusEvent;
                  queryClient.invalidateQueries({
                    queryKey: documentsKeys.bySpace(spaceId),
                  });
                  queryClient.invalidateQueries({
                    queryKey: documentKeys.byId(data.documentId),
                  });
                } catch {
                  // event malformé, on ignore
                }
              }
            },

            onerror(err) {
              if (err instanceof DOMException && err.name === 'AbortError') {
                return;
              }
              // fetch-event-source retry automatiquement avec un delay fixe.
              // En cas d'erreur fatale (4xx non-429 dont 401 post-refresh),
              // la promise rejette et on bascule sur le backoff manuel.
              console.warn('[SSE] Error, will retry:', err);
            },

            onclose() {
              // Le serveur a fermé explicitement (timeout 30min ou restart),
              // ou une erreur fatale a rejeté la promise : fetch-event-source
              // NE retry PAS sur close explicite — on relance manuellement
              // en backoff, avec un token frais.
              scheduleReconnect();
            },
          },
        );
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') {
          return; // Nettoyage normal au démontage
        }
        console.error('[SSE] Connection failed:', err);
        scheduleReconnect();
      }
    }

    // Enregistrer dans le registre global AVANT de connecter
    activeConnections.set(spaceId, {
      refCount: 1,
      close: () => {
        if (reconnectTimeout) clearTimeout(reconnectTimeout);
        controller.abort();
        activeConnections.delete(spaceId);
      },
    });

    connect();

    return () => {
      const entry = activeConnections.get(spaceId);
      if (entry) {
        entry.refCount--;
        if (entry.refCount <= 0) {
          entry.close();
          activeConnections.delete(spaceId);
        }
      }
      abortRef.current = null;
    };
  }, [spaceId, queryClient]);
}
