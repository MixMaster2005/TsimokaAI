import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { Badge } from '@/components/ui/badge';
import { useEspace, espaceQueryOptions } from '@/features/espaces/api/use-espace';
import { useTeacherDashboard, teacherDashboardQueryOptions } from '@/features/dashboard/api/use-teacher-dashboard';
import { useRecommandations, recommandationsQueryOptions } from '@/features/dashboard/api/use-recommandations';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/dashboard')({
  loader: ({ context: { queryClient }, params }) =>
    Promise.all([
      queryClient.ensureQueryData(espaceQueryOptions(params.spaceId)),
      queryClient.ensureQueryData(teacherDashboardQueryOptions(params.spaceId)),
      queryClient.ensureQueryData(recommandationsQueryOptions(params.spaceId)),
    ]),
  component: TableauDeBordEnseignant,
});

function TableauDeBordEnseignant() {
  const { spaceId } = useParams({ from: '/enseignant/espaces/$spaceId/dashboard' });
  const { data: espace } = useEspace(spaceId);
  const { data: dashboard } = useTeacherDashboard(spaceId);
  const { data: recommandations } = useRecommandations(spaceId);

  return (
    <div className="p-8">
      <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">
        {espace?.name ?? 'Espace'}
      </p>
      <h1 className="mb-6 font-display text-2xl font-semibold text-encre">Tableau de bord enseignant</h1>

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

        {/* Évolution agrégée */}
        <section className="rounded-fiche border border-papier-border bg-papier-carte p-4">
          <h2 className="mb-3 font-display text-sm font-semibold text-encre">Évolution de la promotion</h2>
          <div className="flex items-center gap-4">
            <div className="text-center">
              <p className="font-mono text-2xl font-bold text-encre">{dashboard?.nbEtudiantsActifs ?? 0}</p>
              <p className="font-mono text-[0.65rem] text-encre-muted">étudiants actifs</p>
            </div>
          </div>
          <p className="mt-3 text-xs text-encre-muted">
            L'évolution agrégée détaillée sera disponible dans une prochaine mise à jour.
          </p>
        </section>

        {/* Recommandations IA */}
        <section className="rounded-fiche border border-papier-border bg-papier-carte p-4">
          <h2 className="mb-3 font-display text-sm font-semibold text-encre">Recommandations IA</h2>
          {recommandations?.length ? (
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
            <p className="text-xs text-encre-muted">Aucune recommandation pour l'instant.</p>
          )}
        </section>
      </div>

      <div className="mt-6">
        <Link
          to="/enseignant/espaces/$spaceId"
          params={{ spaceId }}
          className="text-sm text-encre-muted underline hover:text-encre"
        >
          ← Retour aux fiches à valider
        </Link>
      </div>
    </div>
  );
}
