# VideoScreen Pro: JAR ohne lokale Entwicklungsumgebung bauen

**Stand:** experimenteller Prototyp, nicht als funktionsfähig auf Paper 26.3 bestätigt. Der GitHub-Workflow kompiliert den vorhandenen Quellcode; wenn die API-Version oder Java-Methoden nicht passen, schlägt der Build fehl. Ein erfolgreicher Build garantiert keine funktionierende Videoausgabe.

1. GitHub-Konto anlegen oder anmelden.
2. Neues **privates** Repository erstellen, z. B. `VideoScreenPro`.
3. Den **Inhalt dieses Projektordners** ins Repository hochladen, einschließlich des versteckten Verzeichnisses `.github/workflows/build.yml`. Für das Hochladen vieler Dateien ist GitHub Desktop oder Git am einfachsten.
4. Im Repository **Actions → Build VideoScreen Pro → Run workflow** öffnen. Alternativ wird der Build beim Push auf `main` oder `master` gestartet.
5. Wenn der Lauf **grün** ist: unter **Artifacts** `VideoScreenPro-JAR` herunterladen, ZIP entpacken und die darin enthaltene `.jar` nutzen.
6. Wenn der Lauf **rot** ist: **Compile plugin** öffnen und die Fehlermeldung zur Korrektur des Projekts weitergeben. Es gibt dann noch keine nutzbare JAR.
7. G-Portal: Server stoppen, Backup anlegen, JAR über Dateimanager/FTP in `plugins/` hochladen, Server starten und Konsole auf Fehler prüfen.
8. Das separate Resource Pack muss als ZIP gehostet und über die `server.properties` eingebunden werden. Im Ordner `resourcepack/` sind die Pack-Dateien enthalten.

## Wichtige Grenzen
- Der aktuelle Quellcode nutzt FFmpeg für MP4/HLS und yt-dlp für YouTube/Twitch. Bei G-Portal ist nicht nachgewiesen, dass diese Programme ausgeführt werden dürfen.
- Der Prototyp ist nicht auf einem echten Paper-26.3-Server getestet; kein Ton, keine vollständige GUI-Steuerung und keine Garantie für Livestreams.
- YouTube/Twitch-URLs können sich ändern oder nicht zugänglich sein. Nur eigene oder rechtmäßig zugängliche Streams verwenden.
- **Keine unbekannten JARs installieren und zuerst Server-Backups erstellen.**
