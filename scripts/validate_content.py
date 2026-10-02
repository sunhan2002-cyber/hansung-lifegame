#!/usr/bin/env python3
"""Validate LifeGame content JSON files.

This script checks the app content files used by ContentRepository:
- events.sample.json
- endings.sample.json
- image_ids.sample.json
- characters.sample.json

It is intentionally dependency-free so every teammate can run it with plain Python.
"""
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets"

STAT_KEYS = {"건강", "운동능력", "지력", "사회성", "경제력", "행복", "운"}
EVENT_REQUIRED = {
    "eventId",
    "stage",
    "type",
    "title",
    "text",
    "imageId",
    "conditions",
    "choices",
    "priority",
    "isFallback",
}
CHOICE_REQUIRED = {"choiceId", "label", "statDelta", "resultText", "addFlags"}
ENDING_REQUIRED = {"endingId", "title", "priority", "conditions", "summary", "isDefault"}
IMAGE_REQUIRED = {"imageId", "usage", "stage", "description", "fileName", "fallbackText"}
VALID_STAGES = {"INFANT", "CHILD", "TEEN", "ADULT"}
VALID_EVENT_TYPES = {"NORMAL", "SPECIAL"}


class ValidationReport:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.metrics: dict[str, Any] = {}

    def error(self, message: str) -> None:
        self.errors.append(message)

    def warn(self, message: str) -> None:
        self.warnings.append(message)

    @property
    def ok(self) -> bool:
        return not self.errors


def load_json(name: str, report: ValidationReport) -> Any:
    path = ASSET_DIR / name
    if not path.exists():
        report.error(f"missing file: {path}")
        return []
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        report.error(f"invalid JSON in {name}: line {exc.lineno}, column {exc.colno}: {exc.msg}")
        return []


def find_duplicates(values: list[str]) -> list[str]:
    return sorted(value for value, count in Counter(values).items() if count > 1)


def ensure_keys(item: dict[str, Any], required: set[str], label: str, report: ValidationReport) -> None:
    missing = sorted(required - set(item.keys()))
    if missing:
        report.error(f"{label} missing fields: {', '.join(missing)}")


def check_stat_object(stats: Any, label: str, report: ValidationReport) -> None:
    if stats is None:
        return
    if not isinstance(stats, dict):
        report.error(f"{label} stats must be an object")
        return
    for key, value in stats.items():
        if key not in STAT_KEYS:
            report.warn(f"{label} uses unknown stat key: {key}")
        if not isinstance(value, int):
            report.error(f"{label} stat {key} must be an integer")


def validate_events(events: Any, image_ids: set[str], report: ValidationReport) -> None:
    if not isinstance(events, list):
        report.error("events.sample.json root must be a list")
        return

    event_ids: list[str] = []
    stages: Counter[str] = Counter()
    types: Counter[str] = Counter()
    choice_counts: Counter[int] = Counter()

    for index, event in enumerate(events):
        label = f"event[{index}]"
        if not isinstance(event, dict):
            report.error(f"{label} must be an object")
            continue
        ensure_keys(event, EVENT_REQUIRED, label, report)

        event_id = str(event.get("eventId", f"<missing:{index}>"))
        event_ids.append(event_id)
        stage = event.get("stage")
        event_type = event.get("type")
        stages[str(stage)] += 1
        types[str(event_type)] += 1

        if stage not in VALID_STAGES:
            report.error(f"{event_id} has invalid stage: {stage}")
        if event_type not in VALID_EVENT_TYPES:
            report.error(f"{event_id} has invalid type: {event_type}")
        if event.get("imageId") not in image_ids:
            report.error(f"{event_id} uses missing imageId: {event.get('imageId')}")

        conditions = event.get("conditions", {})
        if not isinstance(conditions, dict):
            report.error(f"{event_id}.conditions must be an object")
            conditions = {}
        check_stat_object(conditions.get("stats", {}), f"{event_id}.conditions", report)
        for array_key in ("requiredFlags", "blockedFlags"):
            if not isinstance(conditions.get(array_key, []), list):
                report.error(f"{event_id}.conditions.{array_key} must be a list")

        choices = event.get("choices")
        if not isinstance(choices, list):
            report.error(f"{event_id}.choices must be a list")
            continue
        choice_counts[len(choices)] += 1
        if len(choices) < 2:
            report.error(f"{event_id} must have at least 2 choices")

        choice_ids: list[str] = []
        for choice_index, choice in enumerate(choices):
            choice_label = f"{event_id}.choices[{choice_index}]"
            if not isinstance(choice, dict):
                report.error(f"{choice_label} must be an object")
                continue
            ensure_keys(choice, CHOICE_REQUIRED, choice_label, report)
            choice_ids.append(str(choice.get("choiceId", f"<missing:{choice_index}>")))
            check_stat_object(choice.get("statDelta", {}), f"{choice_label}.statDelta", report)
            if not isinstance(choice.get("addFlags", []), list):
                report.error(f"{choice_label}.addFlags must be a list")
        for duplicate in find_duplicates(choice_ids):
            report.error(f"{event_id} has duplicate choiceId: {duplicate}")

    for duplicate in find_duplicates(event_ids):
        report.error(f"duplicate eventId: {duplicate}")

    report.metrics["event_count"] = len(events)
    report.metrics["event_stages"] = dict(stages)
    report.metrics["event_types"] = dict(types)
    report.metrics["choice_counts"] = dict(choice_counts)

    if len(events) < 200:
        report.warn(f"event count is {len(events)}; week6 scenario target is 200+")


def validate_endings(endings: Any, report: ValidationReport) -> None:
    if not isinstance(endings, list):
        report.error("endings.sample.json root must be a list")
        return

    ending_ids: list[str] = []
    default_count = 0
    for index, ending in enumerate(endings):
        label = f"ending[{index}]"
        if not isinstance(ending, dict):
            report.error(f"{label} must be an object")
            continue
        ensure_keys(ending, ENDING_REQUIRED, label, report)
        ending_id = str(ending.get("endingId", f"<missing:{index}>"))
        ending_ids.append(ending_id)
        if ending.get("isDefault") is True:
            default_count += 1
        conditions = ending.get("conditions", {})
        if not isinstance(conditions, dict):
            report.error(f"{ending_id}.conditions must be an object")
            conditions = {}
        check_stat_object(conditions.get("minStats", {}), f"{ending_id}.conditions", report)
        for array_key in ("requiredFlags", "blockedFlags"):
            if array_key in conditions and not isinstance(conditions.get(array_key), list):
                report.error(f"{ending_id}.conditions.{array_key} must be a list")

    for duplicate in find_duplicates(ending_ids):
        report.error(f"duplicate endingId: {duplicate}")
    if default_count < 1:
        report.error("at least one default ending is required")
    if len(endings) < 30:
        report.error(f"ending count must be 30+ for week6; current={len(endings)}")

    report.metrics["ending_count"] = len(endings)
    report.metrics["default_ending_count"] = default_count


def validate_images(images: Any, report: ValidationReport) -> set[str]:
    if not isinstance(images, list):
        report.error("image_ids.sample.json root must be a list")
        return set()

    image_ids: list[str] = []
    for index, image in enumerate(images):
        label = f"image[{index}]"
        if not isinstance(image, dict):
            report.error(f"{label} must be an object")
            continue
        ensure_keys(image, IMAGE_REQUIRED, label, report)
        image_ids.append(str(image.get("imageId", f"<missing:{index}>")))

    for duplicate in find_duplicates(image_ids):
        report.error(f"duplicate imageId: {duplicate}")

    report.metrics["image_count"] = len(images)
    return set(image_ids)


def validate_characters(characters: Any, report: ValidationReport) -> None:
    if not isinstance(characters, list):
        report.error("characters.sample.json root must be a list")
        return
    character_ids = [str(item.get("characterId", item.get("id", f"<missing:{idx}>"))) for idx, item in enumerate(characters) if isinstance(item, dict)]
    for duplicate in find_duplicates(character_ids):
        report.error(f"duplicate character id: {duplicate}")
    report.metrics["character_count"] = len(characters)


def main() -> int:
    report = ValidationReport()
    events = load_json("events.sample.json", report)
    endings = load_json("endings.sample.json", report)
    images = load_json("image_ids.sample.json", report)
    characters = load_json("characters.sample.json", report)

    image_ids = validate_images(images, report)
    validate_events(events, image_ids, report)
    validate_endings(endings, report)
    validate_characters(characters, report)

    print("LifeGame content validation")
    print("===========================")
    for key, value in report.metrics.items():
        print(f"{key}: {value}")

    if report.warnings:
        print("\nWarnings")
        for warning in report.warnings:
            print(f"- {warning}")

    if report.errors:
        print("\nErrors")
        for error in report.errors:
            print(f"- {error}")
        return 1

    print("\nPASS: content JSON files are valid.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
