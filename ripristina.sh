#!/usr/bin/env bash
# Uso: ./ripristina.sh            -> rimette TUTTI i pacchetti rimossi e riattiva quelli sospesi o disattivati
#      ./ripristina.sh pkg1 pkg2  -> rimette/riattiva solo quelli indicati
cd "$(dirname "$0")"
adb get-state >/dev/null 2>&1 || { echo "Telefono non collegato/autorizzato"; exit 1; }
pkgs=("$@"); [ ${#pkgs[@]} -eq 0 ] && mapfile -t pkgs < <(sort -u rimossi.txt sospesi.txt disattivati.txt 2>/dev/null)
for p in "${pkgs[@]}"; do
  echo -n "$p: "; adb shell cmd package install-existing --user 0 "$p" </dev/null | tr -d '\r'
  if grep -qx "$p" disattivati.txt 2>/dev/null; then
    adb shell pm enable --user 0 "$p" </dev/null | tr -d '\r'
  fi
  if grep -qx "$p" sospesi.txt 2>/dev/null; then
    adb shell pm unsuspend --user 0 "$p" </dev/null | tr -d '\r'
    adb shell cmd appops set --user 0 "$p" RUN_ANY_IN_BACKGROUND allow </dev/null
  fi
done
