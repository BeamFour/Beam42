#!/bin/sh
# Generate an upstream regression test for one lens.
#
#   generate_upstream_test.sh <specfile> [exporter flags...]
#
# Runs three steps, all from the same ModelSpec so both sides describe the same
# system: emit the Python model, capture reference values by running it under
# upstream ray-optics, then emit the Java test with those values inlined.
#
# The generated test is written to
#   rayoptics/src/test/java/org/redukti/rayoptics/upstream/
#
# Requires a built tree (mvn compile) and a Python environment with upstream
# ray-optics installed. Set PYTHON to that interpreter; defaults to `python`.
# See README.md.
set -e

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/../../../.." && pwd)
PYTHON=${PYTHON:-python}

if [ $# -lt 1 ]; then
    sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
fi

# Under Git Bash / MSYS the JVM is a native Windows binary: it needs ; between
# classpath entries and Windows paths, not the /c/... form the shell uses.
case "$(uname -s 2>/dev/null)" in
    MINGW*|MSYS*|CYGWIN*) sep=';'; native_path() { cygpath -w "$1"; } ;;
    *)                    sep=':'; native_path() { printf '%s' "$1"; } ;;
esac

# Modules the exporter needs. Java silently ignores classpath entries that do
# not exist, so these are checked rather than assumed.
cp=""
for module in tools rayoptics mathlib render beam42; do
    classes="$repo_root/$module/target/classes"
    if [ ! -d "$classes" ]; then
        echo "Missing $classes - run 'mvn compile' first" >&2
        exit 1
    fi
    cp="${cp:+$cp$sep}$(native_path "$classes")"
done

exporter=org.redukti.exporters.RayOpticsExporter
out_dir="$repo_root/rayoptics/src/test/java/org/redukti/rayoptics/upstream"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
mkdir -p "$out_dir"

spec="$1"
shift

cd "$repo_root"
java -cp "$cp" "$exporter" --specfile "$spec" "$@" > "$work/model.py"
"$PYTHON" "$script_dir/dump_reference.py" "$work/model.py" > "$work/reference.txt"
java -cp "$cp" "$exporter" --specfile "$spec" "$@" \
     --reference "$work/reference.txt" > "$work/test.java"

name=$(sed -n 's/^public class \([A-Za-z0-9_]*\).*/\1/p' "$work/test.java" | head -1)
if [ -z "$name" ]; then
    echo "Could not determine generated class name" >&2
    exit 1
fi
cp "$work/test.java" "$out_dir/$name.java"
echo "$name.java: $(grep -c assertClose "$out_dir/$name.java") assertions from $(grep -vc '^#' "$work/reference.txt") reference values"
