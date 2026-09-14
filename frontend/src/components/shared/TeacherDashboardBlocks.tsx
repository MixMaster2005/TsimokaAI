import { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { useTeacherDashboard } from '@/features/dashboard/api/use-teacher-dashboard';
import { useTeacherRecommandations } from '@/features/dashboard/api/use-teacher-recommandations';
import { useTeacherStudents } from '@/features/dashboard/api/use-teacher-students';

interface TeacherDashboardBlocksProps {
  spaceId: string;
}

export function TeacherDashboardBlocks({ spaceId }: TeacherDashboardBlocksProps) {
  const { data: dashboard } = useTeacherDashboard(spaceId);
  const { data: students } = useTeacherStudents(spaceId);
  const [selectedStudentId, setSelectedStudentId] = useState('');
  const effectiveStudentId = selectedStudentId || students?.[0]?.userId || '';
  const { data: recommandations } = useTeacherRecommandations(spaceId, effectiveStudentId);

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
      {/* Notions les plus consultées */}
      <section className="rounded-fiche border border-papier-border bg-papier-carte p-4">
        <h2 className="mb-3 font-display text-sm font-semibold text-encre">Notions les plus consultées</h2>
        {dashboard?.notionsLesPlusConsultees?.length ? (
          <div className="flex flex-col gap-2">
            {dashboard.notionsLesPlusConsultees.map((n) => (
              <div key={n.notion} className="flex items-center justify-between text-sm">
                <span className="text-encre">{n.notion}</span>
                <div className="flex items-center gap-2 font-mono text-[0.65rem] text-encre-muted">
                  <span>{n.nbConsultations} vues</span>
                  <span>{n.nbQuestions} questions</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-xs text-encre-muted">Pas encore de données pour cet espace.</p>
        )}
      </section>

      {/* Chapitres difficiles */}
      <section className="rounded-fiche border border-papier-border bg-papier-carte p-4">
        <h2 className="mb-3 font-display text-sm font-semibold text-encre">Chapitres difficiles</h2>
        {dashboard?.chapitresDifficiles?.length ? (
          <div className="flex flex-col gap-2">
            {dashboard.chapitresDifficiles.map((c) => (
              <div key={c.chapitre} className="flex items-center justify-between text-sm">
                <span className="text-encre">{c.chapitre}</span>
                <Badge variant={c.scoreDifficulte > 0.7 ? 'erreur' : 'attention'}>
                  {Math.round(c.scoreDifficulte * 100)}% difficulté
                </Badge>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-xs text-encre-muted">Aucun chapitre identifié comme difficile.</p>
        )}
      </section>

      {/* Questions fréquentes */}
      <section className="rounded-fiche border border-papier-border bg-papier-carte p-4">
        <h2 className="mb-3 font-display text-sm font-semibold text-encre">Questions fréquentes</h2>
        {dashboard?.questionsFrequentes?.length ? (
          <div className="flex flex-col gap-2">
            {dashboard.questionsFrequentes.slice(0, 10).map((q) => (
              <div key={`${q.question}-${q.dernierAsk}`} className="flex items-center justify-between gap-3 text-sm">
                <span className="truncate text-encre">{q.question}</span>
                <div className="flex shrink-0 items-center gap-2 font-mono text-[0.65rem] text-encre-muted">
                  <Badge variant="secondary">× {q.nbOccurrences}</Badge>
                  <span>{new Date(q.dernierAsk).toLocaleDateString('fr-FR')}</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-xs text-encre-muted">Aucune question fréquente pour l'instant.</p>
        )}
      </section>

      {/* Évolution agrégée */}
      <section className="rounded-fiche border border-papier-border bg-papier-carte p-4">
        <h2 className="mb-3 font-display text-sm font-semibold text-encre">Évolution de la promotion</h2>
        <div className="flex items-center gap-4">
          <div className="text-center">
            <p className="font-mono text-2xl font-bold text-encre">{dashboard?.nbEtudiantsActifs ?? 0}</p>
            <p className="font-mono text-[0.65rem] text-encre-muted">étudiants actifs</p>
          </div>
        </div>
        {dashboard?.evolution?.length ? (
          <div className="mt-3 flex flex-col gap-2">
            {dashboard.evolution.map((s) => (
              <div key={s.semaine} className="flex items-center gap-2">
                <span className="w-12 shrink-0 font-mono text-[0.65rem] text-encre-muted">
                  {new Date(s.semaine).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' })}
                </span>
                <Progress
                  value={
                    Math.max(...dashboard.evolution.map((e) => e.nbActifs)) > 0
                      ? (s.nbActifs / Math.max(...dashboard.evolution.map((e) => e.nbActifs))) * 100
                      : 0
                  }
                />
                <span className="w-8 shrink-0 text-right font-mono text-[0.65rem] text-encre">{s.nbActifs}</span>
              </div>
            ))}
          </div>
        ) : (
          <p className="mt-3 text-xs text-encre-muted">Pas encore de données d'évolution.</p>
        )}
      </section>

      {/* Recommandations IA — par étudiant (GET /api/v1/dashboard/teacher/recommandations) */}
      <section className="rounded-fiche border border-papier-border bg-papier-carte p-4">
        <h2 className="mb-3 font-display text-sm font-semibold text-encre">Recommandations IA</h2>
        {students?.length ? (
          <select
            className="mb-3 w-full rounded-sm border border-papier-border bg-papier-fond px-2 py-1 text-xs text-encre"
            value={effectiveStudentId}
            onChange={(e) => setSelectedStudentId(e.target.value)}
            aria-label="Étudiant"
          >
            {students.map((s) => (
              <option key={s.userId} value={s.userId}>
                {s.userId.slice(0, 8)}… · {s.nbQuestions} q. · {s.nbFiches} fiches ·{' '}
                {Math.round(s.taux * 100)}%
              </option>
            ))}
          </select>
        ) : (
          <p className="text-xs text-encre-muted">Aucun étudiant dans cet espace pour l'instant.</p>
        )}
        {effectiveStudentId ? (
          recommandations?.length ? (
            <div className="flex flex-col gap-2">
              {recommandations.map((r) => (
                <div key={r.id} className="rounded-sm bg-secondary/50 px-3 py-2 text-sm text-encre">
                  <p>{r.contenu}</p>
                  <p className="mt-1 font-mono text-[0.62rem] text-encre-muted">
                    {r.type.replace(/_/g, ' ').toLowerCase()} · {new Date(r.genereLe).toLocaleDateString('fr-FR')}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-encre-muted">Aucune recommandation pour cet étudiant pour l'instant.</p>
          )
        ) : null}
      </section>
    </div>
  );
}
