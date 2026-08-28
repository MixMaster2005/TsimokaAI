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
 * Hook SSE : ouvre une connexion Server-Sent Events vers l'ingestion-service
 * pour recevoir en temps réel les changements de statut des documents d'un espace.
 *
 * À chaque event reçu, on invalide le query TanStack pour forcer un refetch
 * de la liste des documents (source de vérité = le REST, pas le SSE).
 */
export function useDocumentSse(spaceId: string | null) {
  const queryClient = useQueryClient();
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (!spaceId) return;

    const token = getAccessToken();
    if (!token) return;

    const controller = new AbortController();
    abortRef.current = controller;

    async function connect() {
      try {
        await fetchEventSource(
          `${API_BASE_URL}/api/v1/documents/stream?spaceId=${spaceId}`,
          {
            method: 'GET',
            headers: {
              Authorization: `Bearer ${token}`,
            },
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
                  // Invalider la query pour refetch la liste complète
                  queryClient.invalidateQueries({
                    queryKey: documentsKeys.bySpace(spaceId!),
                  });
                  // Aussi invalider le document individuel si on a un détail ouvert
                  queryClient.invalidateQueries({
                    queryKey: documentKeys.byId(data.documentId),
                  });
                } catch {
                  // event malformé, on ignore
                }
              }
            },
            onerror(err) {
              // En cas d'erreur, fetch-event-source retry automatiquement
              // sauf si c'est une abort error
              if (err instanceof DOMException && err.name === 'AbortError') {
                return;
              }
              console.warn('SSE error, will retry:', err);
            },
            onclose() {
              // Connexion fermée — fetch-event-source ne retry pas
              // si le serveur ferme explicitement
            },
          },
        );
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') {
          // Nettoyage normal au démontage
          return;
        }
        console.error('SSE connection failed:', err);
      }
    }

    connect();

    return () => {
      controller.abort();
      abortRef.current = null;
    };
  }, [spaceId, queryClient]);
}
