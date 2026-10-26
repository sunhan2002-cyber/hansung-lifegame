import copy
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from validate_week10 import ASSETS, ROOT, audit, apply_semantic_review, load_bundle, unique_object


class Week10AuditTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.baseline = load_bundle(ASSETS)
        cls.parser = (ROOT / "app/src/main/java/com/example/lifegame/data/V4ContentParser.kt").read_text(encoding="utf-8")

    def setUp(self):
        self.data = copy.deepcopy(self.baseline)

    def errors(self):
        return str(audit(self.data, self.parser)["errors"])

    def test_current_authored_data_is_structurally_valid(self):
        self.assertEqual([], audit(self.data, self.parser)["errors"])

    def test_duplicate_title_is_rejected(self):
        self.data["titles"].append(self.data["titles"][0])
        self.assertIn("duplicate titleId", self.errors())

    def test_scoped_choice_ids_can_repeat_between_events(self):
        self.assertEqual([], audit(self.data, self.parser)["errors"])
        self.data["events"][0]["choices"].append(self.data["events"][0]["choices"][0])
        self.assertIn("duplicate choiceId", self.errors())

    def test_unknown_stat_and_bool_threshold_are_rejected(self):
        self.data["titles"][0]["conditions"]["minStats"] = {"inteligence": True}
        self.assertIn("unknown stat", self.errors())
        self.assertIn("invalid stat value", self.errors())

    def test_impossible_stat_interval_is_rejected(self):
        c = self.data["titles"][0]["conditions"]
        c["minStats"], c["maxStats"] = {"건강": 80}, {"건강": 60}
        self.assertIn("min > max", self.errors())

    def test_unsourced_and_contradictory_flags_are_rejected(self):
        c = self.data["titles"][0]["conditions"]
        c["requiredFlags"] = c["blockedFlags"] = ["qa_nonexistent_flag"]
        self.assertIn("unsourced", self.errors())
        self.assertIn("required and blocked", self.errors())

    def test_no_open_alternative_is_rejected(self):
        for c in self.data["events"][0]["choices"]:
            c["requiredStats"] = {"건강": 90}
        self.assertIn("no structurally unconditional", self.errors())

    def test_hidden_preview_and_infant_death_are_rejected(self):
        o = self.data["events"][0]["choices"][0]["outcomes"][0]
        o.update(hidden=True, death=True)
        self.assertIn("hidden result leaked", self.errors())
        self.assertIn("forbidden infant/child death", self.errors())

    def test_bad_probability_and_period_are_rejected(self):
        e = self.data["events"][0]
        e["periods"], e["yearInPeriod"] = ["INFANT"], 6
        e["choices"][0]["outcomes"][0]["chance"] = 99
        self.assertIn("year outside period", self.errors())
        self.assertIn("sum 100", self.errors())

    def test_default_title_must_be_unique_and_unconditional(self):
        self.data["titles"][0]["isDefault"] = True
        self.assertIn("exactly one default", self.errors())
        self.assertIn("default must not require", self.errors())

    def test_unsupported_runtime_field_is_not_reported_as_connected(self):
        self.data["titles"][0]["conditions"]["maxStats"] = {"건강": 100}
        report = audit(self.data, "")
        self.assertTrue(any(g["code"] == "MAX_STATS_NOT_PARSED" for g in report["runtimeGaps"]))
        self.assertTrue(any(g["code"] == "TITLE_RUNTIME_NOT_CONNECTED" for g in report["acceptanceFailures"]))

    def test_unknown_tokens_and_json_duplicate_keys_are_rejected(self):
        self.data["titles"][0]["description"] = "{qa_bad_token}"
        self.assertIn("unsupported token", self.errors())
        with self.assertRaises(ValueError):
            unique_object([("chance", 10), ("chance", 100)])

    def test_semantic_review_is_invalidated_after_a_choice_changes(self):
        result = audit(self.data, self.parser)
        row = next(r for r in result["choiceReview"] if r["hardScope"])
        document = {"entries": [{"id": row["eventId"] + ":" + row["choiceId"],
                                  "sourceHash": "outdated", "verdict": "KEEP_OPEN", "reason": "old"}]}
        apply_semantic_review(result, document)
        self.assertEqual("UNREVIEWED_OR_CHANGED", row["semanticReview"])
        self.assertTrue(any(g["code"] == "CHOICE_MEANING_REVIEW_OPEN" for g in result["acceptanceFailures"]))

    def test_unread_fact_type_cannot_silently_pass(self):
        self.data["life_facts"][0]["fact"] = "careerTrak"
        self.assertIn("unread fact type", self.errors())


if __name__ == "__main__":
    unittest.main()
