#!/usr/bin/env sh
set -eu

ROOT="${1:-.}"
OUT="${2:-project_snapshot_$(date +%Y%m%d_%H%M%S).txt}"

# Start fresh
: > "$OUT"
echo "==== Project Snapshot ====" >> "$OUT"
echo "Root: $(cd "$ROOT" && pwd)" >> "$OUT"
echo "Generated: $(date)" >> "$OUT"
echo "" >> "$OUT"

find "$ROOT" \
  -type f \
  \( -name "*.kt" -o -name "*.java" -o -name "build.gradle" -o -name "build.gradle.kts" -o -name "settings.gradle" -o -name "settings.gradle.kts" -o -name "gradle.properties" -o -name "AndroidManifest.xml" -o -name "*.xml" -o -name "*.pro" -o -name "local.properties" \) \
  ! -path "*/build/*" ! -path "*/.gradle/*" ! -path "*/.idea/*" ! -path "*/.git/*" ! -path "*/.venv/*" ! -path "*/venv/*" ! -path "*/node_modules/*" ! -path "*/.cxx/*" ! -path "*/out/*" \
  | sort \
  | while IFS= read -r f; do
      rel="${f#"$ROOT"/}"
      [ "$rel" = "$f" ] && rel="$f"
      echo "===== BEGIN FILE: $rel =====" >> "$OUT"
      if command -v stat >/dev/null 2>&1; then
        sz=$(stat -f %z "$f" 2>/dev/null || stat -c %s "$f" 2>/dev/null || echo "?")
        echo "Size: $sz bytes" >> "$OUT"
      fi
      if command -v wc >/dev/null 2>&1; then
        echo "Lines: $(wc -l < "$f" | tr -d ' ')" >> "$OUT"
      fi
      if command -v shasum >/dev/null 2>&1; then
        echo "SHA256: $(shasum -a 256 "$f" | awk '{print $1}')" >> "$OUT"
      fi
      echo "" >> "$OUT"
      cat "$f" >> "$OUT"
      echo "" >> "$OUT"
      echo "===== END FILE: $rel =====" >> "$OUT"
      echo "" >> "$OUT"
    done

# Summary
COUNT=$(find "$ROOT" -type f \( -name "*.kt" -o -name "*.java" -o -name "build.gradle" -o -name "build.gradle.kts" -o -name "settings.gradle" -o -name "settings.gradle.kts" -o -name "gradle.properties" -o -name "AndroidManifest.xml" -o -name "*.xml" -o -name "*.pro" -o -name "local.properties" \) ! -path "*/build/*" ! -path "*/.gradle/*" ! -path "*/.idea/*" ! -path "*/.git/*" ! -path "*/.venv/*" ! -path "*/venv/*" ! -path "*/node_modules/*" ! -path "*/.cxx/*" ! -path "*/out/*" | wc -l | tr -d ' ')
echo "==== Summary ====" >> "$OUT"
echo "Files included: $COUNT" >> "$OUT"

echo "Wrote snapshot: $OUT"

