#!/bin/bash
# Generate a self-signed HTTPS cert for JARVIS Voice (IP-aware)
set -e
DIR=~/.jarvis/certs
mkdir -p "$DIR"

# prefer a real LAN IP (192.168.x / 10.x / 172.16-31.x) over link-local
IP=$(ip -o addr show 2>/dev/null | grep -w inet | grep -v 127.0.0.1 | awk '{print $4}' | cut -d/ -f1 | grep -E '^(192\.168\.|10\.|172\.(1[6-9]|2[0-9]|3[01])\.)' | head -1)
if [ -z "$IP" ]; then
  IP=$(hostname -I 2>/dev/null | tr ' ' '\n' | grep -E '^(192\.168\.|10\.|172\.(1[6-9]|2[0-9]|3[01])\.)' | head -1)
fi
if [ -z "$IP" ]; then
  IP=$(ip addr show 2>/dev/null | grep -w inet | grep -v 127.0.0.1 | awk '{print $2}' | cut -d/ -f1 | head -1)
fi
[ -z "$IP" ] && IP=192.168.1.109

openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout "$DIR/key.pem" -out "$DIR/cert.pem" \
  -days 825 -subj "/CN=JARVIS" \
  -addext "subjectAltName=IP:$IP,IP:127.0.0.1,DNS:localhost" \
  2>/dev/null

echo "HTTPS cert ready (valid for IP $IP). Open: https://$IP:8443"
