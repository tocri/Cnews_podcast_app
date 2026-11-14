# Version 0.4.2 — 14 novembre 2026

## Repère de lecture

Dans Android Auto, la ligne sous le titre indique **🔵 En cours** lorsqu’un épisode a une position mémorisée supérieure à zéro et n’est pas terminé. Le repère reste présent pendant une pause et après une reconnexion : il signifie « commencé, à terminer », pas nécessairement « le son joue actuellement ». Un épisode terminé conserve **🟢 Lu**. Un épisode jamais commencé conserve le nom d’émission.

Le mécanisme est identique au repère vert confirmé sur l’autoradio par l’utilisateur. Les symboles appartiennent au texte fourni à Android Auto. La position est enregistrée notamment toutes les cinq secondes ; quitter puis rouvrir un dossier affiche le statut actualisé. Aucun rafraîchissement répétitif de la liste automobile n’a été ajouté.

## Ordre chronologique

- Android Auto : **émission → Toutes les périodes → Du plus récent au plus ancien / Du plus ancien au plus récent → période → épisode**. Les périodes et leurs épisodes suivent tous deux l’ordre choisi. Les raccourcis existants Plus récents et Plus anciens restent disponibles.
- Téléphone : bouton **Trier**, à côté de Début et Fin. Le choix est mémorisé sur le téléphone. L’en-tête indique le sens du tri et un changement replace la liste au début.
- Le tri téléphone et le choix du dossier automobile sont indépendants. Les données et l’historique d’écoute restent partagés.
- Il s’agit d’un tri, pas d’une exclusion : tous les épisodes de la sélection restent accessibles, sans doublon. La sélection reste limitée aux 60 derniers épisodes du flux, avec des groupes de huit dans la voiture.

## Vérification en voiture

Compilation de l’application et des tests instrumentés réussie. Les 16 tests JVM passent, dont la couverture des périodes ascendantes et descendantes sans omission, ainsi que les douze snapshots RSS. L’analyse Android ne signale aucune erreur. La signature de l’APK a été vérifiée identique à celle de la version 0.4.1.

À l’arrêt, ouvrir un épisode commencé mais non terminé : sa ligne doit indiquer 🔵 En cours. Marquer un autre épisode terminé sur le téléphone puis rouvrir son dossier dans Android Auto : il doit indiquer 🟢 Lu. Vérifier ensuite les deux sens de tri dans Toutes les périodes, notamment la première et la dernière période.

Cette version n’a pas été installée ni testée sur un appareil dans cette session. Le rendu du nouveau symbole bleu et des menus doit être confirmé sur l’autoradio. L’APK est livré pour une installation manuelle, avec la même clé que les versions précédentes afin de préserver les données.
