#!/usr/bin/env bash
# Compiles ONLY the framework-free Java sources (no Spring/Jakarta/Jackson/jjwt imports) with strict javac
# flags and runs the matching unit tests. It needs a JUnit 5 *API-compatible* runner: pass JUNIT_SHIM_DIR
# pointing at a directory with compiled org.junit.jupiter.api stubs and a `Runner` main class, or run the
# real tests with Maven (`mvn test`) where dependencies are available.
#
# This script exists because Maven Central was unreachable while the pure logic was being written. It does
# NOT replace `mvn verify`.
set -u
: "${JUNIT_SHIM_DIR:?set JUNIT_SHIM_DIR to the compiled JUnit-API stand-in (see docs/TESTING.md)}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
B="$ROOT/backend"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
# Explicit allow-list of the framework-free sources (checked for framework imports below).
MAIN_SRC=$(cd "$B/src/main/java" && find com/mindspace/common com/mindspace/checkin/domain com/mindspace/habit/domain \
    com/mindspace/analytics/domain -name '*.java' ! -path '*/web/*' ! -name 'PageResponse.java'; \
  echo com/mindspace/auth/TokenHasher.java com/mindspace/auth/PasswordPolicy.java)
FRAMEWORK='^import (jakarta|org\.springframework|io\.swagger|io\.jsonwebtoken|com\.fasterxml|org\.slf4j|org\.testcontainers|org\.mockito)'
for f in $MAIN_SRC; do
  if grep -qE "$FRAMEWORK" "$B/src/main/java/$f"; then echo "allow-listed file imports a framework: $f"; exit 2; fi
done
MAIN_FILES=$(for f in $MAIN_SRC; do echo "$B/src/main/java/$f"; done)
TEST_FILES=$(grep -LE "$FRAMEWORK" $(find "$B/src/test/java" -name '*.java' ! -path '*/it/*' ! -path '*/support/*'))
mkdir -p "$OUT/main" "$OUT/test"
javac -Xlint:all,-serial -Werror -d "$OUT/main" $MAIN_FILES || { echo "MAIN COMPILE FAILED"; exit 2; }
javac -Xlint:all,-serial -Werror -cp "$JUNIT_SHIM_DIR:$OUT/main" -d "$OUT/test" $TEST_FILES || { echo "TEST COMPILE FAILED"; exit 2; }
java -cp "$JUNIT_SHIM_DIR:$OUT/main:$OUT/test" Runner "$OUT/test"
