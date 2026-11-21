#!/usr/bin/env bash
# Run from anywhere:  bash tests/run_tests.sh        (needs a JDK; set JAVAC to override)
set -u
cd "$(dirname "$0")/.."
JAVAC=${JAVAC:-javac}
rm -rf out && mkdir out && $JAVAC -d out src/*.java || { echo "Compile failed"; exit 1; }

TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
pass=0; fail=0
OUT=""

ok()   { echo "PASS  $1"; pass=$((pass+1)); }
bad()  { echo "FAIL  $1  ($2)"; fail=$((fail+1)); printf '%s\n' "$OUT" | tail -15; }

# run <config-file> <stdin-text>   (works on a temp copy so saved state never touches your data)
run()  { cp "$1" "$TMP/cfg.txt"; OUT=$(printf "$2" | java -cp out Main "$TMP/cfg.txt" 2>&1); }
has()  { if printf '%s\n' "$OUT" | grep -qF -- "$2"; then ok "$1"; else bad "$1" "expected: $2"; fi; }
hasnt(){ if printf '%s\n' "$OUT" | grep -qF -- "$2"; then bad "$1" "unexpected: $2"; else ok "$1"; fi; }
line() { printf '%s\n' "$OUT" | grep -nF -- "$1" | sed -n "${2:-1}p" | cut -d: -f1; }
order(){ a=$(line "$2" "${4:-1}"); b=$(line "$3" "${5:-1}")
         if [ -n "$a" ] && [ -n "$b" ] && [ "$a" -lt "$b" ]; then ok "$1"; else bad "$1" "'$2' should come before '$3'"; fi; }

D=tests/default.txt; S=tests/single_phase.txt

echo "--- Official test cases (PS 68) ---"
run $D '1\n5\n';                         has "TC1 normal: start -> North GREEN"            "Signal: North GREEN"
run $S '1\n2\n5\n';                      has "TC2 boundary: single phase stays"            "Signal remains in the configured phase."
run $D '1\n3\nZ\n5\n';                   has "TC3 invalid road Z"                          "Invalid road. Request ignored."
run $D '1\n3\nSouth\n\nn\n5\n';          has "TC4 emergency from South"                    "After a safe transition, South receives priority."

echo "--- Extra checks ---"
run $D '3\nZ\n5\n';                      has "invalid road is rejected even before start"  "Invalid road. Request ignored."
run $D '3\nSouth\n5\n';                  has "valid road before start is refused"          "Simulator not started"
run $D '2\n5\n';                         has "next phase before start is refused"          "Simulator not started"
run $D '1\n2\n2\n2\n2\n5\n';             order "cycle order N->E"  "Signal: North GREEN" "Signal: East GREEN"
order "cycle order E->S" "Signal: East GREEN" "Signal: South GREEN"
order "cycle order S->W" "Signal: South GREEN" "Signal: West GREEN"
order "cycle wraps W->N" "Signal: West GREEN" "Signal: North GREEN" 1 2
run $D '1\n3\nsouth\n\nn\n4\n5\n';       has "road names are case-insensitive"             "Emergency request accepted: South (AMBULANCE)"
order "safe sequence: YELLOW before RED" "Signal: North YELLOW" "Signal: North RED"
order "safe sequence: RED before new GREEN" "Signal: North RED" "Signal: South GREEN"
n=$(printf '%s\n' "$OUT" | grep -c '^  [A-Za-z]* *: GREEN$'); [ "$n" = 1 ] && ok "exactly one GREEN in status" || bad "exactly one GREEN in status" "found $n"
run $D '1\n3\nNorth\n\nn\n5\n';          has "emergency on current green: no transition"   "already GREEN. Priority granted without any transition."
run $D '1\n3\nEast\n3\ny\nSouth\n1\nn\n5\n'
order "priority queue: Ambulance(South) before Police(East)" "Signal: South GREEN [EMERGENCY - AMBULANCE]" "Signal: East GREEN [EMERGENCY - POLICE]"
run $D '1\n3\nEast\n2\ny\nWest\n2\nn\n5\n'
order "same priority -> first come first served" "Signal: East GREEN [EMERGENCY" "Signal: West GREEN [EMERGENCY"
run $D '1\n3\nSouth\n\nn\n2\n5\n';       order "cycle resumes at East after emergency" "Signal: South GREEN [EMERGENCY" "Signal: East GREEN" 1 1
run $D '1\n3\nEast\n\nn\n2\n5\n';        has "emergency road already served its turn"      "East is already GREEN (emergency turn counted)"
run $D '1\n3\nSouth\nabc\n2\nn\n5\n';    has "invalid vehicle type re-asked"               "Invalid vehicle type. Try again."
run $D '9\nabc\n5\n';                    has "invalid menu choice"                         "Invalid choice. Enter 1-5."

echo "--- File handling ---"
cp $D "$TMP/p.txt"
printf '1\n2\n5\n' | java -cp out Main "$TMP/p.txt" >/dev/null 2>&1
grep -q 'LAST_GREEN=East' "$TMP/p.txt" && ok "state saved on exit (LAST_GREEN)" || bad "state saved on exit" "LAST_GREEN=East not found"
OUT=$(printf '5\n' | java -cp out Main "$TMP/p.txt" 2>&1);  has "state loaded on next start" "Previous session ended on East GREEN"
printf '1\n' | java -cp out Main "$TMP/p.txt" >/dev/null 2>&1   # EOF must also save
grep -q 'LAST_GREEN=North' "$TMP/p.txt" && ok "EOF (Ctrl+D / Ctrl+Z) also saves" || bad "EOF save" "LAST_GREEN=North not found"
rm -f "$TMP/new.txt"; OUT=$(java -cp out Main "$TMP/new.txt" </dev/null 2>&1)
[ -f "$TMP/new.txt" ] && has "missing config -> default created" "Created default" || bad "missing config" "no file created"
printf 'ROADS=North\nPHASES=Z\n' > "$TMP/bad.txt"
OUT=$(java -cp out Main "$TMP/bad.txt" </dev/null 2>&1); rc=$?
[ $rc -eq 1 ] && has "bad config -> clean error + exit 1" "Config error" || bad "bad config exit code" "rc=$rc"

echo; echo "Passed: $pass   Failed: $fail"
[ $fail -eq 0 ]
