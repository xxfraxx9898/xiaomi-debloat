#!/usr/bin/env bash
# Uso: ./debloat.sh liste/01-telemetria-pubblicita.txt [altra-lista.txt ...]
# Rimuove i pacchetti per l'utente 0 (reversibile con ./ripristina.sh).
# Se Xiaomi blocca la rimozione (Failure [-1000]) il pacchetto viene disattivato.
set -u
cd "$(dirname "$0")"
[ $# -gt 0 ] || { echo "Uso: ./debloat.sh liste/01-telemetria-pubblicita.txt [altra-lista.txt ...]"; exit 1; }
adb get-state >/dev/null 2>&1 || { echo "Telefono non collegato/autorizzato"; exit 1; }

# La prima volta fotografa il telefono com'e' adesso: serve a ./controlla.sh per riconoscere i pacchetti nuovi
if [ ! -s pacchetti_originali.txt ]; then
  adb shell pm list packages -u </dev/null | tr -d '\r' | sort > pacchetti_originali.txt
  for prop in ro.product.marketname ro.product.model ro.product.device ro.mi.os.version.name \
    ro.mi.os.version.incremental ro.build.version.release ro.build.version.sdk ro.miui.region; do
    echo "$prop=$(adb shell getprop "$prop" </dev/null | tr -d '\r')"
  done > info_dispositivo.txt
  echo "Stato iniziale salvato: $(wc -l < pacchetti_originali.txt) pacchetti in pacchetti_originali.txt"
fi

installed=$(adb shell pm list packages | sed 's/^package://' | tr -d '\r')
for list in "$@"; do
  [ -f "$list" ] || { echo "Lista non trovata: $list"; continue; }
  grep -vE '^\s*(#|$)' "$list" | awk '{print $1}' | while read -r pkg; do
    if ! grep -qx "$pkg" <<<"$installed"; then
      echo "SKIP  $pkg (non installato)"; continue
    fi
    out=$(adb shell pm uninstall --user 0 "$pkg" </dev/null 2>&1 | tr -d '\r')
    if [[ "$out" == Success* ]]; then
      echo "OK    $pkg"
      echo "$pkg" >> rimossi.txt
      echo "$(date '+%F %T') OK   $list $pkg" >> rimossi.log
      continue
    fi
    if [[ "$out" == *"-1000"* ]]; then
      dis=$(adb shell pm disable-user --user 0 "$pkg" </dev/null 2>&1 | tr -d '\r')
      if [[ "$dis" == *"disabled-user"* ]]; then
        echo "OFF   $pkg (rimozione bloccata da Xiaomi: disattivato)"
        grep -qx "$pkg" disattivati.txt 2>/dev/null || echo "$pkg" >> disattivati.txt
        echo "$(date '+%F %T') DISABLE $pkg (uninstall bloccato da Xiaomi -1000)" >> rimossi.log
        continue
      fi
      out="$out, disattivazione: $dis"
    fi
    echo "FAIL  $pkg -> $out"
    echo "$(date '+%F %T') FAIL $list $pkg $out" >> rimossi.log
  done
done
