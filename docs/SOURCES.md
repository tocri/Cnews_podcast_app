# Sources et choix de récupération

Vérification directe effectuée le 16 septembre 2026.

La page publique https://www.cnews.fr/les-podcasts a été récupérée et contient les liens des émissions et des derniers épisodes. La page d’un épisode de 100 % Frontières a renvoyé un challenge anti-robot : aucune tentative de contournement. Cela confirme l’intérêt de ne pas utiliser ces pages pour alimenter le lecteur.

Les pages publiques https://shows.acast.com/100-frontieres et les autres pages d’émission exposent un lien RSS. Les alias lisibles des flux fonctionnent, mais leur élément Atom `link rel="self"` fournit un identifiant Acast immuable : le registre embarqué utilise cette adresse canonique.

Les flux ci-dessous ont été téléchargés et analysés comme XML, avec des titres correspondant aux émissions CNEWS. Le nombre correspond aux items présents dans le flux le jour du contrôle, pas à ceux affichés par l’application (60 maximum).

| Émission | Flux canonique | Épisodes dans le RSS | Dernière publication UTC |
|---|---|---:|---|
| 100% Frontières | [RSS](https://feeds.acast.com/public/shows/699730a4e1d8773119135068) | 117 | Wed, 16 Sep 2026 10:59:33 GMT |
| L'Heure des Pros | [RSS](https://feeds.acast.com/public/shows/61308a906d38e90019a4bdd1) | 1131 | Wed, 16 Sep 2026 09:02:15 GMT |
| L'Heure des Pros 2 | [RSS](https://feeds.acast.com/public/shows/6156d7f1a603860012aee1d9) | 886 | Tue, 15 Sep 2026 19:13:48 GMT |
| Face à l'Info | [RSS](https://feeds.acast.com/public/shows/6156d7ab7722140012bbdeb1) | 871 | Tue, 15 Sep 2026 18:09:21 GMT |
| 100% Politique | [RSS](https://feeds.acast.com/public/shows/666c30d9899a1b001200b37e) | 433 | Tue, 15 Sep 2026 20:56:56 GMT |
| Face à Michel Onfray | [RSS](https://feeds.acast.com/public/shows/65dd8f68c54eb600189ca689) | 121 | Sat, 12 Sep 2026 13:00:26 GMT |
| Face à Bock-Côté | [RSS](https://feeds.acast.com/public/shows/624d6de2ebccf20012f3a3b0) | 196 | Sat, 12 Sep 2026 17:54:58 GMT |
| La Matinale | [RSS](https://feeds.acast.com/public/shows/61558e68505bc80015b5625b) | 1125 | Wed, 16 Sep 2026 07:01:09 GMT |
| Face à Face | [RSS](https://feeds.acast.com/public/shows/650d85377da282001172c587) | 131 | Sun, 13 Sep 2026 17:55:49 GMT |
| En quête d'esprit | [RSS](https://feeds.acast.com/public/shows/624d6cda3037db0012943c32) | 254 | Sun, 06 Sep 2026 12:00:04 GMT |
| Face à Philippe de Villiers | [RSS](https://feeds.acast.com/public/shows/64edcb6245b5ac0011588018) | 135 | Fri, 11 Sep 2026 18:06:36 GMT |
| La grande interview | [RSS](https://feeds.acast.com/public/shows/64ec52ca55e02c001170839b) | 146 | Wed, 16 Sep 2026 06:39:33 GMT |

## Preuve du chemin média

Le flux de 100 % Frontières contenait l’épisode du 16/09/2026. Son enclosure commence par `https://sphinx.acast.com/p/open/s/` et se termine par `/media.mp3`. Une requête HTTP partielle a renvoyé le type `audio/mpeg` et le statut 206 après redirection vers `stitcher2.acast.com`. Les paramètres de signature de cette URL finale ne sont ni enregistrés ni embarqués.

Le paramètre `Expires` de l’URL fournie dans la conversation initiale n’est donc pas une source de catalogue. Le lecteur utilise le RSS et son enclosure, pas une URL interceptée dans un navigateur.

## Pièges identifiés

- Le slug Acast de Philippe de Villiers est `face-a-de-villiers`, pas `face-a-philippe-de-villiers` (404).
- Le slug de La Grande Interview conserve le nom historique `la-grande-interview-de-sonia-mabrouk`, même si le titre courant et les présentateurs ont changé. L’identifiant canonique du RSS évite de dépendre de ce nom.
- Les flux peuvent comporter simultanément `title` et `itunes:title` ; le parseur traite les espaces de noms.
- Les pages publiques mises en cache par les moteurs peuvent avoir du retard. Les dates et nombres de ce registre viennent des flux téléchargés directement.
- Aucun annuaire officiel global exploitable n’a été validé. Le registre contient une sélection de douze émissions ; il ne prétend pas représenter la totalité des podcasts CNEWS.

Références publiques : [CNEWS Podcasts](https://www.cnews.fr/les-podcasts), [100 % Frontières chez Acast](https://shows.acast.com/100-frontieres), [Philippe de Villiers](https://shows.acast.com/face-a-de-villiers/episodes), [La Grande Interview](https://shows.acast.com/la-grande-interview-de-sonia-mabrouk).
