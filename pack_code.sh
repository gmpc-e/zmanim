#!/usr/bin/env bash
set -euo pipefail

# Run from repo root (where settings.gradle[.kts] lives)
STAMP="$(date +%Y%m%d_%H%M)"
OUTDIR="dist"
OUTZIP="$OUTDIR/zmanim_payload_${STAMP}.zip"

mkdir -p "$OUTDIR"

zip -r "$OUTZIP" \
  app/src \
  app/build.gradle.kts \
  settings.gradle* \
  gradle/libs.versions.toml \
  --exclude \
  "*/build/*" \
  ".git/*" \
  ".idea/*" \
  "*.iml" \
  "*/.gradle/*" \
  "*/local.properties"

echo "Created: $OUTZIP"

