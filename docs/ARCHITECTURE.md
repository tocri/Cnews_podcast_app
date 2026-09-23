# Architecture du MVP

## Flux de fonctionnement

```text
Android Auto (interface média native) ─┐
                                      ├→ MediaLibrarySession → ExoPlayer → sortie audio
Téléphone (Activity + MediaBrowser) ───┘         │
                                               └→ CatalogRepository → RSS HTTPS Acast
```

Le service démarre indépendamment de l’activité. La racine renvoie immédiatement les émissions embarquées. Une émission est un dossier navigable ; un épisode est un élément lisible. Les clients reçoivent le titre et la date, sans description. Le clic d’un épisode transmet son identifiant au service ; celui-ci résout l’enclosure puis prépare et démarre la lecture via Media3. Android Auto garde la maîtrise de son écran de lecture.

## Récupération

Le XML est téléchargé sur le répartiteur IO avec limites de temps et de taille. Chaque émission possède son verrou, ce qui évite qu’une requête lente bloque les autres émissions. Le parseur SAX respecte les espaces de noms (un `itunes:title` ne remplace pas le titre RSS), décode CDATA et entités XML ordinaires, ignore les épisodes sans enclosure HTTPS, déduplique les GUID et trie les dates.

Les listes RSS restent en mémoire pendant la vie du service. Après cinq minutes, elles sont rechargées. Les abonnements MediaLibrary sont actualisés périodiquement et les clients sont notifiés. En cas d’échec de rafraîchissement, une liste déjà connue reste disponible ; aucun fichier audio n’est sauvegardé. Les métadonnées des épisodes sélectionnés et les positions sont conservées dans SharedPreferences pour la reprise après arrêt du processus. Le registre de complétion 0.2.0 est conservé sans migration destructive.

`onSetMediaItems` résout les identifiants puis choisit la position sauvegardée lorsque le client n’a pas fourni une position explicite. `onPlaybackResumption` reconstruit le dernier média après démarrage à froid, notamment pour un bouton média. Une commande explicite de reprise à zéro reste prioritaire. ExoPlayer conserve son rôle unique de lecteur ; aucune lecture ne démarre au simple chargement du catalogue.

Le parseur lit `itunes:duration` (secondes, mm:ss ou hh:mm:ss), `itunes:image` de l’épisode et la pochette de l’émission comme repli. La durée et l’URI de l’image sont transmises dans MediaMetadata : Media3 alimente ainsi aussi les métadonnées des clients historiques et de la notification. L’activité charge sa miniature hors thread principal avec limitation de taille et cache mémoire.

L’identité d’un épisode est `identifiant-emission/SHA256(guid)`. Si le GUID manque, le chemin de l’enclosure sert de repli. Une URL temporaire issue d’une redirection ne devient jamais un identifiant ou une configuration. ExoPlayer ouvre l’enclosure Acast et suit ses redirections à chaque nouvelle ouverture HTTP.

## Audio focus

ExoPlayer reçoit `USAGE_MEDIA`, `AUDIO_CONTENT_TYPE_SPEECH` et `handleAudioFocus=true`. Son gestionnaire de focus suspend la lecture sur une perte temporaire, y compris `LOSS_TRANSIENT_CAN_DUCK` pour du speech. Au gain, la lecture reprend si l’intention de lecture subsiste. Une pause demandée par l’utilisateur retire cette intention ; une perte définitive ne déclenche pas de reprise spontanée. Aucun second gestionnaire de focus concurrent n’est ajouté.

Le service prend en charge la notification média et le fonctionnement en premier plan via Media3. Les permissions `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` et `WAKE_LOCK` sont déclarées. L’événement audio « becoming noisy » provoque une pause. Le comportement des sorties Bluetooth/Android Auto et des applications GPS reste à vérifier matériellement.

## Interopérabilité

Depuis 0.4.0, le navigateur téléphone s’identifie par un indice de connexion `phone_ui` et son package ; lui seul reçoit la liste complète et les notifications de progression. Les autres clients reçoivent une arborescence courte : raccourcis récent/ancien, périodes de huit épisodes et huit épisodes récents directement sous l’émission. Les identifiants de dossiers virtuels utilisent `~` et n’entrent pas en conflit avec les identifiants d’épisodes GUID. Les pages sont de vrais dossiers, car Android Auto ne prend pas en charge la pagination `page/pageSize` des callbacks. Les notifications périodiques ne redessinent plus la liste automobile en cours ; le RSS reste rafraîchi en mémoire et lu à la prochaine ouverture du dossier. Les restrictions de sécurité de l’hôte restent actives.

Le manifeste expose les actions MediaLibraryService et MediaBrowserService pour les clients Media3 et historiques. `automotive_app_desc.xml` déclare une application média Android Auto. La recherche média par nom d’émission fournit le dernier épisode correspondant. L’API accepte uniquement des identifiants du catalogue : elle ne lance pas une URL arbitraire fournie par un client.

Les clients média externes peuvent se connecter au service exporté, comme attendu pour ce type d’application. Cette version personnelle n’ajoute ni authentification, ni abonnement, ni restrictions à un compte.

## Références techniques

- [MediaLibraryService](https://developer.android.com/media/media3/session/serve-content)
- [Déclaration Android Auto](https://developer.android.com/training/cars/media/auto)
- [Audio focus Android](https://developer.android.com/media/optimize/audio-focus)
- [Implémentation AudioFocusManager de Media3 1.8.0](https://github.com/androidx/media/blob/1.8.0/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/AudioFocusManager.java)
- [Tests des applications média automobiles](https://developer.android.com/training/cars/testing)
- [Hiérarchie des contenus Android Auto](https://developer.android.com/training/cars/media/create-media-browser/content-hierarchy)
