#!/usr/bin/env sh
set -eu

# Usage:
#   ./collect_sources.sh [PROJECT_ROOT] [OUTPUT_TXT] [--numbers]
# Examples:
#   ./collect_sources.sh
#   ./collect_sources.sh . project_snapshot.txt
#   ./collect_sources.sh . project_snapshot.txt --numbers

ROOT="${1:-$(pwd)}"
OUT="${2:-project_snapshot_$(date +%Y%m%d_%H%M%S).txt}"
NUMBERS="no"
case "${3:-}" in
  --numbers) NUMBERS="yes" ;;
  *) : ;;
esac

# Build the 'find' command
# Include: Kotlin/Java, Gradle, manifests, XML resources, proguard/r8, local.properties
# Exclude: build outputs, caches, VCS, IDE, venvs, node_modules, etc.
# Use -print0 so we can handle spaces safely.
FIND_EXCLUDES='
  -path "*/build/*" -o
  -path "*/.gradle/*" -o
  -path "*/.idea/*" -o
  -path "*/.git/*" -o
  -path "*/.venv/*" -o
  -path "*/venv/*" -o
  -path "*/node_modules/*" -o
  -path "*/.cxx/*" -o
  -path "*/out/*"
'

FIND_INCLUDES='
  -name "*.kt" -o
  -name "*.java" -o
  -name "build.gradle" -o
  -name "build.gradle.kts" -o
  -name "settings.gradle" -o
  -name "settings.gradle.kts" -o
  -name "gradle.properties" -o
  -name "AndroidManifest.xml" -o
  -path "*/res/*.xml" -o
  -path "*/xml/*.xml" -o
  -name "*.pro" -o
  -name "local.properties"
'

# Start fresh
: > "$OUT"
{
  echo "==== Project Snapshot ===="
  echo "Root: $ROOT"
  echo "Generated: $(date -Iseconds 2>/dev/null || date)"
  echo
} >> "$OUT"

# Helper: portable file size
filesize() {
  # macOS: stat -f %z ; GNU: stat -c %s
  size=$(stat -f %z "$1" 2>/dev/null || stat -c %s "$1" 2>/dev/null || echo "?")
  printf "%s" "$size"
}

# Helper: sha256
filesha() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  elif command -v openssl >/dev/null 2>&1; then
    openssl dgst -sha256 "$1" | awk '{print $2}'
  else
    printf "%s" "(sha256 unavailable)"
  fi
}

# Iterate files with NUL delimiter
COUNT=0
# shellcheck disable=SC2039
find "$ROOT" -type f \( $FIND_INCLUDES \) ! \( $FIND_EXCLUDES \) -print0 \
| sort -z \
| while IFS= read -r -d '' f; do
    COUNT=$((COUNT+1))
    rel="${f#$ROOT/}"
    sha="$(filesha "$f")"
    lines=$(wc -l < "$f" 2>/dev/null | tr -d ' ' || echo "?")
    size="$(filesize "$f")"

    {
      echo "===== BEGIN FILE: $rel ====="
      echo "Size: $size bytes"
      echo "Lines: $lines"
      echo "SHA256: $sha"
      echo
      if [ "$NUMBERS" = "yes" ] && command -v nl >/dev/null 2>&1; then
        nl -ba "$f"
      else
        cat "$f"
      fi
      echo
      echo "===== END FILE: $rel ====="
      echo
    } >> "$OUT"
  done

# Append final count (note: COUNT inside while is subshell; recompute for footer)
TOTAL=$(find "$ROOT" -type f \( $FIND_INCLUDES \) ! \( $FIND_EXCLUDES \) | wc -l | tr -d ' ')
{
  echo "==== Summary ===="
  echo "Files included: $TOTAL"
} >> "$OUT"

echo "Wrote snapshot: $OUT"

