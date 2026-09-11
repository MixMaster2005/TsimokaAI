import { useValidation } from '../api/validation-query-options';

interface FicheValidationBadgeProps {
  ficheId: string;
}

export function FicheValidationBadge({ ficheId }: FicheValidationBadgeProps) {
  const { data: validation } = useValidation(ficheId);
  if (!validation) return null;

  if (validation.statut === 'VALIDEE') {
    return (
      <span className="inline-flex items-center rounded-sm border border-succes/40 px-1.5 py-0.5 font-mono text-[0.62rem] font-semibold uppercase tracking-wider text-succes">
        Validée
      </span>
    );
  }
  if (validation.statut === 'REJETEE') {
    return (
      <span className="inline-flex items-center rounded-sm border border-attention/50 px-1.5 py-0.5 font-mono text-[0.62rem] font-semibold uppercase tracking-wider text-attention">
        À revoir
      </span>
    );
  }
  return null;
}
