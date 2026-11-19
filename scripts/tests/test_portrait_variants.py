import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import validate_portrait_variants as gate


class PortraitVariantGateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        assets = Path('app/src/main/assets')
        self.metadata_path = self.root / assets / 'image_presentation.json'
        self.metadata_path.parent.mkdir(parents=True)
        for name in ('image_presentation.json', 'image_routing.json', 'events.sample.json', 'image_ids.sample.json'):
            shutil.copyfile(gate.ROOT / assets / name, self.root / assets / name)
        self.data = json.loads(self.metadata_path.read_text(encoding='utf8'))
        for item in self.data['images']:
            paths = [assets / 'images' / item['assetPath']]
            if 'portraitVariant' in item:
                self.variant = item['portraitVariant']
                paths += [assets / 'images' / self.variant['assetPath'], Path(self.variant['reviewEvidence'])]
            for path in paths:
                (self.root / path).parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(gate.ROOT / path, self.root / path)

    def check(self):
        self.metadata_path.write_text(json.dumps(self.data), encoding='utf8')
        return gate.audit(self.root)

    def test_actual_variant_preserves_original_contract(self):
        self.assertEqual([], self.check()['errors'])
        self.assertEqual(sum('portraitVariant' in item for item in self.data['images']), self.check()['reviewedPortraitVariants'])

    def test_portrait_focal_source_requires_explicit_registration(self):
        path = self.root / 'app/src/main/assets/image_ids.sample.json'
        data = json.loads(path.read_text(encoding='utf8'))
        child = next(item for item in data if item['imageId'] == 'img_love_first_child_01_001')
        child.pop('sourceFormat')
        path.write_text(json.dumps(data), encoding='utf8')
        self.assertTrue(self.check()['errors'])

    def test_missing_and_corrupt_variant_rejected(self):
        path = self.root / 'app/src/main/assets/images' / self.variant['assetPath']
        path.unlink()
        self.assertTrue(self.check()['errors'])
        path.write_bytes(b'not a PNG')
        self.assertTrue(self.check()['errors'])

    def test_source_and_variant_hash_changes_rejected(self):
        for target, field in ((self.data['images'][-1], 'sourceSha256'), (self.variant, 'sha256')):
            old = target[field]
            target[field] = '0' * 64
            self.assertTrue(self.check()['errors'])
            target[field] = old

    def test_unreviewed_unsafe_and_incompatible_variants_rejected(self):
        for field, value in (('reviewStatus', 'PENDING'), ('reviewerType', 'HUMAN'), ('reviewer', ''),
                             ('assetPath', '../outside.png'), ('width', 1672), ('reviewEvidence', '../../outside.json')):
            old = self.variant[field]
            self.variant[field] = value
            self.assertTrue(self.check()['errors'], field)
            self.variant[field] = old

    def test_event_changed_after_review_rejected(self):
        path = self.root / 'app/src/main/assets/events.sample.json'
        data = json.loads(path.read_text(encoding='utf8'))
        next(x for x in data if x['eventId'] == 'adult_072')['choices'][0]['label'] += '!'
        path.write_text(json.dumps(data), encoding='utf8')
        self.assertTrue(self.check()['errors'])


if __name__ == '__main__':
    unittest.main()
