#!/usr/bin/env python3
"""Validate LifeGame content JSON files.

Week 7 DB/QA validation covers both the current app-compatible content and the
v4 redesign data that will be connected by the frontend/backend PRs:
- events.sample.json / endings.sample.json / image_ids.sample.json / characters.sample.json
- periods.sample.json / focuses.sample.json

The script is dependency-free. If a referenced PNG file exists, it reads the PNG
header directly and validates 300x225 dimensions; missing image files are allowed
while the project is still using placeholders.
"""
from __future__ import annotations

import json
import re
import struct
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets"
IMAGE_DIRS = [
    ASSET_DIR,
    ASSET_DIR / "images",
    ROOT / "app" / "src" / "main" / "res" / "drawable",
    ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi",
]

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
OUTCOME_REQUIRED = {"outcomeId", "text", "chance", "primary", "hidden", "death"}
ENDING_REQUIRED = {"endingId", "title", "priority", "conditions", "summary", "isDefault"}
IMAGE_REQUIRED = {"imageId", "usage", "stage", "description", "fileName", "fallbackText"}
PERIOD_REQUIRED = {"periodId", "displayName", "chapterTitle", "order", "years"}
FOCUS_REQUIRED = {
    "focusId",
    "period",
    "name",
    "description",
    "annualStatDelta",
    "sceneImageId",
    "momentImageId",
    "yearlyLogTemplates",
}

VALID_STAGES = {"INFANT", "CHILD", "TEEN", "ADULT"}
VALID_PERIODS = {"INFANT", "CHILD", "TEEN", "YOUNG_ADULT", "MIDDLE"}
VALID_EVENT_TYPES = {"NORMAL", "SPECIAL"}
VALID_EVENT_KINDS = {"MILESTONE", "INCIDENT", "BIG_INCIDENT"}
VALID_IMAGE_USAGES = {
    "event",
    "character",
    "ending",
    "scene",
    "pov",
    "moment",
    "face_sheet",
    "body_part",
    "placeholder",
}
ID_PATTERN = re.compile(r"^[a-z0-9_]+$")
PLACEHOLDER_PATTERN = re.compile(r"\{([^{}]+)\}")
ALLOWED_PLACEHOLDERS = {"name", "nameCall"}
REQUIRED_IMAGE_WIDTH = 300
REQUIRED_IMAGE_HEIGHT = 225


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


def load_json(name: str, report: ValidationReport, *, required: bool = True) -> Any:
    path = ASSET_DIR / name
    if not path.exists():
        if required:
            report.error(f"missing file: {path}")
        else:
            report.warn(f"optional file missing: {path}")
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


def check_id(value: Any, label: str, report: ValidationReport) -> None:
    if not isinstance(value, str) or not value:
        report.error(f"{label} must be a non-empty string")
        return
    if not ID_PATTERN.match(value):
        report.error(f"{label} must use lowercase letters, numbers, and underscores only: {value}")


def check_stat_object(stats: Any, label: str, report: ValidationReport, *, allow_empty: bool = True) -> None:
    if stats is None:
        if not allow_empty:
            report.error(f"{label} stats must not be null")
        return
    if not isinstance(stats, dict):
        report.error(f"{label} stats must be an object")
        return
    for key, value in stats.items():
        if key not in STAT_KEYS:
            report.warn(f"{label} uses unknown stat key: {key}")
        if not isinstance(value, int):
            report.error(f"{label} stat {key} must be an integer")


def check_placeholders(text: Any, label: str, report: ValidationReport) -> None:
    if not isinstance(text, str):
        return
    for token in PLACEHOLDER_PATTERN.findall(text):
        if token not in ALLOWED_PLACEHOLDERS:
            report.error(f"{label} uses unsupported placeholder: {{{token}}}")
    if "nameCall" in text and "{nameCall}" not in text:
        report.warn(f"{label} may contain unwrapped nameCall placeholder")
    if "{namecall}" in text or "{NameCall}" in text:
        report.error(f"{label} has incorrectly cased nameCall placeholder")


def check_string_array(value: Any, label: str, report: ValidationReport) -> None:
    if not isinstance(value, list):
        report.error(f"{label} must be a list")
        return
    for index, item in enumerate(value):
        if not isinstance(item, str):
            report.error(f"{label}[{index}] must be a string")


def read_png_size(path: Path) -> tuple[int, int] | None:
    try:
        with path.open("rb") as fp:
            header = fp.read(24)
        if len(header) < 24 or header[:8] != b"\x89PNG\r\n\x1a\n":
            return None
        return struct.unpack(">II", header[16:24])
    except OSError:
        return None


def find_image_file(file_name: str) -> Path | None:
    for directory in IMAGE_DIRS:
        candidate = directory / file_name
        if candidate.exists():
            return candidate
    return None


def validate_images(images: Any, report: ValidationReport) -> set[str]:
    if not isinstance(images, list):
        report.error("image_ids.sample.json root must be a list")
        return set()

    image_ids: list[str] = []
    usage_counts: Counter[str] = Counter()
    for index, image in enumerate(images):
        label = f"image[{index}]"
        if not isinstance(image, dict):
            report.error(f"{label} must be an object")
            continue
        ensure_keys(image, IMAGE_REQUIRED, label, report)
        image_id = str(image.get("imageId", f"<missing:{index}>"))
        image_ids.append(image_id)
        check_id(image_id, f"{label}.imageId", report)

        usage = image.get("usage")
        usage_counts[str(usage)] += 1
        if usage not in VALID_IMAGE_USAGES:
            report.error(f"{image_id} has invalid image usage: {usage}")

        file_name = image.get("fileName")
        if not isinstance(file_name, str) or not file_name.endswith(".png"):
            report.error(f"{image_id}.fileName must be a PNG file name")
        elif file_name != f"{image_id}.png":
            report.error(f"{image_id}.fileName must match imageId: expected {image_id}.png")

        width = image.get("width")
        height = image.get("height")
        if width is not None and width != REQUIRED_IMAGE_WIDTH:
            report.error(f"{image_id}.width must be {REQUIRED_IMAGE_WIDTH}")
        if height is not None and height != REQUIRED_IMAGE_HEIGHT:
            report.error(f"{image_id}.height must be {REQUIRED_IMAGE_HEIGHT}")

        existing_file = find_image_file(file_name) if isinstance(file_name, str) else None
        if existing_file:
            size = read_png_size(existing_file)
            if size is None:
                report.error(f"{image_id} file is not a valid PNG: {existing_file}")
            elif size != (REQUIRED_IMAGE_WIDTH, REQUIRED_IMAGE_HEIGHT):
                report.error(f"{image_id} image size must be 300x225; current={size[0]}x{size[1]}")

        desc = f"{image.get('description', '')} {image.get('fallbackText', '')}"
        if any(word in desc.lower() for word in ["watermark", "logo text", "caption text"]):
            report.warn(f"{image_id} description mentions text/watermark; verify generated asset")

    for duplicate in find_duplicates(image_ids):
        report.error(f"duplicate imageId: {duplicate}")

    report.metrics["image_count"] = len(images)
    report.metrics["image_usage_counts"] = dict(usage_counts)
    return set(image_ids)


def validate_outcomes(
    outcomes: Any,
    label: str,
    report: ValidationReport,
    *,
    stage_or_period: str | None = None,
) -> None:
    if not isinstance(outcomes, list):
        report.error(f"{label}.outcomes must be a list")
        return
    if not outcomes:
        report.error(f"{label}.outcomes must not be empty")
        return

    outcome_ids: list[str] = []
    primary_count = 0
    chance_sum = 0
    for index, outcome in enumerate(outcomes):
        outcome_label = f"{label}.outcomes[{index}]"
        if not isinstance(outcome, dict):
            report.error(f"{outcome_label} must be an object")
            continue
        ensure_keys(outcome, OUTCOME_REQUIRED, outcome_label, report)
        outcome_id = str(outcome.get("outcomeId", f"<missing:{index}>"))
        outcome_ids.append(outcome_id)
        check_id(outcome_id, f"{outcome_label}.outcomeId", report)
        check_placeholders(outcome.get("text"), f"{outcome_label}.text", report)

        chance = outcome.get("chance")
        if not isinstance(chance, int) or chance < 0:
            report.error(f"{outcome_label}.chance must be a non-negative integer")
        else:
            chance_sum += chance

        if outcome.get("primary") is True:
            primary_count += 1
        if not isinstance(outcome.get("hidden"), bool):
            report.error(f"{outcome_label}.hidden must be a boolean")
        if not isinstance(outcome.get("death"), bool):
            report.error(f"{outcome_label}.death must be a boolean")

        if outcome.get("hidden") is True and outcome.get("primary") is True:
            report.error(f"{outcome_label} cannot be both hidden and primary")
        if outcome.get("hidden") is True and outcome.get("previewText"):
            report.error(f"{outcome_label} hidden outcome must not define previewText")
        if outcome.get("death") is True and stage_or_period in {"INFANT", "CHILD"}:
            report.error(f"{outcome_label} death outcome is forbidden in INFANT/CHILD")

        check_stat_object(outcome.get("statDelta", {}), f"{outcome_label}.statDelta", report)
        if "addFlags" in outcome:
            check_string_array(outcome.get("addFlags"), f"{outcome_label}.addFlags", report)

    if chance_sum <= 0:
        report.error(f"{label}.outcomes total chance must be positive")
    if primary_count != 1:
        report.error(f"{label}.outcomes must have exactly one primary outcome; current={primary_count}")
    for duplicate in find_duplicates(outcome_ids):
        report.error(f"{label}.outcomes has duplicate outcomeId: {duplicate}")


def validate_events(events: Any, image_ids: set[str], report: ValidationReport) -> None:
    if not isinstance(events, list):
        report.error("events.sample.json root must be a list")
        return

    event_ids: list[str] = []
    stages: Counter[str] = Counter()
    types: Counter[str] = Counter()
    kinds: Counter[str] = Counter()
    choice_counts: Counter[int] = Counter()
    event_with_outcomes = 0

    for index, event in enumerate(events):
        label = f"event[{index}]"
        if not isinstance(event, dict):
            report.error(f"{label} must be an object")
            continue
        ensure_keys(event, EVENT_REQUIRED, label, report)

        event_id = str(event.get("eventId", f"<missing:{index}>"))
        event_ids.append(event_id)
        check_id(event_id, f"{label}.eventId", report)
        stage = event.get("stage")
        event_type = event.get("type")
        kind = event.get("kind")
        stages[str(stage)] += 1
        types[str(event_type)] += 1
        kinds[str(kind)] += 1

        if stage not in VALID_STAGES:
            report.error(f"{event_id} has invalid stage: {stage}")
        if event_type not in VALID_EVENT_TYPES:
            report.error(f"{event_id} has invalid type: {event_type}")
        if kind not in VALID_EVENT_KINDS:
            report.error(f"{event_id} has invalid v4 kind: {kind}")
        if event.get("imageId") not in image_ids:
            report.error(f"{event_id} uses missing imageId: {event.get('imageId')}")

        check_placeholders(event.get("title"), f"{event_id}.title", report)
        check_placeholders(event.get("text"), f"{event_id}.text", report)

        conditions = event.get("conditions", {})
        if not isinstance(conditions, dict):
            report.error(f"{event_id}.conditions must be an object")
            conditions = {}
        check_stat_object(conditions.get("stats", {}), f"{event_id}.conditions", report)
        for array_key in ("requiredFlags", "blockedFlags"):
            check_string_array(conditions.get(array_key, []), f"{event_id}.conditions.{array_key}", report)

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
            choice_id = str(choice.get("choiceId", f"<missing:{choice_index}>"))
            choice_ids.append(choice_id)
            check_placeholders(choice.get("label"), f"{choice_label}.label", report)
            check_placeholders(choice.get("resultText"), f"{choice_label}.resultText", report)
            check_stat_object(choice.get("statDelta", {}), f"{choice_label}.statDelta", report)
            check_string_array(choice.get("addFlags", []), f"{choice_label}.addFlags", report)
            if "outcomes" in choice:
                event_with_outcomes += 1
                validate_outcomes(choice.get("outcomes"), choice_label, report, stage_or_period=str(stage))
            else:
                report.error(f"{choice_label} missing v4 outcomes")
        for duplicate in find_duplicates(choice_ids):
            report.error(f"{event_id} has duplicate choiceId: {duplicate}")

    for duplicate in find_duplicates(event_ids):
        report.error(f"duplicate eventId: {duplicate}")

    report.metrics["event_count"] = len(events)
    report.metrics["event_stages"] = dict(stages)
    report.metrics["event_types"] = dict(types)
    report.metrics["event_kinds"] = dict(kinds)
    report.metrics["choice_counts"] = dict(choice_counts)
    report.metrics["choices_with_v4_outcomes"] = event_with_outcomes

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
        check_id(ending_id, f"{label}.endingId", report)
        check_placeholders(ending.get("title"), f"{ending_id}.title", report)
        check_placeholders(ending.get("summary"), f"{ending_id}.summary", report)
        if ending.get("isDefault") is True:
            default_count += 1
        conditions = ending.get("conditions", {})
        if not isinstance(conditions, dict):
            report.error(f"{ending_id}.conditions must be an object")
            conditions = {}
        check_stat_object(conditions.get("minStats", {}), f"{ending_id}.conditions.minStats", report)
        check_stat_object(conditions.get("maxStats", {}), f"{ending_id}.conditions.maxStats", report)
        for array_key in ("requiredFlags", "blockedFlags"):
            if array_key in conditions:
                check_string_array(conditions.get(array_key), f"{ending_id}.conditions.{array_key}", report)

    for duplicate in find_duplicates(ending_ids):
        report.error(f"duplicate endingId: {duplicate}")
    if default_count < 1:
        report.error("at least one default ending is required")
    if len(endings) < 30:
        report.error(f"ending count must be 30+; current={len(endings)}")

    report.metrics["ending_count"] = len(endings)
    report.metrics["default_ending_count"] = default_count


def validate_characters(characters: Any, report: ValidationReport) -> None:
    if not isinstance(characters, list):
        report.error("characters.sample.json root must be a list")
        return
    character_ids = [
        str(item.get("characterId", item.get("id", f"<missing:{idx}>")))
        for idx, item in enumerate(characters)
        if isinstance(item, dict)
    ]
    for duplicate in find_duplicates(character_ids):
        report.error(f"duplicate character id: {duplicate}")
    report.metrics["character_count"] = len(characters)


def validate_periods(periods: Any, report: ValidationReport) -> set[str]:
    if not isinstance(periods, list):
        report.error("periods.sample.json root must be a list")
        return set()
    period_ids: list[str] = []
    orders: list[int] = []
    total_years = 0
    for index, period in enumerate(periods):
        label = f"period[{index}]"
        if not isinstance(period, dict):
            report.error(f"{label} must be an object")
            continue
        ensure_keys(period, PERIOD_REQUIRED, label, report)
        period_id = str(period.get("periodId", f"<missing:{index}>"))
        period_ids.append(period_id)
        if period_id not in VALID_PERIODS:
            report.error(f"{label}.periodId has invalid period: {period_id}")
        order = period.get("order")
        years = period.get("years")
        if not isinstance(order, int) or order < 0:
            report.error(f"{period_id}.order must be a non-negative integer")
        else:
            orders.append(order)
        if not isinstance(years, int) or years <= 0:
            report.error(f"{period_id}.years must be a positive integer")
        else:
            total_years += years
    for duplicate in find_duplicates(period_ids):
        report.error(f"duplicate periodId: {duplicate}")
    for duplicate in find_duplicates([str(order) for order in orders]):
        report.error(f"duplicate period order: {duplicate}")
    missing = sorted(VALID_PERIODS - set(period_ids))
    if missing:
        report.error(f"periods.sample.json missing periods: {', '.join(missing)}")
    report.metrics["period_count"] = len(periods)
    report.metrics["period_total_years"] = total_years
    return set(period_ids)


def validate_focuses(focuses: Any, period_ids: set[str], image_ids: set[str], report: ValidationReport) -> None:
    if not isinstance(focuses, list):
        report.error("focuses.sample.json root must be a list")
        return
    focus_ids: list[str] = []
    counts: Counter[str] = Counter()
    for index, focus in enumerate(focuses):
        label = f"focus[{index}]"
        if not isinstance(focus, dict):
            report.error(f"{label} must be an object")
            continue
        ensure_keys(focus, FOCUS_REQUIRED, label, report)
        focus_id = str(focus.get("focusId", f"<missing:{index}>"))
        focus_ids.append(focus_id)
        check_id(focus_id, f"{label}.focusId", report)
        period = focus.get("period")
        counts[str(period)] += 1
        if period not in period_ids:
            report.error(f"{focus_id} uses missing period: {period}")
        check_stat_object(focus.get("annualStatDelta", {}), f"{focus_id}.annualStatDelta", report, allow_empty=False)
        for image_key in ("sceneImageId", "momentImageId"):
            image_id = focus.get(image_key)
            if image_id not in image_ids:
                report.error(f"{focus_id}.{image_key} uses missing imageId: {image_id}")
        logs = focus.get("yearlyLogTemplates")
        if not isinstance(logs, list) or len(logs) < 3:
            report.error(f"{focus_id}.yearlyLogTemplates must have at least 3 strings")
        else:
            for log_index, log in enumerate(logs):
                check_placeholders(log, f"{focus_id}.yearlyLogTemplates[{log_index}]", report)
    for duplicate in find_duplicates(focus_ids):
        report.error(f"duplicate focusId: {duplicate}")
    for period_id in sorted(period_ids):
        if counts[period_id] != 5:
            report.error(f"{period_id} must have exactly 5 focuses; current={counts[period_id]}")
    report.metrics["focus_count"] = len(focuses)
    report.metrics["focus_counts_by_period"] = dict(counts)


def main() -> int:
    report = ValidationReport()
    events = load_json("events.sample.json", report)
    endings = load_json("endings.sample.json", report)
    images = load_json("image_ids.sample.json", report)
    characters = load_json("characters.sample.json", report)
    periods = load_json("periods.sample.json", report)
    focuses = load_json("focuses.sample.json", report)

    image_ids = validate_images(images, report)
    period_ids = validate_periods(periods, report)
    validate_focuses(focuses, period_ids, image_ids, report)
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
