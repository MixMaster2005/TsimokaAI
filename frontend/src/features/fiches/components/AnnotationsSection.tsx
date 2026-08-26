import { useState, type FormEvent } from 'react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useAddAnnotation, annotationsQueryOptions } from '../api/use-annotations';
import type { Annotation } from '../types';
import { useQuery } from '@tanstack/react-query';

/**
 * Annotations de la fiche (liste + ajout). Les annotations sont libres
 * (texte + section visée optionnelle) et ouvertes à tous les utilisateurs de
 * la fiche : elles constituent le fil pédagogique autour du contenu.
 */
export function AnnotationsSection({ ficheId }: { ficheId: string }) {
  const { data: annotations } = useQuery(annotationsQueryOptions(ficheId));
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
        {annotations?.map((a) => <AnnotationItem key={a.id} annotation={a} />)}
        {annotations?.length === 0 && (
          <p className="text-xs text-encre-muted">Aucune annotation pour l'instant.</p>
        )}
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-2 rounded-fiche border border-papier-border bg-papier-carte p-3">
        <Input
          placeholder="Ajouter une annotation…"
          value={contenu}
          onChange={(e) => setContenu(e.target.value)}
        />
        <div className="flex items-center gap-2">
          <Input
            placeholder="Section (optionnel : definition, key_points…)"
            value={sectionRef}
            onChange={(e) => setSectionRef(e.target.value)}
          />
          <Button type="submit" variant="outline" size="sm" disabled={addAnnotation.isPending}>
            {addAnnotation.isPending ? 'Envoi…' : 'Annoter'}
          </Button>
        </div>
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
