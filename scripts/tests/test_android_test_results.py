from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_android_test_results import verify


class AndroidTestResultVerificationTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)

    def write(self, xml):
        (self.root / 'TEST-device.xml').write_text(xml, encoding='utf-8')

    def test_missing_or_zero_test_reports_cannot_certify_device_execution(self):
        self.assertEqual('FAIL', verify(self.root)['status'])
        self.write('<testsuite tests="0" failures="0" errors="0"/>')
        self.assertEqual('FAIL', verify(self.root)['status'])

    def test_required_class_must_run_instead_of_only_being_skipped(self):
        self.write('<testsuite><testcase classname="Core" name="one"><skipped/></testcase>'
                   '<testcase classname="Other" name="two"/></testsuite>')
        result = verify(self.root, ['Core'])
        self.assertEqual('FAIL', result['status'])
        self.assertEqual(1, result['skipped'])

    def test_failures_are_detected_even_when_suite_counters_are_wrong(self):
        self.write('<testsuite failures="0"><testcase classname="Core" name="one"><failure/></testcase>'
                   '<testcase classname="Core" name="two"/></testsuite>')
        self.assertEqual('FAIL', verify(self.root, ['Core'])['status'])

    def test_suite_error_without_testcase_failure_is_not_ignored(self):
        self.write('<testsuite errors="1"><testcase classname="Core" name="one"/></testsuite>')
        self.assertEqual('FAIL', verify(self.root, ['Core'])['status'])

    def test_corrupt_xml_is_a_failure(self):
        self.write('<testsuite>')
        self.assertEqual('FAIL', verify(self.root)['status'])

    def test_successful_required_classes_are_counted(self):
        self.write('<testsuites><testsuite failures="0" errors="0">'
                   '<testcase classname="Core" name="one"/>'
                   '<testcase classname="Save" name="two"/></testsuite></testsuites>')
        result = verify(self.root, ['Core', 'Save'])
        self.assertEqual('PASS', result['status'])
        self.assertEqual({'Core': 1, 'Save': 1}, result['successfulByClass'])


if __name__ == '__main__':
    unittest.main()
