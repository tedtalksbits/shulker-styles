#!/usr/bin/env bash
# Applies every client mixin without starting the game (no window, no natives needed): loads the mixin targets and
# runs Mixin's audit in a Fabric preLaunch entrypoint, then exits 0 (AUDIT RESULT OK) or 1. Catches mixin mistakes
# that otherwise only show up as a broken game (client 0.2.0: black screen). Linux/Git Bash; needs the client built.
#   ci/mixin-audit/run.sh [extra gradle args, e.g. --offline -I init.gradle]
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"; client="$here/../../fabric-client"
work="$(mktemp -d)"; trap 'rm -rf "$work"' EXIT
(cd "$client" && ./gradlew -q -Dcpfile="$work/cp" -I "$here/print-classpath.init.gradle" "$@" classes printRuntimeCp)
cp="$(cat "$work/cp")"
mkdir -p "$work/out" "$work/game/mods"
cp "$here/fabric.mod.json" "$work/out/"
"${JAVA:-java}" -version >/dev/null 2>&1
javac -cp "$cp" -d "$work/out" "$here/MixinAudit.java"
cd "$work" && "${JAVA:-java}" -Dfabric.development=true -cp "$work/out:$cp" net.fabricmc.loader.impl.launch.knot.KnotClient --gameDir "$work/game" 2>&1 | grep -E 'AUDIT|ERROR'
