"""Compare surefire results with known-defects.txt.

Passes only if the failing tests are exactly the documented known defects:
an unexpected failure means a regression, and a known defect that now passes
means the defect was fixed and known-defects.txt should be updated.
Usage: python3 .github/scripts/check_known_defects.py [reports_dir] [known_defects_file]
"""
import glob
import sys
import xml.etree.ElementTree as ET

reports = sys.argv[1] if len(sys.argv) > 1 else "target/surefire-reports"
known_file = sys.argv[2] if len(sys.argv) > 2 else "known-defects.txt"

with open(known_file, encoding="utf-8") as fh:
    known = {ln.strip() for ln in fh if ln.strip() and not ln.startswith("#")}

total, failing = 0, set()
for path in glob.glob(f"{reports}/TEST-*.xml"):
    for case in ET.parse(path).getroot().iter("testcase"):
        total += 1
        name = f"{case.get('classname')}#{case.get('name')}"
        if case.find("failure") is not None or case.find("error") is not None:
            failing.add(name)

if total == 0:
    sys.exit(f"No test results found in {reports}")

print(f"{total} tests, {len(failing)} failing, {len(known)} known defects")
unexpected, fixed = failing - known, known - failing
for name in sorted(failing & known):
    print(f"  expected failure (known defect): {name}")
for name in sorted(unexpected):
    print(f"  UNEXPECTED FAILURE: {name}")
for name in sorted(fixed):
    print(f"  KNOWN DEFECT NOW PASSES (update known-defects.txt): {name}")
sys.exit(1 if unexpected or fixed else 0)
