import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import type { Conversation } from '@/features/chat/types';
import type { Fiche } from '@/features/fiches/types';
import type { Quiz } from '@/features/quiz/types';

export interface SessionRevision {
  id: string;
  spaceId: string;
  titre: string;
  dureeMinutes: number;
  dateSession: string;
  nbFichesRevisees: number;
  nbQuiz: number;
  nbConversations: number;
}

/** Heuristique d'estimation affichée telle quelle (pas une mesure réelle). */
const DUREE_PAR_FICHE = 5;
const DUREE_PAR_QUIZ = 10;
const DUREE_PAR_CONVERSATION = 3;
const DUREE_MIN = 5;
const NB_SESSIONS_MAX = 12;

function jourUtc(iso: string | null | undefined): string | null {
  if (!iso) return null;
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return null;
  return new Date(iso).toISOString().slice(0, 10);
}

interface Compteurs {
  fiches: number;
  quiz: number;
  conversations: number;
}

/**
 * Historique des sessions de révision (Lot 2, V1 sans couplage inter-BD).
 *
 * Agrège côté front les trois listes existantes du space (fiches, quiz,
 * conversations) et les groupe par jour UTC décroissant. La durée est une
 * ESTIMATION (5 min/fiche, 10 min/quiz, 3 min/conversation, min 5) — affichée
 * avec le préfixe « ~ ». Un endpoint backend dédié (`session_revision`
 * alimentée par events) reste prévu en V2 si le volume l'exige.
 *
 * Tolérance : un endpoint en échec (403/404 selon le rôle) vaut liste vide ;
 * si les trois échouent, l'erreur est propagée (état d'erreur du dashboard).
 */
export const sessionHistoryQueryOptions = (spaceId?: string | null) =>
  queryOptions({
    queryKey: ['dashboard', 'sessions', 'v1', spaceId],
    queryFn: async (): Promise<SessionRevision[]> => {
      if (!spaceId) return [];
      const [fiches, quizzes, conversations] = await Promise.allSettled([
        apiClient.get<Fiche[]>(`/api/v1/fiches?spaceId=${spaceId}`),
        apiClient.get<Quiz[]>(`/api/v1/quizzes?spaceId=${spaceId}`),
        apiClient.get<Conversation[]>(`/api/v1/conversations?spaceId=${spaceId}`),
      ]);
      const rejetees = [fiches, quizzes, conversations].filter((r) => r.status === 'rejected');
      if (rejetees.length === 3) {
        const motif = (rejetees[0] as PromiseRejectedResult).reason;
        throw motif instanceof Error ? motif : new Error('Historique des sessions indisponible');
      }
      const parJour = new Map<string, Compteurs>();
      const ajoute = (iso: string | null | undefined, cle: keyof Compteurs) => {
        const jour = jourUtc(iso);
        if (!jour) return;
        const c = parJour.get(jour) ?? { fiches: 0, quiz: 0, conversations: 0 };
        c[cle] += 1;
        parJour.set(jour, c);
      };
      if (fiches.status === 'fulfilled') {
        for (const f of fiches.value ?? []) ajoute(f.generatedAt ?? f.updatedAt, 'fiches');
      }
      if (quizzes.status === 'fulfilled') {
        for (const q of quizzes.value ?? []) ajoute(q.generatedAt ?? q.updatedAt, 'quiz');
      }
      if (conversations.status === 'fulfilled') {
        for (const c of conversations.value ?? []) ajoute(c.createdAt ?? c.updatedAt, 'conversations');
      }
      return [...parJour.entries()]
        .sort(([a], [b]) => (a < b ? 1 : -1))
        .slice(0, NB_SESSIONS_MAX)
        .map(([jour, c]): SessionRevision => {
          const dateFr = new Date(`${jour}T12:00:00.000Z`).toLocaleDateString('fr-FR', {
            day: 'numeric',
            month: 'short',
          });
          return {
            id: `session-${jour}`,
            spaceId,
            titre: `Révision du ${dateFr}`,
            dureeMinutes: Math.max(
              DUREE_MIN,
              c.fiches * DUREE_PAR_FICHE + c.quiz * DUREE_PAR_QUIZ + c.conversations * DUREE_PAR_CONVERSATION,
            ),
            dateSession: `${jour}T12:00:00.000Z`,
            nbFichesRevisees: c.fiches,
            nbQuiz: c.quiz,
            nbConversations: c.conversations,
          };
        });
    },
    enabled: Boolean(spaceId),
  });

export function useSessionHistory(spaceId?: string | null) {
  return useQuery(sessionHistoryQueryOptions(spaceId));
}
