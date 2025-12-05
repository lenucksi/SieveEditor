#!/usr/bin/env python3
"""
Unit tests for generate_flatpak_maven_sources.py

Run with:
    python3 -m unittest test_generate_flatpak_maven_sources.py
"""

import unittest
import tempfile
import shutil
from pathlib import Path
from generate_flatpak_maven_sources import (
    extract_relative_path,
    parse_download_log,
    scan_maven_repo,
    calculate_sha256,
    generate_yaml_entry,
)


class TestExtractRelativePath(unittest.TestCase):
    """Test URL parsing to extract Maven repository paths."""

    def test_maven_central_jar(self):
        """Test Maven Central .jar URL"""
        url = "https://repo.maven.apache.org/maven2/com/example/artifact/1.0/artifact-1.0.jar"
        expected = "com/example/artifact/1.0/artifact-1.0.jar"
        self.assertEqual(extract_relative_path(url), expected)

    def test_maven_central_pom(self):
        """Test Maven Central .pom URL"""
        url = "https://repo.maven.apache.org/maven2/org/junit/junit-bom/5.13.1/junit-bom-5.13.1.pom"
        expected = "org/junit/junit-bom/5.13.1/junit-bom-5.13.1.pom"
        self.assertEqual(extract_relative_path(url), expected)

    def test_jitpack_jar(self):
        """Test JitPack .jar URL"""
        url = "https://jitpack.io/com/github/user/repo/v1.0/repo-v1.0.jar"
        expected = "com/github/user/repo/v1.0/repo-v1.0.jar"
        self.assertEqual(extract_relative_path(url), expected)

    def test_jitpack_pom(self):
        """Test JitPack .pom URL"""
        url = "https://jitpack.io/com/github/lenucksi/ManageSieveJ/managesievej-v0.3.9/ManageSieveJ-managesievej-v0.3.9.pom"
        expected = "com/github/lenucksi/ManageSieveJ/managesievej-v0.3.9/ManageSieveJ-managesievej-v0.3.9.pom"
        self.assertEqual(extract_relative_path(url), expected)

    def test_deep_path(self):
        """Test URL with deep directory structure"""
        url = "https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-plugins/45/maven-plugins-45.pom"
        expected = "org/apache/maven/plugins/maven-plugins/45/maven-plugins-45.pom"
        self.assertEqual(extract_relative_path(url), expected)

    def test_invalid_url(self):
        """Test invalid URL returns None"""
        url = "https://example.com/not/a/maven/url.jar"
        # Should return None or extract using generic pattern
        result = extract_relative_path(url)
        # Allow either None or a path if generic pattern matches
        if result is not None:
            self.assertIn("url.jar", result)

    def test_url_without_maven2(self):
        """Test URL without /maven2/ prefix"""
        url = "https://repository.example.com/repository/com/example/lib/1.0/lib-1.0.jar"
        result = extract_relative_path(url)
        self.assertIsNotNone(result)
        self.assertIn("com/example/lib/1.0/lib-1.0.jar", result)


class TestParseDownloadLog(unittest.TestCase):
    """Test parsing of Maven download logs."""

    def setUp(self):
        """Create temporary directory for test files."""
        self.temp_dir = tempfile.mkdtemp()
        self.temp_path = Path(self.temp_dir)

    def tearDown(self):
        """Clean up temporary directory."""
        shutil.rmtree(self.temp_dir)

    def test_parse_simple_log(self):
        """Test parsing a simple download log"""
        log_content = """[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/com/example/artifact/1.0/artifact-1.0.jar (100 kB at 500 kB/s)
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/com/example/artifact/1.0/artifact-1.0.pom (5 kB at 50 kB/s)
"""
        log_file = self.temp_path / "test.log"
        log_file.write_text(log_content)

        result = parse_download_log(log_file)

        self.assertEqual(len(result), 2)
        self.assertIn("com/example/artifact/1.0/artifact-1.0.jar", result)
        self.assertIn("com/example/artifact/1.0/artifact-1.0.pom", result)
        self.assertEqual(
            result["com/example/artifact/1.0/artifact-1.0.jar"],
            "https://repo.maven.apache.org/maven2/com/example/artifact/1.0/artifact-1.0.jar"
        )

    def test_parse_mixed_log(self):
        """Test parsing log with mixed content"""
        log_content = """[INFO] Building project...
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/junit/junit-bom/5.13.1/junit-bom-5.13.1.pom (5.6 kB at 111 kB/s)
[INFO] Compiling sources...
[INFO] Downloaded from jitpack.io: https://jitpack.io/com/github/user/lib/v1.0/lib-v1.0.jar (200 kB at 1 MB/s)
[INFO] Tests passed
"""
        log_file = self.temp_path / "test.log"
        log_file.write_text(log_content)

        result = parse_download_log(log_file)

        self.assertEqual(len(result), 2)
        self.assertIn("org/junit/junit-bom/5.13.1/junit-bom-5.13.1.pom", result)
        self.assertIn("com/github/user/lib/v1.0/lib-v1.0.jar", result)

    def test_ignore_non_jar_pom(self):
        """Test that non-.jar/.pom files are ignored"""
        log_content = """[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/com/example/artifact/1.0/artifact-1.0.jar (100 kB)
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/com/example/artifact/1.0/artifact-1.0.jar.sha1 (40 B)
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/com/example/artifact/1.0/artifact-1.0.pom (5 kB)
"""
        log_file = self.temp_path / "test.log"
        log_file.write_text(log_content)

        result = parse_download_log(log_file)

        self.assertEqual(len(result), 2)
        # Should only have .jar and .pom, not .sha1
        self.assertIn("com/example/artifact/1.0/artifact-1.0.jar", result)
        self.assertIn("com/example/artifact/1.0/artifact-1.0.pom", result)
        self.assertNotIn("com/example/artifact/1.0/artifact-1.0.jar.sha1", result)

    def test_duplicate_handling(self):
        """Test that duplicate entries are handled"""
        log_content = """[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/com/example/artifact/1.0/artifact-1.0.jar (100 kB)
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/com/example/artifact/1.0/artifact-1.0.jar (100 kB)
"""
        log_file = self.temp_path / "test.log"
        log_file.write_text(log_content)

        result = parse_download_log(log_file)

        # Should only have one entry despite duplicate
        self.assertEqual(len(result), 1)
        self.assertIn("com/example/artifact/1.0/artifact-1.0.jar", result)


class TestScanMavenRepo(unittest.TestCase):
    """Test scanning of Maven repository directory."""

    def setUp(self):
        """Create temporary Maven repository structure."""
        self.temp_dir = tempfile.mkdtemp()
        self.repo_path = Path(self.temp_dir)

    def tearDown(self):
        """Clean up temporary directory."""
        shutil.rmtree(self.temp_dir)

    def test_scan_empty_repo(self):
        """Test scanning empty repository"""
        result = scan_maven_repo(self.repo_path)
        self.assertEqual(len(result), 0)

    def test_scan_with_jar_and_pom(self):
        """Test scanning repository with .jar and .pom files"""
        # Create directory structure
        artifact_dir = self.repo_path / "com" / "example" / "artifact" / "1.0"
        artifact_dir.mkdir(parents=True)

        # Create files
        (artifact_dir / "artifact-1.0.jar").touch()
        (artifact_dir / "artifact-1.0.pom").touch()
        (artifact_dir / "artifact-1.0.jar.sha1").touch()
        (artifact_dir / "_remote.repositories").touch()

        result = scan_maven_repo(self.repo_path)

        # Should only include .jar and .pom, not metadata files
        self.assertEqual(len(result), 2)
        self.assertIn("com/example/artifact/1.0/artifact-1.0.jar", result)
        self.assertIn("com/example/artifact/1.0/artifact-1.0.pom", result)

    def test_scan_multiple_versions(self):
        """Test scanning repository with multiple versions"""
        # Create version 1.0
        dir_v1 = self.repo_path / "com" / "example" / "lib" / "1.0"
        dir_v1.mkdir(parents=True)
        (dir_v1 / "lib-1.0.jar").touch()
        (dir_v1 / "lib-1.0.pom").touch()

        # Create version 2.0
        dir_v2 = self.repo_path / "com" / "example" / "lib" / "2.0"
        dir_v2.mkdir(parents=True)
        (dir_v2 / "lib-2.0.jar").touch()
        (dir_v2 / "lib-2.0.pom").touch()

        result = scan_maven_repo(self.repo_path)

        self.assertEqual(len(result), 4)
        self.assertIn("com/example/lib/1.0/lib-1.0.jar", result)
        self.assertIn("com/example/lib/1.0/lib-1.0.pom", result)
        self.assertIn("com/example/lib/2.0/lib-2.0.jar", result)
        self.assertIn("com/example/lib/2.0/lib-2.0.pom", result)


class TestCalculateSHA256(unittest.TestCase):
    """Test SHA256 calculation."""

    def setUp(self):
        """Create temporary directory for test files."""
        self.temp_dir = tempfile.mkdtemp()
        self.temp_path = Path(self.temp_dir)

    def tearDown(self):
        """Clean up temporary directory."""
        shutil.rmtree(self.temp_dir)

    def test_calculate_sha256_simple(self):
        """Test SHA256 calculation for simple content"""
        test_file = self.temp_path / "test.txt"
        test_file.write_bytes(b"Hello, World!")

        sha256 = calculate_sha256(test_file)

        # Expected SHA256 of "Hello, World!"
        expected = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
        self.assertEqual(sha256, expected)

    def test_calculate_sha256_empty(self):
        """Test SHA256 calculation for empty file"""
        test_file = self.temp_path / "empty.txt"
        test_file.write_bytes(b"")

        sha256 = calculate_sha256(test_file)

        # Expected SHA256 of empty string
        expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        self.assertEqual(sha256, expected)

    def test_calculate_sha256_large_file(self):
        """Test SHA256 calculation for larger file (tests chunking)"""
        test_file = self.temp_path / "large.bin"
        # Create file larger than chunk size
        content = b"x" * (128 * 1024)  # 128 KB
        test_file.write_bytes(content)

        sha256 = calculate_sha256(test_file)

        # Verify it's a valid hex string
        self.assertEqual(len(sha256), 64)
        self.assertTrue(all(c in '0123456789abcdef' for c in sha256))


class TestGenerateYAMLEntry(unittest.TestCase):
    """Test YAML entry generation."""

    def test_generate_yaml_basic(self):
        """Test basic YAML entry generation"""
        dest_path = "com/example/artifact/1.0"
        url = "https://repo.maven.apache.org/maven2/com/example/artifact/1.0/artifact-1.0.jar"
        sha256 = "abc123def456"

        result = generate_yaml_entry(dest_path, url, sha256)

        self.assertIn("- type: file", result)
        self.assertIn(f"dest: .m2/repository/{dest_path}", result)
        self.assertIn(f"url: {url}", result)
        self.assertIn(f"sha256: {sha256}", result)

    def test_generate_yaml_format(self):
        """Test YAML entry has correct format"""
        dest_path = "org/junit/junit-bom/5.13.1"
        url = "https://repo.maven.apache.org/maven2/org/junit/junit-bom/5.13.1/junit-bom-5.13.1.pom"
        sha256 = "fedcba987654"

        result = generate_yaml_entry(dest_path, url, sha256)

        # Check that lines are properly formatted
        lines = result.split('\n')
        self.assertEqual(len(lines), 4)
        self.assertTrue(lines[0].startswith("- type:"))
        self.assertTrue(lines[1].startswith("  dest:"))
        self.assertTrue(lines[2].startswith("  url:"))
        self.assertTrue(lines[3].startswith("  sha256:"))


if __name__ == '__main__':
    unittest.main()
