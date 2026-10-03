# Xiaomi Debloat

🇮🇹 [Versione italiana](README.md)

> [!CAUTION]
> ## 🛑 No warranty, no liability
>
> **You use this tool entirely at your own risk.**
>
> These scripts remove and disable system components of your phone. Used
> incorrectly they can cause malfunctions, data loss, loss of features (calls,
> SMS, payments, notifications, updates), boot loops, or force a factory reset.
>
> The software is provided "as is", without warranty of any kind, express or
> implied. **The author and contributors accept no liability for any damage to
> your device, your data, or anything else arising from its use.** By running the
> scripts you accept that you alone are responsible for what happens to your
> phone.
>
> Back up your phone before you start, and read each list before you run it.
> Independent project, not affiliated with or endorsed by Xiaomi.
> The same terms are in the [license](LICENSE).

Scripts to remove preinstalled apps from Xiaomi, Redmi and POCO phones running HyperOS, from your computer and without root, and to notice when they come back after an update.

- **`debloat.sh`** removes the packages named in a list.
- **`controlla.sh`** ("check") tells you whether anything came back and, with `--ripara` ("repair"), removes it again.
- **`ripristina.sh`** ("restore") puts everything back.
- **`invia-lista.sh`** ("send list") and the **Debloat Watch** app let the phone run the check itself and notify you.

> [!NOTE]
> The scripts, their messages, the list comments and the app are in Italian. This page explains everything you need, and there is a [glossary](#glossary) at the end.

## What can be undone

The scripts use `pm uninstall --user 0`: the app is removed for your user but stays on the system partition, so `./ripristina.sh` brings it back. A factory reset also returns everything to its original state.

The **data** of removed apps (notes, settings, files stored inside the app) cannot be recovered: it is deleted on removal.

## Requirements

- A computer with `adb`, `bash`, `python3` and `curl`. Tested on Linux. macOS needs bash 4 or later; on Windows use WSL.
- A Xiaomi, Redmi or POCO phone with USB debugging enabled.

The lists were tested **only** on a Redmi 15 (`creek`) running HyperOS 3.0, Android 16, European global ROM. On other models or versions some packages may be absent (they are skipped), and packages that are harmless here may be required there.

## Preparing the phone

1. Settings → About phone → tap **OS version** 7 times to enable developer options.
2. Settings → Additional settings → Developer options → enable **USB debugging**.
3. Connect the phone, accept the authorization prompt on its screen and check:

   ```sh
   adb devices
   ```

   The phone must show up as `device`. If commands are refused for lack of permission, also enable **USB debugging (Security settings)**.

## Usage

Download the repository (**Code → Download ZIP**, or `git clone`) and enter the folder. If the scripts are not executable, run `chmod +x *.sh`.

**1. Read the list you want to use.** Each line is a package. Put `#` in front of the ones you want to keep.

**2. Run one list at a time:**

```sh
./debloat.sh liste/01-telemetria-pubblicita.txt
```

On the first run the script saves the phone's initial state to `pacchetti_originali.txt`. For each package it prints a result:

| Result | Meaning |
|---|---|
| `OK` | removed |
| `OFF` | Xiaomi blocks removal (`Failure [-1000]`): the package was disabled instead |
| `SKIP` | not installed on this phone |
| `FAIL` | error, shown next to it |

**3. Reboot and test the phone** before moving to the next list: calls, SMS, camera, Wi-Fi, notifications. If something breaks, you know which list caused it.

**4. Check:**

```sh
./controlla.sh
```

The phone must be unlocked. If an app came back, `./controlla.sh --ripara` removes it again.

Some apps come back on every reboot. In that case write the package name in `sospesi.txt` (one per line) and run `./controlla.sh --ripara` again: the app stays installed but suspended, so it cannot start. On the test Redmi 15 this was needed for Chrome, Digital Wellbeing, Personal Safety, TalkBack and Android Auto.

**5. To go back:**

```sh
./ripristina.sh                      # restore everything
./ripristina.sh com.miui.notes       # restore a single package
```

## The lists

| List | Contents | Risk |
|---|---|---|
| `01-telemetria-pubblicita.txt` | telemetry, ads, sponsored-app installers | low |
| `02-app-terze-parti.txt` | sponsored third-party apps and games | low |
| `03-app-xiaomi.txt` | Xiaomi browser, stores, players and showcases | low |
| `04-servizi-xiaomi.txt` | Xiaomi Cloud, device-to-device sharing, factory tests | medium |
| `05-google-superfluo.txt` | Gmail, Maps, YouTube, Photos, assistant, ads | medium |
| `06-ai-ricerca-xiaomi.txt` | global search and HyperOS AI features | low |
| `07-app-utili-opzionali.txt` | notes, weather, gallery, file manager, Chrome, themes | medium |
| `08-degoogle.txt` | Play Store, Play Services, keyboard, phone, SMS | **high** |

List 07 removes apps that work fine: use it only after installing replacements.

List 08 is for people who want a phone without Google and know what that implies. **Install first** a keyboard, a phone app, an SMS app and an alternative store: without a keyboard you cannot type, without a phone app you cannot make calls. Without Play Services, push notifications and many banking and payment apps stop working.

You can also write your own list: a text file with one package per line; lines starting with `#` are comments.

## After a system update

HyperOS updates can reinstall removed apps and add new ones. After each update:

```sh
./controlla.sh            # show what changed
./controlla.sh --ripara   # remove, suspend and disable again
```

System packages that were not there at the start are listed with a short description, so you can decide whether to remove them.

## Debloat Watch app (optional)

An Android app that runs the same check on the phone itself, after every reboot and every 12 hours, and notifies you if a removed app came back, a suspended one is active again, or a new system package appeared.

The app only checks and notifies: fixing things still requires `./controlla.sh --ripara` from your computer. It has no internet permission, so it sends nothing to anyone. Its interface is in Italian.

1. Download `DebloatWatch.apk` from the [Releases page](../../releases/latest).
2. Install it and send it your list:

   ```sh
   adb install DebloatWatch.apk
   ./invia-lista.sh
   ```

3. Run `./invia-lista.sh` again whenever you remove or suspend more apps.

The source code is in the [`app/`](app/) folder.

## Files created by the scripts

They stay in the script folder and describe your phone. Do not delete them: `controlla.sh` and `ripristina.sh` need them.

| File | Contents |
|---|---|
| `pacchetti_originali.txt` | every package present before you started |
| `info_dispositivo.txt` | model and system version |
| `rimossi.txt` | removed packages |
| `disattivati.txt` | packages disabled because Xiaomi blocks their removal |
| `sospesi.txt` | packages to keep suspended (you fill this in) |
| `manuali.txt` | packages ADB cannot remove and that must be uninstalled on the phone (you fill this in; `controlla.sh` reminds you) |
| `rimossi.log` | history of every operation |
| `uad_lists.json` | package descriptions, downloaded when first needed |

## Glossary

| Italian | English |
|---|---|
| `Telefono non collegato/autorizzato` | phone not connected or not authorized |
| `Sblocca il telefono` | unlock the phone |
| `TORNATE` | came back (were removed, are installed again) |
| `NON PIU' SOSPESE` / `NON PIU' DISATTIVATE` | no longer suspended / no longer disabled |
| `NUOVI PACCHETTI DI SISTEMA` | new system packages |
| `DA DISINSTALLARE A MANO` | uninstall by hand on the phone |
| `Tutto pulito` | all clean |
| `Riparo...` | repairing |
| `Lista non ancora caricata` (app) | list not loaded yet: run `./invia-lista.sh` |
| `Tornate` / `Non più bloccate` / `Nuove di sistema` (app) | came back / no longer blocked / new system apps |
| `rimossi`, `disattivati`, `sospesi`, `manuali` | removed, disabled, suspended, manual |

## Credits

The package descriptions shown by `controlla.sh` come from [Universal Android Debloater Next Generation](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) (GPL-3.0). The file is not included in this repository: it is downloaded to your computer when needed.

## License

[MIT](LICENSE)
