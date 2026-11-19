"""Verify the offline release boundary without reading or printing local credentials.

Source verification needs no Android SDK. --apk additionally checks the built package's DEX
and manifest; it never prints a matched token. Use --sentinel with synthetic test data only.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"
SETTING_FILE = "lifegame_ui_prefs.xml"
SECRET_PATTERN = re.compile(rb"sk-(?:proj-|svcacct-)?[A-Za-z0-9_-]{20,}")


def audit_source(root: Path) -> list[str]:
    errors: list[str] = []
    gradle = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
    if re.search(r"OPENAI_API_KEY|OPENAI_IMAGE_MODEL", gradle):
        errors.append("build: provider configuration must not be compiled into Android")
    if re.search(r"buildConfigField[^\n]*(?:API_KEY|SECRET)", gradle, re.I):
        errors.append("build: credential BuildConfig field detected")
    main = root / "app/src/main"
    manifest = ET.parse(main / "AndroidManifest.xml").getroot()
    permissions = [item.get(ANDROID + "name") for item in manifest.findall("uses-permission")]
    if "android.permission.INTERNET" in permissions:
        errors.append("manifest: offline release must not request INTERNET")
    application = manifest.find("application")
    if application is None or application.get(ANDROID + "usesCleartextTraffic") != "false":
        errors.append("manifest: cleartext traffic must be disabled")
    for attribute, expected in (("fullBackupContent", "@xml/backup_rules"), ("dataExtractionRules", "@xml/data_extraction_rules")):
        if application is None or application.get(ANDROID + attribute) != expected:
            errors.append(f"manifest: missing {attribute} privacy policy")
    for source in (main / "java").rglob("*.kt"):
        text = source.read_text(encoding="utf-8")
        if "api.openai.com" in text or re.search(r"\bOpenAiFaceSheetClient\s*\(", text):
            errors.append(f"source: direct provider client in {source.relative_to(root).as_posix()}")
        if SECRET_PATTERN.search(text.encode("utf-8")):
            errors.append(f"source: credential-shaped literal in {source.relative_to(root).as_posix()}")
    try:
        legacy = ET.parse(main / "res/xml/backup_rules.xml").getroot()
        modern = ET.parse(main / "res/xml/data_extraction_rules.xml").getroot()
        groups = [legacy, modern.find("cloud-backup"), modern.find("device-transfer")]
        for group in groups:
            if group is None or [(r.tag, r.get("domain"), r.get("path")) for r in group] != [
                ("include", "sharedpref", SETTING_FILE)
            ]:
                errors.append("backup: only non-photo UI settings may leave the device")
                break
    except (OSError, ET.ParseError):
        errors.append("backup: missing or malformed backup rules")
    return errors


def audit_apk(apk: Path, sentinel: str | None = None) -> list[str]:
    errors: list[str] = []
    with zipfile.ZipFile(apk) as archive:
        dex_files = [name for name in archive.namelist() if re.fullmatch(r"classes\d*\.dex", name)]
        if not dex_files:
            errors.append("apk: no DEX files found")
        manifest = archive.read("AndroidManifest.xml")
        permission = "android.permission.INTERNET"
        if permission.encode() in manifest or permission.encode("utf-16-le") in manifest:
            errors.append("apk: offline release requests INTERNET")
        forbidden = [b"OPENAI_API_KEY", b"api.openai.com", b"OpenAiFaceSheetClient"]
        if sentinel:
            forbidden.append(sentinel.encode("utf-8"))
        for name in dex_files:
            data = archive.read(name)
            if any(token in data for token in forbidden) or SECRET_PATTERN.search(data):
                errors.append(f"apk: provider credential/client marker detected in {name}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--sentinel", help="Synthetic test marker only; never pass a real credential")
    args = parser.parse_args()
    errors = audit_source(ROOT)
    if args.apk:
        errors.extend(audit_apk(args.apk, args.sentinel))
    print(json.dumps({"passed": not errors, "apk_checked": bool(args.apk), "errors": errors}, ensure_ascii=False))
    return int(bool(errors))


if __name__ == "__main__":
    raise SystemExit(main())
