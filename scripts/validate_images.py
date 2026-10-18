#!/usr/bin/env python3
"""Check packaged image paths and generic mappings without changing story content."""
from __future__ import annotations

import json
import struct
import binascii
import zlib
from pathlib import Path, PurePosixPath

STAT_TAGS = {"health", "fitness", "intelligence", "social", "wealth", "happiness", "luck"}


def valid_dimensions(width, height):
    return (type(width) is int and type(height) is int and width > 0 and height > 0
            and width * 3 == height * 4)


def safe_path(value):
    return (isinstance(value, str) and value.endswith(".png") and "\\" not in value
            and ":" not in value and not value.startswith("/")
            and all(part not in {"", ".", ".."} for part in value.split("/")))


def read_size(path):
    try:
        data = path.read_bytes()
        if len(data) < 33 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
            return None
        position, image_data, finished = 8, [], False
        while position + 12 <= len(data):
            length = struct.unpack(">I", data[position:position + 4])[0]
            chunk_end = position + 12 + length
            if chunk_end > len(data):
                return None
            chunk_type = data[position + 4:position + 8]
            checksum = struct.unpack(">I", data[position + 8 + length:chunk_end])[0]
            if binascii.crc32(data[position + 4:position + 8 + length]) & 0xffffffff != checksum:
                return None
            if position == 8 and (chunk_type != b"IHDR" or length != 13):
                return None
            if chunk_type == b"IDAT":
                image_data.append(data[position + 8:position + 8 + length])
            position = chunk_end
            if chunk_type == b"IEND":
                finished = length == 0
                break
        if not finished or position != len(data) or not image_data:
            return None
        if not zlib.decompress(b"".join(image_data)):
            return None
        return struct.unpack(">II", data[16:24])
    except (OSError, ValueError, struct.error, zlib.error):
        return None


def routed_path(asset_dir, file_name):
    """Resolve the same explicit subdirectory paths as Android, never a random sample."""
    try:
        routing = json.loads((asset_dir / "image_routing.json").read_text(encoding="utf-8"))
        for item in routing["assets"]:
            if item.get("fileName") == file_name and safe_path(item.get("assetPath")):
                candidate = asset_dir / "images" / PurePosixPath(item["assetPath"])
                if candidate.is_file():
                    return candidate
    except (OSError, ValueError, KeyError, TypeError):
        pass
    return None


def audit_routing(root):
    asset_dir = root / "app/src/main/assets"
    errors = []
    try:
        routing = json.loads((asset_dir / "image_routing.json").read_text(encoding="utf-8"))
        catalog = json.loads((asset_dir / "image_ids.sample.json").read_text(encoding="utf-8"))
        periods = json.loads((asset_dir / "periods.sample.json").read_text(encoding="utf-8"))
        focuses = json.loads((asset_dir / "focuses.sample.json").read_text(encoding="utf-8"))
        if routing["schemaVersion"] != 1:
            errors.append("unsupported image routing version")
        items = routing["assets"]
        ids = [item["imageId"] for item in items]
        paths = [item["assetPath"] for item in items]
        if len(ids) != len(set(ids)):
            errors.append("duplicate routed imageId")
        if len(paths) != len(set(paths)):
            errors.append("duplicate routed image path")
        registered = {item["imageId"]: item for item in catalog}
        by_id = {item["imageId"]: item for item in items}
        required = routing["requiredImageIds"]
        if len(required) != 13 or len(set(required)) != 13:
            errors.append("week8 requires exactly 13 distinct new image IDs")
        for item in items:
            image_id = item["imageId"]
            image_path = item["assetPath"]
            if not safe_path(image_path):
                errors.append(f"unsafe image path: {image_path}")
                continue
            if image_id not in registered:
                errors.append(f"unregistered routed image: {image_id}")
                continue
            if PurePosixPath(image_path).name != registered[image_id]["fileName"]:
                errors.append(f"fileName mismatch: {image_id}")
            if item["usage"] != registered[image_id]["usage"]:
                errors.append(f"usage mismatch: {image_id}")
            actual = read_size(asset_dir / "images" / PurePosixPath(image_path))
            if actual is None:
                errors.append(f"missing or invalid PNG: {image_id} ({image_path})")
            elif not valid_dimensions(*actual):
                errors.append(f"image must have positive 4:3 dimensions: {image_id}: {actual}")
            elif any(key in registered[image_id] for key in ("width", "height")):
                if actual != (registered[image_id].get("width"), registered[image_id].get("height")):
                    errors.append(f"metadata dimensions mismatch: {image_id}")
        portraits = routing["periodPortraits"]
        scenes = routing["statScenes"]
        focus_stats = routing["focusStats"]
        if set(portraits) != {item["periodId"] for item in periods}:
            errors.append("period portrait mapping does not match current periods")
        if set(scenes) != STAT_TAGS:
            errors.append("stat scene mapping must cover all seven stats")
        if set(focus_stats) != {item["focusId"] for item in focuses}:
            errors.append("focus image mapping does not match current focus IDs")
        if set(focus_stats.values()) - STAT_TAGS:
            errors.append("unknown stat tag in focus image mapping")
        if set(required) != set(portraits.values()) | set(scenes.values()):
            errors.append("new image set must equal six portraits and seven stat scenes")
        for period, image_id in portraits.items():
            item = by_id.get(image_id, {})
            if item.get("usage") != "character" or item.get("period") != period:
                errors.append(f"portrait mapping mismatch: {period}")
        for tag, image_id in scenes.items():
            item = by_id.get(image_id, {})
            if item.get("usage") != "scene" or item.get("statTag") != tag:
                errors.append(f"stat mapping mismatch: {tag}")
        event_tags = routing.get("eventStatTags", {})
        events = json.loads((asset_dir / "events.sample.json").read_text(encoding="utf-8"))
        event_ids = {event["eventId"] for event in events}
        if not isinstance(event_tags, dict):
            errors.append("eventStatTags must be an object")
            event_tags = {}
        for event_id, tags in event_tags.items():
            if event_id not in event_ids:
                errors.append(f"unknown event in image mapping: {event_id}")
            if (not isinstance(tags, list) or not tags or
                    any(not isinstance(tag, str) or tag not in STAT_TAGS for tag in tags) or
                    len(tags) != len(set(tags))):
                errors.append(f"invalid event image stat tags: {event_id}")
        metrics = {"routed_images": len(items), "required_new_images": len(required),
                   "event_image_mappings": len(event_tags),
                   "portrait_periods": len(portraits), "stat_scenes": len(scenes),
                   "focus_image_mappings": len(focus_stats)}
        return errors, metrics
    except (OSError, ValueError, KeyError, TypeError) as exc:
        return [f"invalid image routing data: {exc}"], {}


if __name__ == "__main__":
    errors, metrics = audit_routing(Path(__file__).resolve().parents[1])
    print(json.dumps({"errors": errors, "metrics": metrics}, ensure_ascii=False, indent=2))
    raise SystemExit(bool(errors))
