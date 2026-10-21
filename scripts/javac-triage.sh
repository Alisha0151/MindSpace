#!/usr/bin/env bash
# Weak but real signal when Maven Central is unreachable: compile ALL backend sources with javac and no
# dependencies, then report any error that is NOT a missing third-party symbol/package.
#   - "package X does not exist / cannot find symbol" for Spring, Jakarta, jjwt, springdoc ... is expected.
#   - anything else (syntax errors, internal types/methods not found, bad overrides of own code) is a real bug.
# Passing this does NOT mean the code compiles with its dependencies. Use `mvn verify` for that.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$(mktemp -d)"; trap 'rm -rf "$OUT"' EXIT
javac -proc:none -Xmaxerrs 100000 -d "$OUT" $(find "$ROOT/backend/src/main/java" -name '*.java') 2> "$OUT/err.txt"
total=$(grep -c 'error:' "$OUT/err.txt")
echo "javac errors total: $total"
echo "--- error kinds other than missing-dependency kinds (expected: none) ---"
grep 'error:' "$OUT/err.txt" | sed 's/.*error: //' | grep -vE 'cannot find symbol|does not exist|does not override' | sort | uniq -c
echo "--- unresolved symbols that belong to this project (expected: none) ---"
# internal packages appear as 'package com.mindspace...' or as symbols whose location is a com.mindspace type
grep -A3 'cannot find symbol' "$OUT/err.txt" | grep -E 'location:.*com\.mindspace' | grep -vE 'location: (class|interface) [A-Za-z]+(Controller|Service|Filter|Handler|Config|Repository|Dtos|Application)\b' | sort | uniq -c | head -20
echo "--- unresolved symbol names (all should be third-party) ---"
grep -A3 'cannot find symbol' "$OUT/err.txt" | grep 'symbol:' | sed 's/^ *symbol: *//' | sort -u | tr '\n' ';' | fold -w 150
echo
