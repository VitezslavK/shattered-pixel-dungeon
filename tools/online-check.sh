#!/bin/bash
# Checks that core/src/main/assets/online.properties points at a working Supabase project
# with the tables from docs/online/supabase_setup.sql. Only reads, never writes.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
F="$ROOT/core/src/main/assets/online.properties"
[ -f "$F" ] || { echo "Chybí $F (zkopíruj online.properties.example)"; exit 1; }
URL=$(grep '^supabase_url=' "$F" | cut -d= -f2- | tr -d ' \r'); URL=${URL%/}
KEY=$(grep '^supabase_publishable_key=' "$F" | cut -d= -f2- | tr -d ' \r')
[ -z "$KEY" ] && KEY=$(grep '^supabase_anon_key=' "$F" | cut -d= -f2- | tr -d ' \r')
case "$KEY" in sb_secret_*) echo "STOP: to je SECRET klíč, do hry patří publishable!"; exit 1;; esac
if [[ "$KEY" == eyJ* ]] && echo "$KEY" | cut -d. -f2 | base64 -d 2>/dev/null | grep -q service_role; then
  echo "STOP: to je service_role klíč, do hry patří anon/publishable!"; exit 1
fi
[[ "$URL" == https://* && -n "$KEY" ]] || { echo "URL nebo klíč nejsou vyplněné"; exit 1; }
AUTH=(); [[ "$KEY" == eyJ* ]] && AUTH=(-H "Authorization: Bearer $KEY")
ok=1
for t in ghosts daily_scores bones; do
  code=$(curl -s -o /tmp/spd-online-check.txt -w '%{http_code}' -H "apikey: $KEY" "${AUTH[@]}" "$URL/rest/v1/$t?select=id&limit=1")
  if [ "$code" = 200 ]; then echo "OK    tabulka $t"; else echo "CHYBA tabulka $t (HTTP $code): $(head -c 200 /tmp/spd-online-check.txt)"; ok=0; fi
done
[ $ok = 1 ] && echo "Vše funguje, můžeš sestavit hru." || exit 1
