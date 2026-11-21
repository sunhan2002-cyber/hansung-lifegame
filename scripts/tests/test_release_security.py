import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile


SCRIPT = Path(__file__).resolve().parents[1] / "verify_release_security.py"
SPEC = importlib.util.spec_from_file_location("release_security", SCRIPT)
security = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(security)


class ReleaseSecurityTests(unittest.TestCase):
    def apk(self, directory, dex, manifest=b"offline-manifest"):
        path = Path(directory) / "synthetic.apk"
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("classes.dex", dex)
            archive.writestr("AndroidManifest.xml", manifest)
        return path

    def test_current_source_uses_offline_release_and_settings_only_backup(self):
        self.assertEqual([], security.audit_source(security.ROOT))

    def test_clean_synthetic_package_passes(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertEqual([], security.audit_apk(self.apk(directory, b"ordinary app code")))

    def test_provider_client_or_key_marker_fails_without_echoing_value(self):
        markers = [b"OPENAI_API_KEY", b"api.openai.com", b"OpenAiFaceSheetClient", b"sk-proj-" + b"z" * 40]
        with tempfile.TemporaryDirectory() as directory:
            for marker in markers:
                with self.subTest(marker_type=markers.index(marker)):
                    errors = security.audit_apk(self.apk(directory, b"prefix" + marker + b"suffix"))
                    self.assertTrue(errors)
                    self.assertNotIn(marker.decode(), " ".join(errors))

    def test_synthetic_sentinel_is_detected(self):
        sentinel = "LIFEGAME_SYNTHETIC_SECRET_FOR_TEST_ONLY"
        with tempfile.TemporaryDirectory() as directory:
            errors = security.audit_apk(self.apk(directory, sentinel.encode()), sentinel)
            self.assertTrue(errors)
            self.assertNotIn(sentinel, " ".join(errors))

    def test_network_permission_is_detected_in_both_manifest_encodings(self):
        with tempfile.TemporaryDirectory() as directory:
            for encoding in ("utf-8", "utf-16-le"):
                manifest = "android.permission.INTERNET".encode(encoding)
                self.assertTrue(security.audit_apk(self.apk(directory, b"code", manifest)))


if __name__ == "__main__":
    unittest.main()
