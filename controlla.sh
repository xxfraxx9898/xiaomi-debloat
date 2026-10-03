#!/usr/bin/env bash
# Controlla se le app tolte sono tornate (es. dopo un aggiornamento HyperOS).
# Uso: ./controlla.sh            -> solo controllo, non cambia niente
#      ./controlla.sh --ripara   -> ritoglie le app tornate e risospende quelle sospese
set -u
cd "$(dirname "$0")"
RIPARA=0; [ "${1:-}" = "--ripara" ] && RIPARA=1
UAD_URL=https://raw.githubusercontent.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/main/resources/assets/uad_lists.json
sh() { adb shell "$@" </dev/null 2>&1 | tr -d '\r'; }
conta() { grep -c . <<<"$1"; }

if [ ! -s rimossi.txt ] && [ ! -s sospesi.txt ] && [ ! -s disattivati.txt ]; then
  echo "Niente da controllare: non hai ancora tolto app con ./debloat.sh."; exit 0
fi

[ "$(adb get-state 2>/dev/null)" = "device" ] || { echo "Telefono non collegato o non autorizzato (controlla 'adb devices')."; exit 1; }
if ! sh dumpsys user | grep -q 'State: RUNNING_UNLOCKED'; then
  echo "Sblocca il telefono (PIN o impronta) e rilancia."; exit 1
fi
echo "Telefono: $(sh getprop ro.product.marketname) - $(sh getprop ro.mi.os.version.incremental)"
echo

installati=$(sh pm list packages | sed 's/^package://' | sort)
sospesi=$(sort -u sospesi.txt 2>/dev/null)
da_togliere=$(sort -u rimossi.txt 2>/dev/null | grep -vxF -e "$sospesi")
problemi=0

tornate=$(grep -xF -e "$da_togliere" <<<"$installati")
if [ -n "$tornate" ]; then
  echo "TORNATE (erano state tolte):"; sed 's/^/  /' <<<"$tornate"; problemi=1
else
  echo "OK  nessuna app tolta e' tornata ($(conta "$da_togliere") controllate)"
fi

non_sospese=""
for p in $sospesi; do
  grep -qx "$p" <<<"$installati" || continue
  sh dumpsys package "$p" | grep -m1 -q 'suspended=true' || non_sospese+="$p"$'\n'
done
non_sospese=$(sed '/^$/d' <<<"$non_sospese")
if [ -n "$non_sospese" ]; then
  echo "NON PIU' SOSPESE:"; sed 's/^/  /' <<<"$non_sospese"; problemi=1
elif [ -n "$sospesi" ]; then
  echo "OK  le $(conta "$sospesi") app sospese sono ancora bloccate"
fi

disattivati=$(sort -u disattivati.txt 2>/dev/null)
spenti=$(sh pm list packages -d | sed 's/^package://')
riattivate=""
for p in $disattivati; do
  grep -qx "$p" <<<"$installati" || continue
  grep -qx "$p" <<<"$spenti" || riattivate+="$p"$'\n'
done
riattivate=$(sed '/^$/d' <<<"$riattivate")
if [ -n "$riattivate" ]; then
  echo "NON PIU' DISATTIVATE:"; sed 's/^/  /' <<<"$riattivate"; problemi=1
elif [ -n "$disattivati" ]; then
  echo "OK  le $(conta "$disattivati") app disattivate sono ancora spente"
fi

manuali=$(grep -xF -f manuali.txt <<<"$installati" 2>/dev/null)
[ -n "$manuali" ] && { echo "DA DISINSTALLARE A MANO dal telefono (Xiaomi blocca ADB):"; sed 's/^/  /' <<<"$manuali"; }

# Pacchetti nuovi preinstallati dal sistema (non installati da te), es. arrivati con un aggiornamento
nuovi=""
if [ -s pacchetti_originali.txt ]; then
  originali=$(sed 's/^package://' pacchetti_originali.txt | sort)
  nuovi=$(sh pm list packages -f | sed 's/^package://' | grep -v '^/data/app/' | sed 's/.*=//' | sort | grep -vxF -e "$originali")
else
  echo "(manca pacchetti_originali.txt: salto il controllo dei pacchetti nuovi di sistema)"
fi
if [ -n "$nuovi" ]; then
  # Descrizioni dei pacchetti dal progetto Universal Android Debloater NG (scaricate la prima volta)
  [ -s uad_lists.json ] || curl -fsSL --max-time 30 "$UAD_URL" -o uad_lists.json 2>/dev/null || rm -f uad_lists.json
  echo "NUOVI PACCHETTI DI SISTEMA (non c'erano all'inizio, da valutare):"
  while read -r p; do
    d=$(python3 -c "import json,sys;u=json.load(open('uad_lists.json')).get(sys.argv[1]);print(f\"[{u['removal']}] {u['description'].splitlines()[0][:70]}\" if u else '[sconosciuto]')" "$p" 2>/dev/null)
    echo "  $p  $d"
  done <<<"$nuovi"
fi

if [ $problemi -eq 0 ]; then echo; echo "Tutto pulito."; exit 0; fi
if [ $RIPARA -eq 0 ]; then echo; echo "Per sistemare: ./controlla.sh --ripara"; exit 2; fi

echo; echo "Riparo..."
for p in $tornate; do
  echo -n "  togli $p: "; r=$(sh pm uninstall --user 0 "$p"); echo "$r"
  echo "$(date '+%F %T') RIPARA uninstall $p $r" >> rimossi.log
done
for p in $non_sospese; do
  sh am force-stop "$p" >/dev/null
  echo -n "  sospendi $p: "; sh pm suspend --user 0 "$p" | tail -1
  sh cmd appops set --user 0 "$p" RUN_ANY_IN_BACKGROUND ignore >/dev/null
  echo "$(date '+%F %T') RIPARA suspend $p" >> rimossi.log
done
for p in $riattivate; do
  echo -n "  disattiva $p: "; sh pm disable-user --user 0 "$p" | tail -1
  echo "$(date '+%F %T') RIPARA disable $p" >> rimossi.log
done
echo "Fatto. Riavvia, sblocca e rilancia ./controlla.sh: se un'app torna di nuovo"
echo "dopo il riavvio, aggiungi il suo nome a sospesi.txt e rilancia --ripara."
