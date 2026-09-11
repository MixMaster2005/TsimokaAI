import { useMemo, useState } from 'react';
import { createFileRoute, Link } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { espacesAllQueryOptions, useEspacesAll } from '@/features/espaces/api/use-espaces-all';
import { useAllFiches } from '@/features/fiches/api/use-all-fiches';
import { useValidateFiche } from '@/features/fiches/api/use-validate-fiche';
import { Badge } from '@/components/ui/badge';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';
import { cn } from '@/lib/utils';
import type { FicheWithSpace } from '@/features/fiches/api/use-all-fiches';

export const Route = createFileRoute('/enseignant/fiches-a-valider')({
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(espacesAllQueryOptions),
  component: FichesAValider,
});

import { useFiche } from '@/features/fiches/api/use-fiche';
import { parseFicheContent } from '@/features/fiches/types';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

function FicheApercu({ ficheId }: { ficheId: string }) {
  const { data: fiche, isLoading } = useFiche(ficheId);

  if (isLoading) {
    return <p className="text-xs text-encre-muted">Chargement de l'aperçu…</p>;
  }

  if (!fiche) {
    return <p className="text-xs text-encre-muted">Aperçu indisponible.</p>;
  }

  const parsed = parseFicheContent(fiche);
  const debut = parsed?.definition ?? fiche.contentJson.slice(0, 280);

  return (
    <div className="mt-3 rounded-md border border-dashed border-papier-border bg-background/50 p-3">
      <p className="text-sm font-medium text-encre">{fiche.title}</p>
      <p className="mt-1 line-clamp-3 text-xs text-encre-muted">{debut}</p>
      {parsed && parsed.key_points.length > 0 && (
        <p className="mt-1 font-mono text-[0.65rem] text-encre-muted">
          {parsed.key_points.length} point{parsed.key_points.length > 1 ? 's' : ''} clés ·{' '}
          {parsed.key_points[0].slice(0, 80)}
          {parsed.key_points[0].length > 80 ? '…' : ''}
        </p>
      )}
    </div>
  );
}

function FicheRow({ item }: { item: FicheWithSpace }) {
  const validateFiche = useValidateFiche(item.fiche.id);
  const statut = item.validation?.statut ?? 'EN_ATTENTE';
  const [detailsOuverts, setDetailsOuverts] = useState(false);
  const [rejectOuvert, setRejectOuvert] = useState(false);
  const [commentaire, setCommentaire] = useState('');
  const [erreur, setErreur] = useState<string | null>(null);

  function handleReject() {
    if (!commentaire.trim()) {
      setErreur('Un commentaire est obligatoire pour rejeter une fiche.');
      return;
    }
    setErreur(null);
    validateFiche.mutate(
      { statut: 'REJETEE', commentaire: commentaire.trim() },
      {
        onSuccess: () => {
          setRejectOuvert(false);
          setCommentaire('');
        },
      },
    );
  }

  return (
    <div className="rounded-fiche border border-papier-border bg-papier-carte p-4 transition-colors">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <Link
              to="/enseignant/espaces/$spaceId/fiches/$ficheId"
              params={{ spaceId: item.space.id, ficheId: item.fiche.id }}
              className="truncate font-display text-base font-semibold text-encre hover:underline"
            >
              {item.fiche.title}
            </Link>
            <Badge
              variant={
                statut === 'VALIDEE' ? 'succes' : statut === 'REJETEE' ? 'erreur' : 'attention'
              }
              className="font-mono text-[0.6rem]"
            >
              {statut === 'EN_ATTENTE' ? 'En attente' : statut === 'VALIDEE' ? 'Validée' : 'Rejetée'}
            </Badge>
          </div>
          <p className="mt-0.5 font-mono text-[0.68rem] text-encre-muted">
            par {item.fiche.userId.slice(0, 8)}… · {new Date(item.fiche.updatedAt).toLocaleDateString('fr-FR')}
          </p>
          <div className="mt-1.5 flex items-center gap-2">
            <span
              className={cn(
                'inline-flex items-center rounded-sm px-1.5 py-0.5 font-mono text-[0.6rem] font-medium uppercase tracking-wide text-white',
                getTagColorClass(item.space.subjectTag),
              )}
            >
              {item.space.subjectTag ?? 'sans tag'}
            </span>
            <span className="font-mono text-[0.65rem] text-encre-muted">{item.space.name}</span>
          </div>
        </div>

        <div className="flex flex-none items-center gap-2">
          {statut === 'EN_ATTENTE' && (
            <>
              <Button
                size="sm"
                disabled={validateFiche.isPending}
                onClick={() => validateFiche.mutate({ statut: 'VALIDEE' })}
              >
                Valider
              </Button>
              <Button
                variant="destructive"
                size="sm"
                disabled={validateFiche.isPending}
                onClick={() => {
                  setErreur(null);
                  setRejectOuvert(true);
                }}
              >
                Rejeter
              </Button>
            </>
          )}
          <Button variant="outline" size="sm" onClick={() => setDetailsOuverts((v) => !v)}>
            {detailsOuverts ? 'Masquer' : 'Détails'}
          </Button>
        </div>
      </div>

      {detailsOuverts && <FicheApercu ficheId={item.fiche.id} />}

      {item.validation?.commentaire && statut !== 'EN_ATTENTE' && (
        <p className="mt-2 text-xs italic text-encre-muted">
          Commentaire enseignant : {item.validation.commentaire}
        </p>
      )}

      <div className="mt-2">
        <Link
          to="/enseignant/espaces/$spaceId/fiches/$ficheId"
          params={{ spaceId: item.space.id, ficheId: item.fiche.id }}
          className="font-mono text-[0.65rem] text-encre-muted hover:underline"
        >
          Ouvrir la fiche complète →
        </Link>
      </div>

      <Dialog open={rejectOuvert} onOpenChange={setRejectOuvert}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Rejeter la fiche</DialogTitle>
            <DialogDescription>
              « {item.fiche.title} » — un commentaire est obligatoire pour expliquer le rejet à
              l'élève.
            </DialogDescription>
          </DialogHeader>
          <textarea
            value={commentaire}
            onChange={(e) => setCommentaire(e.target.value)}
            placeholder="Ex : manque la définition + un exemple concret…"
            rows={4}
            className="w-full rounded-md border border-papier-border bg-background p-2 text-sm text-encre"
          />
          {erreur && <p className="text-xs text-red-600">{erreur}</p>}
          <DialogFooter>
            <Button variant="outline" size="sm" onClick={() => setRejectOuvert(false)}>
              Annuler
            </Button>
            <Button
              variant="destructive"
              size="sm"
              disabled={validateFiche.isPending}
              onClick={handleReject}
            >
              {validateFiche.isPending ? 'Rejet…' : 'Confirmer le rejet'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function FichesAValider() {
  const { data: espaces } = useEspacesAll();
  const { fiches, isLoading } = useAllFiches(espaces);
  const [search, setSearch] = useState('');
  const [selectedSpace, setSelectedSpace] = useState<string>('all');

  const filtered = useMemo(() => {
    let result = fiches;

    if (selectedSpace !== 'all') {
      result = result.filter((item) => item.space.id === selectedSpace);
    }

    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(
        (item) =>
          item.fiche.title.toLowerCase().includes(q) ||
          item.fiche.userId.toLowerCase().includes(q) ||
          item.space.name.toLowerCase().includes(q),
      );
    }

    const ordreStatut: Record<string, number> = { EN_ATTENTE: 0, VALIDEE: 1, REJETEE: 2 };
    return [...result].sort((a, b) => {
      const sa = ordreStatut[a.validation?.statut ?? 'EN_ATTENTE'] ?? 3;
      const sb = ordreStatut[b.validation?.statut ?? 'EN_ATTENTE'] ?? 3;
      if (sa !== sb) return sa - sb;
      return new Date(b.fiche.updatedAt).getTime() - new Date(a.fiche.updatedAt).getTime();
    });
  }, [fiches, search, selectedSpace]);

  return (
    <div className="p-8">
      <div className="mb-6">
        <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Vue enseignant</p>
        <h1 className="font-display text-2xl font-semibold text-encre">
          Fiches à valider
          {filtered.length > 0 && (
            <span className="ml-2 font-mono text-base text-encre-muted">({filtered.length})</span>
          )}
        </h1>
      </div>

      {/* Filtres */}
      <div className="mb-6 flex flex-wrap gap-3">
        <Input
          placeholder="Rechercher par titre, élève…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="max-w-xs"
        />
        <select
          value={selectedSpace}
          onChange={(e) => setSelectedSpace(e.target.value)}
          className="max-w-xs rounded-md border border-papier-border bg-papier-carte px-3 py-2 text-sm"
        >
          <option value="all">Tous les espaces</option>
          {espaces?.map((space) => (
            <option key={space.id} value={space.id}>
              {space.name}
            </option>
          ))}
        </select>
      </div>

      {isLoading && (
        <p className="text-sm text-encre-muted">Chargement des fiches…</p>
      )}

      {!isLoading && filtered.length === 0 && (
        <p className="text-sm text-encre-muted">Aucune fiche à valider.</p>
      )}

      <div className="flex flex-col gap-3">
        {filtered.map((item) => (
          <FicheRow key={item.fiche.id} item={item} />
        ))}
      </div>
    </div>
  );
}
