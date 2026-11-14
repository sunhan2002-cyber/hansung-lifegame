import copy
import sys
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
import audit_week13_plausibility as audit  # noqa: E402


class Week13PlausibilityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.bundle = audit.load_bundle()

    def fresh(self):
        return copy.deepcopy(self.bundle)

    def messages(self, bundle):
        return [e["location"] + " " + e["message"] for e in audit.audit(bundle)["errors"]]

    def event(self, bundle, event_id):
        return next(e for e in bundle["events"] if e["eventId"] == event_id)

    def test_current_assets_pass(self):
        self.assertEqual([], self.messages(self.fresh()))

    def test_label_period_is_reported(self):
        bundle = self.fresh()
        self.event(bundle, "infant_007")["choices"][0]["label"] += "."
        self.assertTrue(any("label ends with a period" in m for m in self.messages(bundle)))

    def test_marriage_without_wedding_steps_is_reported(self):
        bundle = self.fresh()
        outcome = self.event(bundle, "adult_073")["choices"][0]["outcomes"][0]
        outcome["addFlags"] = outcome["addFlags"] + ["married"]
        self.assertTrue(any("adds married without engaged" in m for m in self.messages(bundle)))

    def test_meeting_event_must_block_existing_partner(self):
        bundle = self.fresh()
        self.event(bundle, "adult_074")["conditions"]["blockedFlags"] = []
        self.assertTrue(any("starts a relationship" in m for m in self.messages(bundle)))

    def test_family_meeting_after_marriage_is_reported(self):
        bundle = self.fresh()
        conditions = self.event(bundle, "path_soc_05")["conditions"]
        conditions["requiredFlags"] = ["soc_path", "married"]
        self.assertTrue(any("must require engaged" in m for m in self.messages(bundle)))

    def test_work_text_without_job_gate_is_reported(self):
        bundle = self.fresh()
        self.event(bundle, "adult_039")["conditions"]["requiredFlags"].remove("job_employed")
        self.assertTrue(any(m.startswith("adult_039 workplace words") for m in self.messages(bundle)))

    def test_layoff_must_clear_employment(self):
        bundle = self.fresh()
        for choice in self.event(bundle, "adult_070")["choices"]:
            for outcome in choice["outcomes"]:
                outcome.pop("removeFlags", None)
        self.assertTrue(any("keeps job_employed" in m for m in self.messages(bundle)))

    def test_each_period_needs_five_auto_events(self):
        bundle = self.fresh()
        bundle["auto"]["events"] = [a for a in bundle["auto"]["events"] if a["periods"] != ["OLD_AGE"]]
        self.assertTrue(any("auto:OLD_AGE" in m for m in self.messages(bundle)))

    def test_auto_events_cannot_use_yearly_counter(self):
        bundle = self.fresh()
        bundle["auto"]["events"][0]["triggerRule"] = {"stage": "PERIOD_START", "note": "매년"}
        self.assertTrue(any("yearly counter" in m for m in self.messages(bundle)))

    def test_child_text_needs_child_fact(self):
        bundle = self.fresh()
        grandchild = next(a for a in bundle["auto"]["events"] if a["eventId"] == "auto_old_grandchild")
        grandchild["conditions"].pop("lifeFacts")
        self.assertTrue(any("child text without" in m for m in self.messages(bundle)))

    def test_recall_must_point_to_real_choice(self):
        bundle = self.fresh()
        bundle["ext"]["recallSlots"][0]["sourceChoiceIds"] = ["Z"]
        self.assertTrue(any("unknown sourceChoiceIds" in m for m in self.messages(bundle)))


if __name__ == "__main__":
    unittest.main()
