# Version 0.4.0 — 23 septembre 2026

## Navigation

- Téléphone : boutons persistants « ↑ Début » et « ↓ Fin » au-dessus des épisodes. Conservation de l’épisode visible et de son décalage pendant les rafraîchissements, les retours à l’écran et la recréation de l’activité.
- Android Auto : trois dossiers placés en tête de chaque émission : « ↑ Début · Plus récents », « ↓ Fin · Plus anciens », « Toutes les périodes ». Les huit épisodes récents sont aussi directement accessibles sous ces dossiers.
- Les plus anciens sont présentés dans l’ordre inverse pour rendre le dernier épisode de la sélection immédiatement visible. Les périodes couvrent toute la sélection, par groupes de huit, sans doublons ni omission. Retour revient à l’émission et à ses raccourcis.
- La sélection reste limitée aux 60 épisodes récents du RSS. L’application ne supprime ni ne contourne les avertissements de conduite d’Android Auto.
- Les sauvegardes de progression toutes les cinq secondes ne provoquent plus de notifications de rechargement vers les navigateurs automobiles. Le RSS continue à être actualisé en mémoire ; rouvrir un dossier récupère les données récentes.

## Vérifications

- Compilation de l’application et de ses tests réussie.
- 15 tests JVM réussis : 4 nouveaux tests de navigation, 3 de progression, 7 du parseur et 1 appliqué aux douze snapshots RSS réels. Sans snapshots locaux, ce dernier test est ignoré.
- Analyse Android : 0 erreur, 17 avertissements non bloquants.
- Signature APK vérifiée identique à celle de la version 0.3, pour une mise à jour sans désinstallation.
- Installation finale sur Galaxy S21 SM-G991W, versionCode 4 / versionName 0.4.0, sans effacement de l’historique.
- 3 tests instrumentés réussis : persistance de l’historique, accès aux douze émissions par navigateur média historique, et parcours des dossiers récent/ancien/périodes par ce navigateur. Vérification que toutes les périodes couvrent la sélection sans doublon et que leurs extrémités correspondent aux raccourcis.
- Contrôle manuel sur S21 : « Fin » affiche le dernier épisode de la sélection (21 mai), « Début » revient au 23 septembre ; un passage par l’accueil du téléphone puis retour conserve l’épisode visible et son décalage. Aucun audio lancé pour ces contrôles.

Le rendu réel dans la voiture et le comportement des alertes de conduite restent à vérifier sur l’autoradio. Les dossiers sont exposés par le service média standard ; Android Auto décide de leur présentation et de ses restrictions.

## Dépôt

Le projet conserve la licence GPLv3 déjà présente dans le dépôt de l’utilisateur. `.gitignore` exclut les sorties de compilation, APK, caches, configuration locale du SDK, fichiers d’environnement et clés de signature. Le wrapper Gradle, y compris son JAR et le checksum de sa distribution, est inclus. Aucun historique d’écoute, cliché du téléphone, clé privée ou flux RSS complet n’est publié.
