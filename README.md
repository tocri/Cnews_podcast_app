# Podcasts CNEWS · Perso

Application personnelle Kotlin pour écouter les podcasts CNEWS sur téléphone et Android Auto. Aucun compte, aucune publication Google Play, aucun téléchargement d’épisode sur disque. Application indépendante, non officielle.

## Utilisation

Version 0.4.0 : sur téléphone, les boutons **↑ Début** et **↓ Fin**, toujours accessibles au-dessus des épisodes, sautent aux deux extrémités. La position dans la liste est conservée pendant les rafraîchissements.

Dans Android Auto, chaque émission commence par **↑ Début · Plus récents**, **↓ Fin · Plus anciens** et **Toutes les périodes**, suivis des huit épisodes récents. « Plus anciens » présente les huit derniers de la sélection, du plus ancien au plus récent : l’extrémité de la liste est donc immédiatement accessible. Les périodes permettent de retrouver tous les autres épisodes par groupes de huit. La touche Retour d’Android Auto revient aux raccourcis de l’émission. La sélection reste limitée aux 60 épisodes récents disponibles dans le flux ; il ne s’agit pas de l’archive intégrale.

Ces raccourcis sont des dossiers média natifs, pas des commandes de défilement ajoutées à l’autoradio. Les alertes et restrictions de conduite restent contrôlées par Android Auto. Les sauvegardes de progression ne rechargent plus sa liste en cours de consultation ; les métadonnées actualisées apparaissent à la prochaine ouverture du dossier.

Version 0.3.0 : durée totale dans la liste, pochette de l’épisode (ou de son émission), mémorisation de la position et reprise sur téléphone/Android Auto. Le téléphone affiche un bouton « Reprendre » à côté de chaque épisode commencé, ainsi qu’une barre déplaçable entre ▶ et Ⅱ. Les boutons −15 s et +30 s sont conservés. Les commandes s’activent après sélection ; le déplacement nécessite que le lecteur connaisse la durée et puisse rechercher dans le média.

La position est sauvegardée toutes les cinq secondes et lors des pauses/changements de position ou d’épisode. Une interruption brutale peut donc faire perdre les dernières secondes. Au retour, toucher l’épisode ou « Reprendre » repart à la position mémorisée ; la racine du catalogue propose aussi la dernière écoute inachevée. La reconnexion ne lance pas de son d’elle-même : l’utilisateur ou Android Auto commande la lecture. Dans la voiture, l’interface est celle d’Android Auto, avec le statut « en cours » et la reprise au clic ; elle n’affiche pas nécessairement le même bouton que sur téléphone.

Les épisodes arrivés à la fin sont mémorisés comme écoutés et grisés sur le téléphone. Un appui long ouvre les actions « Marquer comme écouté/non écouté » et « Recommencer au début ». Une simple ouverture, une pause ou une erreur réseau ne marque pas un épisode comme terminé. Atteindre la fin après une avance manuelle compte comme une fin de lecture. Les épisodes terminés repartent du début si on les sélectionne de nouveau.

La durée RSS est affichée avant lecture et la durée réelle fournie par le lecteur est ensuite retenue. L’insertion publicitaire Acast peut modifier la durée et le contenu entre deux sessions ; une position chronologique ne garantit donc pas exactement la même phrase si la publicité a changé. Une image absente ou inaccessible conserve l’icône de secours. La photo est transmise aux métadonnées système pour Android Auto ; le recadrage appartient à l’autoradio.

Android Auto reçoit le statut standard « entièrement lu » et la mention « Écouté » ; la couleur exacte et le dessin de l’indicateur dépendent de l’autoradio. L’historique est local au téléphone, conservé lors des mises à jour, et supprimé si les données de l’application sont effacées ou l’application désinstallée. Les écoutes antérieures à la version 0.2.0 ne peuvent pas être reconstituées automatiquement.

Ouvrir l’application → choisir une émission → toucher un épisode. La lecture commence immédiatement. Android Auto fournit sa propre interface média : émissions, épisodes, puis commandes de lecture. Le téléphone affiche aussi les commandes pause/reprise, −15 s et +30 s.

Les listes sont récupérées par RSS Acast, limitées aux 60 épisodes les plus récents et mises en cache en mémoire pendant cinq minutes. Les métadonnées des épisodes sélectionnés et leur position sont conservées localement pour la reprise, même s’ils quittent cette liste récente (sous réserve que l’audio soit encore accessible chez Acast). Aucun audio n’est enregistré sur disque. Un nouvel accès recharge une liste périmée. Une liste à laquelle un client est abonné est également actualisée en mémoire toutes les cinq minutes tant que le service fonctionne ; dans Android Auto, rouvrir le dossier affiche ces données sans interrompre le défilement en cours.

La liste des émissions est une sélection vérifiée dans `app/src/main/assets/shows.json`. Les **nouveaux épisodes** apparaissent sans mise à jour de l’application. L’ajout automatique d’une émission entièrement nouvelle n’est pas inclus : aucun annuaire RSS global CNEWS stable n’a été établi. Une telle émission doit être ajoutée au registre puis l’APK recompilé. Aucun scraping CNEWS n’est exécuté sur le téléphone.

## Construire l’APK

1. Installer Android Studio et ouvrir ce dossier comme projet.
2. Installer le SDK Android 35 et les Build Tools 35.0.0 dans le gestionnaire de SDK. Utiliser Java 17 pour Gradle.
3. Laisser Android Studio créer `local.properties` avec le chemin de votre SDK ; ne pas reprendre un chemin provenant d’un autre ordinateur.
4. Dans le terminal du projet sous Windows :

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Sous macOS/Linux : `sh ./gradlew assembleDebug testDebugUnitTest lintDebug`.

Le dépôt exclut `local.properties`, les caches, les APK et les clés de signature. Pour mettre à jour une installation existante sans perdre son historique, conserver en lieu sûr la même clé de signature sur la machine de compilation. Une compilation sur une autre machine peut créer une nouvelle clé de développement, incompatible avec la mise à jour de l’installation précédente.

L’APK est créé dans `app/build/outputs/apk/debug/app-debug.apk`. La configuration utilise Gradle 8.13, Android Gradle Plugin 8.10.1, Kotlin 2.1.21 et Media3 1.8.0. Les versions sont épinglées pour reproduire cette compilation ; elles ne prétendent pas être les dernières disponibles.

## Installer sur un téléphone

L’application exige Android 8 minimum ; Android Auto a ses propres exigences de version et de compatibilité matérielle.

Transférer l’APK sur le téléphone et l’ouvrir depuis le gestionnaire de fichiers. Autoriser ce gestionnaire à installer l’application si Android le demande. Autre méthode : activer le débogage USB, brancher le téléphone, accepter la connexion sur son écran puis lancer :

```powershell
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Ouvrir une fois « Podcasts CNEWS · Perso » et vérifier un épisode avec Internet. Les médias sont lus en streaming ; le forfait de données et les éventuelles publicités du flux Acast s’appliquent. Une liste déjà chargée peut rester visible si le réseau tombe, mais cela ne rend pas l’audio disponible hors connexion.

## Faire apparaître l’application dans Android Auto

1. Ouvrir les paramètres Android Auto du téléphone.
2. En bas, ouvrir les informations de version, puis toucher dix fois la zone version/informations pour activer le mode développeur.
3. Dans le menu à trois points, ouvrir les paramètres développeur et cocher **Sources inconnues**. Cette option Android Auto est distincte de l’autorisation Android d’installer un APK.
4. Vérifier la présence de « Podcasts CNEWS · Perso » dans la personnalisation du lanceur Android Auto, puis reconnecter le téléphone à la voiture.
5. Choisir l’émission puis l’épisode depuis l’écran de la voiture.

Google documente cette exception pour les applications média installées personnellement : [tests Android Auto](https://developer.android.com/training/cars/testing). Les libellés peuvent varier suivant le téléphone. Aucun compte développeur Play n’est nécessaire à cette méthode.

Pour tester au bureau avec un téléphone Android et le simulateur d’autoradio officiel : [Desktop Head Unit](https://developer.android.com/training/cars/testing/dhu). Il ne s’agit pas d’une application Android Automotive OS à installer directement dans un véhicule.

## Validation sur téléphone et autoradio

Effectuer les manipulations de test à l’arrêt.

| Scénario | Résultat attendu |
|---|---|
| Lancer depuis Android Auto avant d’ouvrir l’interface téléphone | Catalogue disponible sans connexion à un compte |
| Choisir 100 % Frontières puis le dernier épisode | Lecture en streaming sans confirmation ni écran intermédiaire ajouté par l’application |
| Éteindre l’écran du téléphone | Lecture et commandes de notification/autoradio maintenues |
| Déclencher une instruction Maps/Waze | Pause pendant la perte temporaire de focus, reprise après restitution |
| Faire pause soi-même pendant l’interruption | Aucune reprise automatique après l’instruction |
| Une autre application prend définitivement le focus | Pas de reprise automatique intempestive |
| Débrancher le casque ou perdre la sortie audio | Pause ; vérifier aussi la déconnexion Android Auto sur le véhicule utilisé |
| Couper puis rétablir Internet | Erreur compréhensible et possibilité de relancer la lecture |
| Revenir après plus de cinq minutes | RSS relu ; nouveaux épisodes visibles s’ils ont été publiés |
| Changer rapidement d’émission/épisode | Pas de liste d’une autre émission ni de lecture concurrente |

Le focus est géré par ExoPlayer avec `USAGE_MEDIA`, `CONTENT_TYPE_SPEECH` et gestion automatique activée. Media3 traite la perte temporaire autorisant le ducking comme une interruption de parole et reprend au gain de focus. Cela dépend d’une demande de focus effective par l’application GPS ; une instruction qui ne demande aucun focus ne peut pas être détectée de façon générale.

Une compilation et des tests JVM ne valident pas le comportement d’un autoradio réel : voir `VALIDATION.md` pour la distinction entre contrôles effectués et essais matériels restant à faire.

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

Un unique lecteur appartient au service, jamais à l’écran du téléphone. Le navigateur téléphone et Android Auto utilisent la même session. Les identifiants d’épisodes dérivent du GUID RSS et non des URL signées. Le service résout les identifiants demandés vers les liens `enclosure` ; les redirections HTTP sont laissées au lecteur et ne sont pas enregistrées.

La recherche média accepte le nom d’une émission et propose son dernier épisode (jusqu’à trois émissions correspondantes). La transmission d’une commande vocale par l’assistant doit être vérifiée sur l’appareil ; aucune reconnaissance vocale propre à l’application n’est ajoutée.

Limites du MVP : pas de favoris, téléchargements ni historique chronologique. Les flux peuvent retirer d’anciens épisodes ; seuls les 60 récents sont exposés dans chaque émission. Les positions antérieures à la version 0.3.0 ne peuvent pas être reconstituées. Le catalogue n’inclut pas automatiquement toutes les émissions de la chaîne.
