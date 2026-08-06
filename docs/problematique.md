# Problématique du mémoire

Document de travail pour le mémoire — problématique, objectif principal et questions de
recherche. La formulation reste volontairement ouverte : elle ne présuppose ni que
l'architecture microservices soit la solution optimale, ni que les contraintes rencontrées
soient nécessairement pénalisantes — ce sont précisément les questions de recherche qui
permettent de le discuter.

## Problématique

> Comment une architecture microservices, de sa conception à son exploitation, peut-elle
> répondre aux besoins d'un dispositif d'apprentissage augmenté par l'intelligence
> artificielle tout en s'adaptant aux contraintes d'un projet académique disposant de
> ressources limitées ?

## Objectif principal

Interroger la pertinence et les conditions de mise en œuvre d'une architecture
microservices pour un système d'apprentissage augmenté par IA, en articulant les dimensions
de conception, de gestion de projet et de performance, depuis l'identification des besoins
jusqu'à l'exploitation du système.

## Questions de recherche

### QR1 — Analyse des besoins et conception

**Question :** Comment les besoins fonctionnels et pédagogiques d'un dispositif
d'apprentissage augmenté par IA peuvent-ils être traduits en une architecture logicielle
cohérente ?

**Objectifs et résultats attendus :** identifier et catégoriser les besoins des parties
prenantes (étudiants, enseignants) ; examiner comment ces besoins orientent ou contraignent
le découpage fonctionnel et la modélisation ; situer les choix retenus face à des
alternatives. Livrables : artefacts de modélisation (cas d'utilisation, représentation
architecturale, découpage par service) et analyse argumentée (type SWOT) des apports et
limites des choix.

### QR2 — Gestion de projet et développement

**Question :** Comment la conduite d'un projet de développement logiciel, menée dans un
cadre individuel et temporellement contraint, influence-t-elle les choix techniques et la
trajectoire du produit développé ?

**Objectifs et résultats attendus :** planifier le projet et suivre son exécution ;
documenter les écarts entre planification et réalisation ainsi que les réorientations
survenues ; examiner le poids des contraintes de gestion (temps, ressources, périmètre) sur
les décisions techniques. Livrables : planification comparée (prévisionnelle / réalisée),
journal de bord des points de décision et d'ajustement, mise en perspective des écarts au
regard de la conduite de projet individuelle.

### QR3 — Performance en architecture microservices

**Question :** Dans quelle mesure une architecture microservices permet-elle de maintenir
un comportement satisfaisant du système sous sollicitation concurrente, dans un
environnement aux ressources matérielles restreintes ?

**Objectifs et résultats attendus :** caractériser le comportement du système et de ses
composants sous différentes conditions de sollicitation ; identifier les facteurs
déterminants de la performance dans une architecture distribuée en environnement contraint ;
discuter des marges de manœuvre et limites observées. Livrables : mesures du comportement
sous charge, identification des éléments structurants (techniques ou architecturaux) de la
performance, discussion des limites et des pistes d'évolution.

## Points d'ancrage dans le projet

- **QR1** : `ARCHITECTURE.md` (décisions et compromis), diagrammes des README par service.
- **QR2** : planification et journal de bord à tracer ; écarts à documenter en cours de
  projet.
- **QR3** : [`docs/ingestion-concurrence.md`](ingestion-concurrence.md) (concurrence
  d'ingestion, ressources limitées, pistes de gestion) — première brique des mesures de
  comportement sous charge.
