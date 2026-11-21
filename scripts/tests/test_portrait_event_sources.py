import copy
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import validate_content as content
import validate_images as gate


class PortraitEventSourceTest(unittest.TestCase):
    def setUp(self):
        self.image = dict(imageId='img_event', fileName='img_event.png', usage='pov', stage='YOUNG_ADULT',
                          sourceFormat='portrait_9_16', width=941, height=1672,
                          description='A first-person scene', fallbackText='Loading')
        self.routing = dict(assets=[dict(imageId='img_event', assetPath='img_event.png')],
                            requiredImageIds=[], periodPortraits={}, statScenes={})
        self.events = [dict(eventId='event', imageId='img_event')]

    def test_explicit_measured_portrait_accepts_integer_rounding(self):
        self.assertEqual([], gate.image_dimension_errors(self.image, (941, 1672)))
        self.assertEqual([], gate.portrait_registration_errors([self.image], self.routing, self.events))
        self.assertTrue(gate.valid_portrait_dimensions(900, 1600))
        self.assertTrue(gate.valid_portrait_dimensions(908, 1600))
        self.assertFalse(gate.valid_portrait_dimensions(909, 1600))  # Exactly 1% is not <1%.

    def test_unmarked_original_stays_strictly_four_by_three(self):
        legacy = dict(self.image)
        legacy.pop('sourceFormat')
        self.assertTrue(gate.image_dimension_errors(legacy, (941, 1672)))
        legacy.update(width=1448, height=1086)
        self.assertEqual([], gate.image_dimension_errors(legacy, (1448, 1086)))
        self.assertFalse(gate.valid_dimensions(941, 1672))

    def test_missing_mistyped_and_oversized_metadata_cannot_opt_in(self):
        for updates in ({'width': None}, {'height': None}, {'width': True}, {'height': '1672'},
                        {'width': 2304, 'height': 4097}, {'width': 0}, {'width': 1672, 'height': 941}):
            with self.subTest(updates=updates):
                self.assertTrue(gate.image_dimension_errors(dict(self.image, **updates), (941, 1672)))
        self.assertTrue(gate.valid_portrait_dimensions(2304, 4096))

    def test_actual_dimensions_must_equal_declaration_even_at_valid_ratio(self):
        errors = gate.image_dimension_errors(self.image, (900, 1600))
        self.assertTrue(any('differ from actual PNG' in error for error in errors))
        self.assertTrue(gate.image_dimension_errors(self.image, (1448, 1086)))

    def test_unknown_format_and_non_pov_usage_are_rejected(self):
        self.assertTrue(gate.image_dimension_errors(dict(self.image, sourceFormat='any'), (941, 1672)))
        for usage in ('character', 'scene', 'ending', 'face_sheet'):
            with self.subTest(usage=usage):
                self.assertTrue(gate.image_dimension_errors(dict(self.image, usage=usage), (941, 1672)))

    def test_generic_images_cannot_be_relabelled_as_portrait_events(self):
        for key in ('requiredImageIds', 'periodPortraits', 'statScenes', 'periodScenes', 'endingScenes'):
            routing = copy.deepcopy(self.routing)
            routing[key] = ['img_event'] if key == 'requiredImageIds' else {'generic': 'img_event'}
            with self.subTest(mapping=key):
                self.assertTrue(gate.portrait_registration_errors([self.image], routing, self.events))

    def test_portrait_requires_preferred_event_membership_and_explicit_route(self):
        self.assertTrue(gate.portrait_registration_errors([self.image], self.routing, []))
        self.assertTrue(gate.portrait_registration_errors([self.image], dict(self.routing, assets=[]), self.events))
        overridden = [dict(eventId='event', imageId='img_event', povImageId='different_preferred')]
        self.assertTrue(gate.portrait_registration_errors([self.image], self.routing, overridden))

    def test_content_gate_shares_dimension_rules_and_requires_portrait_file(self):
        report = content.ValidationReport()
        with patch.object(content, 'find_image_file', return_value=Path('scene.png')), \
                patch.object(content, 'read_png_size', return_value=(941, 1672)):
            content.validate_images([self.image], report)
        self.assertEqual([], report.errors)
        report = content.ValidationReport()
        with patch.object(content, 'find_image_file', return_value=None):
            content.validate_images([self.image], report)
        self.assertTrue(any('must exist' in error for error in report.errors))
        report = content.ValidationReport()
        legacy = {k: v for k, v in self.image.items() if k not in {'sourceFormat', 'width', 'height'}}
        with patch.object(content, 'find_image_file', return_value=None):
            content.validate_images([legacy], report)
        self.assertEqual([], report.errors)


if __name__ == '__main__':
    unittest.main()
