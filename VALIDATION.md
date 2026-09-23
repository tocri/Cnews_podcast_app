# Validation du 16 septembre 2026

**Mise à jour : consulter [la validation 0.4.0 du 23 septembre](docs/VERSION-0.4.md) pour l’état courant et [la validation 0.3.0](docs/VERSION-0.3.md) pour les essais précédents. Les sections ci-dessous sont l’historique des premières versions.**

## Contrôles effectués

- Premier APK créé avec uniquement 100 % Frontières, puis compilation/test/analyse Android réussis avant extension du catalogue.
- APK final construit avec les douze émissions de `shows.json` : `assembleDebug` réussi.
- `testDebugUnitTest` : 5 tests exécutés, 0 échec, 0 erreur, 0 ignoré lors du contrôle final.
- Quatre tests ciblent les espaces de noms et CDATA, les GUID stables, les dates/tri/limite, les doublons et enclosures invalides, ainsi que la désactivation des entités XML externes.
- Un test supplémentaire applique le véritable parseur Kotlin aux douze flux XML récupérés le jour du contrôle. Les douze produisent des épisodes HTTPS, des dates et des identifiants distincts.
- `lintDebug` : 0 erreur, 16 avertissements. Ils concernent principalement les nouvelles versions disponibles, les chaînes françaises non externalisées, le service média volontairement exporté et les règles de sauvegarde. Les versions utilisées restent épinglées.
- Contrôle HTTP de l’enclosure de 100 % Frontières : réponse 206, type `audio/mpeg`, redirection vers `stitcher2.acast.com`. Seize octets lus pour le contrôle technique, aucun épisode enregistré.
- Recherche de téléphone via ADB : aucun appareil connecté.

## Non validé matériellement

L’application n’a pas été exécutée sur téléphone, émulateur Android ou Desktop Head Unit dans cette session. Il reste donc à vérifier la navigation effective Android Auto, le décodage audio continu, les commandes de notification et au volant, la mise en arrière-plan, les déconnexions, ainsi que la pause/reprise Maps/Waze et les appels. La compilation, la signature et les tests RSS ne constituent pas une certification de compatibilité du véhicule.

Les étapes et résultats attendus figurent dans `README.md`. L’APK livré est une version de développement signée pour installation personnelle, pas une version Google Play.

## Reproduire le test des flux réels

Les snapshots RSS complets ne sont pas distribués avec le projet. Les télécharger dans un dossier de travail en utilisant les adresses de `docs/feeds-verified.json` (un fichier `.xml` par émission), puis lancer :

```powershell
.\gradlew.bat testDebugUnitTest "-PrssFixtures=C:/chemin/vers/les/flux"
```

Sans ce paramètre, les quatre tests autonomes s’exécutent et le test des snapshots est indiqué comme ignoré. Les tests n’émettent aucune requête réseau par eux-mêmes.

## Mise à jour 0.2.0 — épisodes écoutés

Compilation, tests autonomes et analyse Android réussis. Installation par mise à jour sur le Galaxy S21 SM-G991W réussie. Contrôle sur téléphone : appui long sur le premier épisode de 100 % Frontières, apparition de « Écouté », conservation après arrêt et relance du processus, puis annulation du marquage de test vérifiée. Le déclenchement automatique à la fin du média est implémenté via les événements ExoPlayer ; une écoute intégrale et le rendu Android Auto restent à tester. Les contrôles matériels ci-dessus remplacent la mention initiale d’absence totale de test sur téléphone, pour ces seuls scénarios.
