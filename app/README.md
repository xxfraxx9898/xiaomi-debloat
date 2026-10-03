# Debloat Watch

App Android che ti avvisa se le app tolte con gli script di questo repository tornano sul telefono. Controlla dopo ogni riavvio e ogni 12 ore.

> [!CAUTION]
> Software fornito senza alcuna garanzia e senza responsabilità dell'autore: leggi l'[esclusione di responsabilità](../README.md) prima di usarlo.

## Come funziona

L'app non contiene nessuna lista. Gliela manda `./invia-lista.sh` dal PC, che la deposita con `adb push` in `/sdcard/Android/data/com.xiaomiwatch/files/watchlist.json`. Finché la lista non arriva l'app mostra «Lista non ancora caricata» e non manda notifiche.

A ogni controllo confronta la lista con le app installate e segnala:

- app rimosse che sono di nuovo installate;
- app sospese o disattivate che sono di nuovo attive;
- app di sistema che non c'erano nello stato iniziale.

L'app controlla e avvisa soltanto: non può rimuovere niente. Permessi usati: elenco delle app installate (`QUERY_ALL_PACKAGES`), notifiche, avvio dopo il riavvio. Non ha il permesso di accedere a internet.

## Struttura

- `App.tsx` — interfaccia (React Native)
- `specs/NativeXiaomiWatch.ts` — interfaccia del modulo nativo
- `android/app/src/main/java/com/xiaomiwatch/watch/` — controllo, notifiche e lavoro in background (Kotlin)

## Compilare

Servono Node 22 o superiore, JDK 17 e l'Android SDK ([guida di React Native](https://reactnative.dev/docs/set-up-your-environment)).

```sh
npm install
npm test
cd android && ./gradlew assembleRelease
```

L'APK esce in `android/app/build/outputs/apk/release/app-release.apk`.

Senza altra configurazione la build è firmata con la chiave di debug, che va bene solo per provare. Per firmarla con una tua chiave crea `android/keystore.properties` (non va nel repository):

```properties
storeFile=/percorso/assoluto/tua-chiave.keystore
storePassword=...
keyAlias=...
keyPassword=...
```
