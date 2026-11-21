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
        self.assertEqual("SOURCE_CHANGED", row["semanticPendingReason"])
        self.assertEqual("KEEP_OPEN", row["previousSemanticVerdict"])
        self.assertTrue(any(g["code"] == "CHOICE_MEANING_REVIEW_OPEN" for g in result["acceptanceFailures"]))

    def test_missing_review_is_distinct_from_changed_review_but_remains_blocking(self):
        result = audit(self.data, self.parser)
        apply_semantic_review(result, {"entries": []})
        hard_rows = [r for r in result["choiceReview"] if r["hardScope"]]
        self.assertTrue(hard_rows)
        self.assertTrue(all(r["semanticPendingReason"] == "MISSING_REVIEW" for r in hard_rows))
        self.assertEqual(len(hard_rows), result["semanticPendingReasonCounts"]["MISSING_REVIEW"])
        self.assertTrue(all(r["semanticReview"] == "UNREVIEWED_OR_CHANGED" for r in hard_rows))

    def test_current_review_keeps_its_verdict_without_new_certification(self):
        result = audit(self.data, self.parser)
        row = next(r for r in result["choiceReview"] if r["hardScope"])
        key = row["eventId"] + ":" + row["choiceId"]
        document = {"entries": [{"id": key, "sourceHash": row["sourceHash"],
                                  "verdict": "KEEP_CONDITION", "reason": "recorded review"}]}
        apply_semantic_review(result, document)
        self.assertEqual("KEEP_CONDITION", row["semanticReview"])
        self.assertNotIn("semanticPendingReason", row)
        self.assertFalse(any(g.get("location") == key for g in result["acceptanceFailures"]))

    def test_a_matching_review_that_requested_changes_still_blocks_acceptance(self):
        result = audit(self.data, self.parser)
        row = next(r for r in result["choiceReview"] if r["hardScope"])
        document = {"entries": [{"id": row["eventId"] + ":" + row["choiceId"],
                                  "sourceHash": row["sourceHash"], "verdict": "CHANGE", "reason": "needs correction"}]}
        apply_semantic_review(result, document)
        self.assertEqual("CHANGE", row["semanticReview"])
        self.assertEqual("REVIEW_ACTION_REQUIRED", row["semanticPendingReason"])

    def test_unread_fact_type_cannot_silently_pass(self):
        self.data["life_facts"][0]["fact"] = "careerTrak"
        self.assertIn("unread fact type", self.errors())

    def v2_review(self, event_id="love_child_plan_01", choice_id="C"):
        result = audit(self.data, self.parser)
        row = next(r for r in result["choiceReview"] if r["eventId"] == event_id and r["choiceId"] == choice_id)
        entry = {"id": event_id + ":" + choice_id, "sourceHash": row["sourceHash"],
                 "fullSourceHash": row["fullSourceHash"], "reviewVersion": 2,
                 "verdict": "KEEP_OPEN", "reason": "reviewed conditions and all outcomes"}
        return row, {"entries": [entry]}

    def test_v2_review_is_invalidated_when_only_an_outcome_changes(self):
        for field, value in [("text", "다른 결과"), ("statDelta", {"행복": -5}),
                             ("addFlags", ["no_child_plan"])]:
            with self.subTest(field=field):
                self.data = copy.deepcopy(self.baseline)
                original, document = self.v2_review()
                event = next(e for e in self.data["events"] if e["eventId"] == "love_child_plan_01")
                next(c for c in event["choices"] if c["choiceId"] == "C")["outcomes"][0][field] = value
                result = audit(self.data, self.parser)
                apply_semantic_review(result, document)
                row = next(r for r in result["choiceReview"] if r["eventId"] == "love_child_plan_01" and r["choiceId"] == "C")
                self.assertEqual(original["sourceHash"], row["sourceHash"], "V1 did not cover outcomes")
                self.assertEqual("UNREVIEWED_OR_CHANGED", row["semanticReview"])
                self.assertEqual("EVENT_CONDITIONS_OR_CHOICE_OUTCOMES", row["semanticChangedScope"])

    def test_v2_review_is_invalidated_when_event_eligibility_changes(self):
        original, document = self.v2_review()
        event = next(e for e in self.data["events"] if e["eventId"] == "love_child_plan_01")
        event["conditions"]["requiredFlags"].append("job_employed")
        result = audit(self.data, self.parser)
        apply_semantic_review(result, document)
        row = next(r for r in result["choiceReview"] if r["eventId"] == "love_child_plan_01" and r["choiceId"] == "C")
        self.assertEqual(original["sourceHash"], row["sourceHash"])
        self.assertEqual("SOURCE_CHANGED", row["semanticPendingReason"])

    def test_v2_requires_a_matching_full_hash_and_supported_version(self):
        for change in ("missing_hash", "future_version"):
            with self.subTest(change=change):
                _, document = self.v2_review()
                entry = document["entries"][0]
                if change == "missing_hash":
                    entry.pop("fullSourceHash")
                else:
                    entry["reviewVersion"] = 3
                result = audit(self.data, self.parser)
                apply_semantic_review(result, document)
                row = next(r for r in result["choiceReview"] if r["eventId"] == "love_child_plan_01" and r["choiceId"] == "C")
                self.assertEqual("UNREVIEWED_OR_CHANGED", row["semanticReview"])

    def test_art_changes_do_not_claim_or_invalidate_a_textual_v2_review(self):
        original, document = self.v2_review()
        event = next(e for e in self.data["events"] if e["eventId"] == "love_child_plan_01")
        event["imageId"] = self.data["events"][0]["imageId"]
        result = audit(self.data, self.parser)
        apply_semantic_review(result, document)
        row = next(r for r in result["choiceReview"] if r["eventId"] == "love_child_plan_01" and r["choiceId"] == "C")
        self.assertEqual(original["fullSourceHash"], row["fullSourceHash"])
        self.assertEqual("KEEP_OPEN", row["semanticReview"])
        self.assertEqual(1, result["acceptedSemanticReviewScopeCounts"]["CHOICE_AVAILABILITY_AND_OUTCOME_CAUSALITY_V2"])

    def test_legacy_review_remains_explicitly_limited_to_choice_availability(self):
        _, document = self.v2_review()
        document["entries"][0].pop("fullSourceHash")
        document["entries"][0].pop("reviewVersion")
        result = audit(self.data, self.parser)
        apply_semantic_review(result, document)
        self.assertEqual(1, result["acceptedSemanticReviewScopeCounts"]["CHOICE_AVAILABILITY_V1"])
        self.assertNotIn("CHOICE_AVAILABILITY_AND_OUTCOME_CAUSALITY_V2", result["acceptedSemanticReviewScopeCounts"])


if __name__ == "__main__":
    unittest.main()
