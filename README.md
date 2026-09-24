# 🎙️ CNEWS Podcast pour Android & Android Auto

Cette application personnelle, développée en **Kotlin**, permet d’écouter simplement les podcasts de **CNEWS** depuis un téléphone Android et **Android Auto**.

## 💡 Pourquoi cette application ?

Le projet est né d’une frustration toute simple : je voulais pouvoir écouter les podcasts de la chaîne en voiture, mais il n’existait pas d’application Android Auto répondant à ce besoin.

Pour lancer un podcast, je devais prendre mon téléphone, ouvrir l’application, sélectionner une thématique, rechercher le podcast puis démarrer la lecture. Autant de manipulations que je trouvais peu pratiques et surtout **inadaptées à une utilisation en voiture**.

J’ai donc décidé de créer ma propre application avec un objectif simple :

> **Accéder rapidement aux podcasts et les écouter depuis Android Auto avec un minimum d’interactions.**

Grâce aux possibilités offertes aujourd’hui par **l’IA générative et ChatGPT**, j’ai pu transformer ce besoin personnel en une véritable application Android, alors que développer seul un tel projet aurait été beaucoup plus difficile pour moi il y a encore quelques années.

## 📱 Distribution

L’application est proposée **gratuitement** sous licence *GPL3** et n’est pas distribuée sur le Google Play Store.

## ⚠️ Avertissement

> **Projet indépendant et non officiel.**
>
> CNEWS n’est ni associé, ni affilié, ni impliqué dans le développement de cette application. Les marques, noms et contenus associés à CNEWS restent la propriété de leurs détenteurs respectifs.

## Installation sur Android

1. Télécharger l'archive zip [Release 0.4.1](https://github.com/tocri/Cnews_podcast_app/releases/tag/0.41)
2. Décompresser l'archive afin d'obtenir le .apk
3. Copier l'apk dans votre android
4. Ouvrir l'apk à partir de votre téléphone.

Un message d'avertissement va apparaître. Il suffit de continuer

## Installation sur Android auto

1. Sur votre téléphone, aller dans Paramètres et chercher Android Auto (🔎).
2. Tout en bas, appuyer 10 fois sur version pour activer les paramètres développeur.
3. Ouvrir le menu ⋮ en haut à droite d'Android Auto.
4. Aller dans Paramètres pour développeurs.
5. Activer Sources inconnues.
6. Reconnecter Android Auto à la voiture

## Fonctionnalités

Il existe 2 versions de l'application. L'application sur le téléphone qui est assez rudimentaire du point de vue graphique et l'application Android Auto.

**Fonctionnalités communes :**

* Les podcast sont listés par thématique
* Possible d'atteindre le haut ou le bas de la liste des podcast en 1 clic
* Fonction de resume. Le podcast reprend là où il s'est arrêté même après une déconnexion d'android auto
* Le podcast se met en pause lorsqu'une autre application prend la parole (type logiciel GPS) et reprend quand c'est terminé

**Sur le téléphone :**

* Possible de marquer un podcast comme lu en laissant le doigt sur le podcast - important car cela permet de synchroniser le statut sur Android Auto



## Images

## 📸 Captures d'écran

|  **Application mobile**| **Interface Android Auto** |
|:---:|:---:|
| <img src="docs/podcast_cnews_ecran android.jpg" width="300" alt="Application Android"> <br> <img src="docs/podcast_cnews_ecoute.jpg" width="300" alt="Application Android"> | <img src="docs/podcast_cnews_statut.jpg" width="300" alt="Application Android"> <br> <img src="docs/podcast_cnews_defilement.jpg" width="300" alt="Application Android"> <br> <img src="docs/podcast_cnews_thematiques.jpg" width="300" alt="Application Android"> <br> <img src="docs/podcast_cnews_lecture.jpg" width="300" alt="Application Android"> <br> <img src="docs/podcast_cnews_statut_non_ecoute.jpg" width="300" alt="Application Android">|



## Développer avec Visual Studio Code

Le [guide Visual Studio Code](docs/VISUAL-STUDIO-CODE.md) explique la préparation du SDK sans Android Studio, le clonage du dépôt, les fichiers à modifier, la compilation et l’installation de l’APK.

Une fois Java 17 et le SDK Android 35 configurés, ouvrir le dossier du projet dans VS Code et appuyer sur **Ctrl+Maj+B** pour lancer **Générer l’APK**. La tâche **Vérifier le projet** lance les tests locaux et l’analyse Android. Les tâches sont incluses dans `.vscode/tasks.json` ; aucune extension n’est indispensable à la compilation.

Le fichier produit est `app/build/outputs/apk/debug/app-debug.apk`. Le projet utilise Gradle 8.13, Android Gradle Plugin 8.10.1, Kotlin 2.1.21 et Media3 1.8.0. Ces versions sont épinglées pour reproduire la compilation.


Pour tester au bureau avec un téléphone Android et le simulateur d’autoradio officiel : [Desktop Head Unit](https://developer.android.com/training/cars/testing/dhu). Il ne s’agit pas d’une application Android Automotive OS à installer directement dans un véhicule.


## Structure

```text
app/src/main/
  AndroidManifest.xml              Permissions, service média et déclaration Android Auto
  assets/shows.json                Registre des flux Acast vérifiés
  java/fr/perso/cnewsauto/
    RssParser.kt                   Titres, dates, GUID et enclosures HTTPS
    CatalogRepository.kt           Réseau hors thread principal, cache mémoire, identifiants
    PlaybackService.kt             MediaLibrarySession, ExoPlayer, focus et navigation Auto
    MainActivity.kt                Interface minimale du téléphone avec MediaBrowser
  res/xml/automotive_app_desc.xml   Capacité média Android Auto
app/src/test/                      Tests RSS hors appareil
docs/SOURCES.md                    Analyse et registre des sources vérifiées
```


