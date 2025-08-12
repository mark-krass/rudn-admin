#!/bin/sh
set -eu

# Использование: ./create-remote-vpn.sh <имя_клиента>
if [ "$#" -ne 1 ]; then
  echo "Использование: $0 <имя_клиента>"
  exit 1
fi

CLIENT="$1"

LOCAL_DIR=${VPN_OUTPUT_DIR}
mkdir -p "$LOCAL_DIR"
LOCAL_FILE="$LOCAL_DIR/${CLIENT}.ovpn"

{
  echo "client"
  echo "# demo file for $CLIENT"
} > "$LOCAL_FILE"

echo "Готово: файл получен -> $LOCAL_FILE"
