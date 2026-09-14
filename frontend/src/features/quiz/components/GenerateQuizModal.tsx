import { useState} from 'react';

import { Button } from '@/components/ui/button';
// import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
// import { Checkbox } from '@/components/ui/checkbox';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { useDocuments } from '@/features/documents/api/use-documents';
import { useGenerateQuiz } from '@/features/quiz/api/use-generate-quiz';
import { cn } from '@/lib/utils';
import type { AppDocument } from '@/features/documents/types';
import type { QuizScope, QuizDifficulty, GenerateQuizRequest } from '@/features/quiz/types';

interface GenerateQuizModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  spaceId: string;
}

type Step = 'scope' | 'difficulty' | 'count';

const DIFFICULTIES: { value: QuizDifficulty; label: string; description: string }[] = [
  { value: 'FACILE', label: 'Facile', description: 'Questions de compréhension basique' },
  { value: 'MOYEN', label: 'Moyen', description: 'Questions d\'application' },
  { value: 'DIFFICILE', label: 'Difficile', description: 'Questions d\'analyse et de synthèse' },
];

const QUESTION_COUNTS = [5, 10, 15, 20];

export function GenerateQuizModal({ open, onOpenChange, spaceId }: GenerateQuizModalProps) {
  const [step, setStep] = useState<Step>('scope');
  const [scope, setScope] = useState<QuizScope>('SPACE');
  const [selectedDocId, setSelectedDocId] = useState<string | null>(null);
  const [topic, setTopic] = useState('');
  const [difficulty, setDifficulty] = useState<QuizDifficulty>('MOYEN');
  const [questionCount, setQuestionCount] = useState(10);

  const { data: documents } = useDocuments(spaceId);
  const generateQuiz = useGenerateQuiz();

  const readyDocs = documents?.filter((d) => d.status === 'READY') ?? [];

  function reset() {
    setStep('scope');
    setScope('SPACE');
    setSelectedDocId(null);
    setTopic('');
    setDifficulty('MOYEN');
    setQuestionCount(10);
  }

  function handleGenerate() {
    const payload: GenerateQuizRequest = { spaceId, scope, difficulty, questionCount, statut: 'BROUILLON' };

    if (scope === 'DOCUMENT' && selectedDocId) {
      payload.targetDocumentId = selectedDocId;
    } else if (scope === 'TOPIC' && topic.trim()) {
      payload.targetTopic = topic.trim();
    }

    generateQuiz.mutate(payload, {
      onSuccess: () => {
        onOpenChange(false);
        reset();
      },
    });
  }

  function nextStep() {
    if (step === 'scope') setStep('difficulty');
    else if (step === 'difficulty') setStep('count');
  }

  function prevStep() {
    if (step === 'count') setStep('difficulty');
    else if (step === 'difficulty') setStep('scope');
  }

  const canNext =
    (step === 'scope' &&
      (scope === 'SPACE' || (scope === 'DOCUMENT' && selectedDocId) || (scope === 'TOPIC' && topic.trim().length > 0))) ||
    step === 'difficulty';

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
          <DialogTitle>Générer un Quiz</DialogTitle>
          <DialogDescription>
            Étape {step === 'scope' ? '1' : step === 'difficulty' ? '2' : '3'} sur 3
          </DialogDescription>
        </DialogHeader>

        {step === 'scope' && (
          <div className="flex flex-col gap-3 py-2">
            <label
              className={cn(
                'flex cursor-pointer items-start gap-3 rounded-fiche border border-border p-3 transition-colors hover:bg-secondary',
                scope === 'SPACE' && 'border-tag-sciences bg-secondary',
              )}
            >
              <input
                type="radio"
                name="scope"
                checked={scope === 'SPACE'}
                onChange={() => setScope('SPACE')}
                className="mt-0.5"
              />
              <div>
                <p className="text-sm font-medium text-foreground">Corpus entier</p>
                <p className="text-xs text-muted-foreground">
                  Tous les documents prêts ({readyDocs.length})
                </p>
              </div>
            </label>

            <label
              className={cn(
                'flex cursor-pointer items-start gap-3 rounded-fiche border border-border p-3 transition-colors hover:bg-secondary',
                scope === 'DOCUMENT' && 'border-tag-sciences bg-secondary',
              )}
            >
              <input
                type="radio"
                name="scope"
                checked={scope === 'DOCUMENT'}
                onChange={() => setScope('DOCUMENT')}
                className="mt-0.5"
              />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-foreground">Document spécifique</p>
                <p className="text-xs text-muted-foreground">
                  Choisissez parmi les documents prêts
                </p>
              </div>
            </label>

            {scope === 'DOCUMENT' && (
              <div className="ml-6 flex max-h-40 flex-col gap-1 overflow-y-auto">
                {readyDocs.length === 0 && (
                  <p className="text-xs text-muted-foreground">Aucun document prêt.</p>
                )}
                {readyDocs.map((doc) => (
                  <DocRadio
                    key={doc.id}
                    doc={doc}
                    checked={selectedDocId === doc.id}
                    onSelect={setSelectedDocId}
                  />
                ))}
              </div>
            )}

            <label
              className={cn(
                'flex cursor-pointer items-start gap-3 rounded-fiche border border-border p-3 transition-colors hover:bg-secondary',
                scope === 'TOPIC' && 'border-tag-sciences bg-secondary',
              )}
            >
              <input
                type="radio"
                name="scope"
                checked={scope === 'TOPIC'}
                onChange={() => setScope('TOPIC')}
                className="mt-0.5"
              />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-foreground">Thème libre</p>
                <p className="text-xs text-muted-foreground">
                  Décrivez le sujet du quiz
                </p>
              </div>
            </label>

            {scope === 'TOPIC' && (
              <div className="ml-6">
                <Input
                  placeholder="Ex : Les algorithmes de tri"
                  value={topic}
                  onChange={(e) => setTopic(e.target.value)}
                />
              </div>
            )}
          </div>
        )}

        {step === 'difficulty' && (
          <div className="flex flex-col gap-3 py-2">
            {DIFFICULTIES.map((d) => (
              <label
                key={d.value}
                className={cn(
                  'flex cursor-pointer items-start gap-3 rounded-fiche border border-border p-3 transition-colors hover:bg-secondary',
                  difficulty === d.value && 'border-tag-sciences bg-secondary',
                )}
              >
                <input
                  type="radio"
                  name="difficulty"
                  checked={difficulty === d.value}
                  onChange={() => setDifficulty(d.value)}
                  className="mt-0.5"
                />
                <div>
                  <p className="text-sm font-medium text-foreground">{d.label}</p>
                  <p className="text-xs text-muted-foreground">{d.description}</p>
                </div>
              </label>
            ))}
          </div>
        )}

        {step === 'count' && (
          <div className="flex flex-col gap-3 py-2">
            <p className="text-sm text-muted-foreground">Nombre de questions</p>
            <div className="grid grid-cols-4 gap-2">
              {QUESTION_COUNTS.map((count) => (
                <button
                  key={count}
                  onClick={() => setQuestionCount(count)}
                  className={cn(
                    'rounded-fiche border border-border p-3 text-center text-sm font-medium transition-colors hover:bg-secondary',
                    questionCount === count && 'border-tag-sciences bg-secondary text-foreground',
                  )}
                >
                  {count}
                </button>
              ))}
            </div>
          </div>
        )}

        <DialogFooter>
          {step !== 'scope' && (
            <Button variant="ghost" onClick={prevStep}>
              Retour
            </Button>
          )}
          {step !== 'count' ? (
            <Button onClick={nextStep} disabled={!canNext}>
              Suivant
            </Button>
          ) : (
            <Button onClick={handleGenerate} disabled={generateQuiz.isPending}>
              {generateQuiz.isPending ? 'Génération…' : 'Générer'}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function DocRadio({
  doc,
  checked,
  onSelect,
}: {
  doc: AppDocument;
  checked: boolean;
  onSelect: (id: string) => void;
}) {
  return (
    <button
      type="button"
      onClick={() => onSelect(doc.id)}
      className="flex items-center gap-2 rounded-md px-2 py-1 text-left hover:bg-secondary"
    >
      <input
        type="radio"
        name="doc-select"
        checked={checked}
        onChange={() => onSelect(doc.id)}
        className="accent-primary"
      />
      <span className="truncate text-xs text-foreground">{doc.filename}</span>
    </button>
  );
}
