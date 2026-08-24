/**
 * ⚠️ Détail d'architecture à connaître : côté back, `subjectTag` est un
 * String libre (CreateSpaceRequest ne le contraint à aucun enum) — alors
 * que le contrat de design suppose 6 couleurs fixes. Ce fichier fait le pont :
 * - si le tag correspond à une des 6 familles connues -> sa couleur dédiée
 * - sinon (tag libre tapé par un enseignant, ex: "Philosophie") -> une
 *   couleur choisie par hash, stable dans le temps pour ce tag précis
 *
 * Si un jour `subjectTag` devient un enum côté back, ce fichier est le
 * seul à mettre à jour, aucun composant n'a à changer.
 */

const TAG_COLOR_CLASSES = [
  'bg-tag-sciences',
  'bg-tag-info',
  'bg-tag-lettres',
  'bg-tag-eco',
  'bg-tag-droit-shs',
  'bg-tag-langues',
] as const;

const KNOWN_TAGS: Record<string, (typeof TAG_COLOR_CLASSES)[number]> = {
  sciences: 'bg-tag-sciences',
  algorithmique: 'bg-tag-sciences',
  maths: 'bg-tag-sciences',
  info: 'bg-tag-info',
  informatique: 'bg-tag-info',
  reseaux: 'bg-tag-info',
  lettres: 'bg-tag-lettres',
  litterature: 'bg-tag-lettres',
  histoire: 'bg-tag-lettres',
  eco: 'bg-tag-eco',
  economie: 'bg-tag-eco',
  gestion: 'bg-tag-eco',
  droit: 'bg-tag-droit-shs',
  shs: 'bg-tag-droit-shs',
  langues: 'bg-tag-langues',
  anglais: 'bg-tag-langues',
};

function hashString(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
}

export function getTagColorClass(subjectTag: string | null | undefined): string {
  if (!subjectTag) return 'bg-encre-muted';
  const normalized = subjectTag.trim().toLowerCase();
  if (KNOWN_TAGS[normalized]) return KNOWN_TAGS[normalized];
  return TAG_COLOR_CLASSES[hashString(normalized) % TAG_COLOR_CLASSES.length];
}
