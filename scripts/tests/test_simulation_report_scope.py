from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from summarize_week10 import initialization_description


class SimulationReportScopeTest(unittest.TestCase):
    def test_legacy_results_do_not_claim_current_app_parity(self):
        text = initialization_description({'schemaVersion': 1})
        self.assertIn('구 schemaVersion 1', text)
        self.assertIn('자동 사건 경로를 포함하지 않는다', text)
        self.assertNotIn('같은 세션 팩토리', text)

    def test_profile_results_explain_actual_and_synthetic_start_conditions(self):
        text = initialization_description({'schemaVersion': 2})
        self.assertIn('같은 세션 팩토리·자동 사건', text)
        self.assertIn('재굴림 없음', text)
        self.assertIn('합성 경계 정책', text)
        self.assertIn('startProfile', text)
        self.assertNotIn('성별/시작 캐릭터 보정 플래그 없음', text)


if __name__ == '__main__':
    unittest.main()
