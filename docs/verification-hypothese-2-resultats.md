## 2.2.2.2 Résultats de vérification de l’hypothèse 2 : « Un développement structuré du système permet d’assurer un accompagnement pédagogique fiable et personnalisé »

### 1. Correspondance entre les cas d’usage et les modules du système

Le développement réalisé a permis de mettre en correspondance les principaux cas d’usage identifiés lors de la conception avec les modules qui assurent leur prise en charge. Le tableau suivant présente cette correspondance ainsi que la réalisation observée pour chaque fonctionnalité.

| Cas d’usage                         | Modules concernés                                                                   | Réalisation constatée                                                                                                                                                                                         |
| ----------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Se connecter / créer un compte      | Portail web, Gestion des identités                                                  | L’utilisateur peut créer un compte ou se connecter avant d’accéder à ses espaces.                                                                                                                             |
| Consulter ses espaces               | Portail web, Gestion des espaces                                                    | Les espaces de cours associés à l’utilisateur sont affichés avec son rôle et ses groupes.                                                                                                                     |
| Rejoindre un espace                 | Portail web, Gestion des espaces                                                    | L’étudiant peut rejoindre un espace à l’aide d’un code d’invitation.                                                                                                                                          |
| Gérer un espace et ses membres      | Portail web, Gestion des espaces                                                    | L’enseignant peut créer un espace, gérer ses membres et ses groupes, régénérer le code d’invitation et gérer le persona pédagogique.                                                                          |
| Consulter les documents d’un espace | Portail web, Ingestion des contenus                                                 | Les documents déposés sont consultables avec leur état de traitement.                                                                                                                                         |
| Déposer un support de cours         | Portail web, Ingestion des contenus                                                 | Les documents sont enregistrés, convertis en contenu structuré, découpés puis préparés pour la recherche. Le traitement peut être suivi et relancé en cas d’échec.                                            |
| Poser une question sur un cours     | Portail web, Assistant conversationnel, Gestion des espaces, Ingestion des contenus | Les passages pertinents du corpus de l’espace sont recherchés, puis une réponse est générée à partir du contexte, du persona et de l’historique de la conversation. Les sources sont conservées et affichées. |
| Générer une fiche de révision       | Portail web, Atelier de révision                                                    | Une fiche structurée est générée à partir des documents de l’espace, avec réutilisation des résumés déjà produits.                                                                                            |
| Consulter une fiche                 | Portail web, Atelier de révision                                                    | Les fiches disponibles dans l’espace peuvent être consultées.                                                                                                                                                 |
| Annoter une fiche                   | Portail web, Atelier de révision                                                    | L’étudiant peut ajouter ses propres annotations aux fiches de révision.                                                                                                                                       |
| Valider une fiche                   | Portail web, Atelier de révision                                                    | L’enseignant peut valider une fiche ou demander sa reprise.                                                                                                                                                   |
| Générer un quiz                     | Portail web, Atelier de révision                                                    | Un quiz peut être généré à partir d’un document, d’un espace ou d’un sujet, avec un nombre de questions et un niveau de difficulté définis.                                                                   |
| Publier un quiz                     | Portail web, Atelier de révision                                                    | L’enseignant peut publier un quiz initialement conservé à l’état de brouillon.                                                                                                                                |
| Répondre à un quiz                  | Portail web, Atelier de révision                                                    | L’étudiant peut répondre au quiz et obtenir son score ainsi que le détail de ses réponses.                                                                                                                    |
| Consulter les résultats d’un quiz   | Portail web, Atelier de révision                                                    | Les résultats, tentatives et corrections peuvent être consultés.                                                                                                                                              |
| Consulter son tableau de bord       | Portail web, Suivi des apprentissages                                               | L’étudiant dispose d’indicateurs relatifs à sa progression, aux notions maîtrisées et aux difficultés identifiées.                                                                                            |
| Consulter ses recommandations       | Portail web, Suivi des apprentissages                                               | Des recommandations sont générées à partir des activités et des difficultés observées.                                                                                                                        |
| Suivre la progression des étudiants | Portail web, Suivi des apprentissages                                               | L’enseignant peut consulter l’activité de l’espace ainsi que les questions et chapitres faisant l’objet d’une attention particulière.                                                                         |
| Gérer ses objectifs de révision     | Portail web, Motivation et engagement                                               | L’étudiant peut définir des objectifs hebdomadaires et suivre leur progression, avec attribution de badges et rappels.                                                                                        |

La correspondance obtenue montre que les cas d’usage définis lors de la conception disposent d’une prise en charge dans les modules du système. Les fonctionnalités liées à l’accompagnement pédagogique sont principalement réparties entre les modules d’ingestion des contenus, d’assistance conversationnelle, de révision et de suivi des apprentissages.

### 2. Technologies utilisées pour la réalisation des fonctionnalités

Les fonctionnalités développées reposent sur un socle technique commun. Le backend est réalisé avec Java 17 et Spring Boot 3.3.5, tandis que les fonctions faisant intervenir l’intelligence artificielle utilisent Spring AI. La génération de contenu peut s’appuyer sur plusieurs fournisseurs de modèles, notamment Groq, Gemini et Ollama. La recherche des passages dans les documents est assurée par Qdrant, les données métier sont stockées dans PostgreSQL et les fichiers dans MinIO. L’interface utilisateur repose sur React et Vite avec TanStack Router.

| Élément                  | Technologie retenue          | Utilisation                                                                                |
| ------------------------ | ---------------------------- | ------------------------------------------------------------------------------------------ |
| Langage                  | Java 17                      | Développement des services backend.                                                        |
| Framework applicatif     | Spring Boot 3.3.5            | API REST, persistance, validation et documentation des services.                           |
| Intégration de l’IA      | Spring AI 1.1.8              | Génération des personas, réponses, fiches et quiz, ainsi que recherche dans les contenus.  |
| Modèles d’IA             | Groq, Gemini, Ollama         | Génération des contenus avec possibilité de changer de fournisseur selon la configuration. |
| Recherche documentaire   | Qdrant 1.13.0                | Recherche des passages pertinents dans le corpus de l’espace pédagogique.                  |
| Données métier           | PostgreSQL 16, Flyway        | Stockage des données fonctionnelles et gestion des migrations.                             |
| Stockage des fichiers    | MinIO                        | Conservation des supports déposés et des figures extraites.                                |
| Extraction des documents | Docling Worker               | Conversion des documents en contenu structuré avant leur découpage.                        |
| Interface                | React, Vite, TanStack Router | Réalisation des parcours étudiants et enseignants.                                         |

Ces technologies sont associées aux principales règles de fonctionnement des fonctionnalités d’accompagnement. Le dépôt de documents conserve leur rattachement à l’espace et permet de suivre leur état de traitement. Le persona est généré à partir des informations de l’espace puis enrichi à partir des nouveaux documents. Le module conversationnel utilise le persona, l’historique des échanges et les passages issus du corpus de l’espace. Les fiches et les quiz sont générés à partir des contenus disponibles, tandis que les activités alimentent les mécanismes de suivi et de recommandation.

### 3. Mécanismes participant à la personnalisation de l’accompagnement

La personnalisation repose sur plusieurs catégories d’informations associées à l’espace pédagogique et à l’utilisateur. Le persona est d’abord défini à partir du nom, de la matière et de la description de l’espace, puis peut être enrichi à partir du contenu des documents déposés. Les réponses de l’assistant prennent également en compte l’historique de la conversation ainsi que les passages pertinents issus du seul espace concerné.

| Fonctionnalité                     | Informations mobilisées                                                 | Rôle dans la personnalisation                                                          |
| ---------------------------------- | ----------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| Génération initiale du persona     | Nom, matière et description de l’espace                                 | Définition du cadre disciplinaire et du registre des réponses.                         |
| Enrichissement du persona          | Passages issus des nouveaux documents et persona existant               | Adaptation du persona au vocabulaire et au contenu réel des cours.                     |
| Assistant conversationnel          | Persona, passages pertinents, historique, figures associées             | Adaptation de la réponse au contexte de l’espace et aux échanges précédents.           |
| Fiche de révision                  | Passages du corpus et résumés intermédiaires                            | Production d’un contenu de révision correspondant au document ou à l’espace demandé.   |
| Quiz                               | Passages du corpus, nombre de questions, difficulté, sujet et périmètre | Génération d’exercices ciblés selon les paramètres choisis.                            |
| Tableau de bord et recommandations | Questions, fiches, quiz, événements d’activité                          | Identification des difficultés, suivi de progression et production de recommandations. |

La personnalisation observée s’appuie donc sur des informations contextualisées, notamment le contenu de l’espace, le persona associé, l’historique des échanges et les activités réalisées. Les événements enregistrés permettent également d’alimenter les mécanismes de suivi et de recommandation.

### 4. Enchaînement des fonctionnalités et interactions entre les modules

Les diagrammes de séquence réalisés permettent de représenter le fonctionnement des principales fonctionnalités et les interactions entre les modules du système. Cinq séquences ont été retenues : la gestion du persona pédagogique, l’ingestion des supports, l’interrogation de l’assistant, la génération d’une fiche de révision ainsi que le parcours associé aux quiz, à la validation et au suivi.

#### 4.1 Persona pédagogique

**Figure X : Diagramme de séquence de génération et d’enrichissement du persona**

Lors de la création d’un espace, un persona initial est généré à partir des informations fournies par l’enseignant. Lorsque de nouveaux documents sont intégrés, leur contenu peut être utilisé pour enrichir le persona existant. Le système conserve une version du persona afin d’identifier celle utilisée lors du traitement des réponses.

#### 4.2 Ingestion d’un support de cours

**Figure X : Diagramme de séquence de traitement d’un support de cours**

Le traitement d’un document comprend sa conservation, l’extraction de son contenu, son découpage en passages puis leur préparation pour la recherche. Le document reste rattaché à son espace d’origine. Le système conserve également l’état du traitement et le motif d’échec lorsqu’une erreur survient.

#### 4.3 Interrogation de l’assistant conversationnel

**Figure X : Diagramme de séquence d’une question adressée à l’assistant**

Lorsqu’une question est posée, celle-ci est enregistrée puis associée au contexte de l’espace. Les passages pertinents sont recherchés dans le corpus correspondant avant la génération de la réponse. Le résultat conserve les sources utilisées, le modèle mobilisé et la version du persona. Lorsque les documents disponibles ne permettent pas de répondre à la question, le système retourne un message indiquant l’absence d’information dans le corpus au lieu de produire une réponse non fondée.

#### 4.4 Génération d’une fiche de révision

**Figure X : Diagramme de séquence de génération d’une fiche de révision**

La génération d’une fiche s’appuie sur les passages des documents concernés. Des résumés intermédiaires peuvent être réutilisés afin d’éviter une nouvelle génération lorsque ceux-ci sont disponibles. Les différents résultats sont ensuite réunis dans une fiche structurée dont le format est contrôlé. En cas d’échec de la génération, une erreur explicite est retournée.

#### 4.5 Quiz, validation et suivi

**Figure X : Diagramme de séquence des quiz, de la validation et du suivi**

Le quiz est généré selon le périmètre et les paramètres définis. Il est conservé à l’état de brouillon avant sa publication par l’enseignant. La tentative de l’étudiant est ensuite évaluée localement. Les résultats des quiz et les validations de fiches alimentent les mécanismes de suivi et de motivation, notamment la progression, les recommandations, les badges et les objectifs.

### 5. Vérification par les tests automatiques

Les tests automatiques ont permis de vérifier plusieurs règles de fonctionnement indépendamment de l’exécution complète de la plateforme. Les résultats obtenus le 5 octobre 2026 sont présentés dans le tableau suivant.

| Module                 | Élément vérifié                                   | Résultat                |
| ---------------------- | ------------------------------------------------- | ----------------------- |
| `ingestion-service`    | Découpage structurel et découpage de secours      | **26/26 tests réussis** |
| `chat-service`         | Traitement et rendu des réponses                  | **19/19 tests réussis** |
| `analytics-service`    | Logique des indicateurs et détection des doublons | **11/11 tests réussis** |
| `gamification-service` | Objectifs hebdomadaires et idempotence            | **5/5 tests réussis**   |
| `fiche-service`        | Tentatives, corrections et validation             | **8/8 tests réussis**   |
| `fiche-service`        | Génération des quiz                               | **3/4 tests réussis**   |

Au total, **61 tests sur 62 ont été validés**. La seule anomalie relevée concerne un test de génération de quiz devenu incompatible avec la règle actuelle du système : le test attend encore la génération d’un quiz de type `DOCUMENT` sans identifiant de document, alors que le fonctionnement actuel impose cet identifiant pour ce périmètre. L’erreur est donc identifiée et localisée au niveau du test, qui doit être mis à jour.

Ces résultats apportent une première vérification du fonctionnement de plusieurs mécanismes essentiels, notamment le traitement des contenus, le rendu des réponses, le suivi d’activité, la gestion des objectifs, les tentatives de quiz et la validation des fiches.

### 6. Captures d’écran obligatoires (scénarios imposés)

Les captures d’écran suivantes permettent d’illustrer les principaux parcours fonctionnels vérifiés sur la plateforme TsimokaAI. Chaque capture correspond à un état précis de l’application et doit pouvoir être reproduite à partir d’un même jeu de données de référence, constitué notamment d’un espace **Algèbre linéaire** et d’un support de cours au format PDF. Elles sont présentées avec une résolution suffisante pour rendre les informations et les résultats visibles, tandis que les éventuelles informations sensibles ou secrets techniques sont masqués.

L’objectif de ces captures est de mettre en évidence le fonctionnement observable des fonctionnalités développées, aussi bien dans les parcours nominaux que dans certaines situations d’erreur. Elles permettent également d’illustrer la prise en compte du contexte pédagogique, la traçabilité des traitements et les interactions entre les différents modules de la plateforme.

### 6.1 Dépôt et traitement d’un support de cours

**Figure X : Dépôt d’un document PDF et affichage de son traitement**

*[PLACEHOLDER — C1 : écran « Documents » de l’espace Algèbre linéaire après dépôt du PDF, avec le statut du document à l’état prêt et le nombre de passages extraits visible]*

La capture présente le résultat du dépôt d’un support de cours au format PDF dans un espace pédagogique. Le document apparaît comme correctement traité et son état est affiché à l’utilisateur. Le nombre de passages obtenus à partir du document est également visible, ce qui permet de constater que le support a été transformé en unités exploitables par les fonctionnalités reposant sur la recherche dans le corpus. Cette étape constitue la base nécessaire à l’utilisation du document dans l’assistant conversationnel et dans la génération des contenus de révision.

### 6.2 Génération du persona pédagogique

**Figure X : Persona pédagogique généré et version associée**

*[PLACEHOLDER — C2 : écran « Paramètres » avec la fenêtre du persona, affichant le persona généré, sa version et le bouton de régénération]*

La capture illustre la consultation du persona pédagogique associé à l’espace. Celui-ci est présenté à travers une fenêtre dédiée permettant d’identifier le contenu généré ainsi que la version actuellement utilisée. La présence d’un bouton de régénération permet également de relancer sa production lorsque cela est nécessaire. La conservation d’une version permet de distinguer les différents états du persona et de rattacher les réponses produites par l’assistant au contexte de personnalisation utilisé au moment de leur génération.

### 6.3 Interrogation de l’assistant conversationnel

**Figure X : Réponse de l’assistant à une question issue du cours**

*[PLACEHOLDER — C3a : écran « Chat » avec une question portant sur l’algèbre linéaire, la réponse de l’assistant, les citations des passages utilisés et la pastille indiquant la version du persona]*

La capture montre une interaction dans laquelle l’étudiant adresse à l’assistant une question portant sur le contenu du cours. La réponse est accompagnée des citations correspondant aux passages mobilisés dans le corpus documentaire. La présence d’une indication de version permet en outre d’identifier le contexte de personnalisation utilisé lors de la production de la réponse. L’ensemble permet de relier le résultat présenté à la fois au contenu documentaire disponible et au contexte pédagogique de l’espace.

**Figure X : Refus d’une question hors du périmètre documentaire**

*[PLACEHOLDER — C3b : écran « Chat » avec une question hors-cours et un message explicite indiquant que l’information recherchée n’est pas disponible dans le corpus]*

La seconde capture présente un cas dans lequel la question posée ne peut pas être justifiée par les documents disponibles dans l’espace. L’assistant indique explicitement l’absence d’information suffisante dans le corpus au lieu de produire une réponse présentée comme certaine. Ce comportement permet de matérialiser la limite du périmètre documentaire et de réduire le risque de réponses non fondées dans un contexte pédagogique.

### 6.4 Génération et consultation d’une fiche de révision

**Figure X : Fenêtre de génération d’une fiche de révision**

*[PLACEHOLDER — C4a : écran « Fiches » avec la fenêtre de génération et les paramètres de production de la fiche]*

La capture présente l’étape de lancement de la génération d’une fiche de révision. Les paramètres affichés permettent de préciser le contenu ou le périmètre concerné avant de déclencher la génération. Cette étape matérialise l’accès à une production de contenu pédagogique à partir des supports déjà intégrés dans l’espace.

**Figure X : Fiche de révision générée et annotée**

*[PLACEHOLDER — C4b : page de consultation d’une fiche avec son contenu rendu, ses différentes sections et les annotations associées]*

La seconde capture montre le résultat de la génération sous la forme d’une fiche structurée. Le contenu est présenté de manière lisible et les annotations permettent d’apporter des indications complémentaires sur les éléments affichés. La fiche constitue ainsi un support de révision directement exploitable par l’étudiant à partir des documents disponibles dans l’espace pédagogique.

### 6.5 Passation d’un quiz et affichage du résultat

**Figure X : Passation d’un quiz et affichage du score**

*[PLACEHOLDER — C5 : écran « Take » d’un quiz avec les réponses de l’étudiant puis l’état final affichant le score obtenu]*

La capture illustre le parcours de passation d’un quiz par l’étudiant. Après sélection des réponses, celui-ci termine sa tentative et obtient immédiatement le résultat correspondant à son évaluation. Le score affiché constitue une donnée exploitable par les mécanismes de suivi de la plateforme, notamment pour caractériser la progression de l’étudiant et identifier les notions nécessitant davantage de révision.

### 6.6 Validation des fiches par l’enseignant

**Figure X : Validation ou rejet d’une fiche de révision**

*[PLACEHOLDER — C6 : écran « Fiches à valider » de l’espace enseignant avec la fiche sélectionnée et la section permettant sa validation ou son rejet]*

La capture présente le parcours de contrôle d’une fiche générée par la plateforme du point de vue de l’enseignant. Celui-ci peut consulter le contenu proposé puis décider de sa validation ou de son rejet. Cette étape introduit un contrôle humain sur les contenus produits automatiquement et permet de distinguer les fiches effectivement approuvées des contenus encore soumis à vérification.

### 6.7 Suivi pédagogique et recommandations

**Figure X : Tableau de bord de progression de l’étudiant**

*[PLACEHOLDER — C7a : tableau de bord étudiant avec progression, notions faibles ou maîtrisées et recommandations]*

La capture présente les informations de suivi disponibles pour l’étudiant. La progression réalisée, les notions identifiées comme faibles ou maîtrisées ainsi que les recommandations proposées sont regroupées dans un même espace. Ces éléments permettent à l’étudiant d’identifier ses principaux points d’attention et d’orienter ses activités de révision à partir des résultats obtenus.

**Figure X : Tableau de bord pédagogique de l’enseignant**

*[PLACEHOLDER — C7b : tableau de bord enseignant affichant la progression et les indicateurs synthétiques associés aux étudiants de l’espace]*

La capture complémentaire montre le suivi proposé à l’enseignant. Les indicateurs agrégés lui permettent d’observer l’évolution des apprentissages au sein de son espace et d’identifier les notions ou parcours nécessitant une attention particulière. La mise en regard des résultats et des recommandations fournit ainsi un support de suivi complémentaire aux interactions pédagogiques.

### 6.8 Gestion des situations d’erreur

**Figure X : Indisponibilité du modèle de langage dans le module de chat**

*[PLACEHOLDER — C8a : écran « Chat » avec le service LLM volontairement indisponible et un message d’erreur explicite destiné à l’utilisateur]*

Cette capture présente le comportement de la plateforme lorsque le modèle de langage requis pour produire une réponse n’est pas disponible. Le système affiche un message explicite signalant l’indisponibilité du service au lieu de présenter un résultat incomplet ou trompeur. L’utilisateur est ainsi informé de la nature du problème et peut distinguer une erreur de service d’une absence d’information dans le cours.

**Figure X : Document en échec de traitement**

*[PLACEHOLDER — C8b : écran « Documents » avec un document affiché à l’état d’échec et les informations associées au traitement]*

La seconde capture illustre le cas d’un document dont le traitement n’a pas abouti. L’état d’échec est rendu visible depuis l’interface afin que l’utilisateur puisse identifier le support concerné. Cette représentation permet de conserver une information claire sur l’état du traitement et d’éviter qu’un document non exploitable soit considéré comme disponible pour les fonctionnalités dépendantes de son contenu.

**Figure X : Relance du traitement d’un document en échec**

*[PLACEHOLDER — C8c : document en état d’échec avec le bouton permettant de relancer son traitement]*

La dernière capture présente la possibilité de relancer le traitement du document concerné. Le bouton de relance offre une action explicite permettant de tenter à nouveau l’ingestion sans nécessiter un nouveau dépôt du fichier. Ce comportement facilite la récupération après une erreur temporaire et rend la gestion des incidents accessible directement depuis l’interface.

### 8. Vue globale des interactions entre acteurs et modules

Le diagramme global d’interactions synthétise les principaux échanges entre les étudiants, les enseignants, l’interface de la plateforme et les différents modules du système. Il met notamment en évidence les échanges avec la gestion des espaces, l’ingestion des contenus, l’assistant conversationnel, les fonctionnalités de révision, le suivi des apprentissages et la motivation.

**Figure X : Diagramme global d’interactions entre les acteurs et les modules**

Cette représentation permet de visualiser les relations entre les fonctionnalités d’accompagnement et les données nécessaires à leur fonctionnement, notamment les documents, les passages indexés, les données métier et les modèles d’intelligence artificielle. Elle complète ainsi les diagrammes de séquence en offrant une vue synthétique du fonctionnement global du système.

### 9. Synthèse des résultats de vérification

Les résultats obtenus montrent que les principales fonctionnalités définies lors de la conception disposent d’une réalisation dans les modules correspondants. Les mécanismes de personnalisation reposent notamment sur le contexte de l’espace, le persona pédagogique, les contenus documentaires, l’historique des échanges et les données d’activité.

Les tests automatiques constituent une première preuve de fonctionnement, avec **61 validations sur 62** lors de l’exécution du 5 octobre 2026. La seule erreur relevée a été identifiée comme un test obsolète par rapport aux règles actuellement implémentées.

Toutefois, la vérification de l’hypothèse n’est pas encore entièrement documentée par des preuves en environnement complet : les scénarios fonctionnels T1 à T8 ainsi que les captures associées doivent encore être exécutés et intégrés aux résultats finaux.

Ainsi, les éléments actuellement disponibles permettent de constater la réalisation et la structuration des principales fonctionnalités d’accompagnement ainsi qu’une première vérification de leurs règles de fonctionnement. Les validations en environnement complet permettront de compléter cette vérification par des observations du comportement global de la plateforme.
