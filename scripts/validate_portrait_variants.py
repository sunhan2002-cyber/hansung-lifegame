"""Validate optional portrait presentation assets without weakening legacy 4:3 acceptance."""
import argparse
import hashlib
import json
import math
from pathlib import Path

from validate_images import read_size, safe_path, valid_dimensions, image_dimension_errors, portrait_registration_errors, PORTRAIT_SOURCE_FORMAT
from validate_followup_image_coverage import event_hash

ROOT = Path(__file__).resolve().parents[1]


def audit(root):
    errors, checked = [], 0
    assets = root / 'app/src/main/assets'
    load = lambda p: json.loads(p.read_text(encoding='utf-8-sig'))
    try:
        data = load(assets / 'image_presentation.json')
        assert data['schemaVersion'] == 1 and isinstance(data['images'], list), 'unsupported presentation schema'
        routing = load(assets / 'image_routing.json')
        catalog = load(assets / 'image_ids.sample.json')
        catalog_by_id = {x['imageId']: x for x in catalog}
        routed = {x['assetPath'] for x in routing['assets']}
        events = {x['eventId']: x for x in load(assets / 'events.sample.json')}
        assert not portrait_registration_errors(catalog, routing, list(events.values())), 'invalid portrait source registration'
        seen, variants = set(), set()
        for item in data['images']:
            source = item['assetPath']
            assert safe_path(source) and source in routed and source not in seen, 'invalid/duplicate/unrouted source'
            seen.add(source)
            original = assets / 'images' / source
            size = read_size(original)
            source_id = next(x['imageId'] for x in routing['assets'] if x['assetPath'] == source)
            source_catalog = catalog_by_id.get(source_id, {})
            if source_catalog.get('sourceFormat') == PORTRAIT_SOURCE_FORMAT:
                assert size and not image_dimension_errors(source_catalog, size), 'invalid explicitly registered portrait source'
            else:
                assert size and valid_dimensions(*size), 'original must remain valid 4:3 PNG'
            assert item['sourceSha256'] == hashlib.sha256(original.read_bytes()).hexdigest(), 'stale source hash'
            for axis in ('focalX', 'focalY'):
                value = item[axis]
                assert type(value) in (int, float) and math.isfinite(value) and 0 <= value <= 1, 'invalid focal point'
            variant = item.get('portraitVariant')
            if variant is None:
                continue
            path = variant['assetPath']
            assert safe_path(path) and path.startswith('presentation/portraits/'), 'variant must use dedicated portrait directory'
            assert path not in routed and path not in variants, 'variant must not replace legacy catalogue or another variant'
            variants.add(path)
            image = assets / 'images' / path
            size = read_size(image)
            assert size is not None, 'missing/corrupt portrait PNG'
            width, height = size
            for axis in ('focalX', 'focalY'):
                value = variant.get(axis, .5)
                assert type(value) in (int, float) and math.isfinite(value) and 0 <= value <= 1, 'invalid portrait focal point'
            assert 0 < width <= 4096 and 0 < height <= 4096 and height > width * 1.35, 'invalid portrait dimensions'
            assert type(variant['width']) is int and type(variant['height']) is int, 'dimensions must be integers'
            assert (variant['width'], variant['height']) == size, 'portrait dimensions differ from metadata'
            assert variant['sha256'] == hashlib.sha256(image.read_bytes()).hexdigest(), 'stale portrait hash'
            assert variant['reviewStatus'] == 'AI_REVIEWED_PRESENTATION', 'portrait review pending'
            assert variant['reviewerType'] == 'AI' and variant['reviewer'].strip(), 'explicit AI reviewer required'
            evidence_path = variant['reviewEvidence']
            assert evidence_path.startswith('docs/refactor/art-candidates/') and '\\' not in evidence_path, 'invalid review evidence path'
            assert all(part not in ('', '.', '..') for part in evidence_path.split('/')), 'unsafe review evidence path'
            evidence = load(root / evidence_path)
            assert evidence['sourceAssetPath'] == source and evidence['sourceSha256'] == item['sourceSha256'], 'review source mismatch'
            assert evidence['variant'] == variant, 'review variant mismatch'
            assert evidence['reviewerType'] == 'AI' and evidence['reviewer'] == variant['reviewer'], 'review identity mismatch'
            assert evidence['reviewStatus'] == variant['reviewStatus'], 'review status mismatch'
            assert evidence['eventSourceHash'] == event_hash(events[evidence['eventId']]), 'event changed after portrait review'
            event = events[evidence['eventId']]
            image_id = event.get('povImageId') or event.get('imageId')
            assert any(x['imageId'] == image_id and x['assetPath'] == source for x in routing['assets']), 'portrait event/source mismatch'
            assert all(str(evidence.get(key, '')).strip() for key in ('semanticReview', 'visualReview', 'cropReview', 'actualProducer', 'generationMethod')), 'review evidence incomplete'
            checked += 1
    except (OSError, ValueError, KeyError, TypeError, AssertionError) as exc:
        errors.append(str(exc))
    return {'errors': errors, 'reviewedPortraitVariants': checked,
            'scope': 'Optional AI-reviewed presentation variants; no legacy image acceptance or human approval implied.'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT)
    args = parser.parse_args()
    report = audit(args.root)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    raise SystemExit(bool(report['errors']))
