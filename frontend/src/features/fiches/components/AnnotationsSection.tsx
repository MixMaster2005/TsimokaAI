import { useState, type FormEvent } from 'react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { useAddAnnotation } from '../api/use-add-annotation';
import { annotationsQueryOptions } from '../api/annotations-query-options';
import type { Annotation } from '../types';
import { useQuery } from '@tanstack/react-query';

/**
 * Annotations de la fiche (liste + ajout). Les annotations sont libres
 * (texte + section visée optionnelle) et ouvertes à tous les utilisateurs de
 * la fiche : elles constituent le fil pédagogique autour du contenu.
 */
export function AnnotationsSection({ ficheId }: { ficheId: string }) {
  const { data: annotations, isLoading, isError, refetch } = useQuery(annotationsQueryOptions(ficheId));
  const addAnnotation = useAddAnnotation(ficheId);
  const [contenu, setContenu] = useState('');
  const [sectionRef, setSectionRef] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!contenu.trim()) return;
    addAnnotation.mutate(
      // sectionRef vide = absent : le back stocke null plutôt qu'une string vide
      { contenu, ...(sectionRef.trim() ? { sectionRef: sectionRef.trim() } : {}) },
      {
        onSuccess: () => {
          setContenu('');
          setSectionRef('');
        },
      },
    );
  }

  return (
    <section className="flex flex-col gap-3">
      <h2 className="font-display text-sm font-semibold text-encre">
        Annotations{annotations ? ` (${annotations.length})` : ''}
      </h2>

      <div className="flex flex-col gap-2">
        {isLoading && (
          <div role="status" aria-live="polite" aria-busy="true" className="flex flex-col gap-2">
            <Skeleton className="h-16 w-full" />
            <span className="sr-only">Chargement des annotations…</span>
          </div>
        )}

        {isError && (
          <div className="flex flex-col items-start gap-2">
            <p role="alert" className="text-xs text-erreur">
              Impossible de charger les annotations. Vérifie ta connexion puis réessaie.
            </p>
            <Button variant="outline" size="sm" onClick={() => refetch()}>
              Réessayer
            </Button>
          </div>
        )}

        {!isLoading && !isError && annotations?.map((a) => <AnnotationItem key={a.id} annotation={a} />)}
        {!isLoading && !isError && annotations?.length === 0 && (
          <p className="text-xs text-encre-muted">Aucune annotation pour l'instant.</p>
        )}
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-2 rounded-fiche border border-papier-border bg-papier-carte p-3">
        <label htmlFor="annotation-contenu" className="sr-only">
          Ajouter une annotation…
        </label>
        <Input
          id="annotation-contenu"
          placeholder="Ajouter une annotation… — ex : revoir cet exemple…"
          value={contenu}
          onChange={(e) => setContenu(e.target.value)}
          autoComplete="off"
        />
        <div className="flex items-center gap-2">
          <label htmlFor="annotation-section" className="sr-only">
            Section visée (optionnel)…
          </label>
          <Input
            id="annotation-section"
            placeholder="Section (optionnel) — ex : definition…"
            value={sectionRef}
            onChange={(e) => setSectionRef(e.target.value)}
            autoComplete="off"
          />
          <Button type="submit" variant="outline" size="sm" disabled={addAnnotation.isPending}>
            {addAnnotation.isPending ? 'Envoi…' : 'Annoter'}
          </Button>
        </div>
        {addAnnotation.isError && (
          <p role="alert" className="text-xs text-erreur">
            {addAnnotation.error.message} — vérifie ta connexion puis réessaie.
          </p>
        )}
      </form>
    </section>
  );
}

function AnnotationItem({ annotation }: { annotation: Annotation }) {
  return (
    <div className="rounded-fiche border border-papier-border bg-papier-carte p-3">
      <p className="text-sm text-encre">{annotation.contenu}</p>
      <p className="mt-1 font-mono text-[0.68rem] text-encre-muted">
        {annotation.sectionRef && <span>{annotation.sectionRef} · </span>}
        par {annotation.auteurId.slice(0, 8)}… ·{' '}
        {new Date(annotation.createdAt).toLocaleDateString('fr-FR')}
      </p>
    </div>
  );
}
