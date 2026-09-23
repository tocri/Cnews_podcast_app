# Version 0.3.0 — 20 septembre 2026

## Changements

- Durée totale de chaque épisode issue du RSS, puis mise à jour avec la durée réelle du lecteur.
- Pochette de l’épisode, avec repli sur celle de l’émission. L’image est fournie au téléphone, à la notification média et à Android Auto via les métadonnées Media3.
- Position sauvegardée toutes les cinq secondes, lors d’une pause et lors d’un déplacement manuel. Reprise par épisode après fermeture du processus.
- Bouton « Reprendre » dans la liste du téléphone ; raccourci vers la dernière écoute inachevée à la racine. Android Auto reprend la position lors de la sélection de l’épisode et utilise son interface native.
- Barre de progression interactive avec lecture à gauche, pause à droite, temps courant/total ; −15 s et +30 s conservés. Commandes désactivées sans média sélectionné.
- À la perte d’une connexion Android Auto signalée par `CarConnection`, pause et sauvegarde. Aucun démarrage spontané à la reconnexion par l’application.
- L’historique « Écouté » de la version précédente est conservé. Un appui long donne accès au marquage et à « Recommencer au début ».
- Aide pour afficher l’application dans Android Auto.

## Android Auto

L’utilisateur a confirmé le 20 septembre que l’application apparaît maintenant dans le lanceur. Le manifeste média était déjà déclaré dans les versions précédentes ; cette livraison ne prétend pas avoir corrigé un défaut de déclaration. La visibilité d’un APK personnel dépend notamment des réglages Android Auto « Sources inconnues » et « Personnaliser le lanceur ».

Le bouton de reprise, les couleurs, l’agencement du lecteur et le recadrage de la pochette sur l’autoradio sont contrôlés par Android Auto. La nouvelle barre personnalisée concerne l’interface téléphone.

## Vérifications

- Compilation APK et APK de tests, tests JVM et analyse Android réussis ; 0 erreur lint (avertissements non bloquants conservés dans le rapport).
- 11 tests JVM réussis : formats de durée, images d’épisode/émission, données invalides, parseur RSS, bornage des positions et douze snapshots RSS réels du 16 septembre.
- 2 tests instrumentés réussis sur le Galaxy S21 SM-G991W, Android 15 : persistance des positions/métadonnées/complétions, et accès aux douze émissions par un navigateur média historique sans ouvrir l’activité téléphone.
- Vérification manuelle sur S21 : durée 1:34:17 sur l’épisode du 18 septembre de 100 % Frontières, pochette visible, commandes initialement désactivées puis activées après sélection, lecture, pause, déplacement de la barre, arrêt forcé puis relance, raccourci de reprise et reprise effective vers 6:58 (7:00 quelques secondes après démarrage).
- L’APK a été installé en mise à jour, sans désinstaller ni effacer les données utilisateur.

Le rendu de la nouvelle pochette sur l’autoradio, la déconnexion réelle de la voiture et la pause/reprise GPS avec cette version restent à vérifier en véhicule. Les positions d’écoute antérieures à 0.3.0 ne sont pas récupérables. Une publicité Acast différente peut décaler la correspondance entre un temps mémorisé et une phrase du podcast.

## Reproduire les contrôles

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug "-PrssFixtures=C:/chemin/vers/rss-fixtures"
.\gradlew.bat connectedDebugAndroidTest
```

Les tests instrumentés utilisent un identifiant de test isolé, nettoyé après exécution, et rétablissent l’identifiant de dernière écoute. Ils ne démarrent pas de lecture audio. Sans dossier de snapshots RSS, le test de flux réels est ignoré et les dix autres tests JVM restent autonomes.
