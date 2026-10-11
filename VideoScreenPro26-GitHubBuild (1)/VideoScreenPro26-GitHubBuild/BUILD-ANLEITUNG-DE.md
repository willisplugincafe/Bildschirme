# VideoScreen Pro – Build und Installation

**Status:** Experimenteller Quellcode. Eine kompilierte, getestete JAR ist **nicht** enthalten.

1. Installiere JDK 25 und eine aktuelle kompatible Gradle-Version.
2. Starte `build-windows.bat` (Windows) oder `bash build-linux.sh` (Linux).
3. Bei erfolgreichem Build liegt die JAR unter `build/libs/`. Kopiere sie nach `plugins/` des Paper-Servers.
4. Installiere FFmpeg auf dem Server; für YouTube/Twitch außerdem yt-dlp. Beide Programme müssen im PATH verfügbar sein oder in `plugins/VideoScreenPro/config.yml` eingetragen werden.
5. Lege lokale MP4-Dateien unter `plugins/VideoScreenPro/videos/` ab.
6. Hoste die Datei `VideoScreenPro26-resourcepack.zip` als Server-Resource-Pack und trage URL und SHA-1 in `server.properties` ein.

**Achtung:** Nicht auf einem Produktivserver ohne Backup testen. GUI, Live-Streams, Kartenrahmen und Paper-26.3-Kompatibilität wurden noch nicht praktisch verifiziert. Die Videowiedergabe hat keinen Ton.
