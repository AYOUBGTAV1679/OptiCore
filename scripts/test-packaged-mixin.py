#!/usr/bin/env python3
"""Regression tests for check-packaged-mixin.py using synthetic JAR fixtures."""
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

CHECKER = Path(__file__).with_name("check-packaged-mixin.py")


def make_jar(path: Path, manifest: str, *, config=True, refmap=True):
    with ZipFile(path, "w") as jar:
        jar.writestr("META-INF/MANIFEST.MF", manifest)
        if config:
            jar.writestr("opticore.mixins.json", "{}")
        if refmap:
            jar.writestr("opticore.refmap.json", "{}")


class PackagedMixinCheckerTests(unittest.TestCase):
    def run_checker(self, directory):
        return subprocess.run(
            [sys.executable, str(CHECKER), str(directory)],
            text=True, capture_output=True, check=False,
        )

    def test_accepts_complete_jar(self):
        with tempfile.TemporaryDirectory() as tmp:
            make_jar(Path(tmp) / "opticore.jar",
                     "Manifest-Version: 1.0\r\nMixinConfigs: opticore.mixins.json\r\n\r\n")
            result = self.run_checker(tmp)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("Packaged Mixin check: OK", result.stdout)

    def test_accepts_folded_manifest_attribute(self):
        with tempfile.TemporaryDirectory() as tmp:
            make_jar(Path(tmp) / "opticore.jar",
                     "Manifest-Version: 1.0\r\nMixinConfigs: opticore.mixins.\r\n json\r\n\r\n")
            result = self.run_checker(tmp)
            self.assertEqual(result.returncode, 0, result.stderr)

    def test_rejects_missing_refmap(self):
        with tempfile.TemporaryDirectory() as tmp:
            make_jar(Path(tmp) / "opticore.jar",
                     "MixinConfigs: opticore.mixins.json\r\n\r\n", refmap=False)
            result = self.run_checker(tmp)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("missing opticore.refmap.json", result.stderr)

    def test_rejects_missing_manifest_registration(self):
        with tempfile.TemporaryDirectory() as tmp:
            make_jar(Path(tmp) / "opticore.jar",
                     "Manifest-Version: 1.0\r\n\r\n")
            result = self.run_checker(tmp)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("manifest lacks MixinConfigs", result.stderr)

    def test_rejects_empty_directory(self):
        with tempfile.TemporaryDirectory() as tmp:
            result = self.run_checker(tmp)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("No mod JAR found", result.stderr)


if __name__ == "__main__":
    unittest.main(verbosity=2)
