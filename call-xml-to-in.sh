#!/usr/bin/env bash
set -euo pipefail

SERVICE_URL="${SERVICE_URL:-http://localhost:8080/v1/ws/address-records}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-30}"
PAYLOAD_FILE=""
TOKEN="${TOKEN:-}"
BASIC_USER="${BASIC_USER:-}"
BASIC_PASS="${BASIC_PASS:-}"

usage() {
  cat <<'EOF'
Usage:
  ./call-xml-to-in.sh --payload <soap-payload.xml> [options]
  ./call-xml-to-in.sh --wsdl [options]

Options:
  --payload <file>       SOAP XML payload file to POST.
  --wsdl                 Fetch and print WSDL from the service.
  --service-url <url>    Endpoint URL (default: http://localhost:8080/v1/ws/address-records).
  --token <jwt>          Bearer token for Authorization header.
  --basic-user <user>    Basic auth username.
  --basic-pass <pass>    Basic auth password.
  --timeout <seconds>    Curl timeout in seconds (default: 30).
  -h, --help             Show this help.

Auth precedence:
  1) Bearer token (--token or TOKEN env)
  2) Basic auth (--basic-user/--basic-pass or BASIC_USER/BASIC_PASS env)

Examples:
  ./call-xml-to-in.sh --wsdl
  ./call-xml-to-in.sh --payload sample-soap-request.xml --token "$TOKEN"
  ./call-xml-to-in.sh --payload sample-soap-request.xml --basic-user johndoe --basic-pass password123
EOF
}

WSDL_ONLY="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --payload)
      PAYLOAD_FILE="$2"
      shift 2
      ;;
    --wsdl)
      WSDL_ONLY="true"
      shift
      ;;
    --service-url)
      SERVICE_URL="$2"
      shift 2
      ;;
    --token)
      TOKEN="$2"
      shift 2
      ;;
    --basic-user)
      BASIC_USER="$2"
      shift 2
      ;;
    --basic-pass)
      BASIC_PASS="$2"
      shift 2
      ;;
    --timeout)
      TIMEOUT_SECONDS="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

AUTH_ARGS=()
if [[ -n "$TOKEN" ]]; then
  AUTH_ARGS+=( -H "Authorization: Bearer $TOKEN" )
elif [[ -n "$BASIC_USER" || -n "$BASIC_PASS" ]]; then
  AUTH_ARGS+=( -u "$BASIC_USER:$BASIC_PASS" )
fi

if [[ "$WSDL_ONLY" == "true" ]]; then
  curl -sS --max-time "$TIMEOUT_SECONDS" "${SERVICE_URL}?wsdl" "${AUTH_ARGS[@]}"
  echo
  exit 0
fi

if [[ -z "$PAYLOAD_FILE" ]]; then
  echo "Missing required --payload argument (or use --wsdl)." >&2
  usage
  exit 1
fi

if [[ ! -f "$PAYLOAD_FILE" ]]; then
  echo "Payload file not found: $PAYLOAD_FILE" >&2
  exit 1
fi

echo "POST ${SERVICE_URL}"
HTTP_CODE=$(curl -sS --max-time "$TIMEOUT_SECONDS" \
  -o /tmp/xml-to-in-response.xml \
  -w "%{http_code}" \
  -X POST "$SERVICE_URL" \
  -H "Content-Type: text/xml" \
  "${AUTH_ARGS[@]}" \
  --data-binary "@${PAYLOAD_FILE}")

echo "HTTP ${HTTP_CODE}"
echo "Response:"
cat /tmp/xml-to-in-response.xml
echo

if [[ "$HTTP_CODE" -lt 200 || "$HTTP_CODE" -ge 300 ]]; then
  exit 1
fi
