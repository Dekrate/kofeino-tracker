#!/usr/bin/env python3
"""
Verifies that all string resource keys in values/strings.xml (default / English)
also exist in values-pl/strings.xml (Polish) and vice versa.
Exits with non-zero code if mismatches found.
"""

import xml.etree.ElementTree as ET
import sys
import os

def extract_keys(filepath):
    """Returns a set of all <string name="..."> keys from the file."""
    tree = ET.parse(filepath)
    root = tree.getroot()
    keys = set()
    for child in root:
        if child.tag == "string" and "name" in child.attrib:
            keys.add(child.attrib["name"])
    return keys

def check_module(module_path, module_name):
    default_file = os.path.join(module_path, "values", "strings.xml")
    pl_file = os.path.join(module_path, "values-pl", "strings.xml")

    if not os.path.exists(default_file):
        print(f"[FAIL] {module_name}: Missing default strings.xml at {default_file}")
        return False
    if not os.path.exists(pl_file):
        print(f"[FAIL] {module_name}: Missing Polish strings.xml at {pl_file}")
        return False

    en_keys = extract_keys(default_file)
    pl_keys = extract_keys(pl_file)

    missing_in_pl = en_keys - pl_keys
    missing_in_en = pl_keys - en_keys

    ok = True
    if missing_in_pl:
        print(f"[WARN] {module_name}: Keys in English but MISSING in Polish ({len(missing_in_pl)}):")
        for k in sorted(missing_in_pl):
            print(f"       - {k}")
        ok = False

    if missing_in_en:
        print(f"[WARN] {module_name}: Keys in Polish but MISSING in English ({len(missing_in_en)}):")
        for k in sorted(missing_in_en):
            print(f"       - {k}")
        ok = False

    if ok:
        print(f"[OK]   {module_name}: All {len(en_keys)} keys match perfectly between locales.")

    return ok

def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

    all_ok = True
    all_ok &= check_module(os.path.join(repo_root, "wear", "src", "main", "res"), "wear")
    all_ok &= check_module(os.path.join(repo_root, "app", "src", "main", "res"), "app (phone)")

    if all_ok:
        print("\n[PASS] All translation files are consistent!")
    else:
        print("\n[FAIL] Some translation files have mismatches. Fix before proceeding.")
        sys.exit(1)

if __name__ == "__main__":
    main()
