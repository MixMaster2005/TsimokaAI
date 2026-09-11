import { useEffect, useRef } from 'react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { useQueryClient } from '@tanstack/react-query';
import { getAccessToken } from '@/lib/auth-tokens';
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
 *  - Refresh du token à chaque reconnexion (le token est lu à chaque tentative,
 *    pas capturé dans une closure figée)
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

    function invalidateCache() {
      queryClient.invalidateQueries({ queryKey: documentsKeys.bySpace(spaceId) });
    }

    async function connect() {
      // Lecture fraîche du token à chaque tentative de connexion/reconnexion
      const token = getAccessToken();
      if (!token) {
        console.warn(`[SSE] No access token for space ${spaceId}, will retry on next mount`);
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
              if (!response.ok) {
                throw new Error(`SSE connection failed: ${response.status}`);
              }
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
              // En cas d'erreur fatale (4xx non-429), la promise rejette.
              console.warn('[SSE] Error, will retry:', err);
            },

            onclose() {
              // Le serveur a fermé explicitement (timeout 30min ou restart).
              // fetch-event-source NE retry PAS sur close explicite —
              // on relance manuellement avec un token frais après un court délai.
              if (!controller.signal.aborted) {
                reconnectTimeout = setTimeout(() => {
                  if (!controller.signal.aborted) {
                    connect();
                  }
                }, 3000);
              }
            },
          },
        );
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') {
          return; // Nettoyage normal au démontage
        }
        console.error('[SSE] Connection failed:', err);
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
