import { useState } from 'react';

import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { useDocuments } from '@/features/documents/api/use-documents';
import { useGenerateFiche } from '@/features/fiches/api/use-generate-fiche';
import { cn } from '@/lib/utils';
import type { AppDocument } from '@/features/documents/types';

type Perimetre = 'corpus' | 'documents' | 'theme';

interface GenerateFicheModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  spaceId: string;
}

/**
 * Modal de génération de fiche avec choix du périmètre :
 * - Corpus entier (tous les docs READY)
 * - Documents sélectionnés (checkboxes)
 * - Thème libre (champ texte → title)
 *
 * Contrat de design §2 : le modal s'ouvre sur la couche Espace (Papier),
 * pas de mélange de surfaces.
 */
export function GenerateFicheModal({ open, onOpenChange, spaceId }: GenerateFicheModalProps) {
  const [perimetre, setPerimetre] = useState<Perimetre>('corpus');
  const [selectedDocs, setSelectedDocs] = useState<Set<string>>(new Set());
  const [theme, setTheme] = useState('');

  const { data: documents } = useDocuments(spaceId);
  const generateFiche = useGenerateFiche();

  const readyDocs = documents?.filter((d) => d.status === 'READY') ?? [];

  function toggleDoc(id: string) {
    setSelectedDocs((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function handleGenerate() {
    const payload: { spaceId: string; documentIds?: string[]; title?: string } = { spaceId };

    if (perimetre === 'documents') {
      payload.documentIds = Array.from(selectedDocs);
    } else if (perimetre === 'theme' && theme.trim()) {
      payload.title = theme.trim();
    }

    generateFiche.mutate(payload, {
      onSuccess: () => {
        onOpenChange(false);
        reset();
      },
    });
  }

  function reset() {
    setPerimetre('corpus');
    setSelectedDocs(new Set());
    setTheme('');
  }

  const canGenerate =
    perimetre === 'corpus' ||
    (perimetre === 'documents' && selectedDocs.size > 0) ||
    (perimetre === 'theme' && theme.trim().length > 0);

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        onOpenChange(v);
        if (!v) reset();
      }}
    >
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Générer une fiche</DialogTitle>
          <DialogDescription>Choisissez le périmètre de génération.</DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4 py-2">
          {/* Corpus entier */}
          <label
            className={cn(
              'flex cursor-pointer items-start gap-3 rounded-fiche border border-border p-3 transition-colors hover:bg-secondary',
              perimetre === 'corpus' && 'border-tag-sciences bg-secondary',
            )}
          >
            <input
              type="radio"
              name="perimetre"
              checked={perimetre === 'corpus'}
              onChange={() => setPerimetre('corpus')}
              className="mt-0.5"
            />
            <div>
              <p className="text-sm font-medium text-foreground">Corpus entier</p>
              <p className="text-xs text-muted-foreground">
                Tous les documents prêts ({readyDocs.length})
              </p>
            </div>
          </label>

          {/* Documents sélectionnés */}
          <label
            className={cn(
              'flex cursor-pointer items-start gap-3 rounded-fiche border border-border p-3 transition-colors hover:bg-secondary',
              perimetre === 'documents' && 'border-tag-sciences bg-secondary',
            )}
          >
            <input
              type="radio"
              name="perimetre"
              checked={perimetre === 'documents'}
              onChange={() => setPerimetre('documents')}
              className="mt-0.5"
            />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium text-foreground">Documents sélectionnés</p>
              <p className="text-xs text-muted-foreground">
                Choisissez parmi les documents prêts
              </p>
            </div>
          </label>

          {perimetre === 'documents' && (
            <div className="ml-6 flex max-h-40 flex-col gap-1 overflow-y-auto">
              {readyDocs.length === 0 && (
                <p className="text-xs text-muted-foreground">Aucun document prêt.</p>
              )}
              {readyDocs.map((doc) => (
                <DocCheckbox key={doc.id} doc={doc} checked={selectedDocs.has(doc.id)} onToggle={toggleDoc} />
              ))}
            </div>
          )}

          {/* Thème libre */}
          <label
            className={cn(
              'flex cursor-pointer items-start gap-3 rounded-fiche border border-border p-3 transition-colors hover:bg-secondary',
              perimetre === 'theme' && 'border-tag-sciences bg-secondary',
            )}
          >
            <input
              type="radio"
              name="perimetre"
              checked={perimetre === 'theme'}
              onChange={() => setPerimetre('theme')}
              className="mt-0.5"
            />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium text-foreground">Thème libre</p>
              <p className="text-xs text-muted-foreground">
                Décrivez le sujet de la fiche
              </p>
            </div>
          </label>

          {perimetre === 'theme' && (
            <div className="ml-6">
              <Input
                placeholder="Ex : Les algorithmes de tri"
                value={theme}
                onChange={(e) => setTheme(e.target.value)}
              />
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Annuler
          </Button>
          <Button onClick={handleGenerate} disabled={!canGenerate || generateFiche.isPending}>
            {generateFiche.isPending ? 'Génération…' : 'Générer'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function DocCheckbox({
  doc,
  checked,
  onToggle,
}: {
  doc: AppDocument;
  checked: boolean;
  onToggle: (id: string) => void;
}) {
  return (
    <button
      type="button"
      onClick={() => onToggle(doc.id)}
      className="flex items-center gap-2 rounded-md px-2 py-1 text-left hover:bg-secondary"
    >
      <Checkbox checked={checked} />
      <span className="truncate text-xs text-foreground">{doc.filename}</span>
    </button>
  );
}
