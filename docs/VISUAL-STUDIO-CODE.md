# Développer avec Visual Studio Code (Windows)

Pour **utiliser l’application**, il suffit d’ouvrir le fichier APK sur le téléphone : aucun outil de développement n’est nécessaire. Cette procédure concerne uniquement la modification et la compilation du code.

## 1. Préparer le poste une seule fois

Installer Visual Studio Code, Git et un JDK 17. Définir la variable d’environnement utilisateur `JAVA_HOME` avec le dossier du JDK (sans `/bin`) et ajouter `%JAVA_HOME%\bin` au `Path`. Fermer puis rouvrir VS Code après modification des variables.

Le SDK Android est nécessaire, mais pas l’éditeur Android Studio. Si le SDK est déjà installé, réutiliser son dossier. Sinon, télécharger les **Command line tools only — Windows** depuis la [page Android](https://developer.android.com/studio#command-tools). Extraire leur contenu pour obtenir cette structure, par exemple :

```text
C:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat
C:\Android\Sdk\cmdline-tools\latest\lib\...
```

Dans PowerShell, installer les composants utilisés par ce projet et accepter leurs licences après lecture :

```powershell
& 'C:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat' --sdk_root=C:\Android\Sdk 'platforms;android-35' 'build-tools;35.0.0'
& 'C:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat' --sdk_root=C:\Android\Sdk --licenses
```

Cette procédure utilise l’outil `sdkmanager` encore documenté par Google ; Google recommande désormais son nouvel Android CLI pour la gestion du SDK. Si les outils téléchargés demandent une version de Java plus récente, utiliser cette version pour leur installation, puis revenir au JDK 17 pour compiler ce projet. Voir la [documentation SDK officielle](https://developer.android.com/tools/sdkmanager).

## 2. Ouvrir le projet

Dans VS Code : **Ctrl+Maj+P → Git: Clone**, saisir `https://github.com/tocri/Cnews_podcast_app.git`, choisir un dossier, puis **Ouvrir**. Si le projet est déjà cloné, utiliser **Fichier → Ouvrir un dossier** et sélectionner celui qui contient `gradlew.bat` et `settings.gradle.kts`.

Créer à la racine le fichier `local.properties` avec le chemin réel du SDK. Exemple :

```properties
sdk.dir=C:/Android/Sdk
```

Ce fichier reste local et est exclu de Git. Il ne faut pas copier le chemin du SDK d’un autre ordinateur. Aucune extension VS Code n’est indispensable pour compiler ; une extension Kotlin est facultative pour l’aide à l’édition.

Dans **Terminal → Nouveau terminal**, vérifier le JDK :

```powershell
java -version
.\gradlew.bat --version
```

Gradle doit utiliser Java 17. Le wrapper fourni télécharge automatiquement Gradle 8.13 à la première utilisation ; Internet est nécessaire pour les dépendances. Il n’est pas nécessaire d’installer Gradle séparément.

## 3. Modifier l’application

| Fichier | Rôle |
|---|---|
| `app/src/main/java/fr/perso/cnewsauto/MainActivity.kt` | Écran et boutons du téléphone |
| `app/src/main/java/fr/perso/cnewsauto/CatalogRepository.kt` | Catalogue et dossiers Android Auto |
| `app/src/main/java/fr/perso/cnewsauto/EpisodeNavigation.kt` | Groupes de huit épisodes |
| `app/src/main/java/fr/perso/cnewsauto/PlaybackService.kt` | Lecture, reprise et service média |
| `app/src/main/assets/shows.json` | Émissions et adresses RSS |
| `app/build.gradle.kts` | Versions et dépendances |

Avant une nouvelle livraison, augmenter `versionCode` et adapter `versionName` dans `app/build.gradle.kts`.

## 4. Générer l’APK dans VS Code

**Ctrl+Maj+B** lance la tâche fournie **Générer l’APK**. Attendre le message `BUILD SUCCESSFUL` dans le terminal. Le fichier à récupérer est :

```text
app/build/outputs/apk/debug/app-debug.apk
```

Pour les vérifications, utiliser **Terminal → Exécuter la tâche → Vérifier le projet**. Ces tâches utilisent les [tâches intégrées de VS Code](https://code.visualstudio.com/docs/debugtest/tasks).

On peut aussi saisir les commandes dans le terminal :

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
```

Ce parcours couvre l’édition, la compilation et les tests locaux. Il ne configure pas le débogage interactif Android avec points d’arrêt. Les essais réels de l’interface et d’Android Auto se font sur le téléphone et dans la voiture à l’arrêt.

## 5. Installer sans ADB

1. Copier l’APK sur le téléphone (transfert de fichiers USB, stockage partagé, ou téléchargement depuis un lien fourni).
2. Sur le Samsung, ouvrir **Mes fichiers → Téléchargements** ou le dossier de destination.
3. Toucher le fichier `.apk`. Si Android le demande, autoriser l’application qui ouvre le fichier à installer des applications.
4. Choisir **Installer** ou **Mettre à jour**, puis ouvrir **Podcasts CNEWS · Perso**.
5. Pour l’affichage dans la voiture, suivre la rubrique Android Auto du README : son option « Sources inconnues » est un réglage distinct.

Ni ADB, ni débogage USB/Wi-Fi, ni VS Code ne sont nécessaires pour l’utilisateur qui installe l’APK.

## 6. Préserver les mises à jour et publier le code

L’APK de développement est signé automatiquement. Pour mettre à jour une installation existante et garder son historique, il faut **la même clé de signature**. Sur le poste d’origine, elle est normalement dans `%USERPROFILE%\.android\debug.keystore`. En conserver une sauvegarde privée ; ne jamais la pousser sur GitHub. Une compilation sur un autre poste peut produire une clé différente et Android refusera alors la mise à jour. Ne pas désinstaller l’application pour contourner ce refus si l’on veut conserver ses données.

Dans le panneau **Contrôle de code source** de VS Code, examiner les modifications, les indexer, écrire un message, créer un commit puis synchroniser avec GitHub. Le `.gitignore` exclut le SDK local, les caches, les APK et les clés. Le fichier `.vscode/tasks.json` est partagé ; les réglages personnels de VS Code restent locaux.
