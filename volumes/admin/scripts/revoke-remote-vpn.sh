#!/bin/sh
set -eu

# Использование: ./revoke-remote-vpn.sh <имя_клиента>
if [ "$#" -ne 1 ]; then
  echo "Использование: $0 <имя_клиента>"
  exit 1
fi

CLIENT="$1"

LOCAL_DIR=${VPN_OUTPUT_DIR}
LOCAL_FILE="$LOCAL_DIR/${CLIENT}.ovpn"

rm -f "$LOCAL_FILE"

echo "Готово: клиент '$CLIENT' отозван."
