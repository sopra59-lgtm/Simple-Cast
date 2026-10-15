# Configuration Google Cast (nécessaire pour la duplication)

1. Héberger `receiver/index.html` sur une URL **HTTPS** publiquement accessible (GitHub Pages possible).
2. Sur https://cast.google.com/publish/ créer une application **Custom Receiver** avec cette URL, puis récupérer l'Application ID.
3. En développement, enregistrer le numéro de série du Chromecast de test dans la console Cast. Pour un déploiement général, publier le récepteur.
4. Dans GitHub → Settings → Secrets and variables → Actions → **Variables**, ajouter `CAST_RECEIVER_ID` avec l'Application ID (ce n'est pas un secret).
5. Relancer une compilation. Dans l'application Android, choisir l'icône Cast, sélectionner l'appareil, puis cliquer « Démarrer l'écran » et approuver l'autorisation de capture.

Sans cet identifiant, l'application utilise le récepteur Google par défaut pour permettre la découverte, **mais la duplication directe est impossible**.

## Limitations prototype
Images JPEG compressées envoyées via le canal de messages Cast; capture ~1 image/s, basse résolution, sans audio. Ceci démontre un chemin de bout en bout mais n'est PAS un streaming vidéo fluide. La prochaine étape technique est MediaCodec H.264 + pipeline streaming pris en charge par le récepteur, et la gestion des rotations d'écran. Android exige une autorisation explicite de capture par session. Ne pas diffuser de contenu sensible : aucun chiffrement de bout-en-bout supplémentaire n'est ajouté au protocole Cast. Déployer uniquement sur un réseau de confiance.
