#!/usr/bin/env bash
# Manda all'app Debloat Watch la lista delle app che hai tolto, sospeso o disattivato,
# cosi' il telefono ti avvisa da solo se tornano.
# Da rilanciare ogni volta che togli o sospendi altre app.
set -u
cd "$(dirname "$0")"
APP=com.xiaomiwatch
DEST=/sdcard/Android/data/$APP/files
sh() { adb shell "$@" </dev/null 2>&1 | tr -d '\r'; }

[ -s pacchetti_originali.txt ] || { echo "Manca pacchetti_originali.txt: lancia prima ./debloat.sh con almeno una lista."; exit 1; }
[ "$(adb get-state 2>/dev/null)" = "device" ] || { echo "Telefono non collegato o non autorizzato (controlla 'adb devices')."; exit 1; }
sh pm list packages "$APP" | grep -qx "package:$APP" || { echo "Sul telefono manca l'app Debloat Watch: installala con 'adb install DebloatWatch.apk'."; exit 1; }

python3 - <<'PY' || exit 1
import json, os
def rd(name):
    if not os.path.exists(name):
        return set()
    return {l.strip().replace('package:', '') for l in open(name) if l.strip() and not l.startswith('#')}
suspended = rd('sospesi.txt')
disabled = rd('disattivati.txt')
data = {
    'removed': sorted(rd('rimossi.txt') - suspended - disabled),
    'suspended': sorted(suspended),
    'disabled': sorted(disabled),
    'manual': sorted(rd('manuali.txt') - disabled),
    'original': sorted(rd('pacchetti_originali.txt')),
}
json.dump(data, open('watchlist.json', 'w'), indent=1)
print(f"Lista: {len(data['removed'])} tolte, {len(data['suspended'])} sospese, {len(data['disabled'])} disattivate, "
      f"{len(data['manual'])} manuali, {len(data['original'])} originali")
PY

# La cartella di destinazione la crea l'app al primo avvio
sh am start -n "$APP/.MainActivity" >/dev/null
for _ in 1 2 3 4 5 6 7 8 9 10; do
  sh ls -d "$DEST" | grep -qx "$DEST" && break
  sleep 1
done
adb push watchlist.json "$DEST/watchlist.json" >/dev/null || { echo "Invio non riuscito: apri l'app Debloat Watch sul telefono e riprova."; exit 1; }

# Riavvia l'app per farle leggere la lista nuova
sh am force-stop "$APP"
sh am start -n "$APP/.MainActivity" >/dev/null
echo "Lista inviata: guarda l'app Debloat Watch sul telefono."
