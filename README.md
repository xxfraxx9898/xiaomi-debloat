# Xiaomi Debloat

🇬🇧 [English version](README.en.md)

> [!CAUTION]
> ## 🛑 Nessuna garanzia, nessuna responsabilità
>
> **Usi questo strumento a tuo esclusivo rischio.**
>
> Questi script rimuovono e disattivano componenti di sistema del telefono. Un uso
> errato può causare malfunzionamenti, perdita di dati, perdita di funzioni
> (chiamate, SMS, pagamenti, notifiche, aggiornamenti), riavvii continui o rendere
> necessario il ripristino ai dati di fabbrica.
>
> Il software è fornito «così com'è», senza garanzie di alcun tipo, esplicite o
> implicite. **L'autore e i contributori non si assumono alcuna responsabilità per
> danni al dispositivo, ai dati o di qualsiasi altra natura derivanti dal suo
> utilizzo.** Eseguendo gli script accetti di essere l'unico responsabile di ciò
> che accade al tuo telefono.
>
> Fai un backup prima di cominciare e leggi ogni lista prima di lanciarla.
> Progetto indipendente, non affiliato a Xiaomi né da essa approvato.
> Le stesse condizioni sono nella [licenza](LICENSE).

Script per togliere le app preinstallate dai telefoni Xiaomi, Redmi e POCO con HyperOS, dal PC e senza root, e per accorgersi se tornano dopo un aggiornamento.

- **`debloat.sh`** toglie i pacchetti elencati in una lista.
- **`controlla.sh`** verifica se qualcosa è tornato e, con `--ripara`, lo ritoglie.
- **`ripristina.sh`** rimette tutto com'era.
- **`invia-lista.sh`** e l'app **Debloat Watch** fanno fare il controllo al telefono, che ti avvisa con una notifica.

## Cosa si può annullare

Gli script usano `pm uninstall --user 0`: l'app viene tolta per il tuo utente ma resta nella partizione di sistema, quindi `./ripristina.sh` la rimette. Anche un ripristino ai dati di fabbrica riporta tutto allo stato originale.

Non si recuperano i **dati** delle app tolte (note, impostazioni, file salvati dentro l'app): vengono cancellati alla rimozione.

## Requisiti

- Un PC con `adb`, `bash`, `python3` e `curl`. Provato su Linux. Su macOS serve bash 4 o superiore; su Windows usa WSL.
- Un telefono Xiaomi, Redmi o POCO con il debug USB attivo.

Le liste sono state provate **solo** su un Redmi 15 (`creek`) con HyperOS 3.0, Android 16, ROM globale europea. Su altri modelli o altre versioni possono mancare dei pacchetti (vengono saltati) e pacchetti innocui qui possono essere necessari lì.

## Preparare il telefono

1. Impostazioni → Info sul telefono → tocca 7 volte **Versione OS** per attivare le opzioni sviluppatore.
2. Impostazioni → Impostazioni aggiuntive → Opzioni sviluppatore → attiva **Debug USB**.
3. Collega il telefono al PC, accetta la richiesta di autorizzazione sullo schermo e controlla:

   ```sh
   adb devices
   ```

   Il telefono deve comparire come `device`. Se i comandi vengono rifiutati per mancanza di permessi, attiva anche **Debug USB (Impostazioni di sicurezza)**.

## Uso

Scarica il repository (pulsante **Code → Download ZIP**, oppure `git clone`) ed entra nella cartella. Se gli script non risultano eseguibili, lancia `chmod +x *.sh`.

**1. Leggi la lista che vuoi usare.** Ogni riga è un pacchetto. Metti `#` davanti a quelli che vuoi tenere.

**2. Lancia una lista alla volta:**

```sh
./debloat.sh liste/01-telemetria-pubblicita.txt
```

La prima volta lo script salva lo stato iniziale del telefono in `pacchetti_originali.txt`. Per ogni pacchetto stampa l'esito:

| Esito | Significato |
|---|---|
| `OK` | rimosso |
| `OFF` | Xiaomi blocca la rimozione (`Failure [-1000]`): il pacchetto è stato disattivato |
| `SKIP` | non installato su questo telefono |
| `FAIL` | errore, riportato accanto |

**3. Riavvia e prova il telefono** prima di passare alla lista successiva: chiamate, SMS, fotocamera, Wi-Fi, notifiche. Se qualcosa non va, sai quale lista l'ha causato.

**4. Controlla:**

```sh
./controlla.sh
```

Il telefono deve essere sbloccato. Se un'app è tornata, `./controlla.sh --ripara` la ritoglie.

Alcune app tornano a ogni riavvio. In quel caso scrivi il nome del pacchetto in `sospesi.txt` (uno per riga) e rilancia `./controlla.sh --ripara`: l'app resta installata ma sospesa e non può partire. Sul Redmi 15 di prova è servito per Chrome, Benessere digitale, Sicurezza personale, TalkBack e Android Auto.

**5. Per tornare indietro:**

```sh
./ripristina.sh                      # rimette tutto
./ripristina.sh com.miui.notes       # rimette solo un pacchetto
```

## Le liste

| Lista | Contenuto | Rischio |
|---|---|---|
| `01-telemetria-pubblicita.txt` | raccolta dati, annunci, installatori di app sponsorizzate | basso |
| `02-app-terze-parti.txt` | app e giochi sponsorizzati | basso |
| `03-app-xiaomi.txt` | browser, store, lettori e vetrine Xiaomi | basso |
| `04-servizi-xiaomi.txt` | Xiaomi Cloud, condivisione tra dispositivi, test di fabbrica | medio |
| `05-google-superfluo.txt` | Gmail, Maps, YouTube, Foto, assistente, pubblicità | medio |
| `06-ai-ricerca-xiaomi.txt` | ricerca globale e funzioni AI di HyperOS | basso |
| `07-app-utili-opzionali.txt` | note, meteo, galleria, gestore file, Chrome, temi | medio |
| `08-degoogle.txt` | Play Store, Play Services, tastiera, telefono, SMS | **alto** |

La lista 07 toglie app che funzionano: usala solo dopo aver installato le alternative.

La lista 08 è per chi vuole un telefono senza Google e sa cosa comporta. **Installa prima** una tastiera, un'app telefono, un'app SMS e uno store alternativo: senza tastiera non puoi più scrivere, senza app telefono non puoi chiamare. Senza Play Services smettono di funzionare le notifiche push e molte app di banche e pagamenti.

Puoi anche scrivere una lista tua: un file di testo con un pacchetto per riga, le righe che iniziano con `#` sono commenti.

## Dopo un aggiornamento di sistema

Gli aggiornamenti di HyperOS possono reinstallare le app tolte e aggiungerne di nuove. Dopo ogni aggiornamento:

```sh
./controlla.sh            # mostra cosa è cambiato
./controlla.sh --ripara   # ritoglie, risospende e ridisattiva
```

I pacchetti di sistema che non c'erano all'inizio vengono elencati con una breve descrizione, perché tu possa decidere se toglierli.

## App Debloat Watch (facoltativa)

Un'app Android che fa lo stesso controllo direttamente sul telefono, dopo ogni riavvio e ogni 12 ore, e ti manda una notifica se un'app tolta è tornata, se una sospesa è di nuovo attiva o se è comparso un pacchetto di sistema nuovo.

L'app controlla e avvisa soltanto: per sistemare serve comunque `./controlla.sh --ripara` dal PC. Non ha il permesso di accedere a internet, quindi non invia niente a nessuno. L'interfaccia è in italiano.

1. Scarica `DebloatWatch.apk` dalla [pagina Releases](../../releases/latest).
2. Installala e mandale la tua lista:

   ```sh
   adb install DebloatWatch.apk
   ./invia-lista.sh
   ```

3. Rilancia `./invia-lista.sh` ogni volta che togli o sospendi altre app.

I sorgenti sono nella cartella [`app/`](app/).

## File creati dagli script

Restano nella cartella degli script e descrivono il tuo telefono. Non cancellarli: servono a `controlla.sh` e `ripristina.sh`.

| File | Contenuto |
|---|---|
| `pacchetti_originali.txt` | tutti i pacchetti presenti prima di cominciare |
| `info_dispositivo.txt` | modello e versione del sistema |
| `rimossi.txt` | pacchetti rimossi |
| `disattivati.txt` | pacchetti disattivati perché Xiaomi ne blocca la rimozione |
| `sospesi.txt` | pacchetti da tenere sospesi (lo compili tu) |
| `manuali.txt` | pacchetti che ADB non riesce a togliere e che vanno disinstallati dal telefono (lo compili tu, `controlla.sh` te li ricorda) |
| `rimossi.log` | cronologia di tutte le operazioni |
| `uad_lists.json` | descrizioni dei pacchetti, scaricate al primo bisogno |

## Crediti

Le descrizioni dei pacchetti mostrate da `controlla.sh` vengono dal progetto [Universal Android Debloater Next Generation](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) (GPL-3.0). Il file non è incluso in questo repository: viene scaricato sul tuo PC quando serve.

## Licenza

[MIT](LICENSE)
