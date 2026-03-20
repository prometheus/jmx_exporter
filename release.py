#!/usr/bin/env python3

#
# Copyright (C) The Prometheus jmx_exporter Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
Release script for Prometheus JMX Exporter

This script performs a complete release workflow including:
- Version updates across all Maven modules
- Building and verification
- Artifact deployment to Maven Central
- Creating release artifacts with GPG signatures and checksums
- Git tagging and branch management
- Post-release version bump

Usage:
    release.py <version> [options]
    release.py 1.6.0              # Dry-run mode (default)
    release.py 1.6.0 --execute   # Execute actual release
    release.py --help            # Show help
"""

import argparse
import atexit
import hashlib
import os
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass, field
from enum import Enum, auto
from typing import Optional

RELEASE_DIR = "RELEASE"
SETTINGS_FILE = "~/.m2/prometheus.settings.xml"
LOCK_FILE = "/tmp/jmx_exporter_release.pid"
VERSION_PATTERN = r"^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9._-]+)?$"

ARTIFACT_PATHS = [
    "jmx_prometheus_javaagent/target/jmx_prometheus_javaagent-{version}.jar",
    "jmx_prometheus_isolator_javaagent/target/jmx_prometheus_isolator_javaagent-{version}.jar",
    "jmx_prometheus_standalone/target/jmx_prometheus_standalone-{version}.jar",
]


class Step(Enum):
    CHECK_PREREQUISITES = auto()
    VALIDATE_GIT = auto()
    UPDATE_VERSION = auto()
    BUILD_VERIFY = auto()
    DEPLOY = auto()
    ASSEMBLE_ARTIFACTS = auto()
    GIT_RELEASE = auto()
    POST_RELEASE = auto()


class Color:
    RED = "\033[0;31m"
    GREEN = "\033[0;32m"
    YELLOW = "\033[1;33m"
    BLUE = "\033[0;34m"
    CYAN = "\033[0;36m"
    RESET = "\033[0m"

    @classmethod
    def disable(cls) -> None:
        cls.RED = ""
        cls.GREEN = ""
        cls.YELLOW = ""
        cls.BLUE = ""
        cls.CYAN = ""
        cls.RESET = ""


@dataclass
class Config:
    release_version: str
    dry_run: bool = True
    verbose: bool = False
    gpg_key_id: Optional[str] = None
    git_remote: Optional[str] = None
    resume_step: Optional[str] = None
    original_branch: Optional[str] = None
    original_version: Optional[str] = None
    release_completed: bool = False
    lock_file: str = field(default_factory=lambda: LOCK_FILE)


class ReleaseScript:
    def __init__(self, config: Config) -> None:
        self.config = config
        self.settings_file = os.path.expanduser(SETTINGS_FILE)

        if not sys.stdout.isatty():
            Color.disable()

        atexit.register(self._cleanup)

    def _cleanup(self) -> None:
        if os.path.exists(self.config.lock_file):
            os.remove(self.config.lock_file)

    def log(self, message: str, level: str = "INFO") -> None:
        if level == "ERROR":
            print(f"{Color.RED}[ERROR]{Color.RESET} {message}", file=sys.stderr)
        elif level == "WARN":
            print(f"{Color.YELLOW}[WARN]{Color.RESET} {message}", file=sys.stderr)
        elif level == "STEP":
            print(f"\n{Color.CYAN}{'=' * 80}{Color.RESET}")
            print(f"{Color.CYAN}[STEP]{Color.RESET} {message}")
            print(f"{Color.CYAN}{'=' * 80}{Color.RESET}")
        elif level == "VERBOSE":
            if self.config.verbose:
                print(f"{Color.BLUE}[VERBOSE]{Color.RESET} {message}")
        elif level == "DRY_RUN":
            print(f"{Color.BLUE}[DRY-RUN]{Color.RESET} {message}")
        else:
            print(f"{Color.GREEN}[INFO]{Color.RESET} {message}")

    def run_cmd(
        self, *args, check: bool = True, cwd: Optional[str] = None
    ) -> subprocess.CompletedProcess:
        cmd_str = " ".join(str(a) for a in args)
        self.log(f"Executing: {cmd_str}", "VERBOSE")

        if self.config.dry_run:
            self.log(f"Would execute: {cmd_str}", "DRY_RUN")
            return subprocess.CompletedProcess(args, 0, "", "")

        try:
            result = subprocess.run(
                args, cwd=cwd, capture_output=True, text=True, check=False
            )

            if check and result.returncode != 0:
                self.log(f"Command failed: {cmd_str}", "ERROR")
                if result.stdout:
                    self.log(f"stdout: {result.stdout}", "VERBOSE")
                if result.stderr:
                    self.log(f"stderr: {result.stderr}", "ERROR")
                raise subprocess.CalledProcessError(result.returncode, args)

            return result
        except FileNotFoundError:
            self.log(f"Command not found: {args[0]}", "ERROR")
            raise
        except subprocess.CalledProcessError:
            raise
        except OSError as e:
            self.log(f"OS error executing command: {e}", "ERROR")
            raise

    def run_cmd_output(self, *args, **kwargs) -> str:
        kwargs.setdefault("check", True)
        result = self.run_cmd(*args, **kwargs)
        return result.stdout.strip()

    def check_prerequisites(self) -> bool:
        self.log("Checking prerequisites", "STEP")

        if not os.path.exists("mvnw"):
            self.log("mvnw not found in current directory", "ERROR")
            return False

        if not os.path.exists(self.settings_file):
            self.log(f"{self.settings_file} not found", "ERROR")
            self.log("This file is required for Maven Central deployment", "ERROR")
            return False

        self.log(f"Found Maven settings: {self.settings_file}", "VERBOSE")

        try:
            result = self.run_cmd("gpg", "--version", check=False)
            if result.returncode != 0:
                self.log("GPG not found - required for artifact signing", "ERROR")
                return False
        except FileNotFoundError:
            self.log("GPG not found - required for artifact signing", "ERROR")
            return False

        if self.config.gpg_key_id:
            result = self.run_cmd(
                "gpg", "--list-secret-keys", self.config.gpg_key_id, check=False
            )
            if result.returncode != 0:
                self.log(f"GPG secret key not found: {self.config.gpg_key_id}", "ERROR")
                return False
            self.log(f"Using GPG key: {self.config.gpg_key_id}", "VERBOSE")
        else:
            self.log("Using default GPG key", "VERBOSE")

        try:
            self.run_cmd("git", "--version", check=True)
        except (FileNotFoundError, subprocess.CalledProcessError):
            self.log("Git not found", "ERROR")
            return False

        try:
            self.run_cmd_or_simulate("sha256sum", "--version", check=False)
        except FileNotFoundError:
            self.log("sha256sum not found", "ERROR")
            return False

        if not self.config.dry_run:
            with open(self.config.lock_file, "w") as f:
                f.write(str(os.getpid()))

        self.log("All prerequisites satisfied")
        return True

    def run_cmd_or_simulate(
        self, *args, check: bool = True, **kwargs
    ) -> subprocess.CompletedProcess:
        if self.config.dry_run:
            self.log(f"Would execute: {' '.join(str(a) for a in args)}", "DRY_RUN")
            return subprocess.CompletedProcess(args, 0, "", "")
        return self.run_cmd(*args, check=check, **kwargs)

    def validate_git_state(self) -> bool:
        self.log("Validating git state", "STEP")

        try:
            result = self.run_cmd("git", "status", "--porcelain")
            if result.stdout.strip() and not self.config.dry_run:
                self.log("Working directory is not clean", "ERROR")
                self.log("Uncommitted changes detected:", "ERROR")
                for line in result.stdout.strip().split("\n")[:20]:
                    self.log(f"  {line}", "ERROR")
                self.log("Commit or stash changes before releasing", "ERROR")
                return False
        except subprocess.CalledProcessError:
            self.log("Not a git repository", "ERROR")
            return False

        if not self.config.dry_run:
            self.log("Working directory is clean", "VERBOSE")

        result = self.run_cmd("git", "rev-parse", "--abbrev-ref", "HEAD", check=True)
        branch = result.stdout.strip() if not self.config.dry_run else "main"
        self.config.original_branch = branch
        if not self.config.dry_run:
            self.log(f"Current branch: {self.config.original_branch}", "VERBOSE")
        else:
            self.log("Would check current git branch", "VERBOSE")

        if branch not in ("main", "master"):
            self.log(
                f"Not on main or master branch (currently on {branch})",
                "ERROR",
            )
            self.log("Releases should be made from main/master", "ERROR")
            return False

        if not self.config.git_remote:
            if not self.config.dry_run:
                result = self.run_cmd(
                    "git",
                    "config",
                    "--get",
                    f"branch.{self.config.original_branch}.remote",
                    check=False,
                )
                self.config.git_remote = (
                    result.stdout.strip()
                    if result.returncode == 0 and result.stdout.strip()
                    else "origin"
                )
            else:
                self.config.git_remote = "origin"
            self.log(f"Auto-detected git remote: {self.config.git_remote}", "VERBOSE")

        if not self.config.dry_run:
            result = self.run_cmd(
                "git", "remote", "get-url", self.config.git_remote, check=False
            )
            if result.returncode != 0:
                self.log(f"Git remote '{self.config.git_remote}' not found", "ERROR")
                return False

        if not self.config.dry_run:
            original_version = self.run_cmd_output(
                "./mvnw",
                "help:evaluate",
                "-Dexpression=project.version",
                "-q",
                "-DforceStdout",
            )
            if original_version:
                original_version = original_version.split("\n")[0]
            self.config.original_version = original_version
            self.log(
                f"Current project version: {self.config.original_version}", "VERBOSE"
            )

        self.log("Git state validated successfully")
        return True

    def update_version(self) -> bool:
        self.log(f"Updating version to {self.config.release_version}", "STEP")

        args = [
            "./mvnw",
            "-B",
            "versions:set",
            f"-DnewVersion={self.config.release_version}",
            "-DprocessAllModules",
        ]
        if self.config.dry_run:
            self.log(f"Would execute: {' '.join(args)}", "DRY_RUN")
        else:
            self.run_cmd(*args, check=False)
            self.run_cmd(*args, check=True)
            self._cleanup_version_backups()
            self.log(f"Version updated to {self.config.release_version}")

        return True

    def _cleanup_version_backups(self) -> None:
        for root, dirs, files in os.walk("."):
            for f in files:
                if f.endswith(".versionsBackup"):
                    os.remove(os.path.join(root, f))
                    self.log(f"Removed: {os.path.join(root, f)}", "VERBOSE")

    def build_and_verify(self) -> bool:
        self.log("Building and verifying", "STEP")

        if self.config.dry_run:
            self.log("Would run: ./mvnw -B clean verify", "DRY_RUN")
            return True

        self.run_cmd("./mvnw", "-B", "clean", "verify", check=True)
        self.log("Build and verification completed")
        return True

    def deploy(self) -> bool:
        self.log("Deploying to Maven Central", "STEP")

        if self.config.dry_run:
            self.log(
                f"Would run: ./mvnw -s {self.settings_file} -P release clean deploy",
                "DRY_RUN",
            )
            return True

        self.run_cmd(
            "./mvnw",
            "-s",
            self.settings_file,
            "-P",
            "release",
            "clean",
            "deploy",
            check=True,
        )
        self.log("Deployment completed")
        return True

    def assemble_artifacts(self) -> bool:
        self.log("Assembling release artifacts", "STEP")

        if self.config.dry_run:
            self.log(
                f"Would create {RELEASE_DIR} directory and copy artifacts", "DRY_RUN"
            )
            return True

        if os.path.exists(RELEASE_DIR):
            shutil.rmtree(RELEASE_DIR)
        os.makedirs(RELEASE_DIR)

        for artifact_template in ARTIFACT_PATHS:
            artifact_path = artifact_template.format(
                version=self.config.release_version
            )
            if not os.path.exists(artifact_path):
                self.log(f"Artifact not found: {artifact_path}", "ERROR")
                return False
            shutil.copy(artifact_path, RELEASE_DIR)
            self.log(f"Copied: {artifact_path}", "VERBOSE")

        self.log(f"Artifacts copied to {RELEASE_DIR}/")

        self._generate_signatures_and_checksums()

        self.log("Signatures and checksums generated")
        self._list_release_dir()
        return True

    def _generate_signatures_and_checksums(self) -> None:
        gpg_opts = ["gpg"]
        if self.config.gpg_key_id:
            gpg_opts.extend(["--default-key", self.config.gpg_key_id])
        gpg_opts.extend(["--armor", "--detach-sign"])

        for filename in os.listdir(RELEASE_DIR):
            if filename.endswith(".jar"):
                filepath = os.path.join(RELEASE_DIR, filename)
                self.run_cmd(*gpg_opts, filepath, check=True)
                self.log(f"Signed: {filename}.asc", "VERBOSE")

                sha256 = hashlib.sha256()
                with open(filepath, "rb") as f:
                    for chunk in iter(lambda: f.read(8192), b""):
                        sha256.update(chunk)

                sha256_file = filepath + ".sha256"
                with open(sha256_file, "w") as f:
                    f.write(f"{sha256.hexdigest()}  {filename}\n")

                self.log(f"Checksum: {filename}.sha256", "VERBOSE")

    def _list_release_dir(self) -> None:
        print()
        for item in sorted(os.listdir(RELEASE_DIR)):
            path = os.path.join(RELEASE_DIR, item)
            size = os.path.getsize(path)
            print(f"  {item:50s} {size:>12,} bytes")
        print()

    def git_release(self) -> bool:
        self.log("Creating git tag and release branch", "STEP")

        if self.config.dry_run:
            self.log(f"Would create tag: {self.config.release_version}", "DRY_RUN")
            self.log(f"Would push tag to: {self.config.git_remote}", "DRY_RUN")
            self.log(
                f"Would create branch: release-{self.config.release_version}", "DRY_RUN"
            )
            self.log(f"Would push branch to: {self.config.git_remote}", "DRY_RUN")
            return True

        self.run_cmd("git", "add", "-u", check=True)
        self.run_cmd(
            "git",
            "commit",
            "-s",
            "-m",
            f"Release {self.config.release_version}",
            check=True,
        )
        self.log("Committed version change", "VERBOSE")

        release_branch = f"release-{self.config.release_version}"

        self.run_cmd("git", "tag", self.config.release_version, check=True)
        self.log(f"Created tag: {self.config.release_version}", "VERBOSE")

        self.run_cmd("git", "push", self.config.git_remote, "--tags", check=True)
        self.log(f"Pushed tag to: {self.config.git_remote}", "VERBOSE")

        self.run_cmd("git", "checkout", "-b", release_branch, check=True)
        self.run_cmd(
            "git",
            "push",
            "--set-upstream",
            self.config.git_remote,
            release_branch,
            check=True,
        )
        self.log(f"Created and pushed branch: {release_branch}", "VERBOSE")

        self.run_cmd("git", "checkout", self.config.original_branch, check=True)

        self.log("Git tag and release branch created")
        return True

    def post_release_version(self) -> bool:
        self.log("Bumping to post-release version", "STEP")

        post_version = f"{self.config.release_version}-post"

        if self.config.dry_run:
            self.log(f"Would update version to: {post_version}", "DRY_RUN")
            self.log(f"Would commit and push to: {self.config.git_remote}", "DRY_RUN")
            return True

        self.run_cmd(
            "./mvnw",
            "-B",
            "versions:set",
            f"-DnewVersion={post_version}",
            "-DprocessAllModules",
            check=False,
        )
        self.run_cmd(
            "./mvnw",
            "-B",
            "versions:set",
            f"-DnewVersion={post_version}",
            "-DprocessAllModules",
            check=True,
        )
        self._cleanup_version_backups()

        self.run_cmd("git", "add", "-u", check=True)
        self.run_cmd("git", "commit", "-s", "-m", "Prepare for development", check=True)

        self.run_cmd("git", "push", self.config.git_remote, check=True)

        self.log(f"Post-release version set to {post_version}")
        return True

    def validate_version(self) -> bool:
        if not re.match(VERSION_PATTERN, self.config.release_version):
            self.log(f"Invalid version format: {self.config.release_version}", "ERROR")
            self.log("Expected: MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH-label", "ERROR")
            return False

        if self.config.release_version.endswith("-post"):
            self.log(
                "Version should not end with '-post' (that's reserved for post-release)",
                "ERROR",
            )
            return False

        self.log(f"Version format validated: {self.config.release_version}", "VERBOSE")
        return True

    def rollback(self) -> None:
        self.log("Attempting rollback...", "ERROR")

        if self.config.original_branch:
            self.run_cmd("git", "checkout", self.config.original_branch, check=False)

        if self.config.original_version:
            self.run_cmd(
                "./mvnw",
                "-B",
                "versions:set",
                f"-DnewVersion={self.config.original_version}",
                "-DprocessAllModules",
                check=False,
            )
            self._cleanup_version_backups()

        self.log("Rollback attempted. Please check repository state.", "ERROR")

    def run(self) -> int:
        print("\n========================================")
        print("  Prometheus JMX Exporter Release")
        print("========================================\n")

        if self.config.dry_run:
            print(f"{Color.YELLOW}Mode: DRY-RUN (no changes will be made){Color.RESET}")
            print(
                f"{Color.YELLOW}Use --execute to perform actual release{Color.RESET}\n"
            )
        else:
            print(f"{Color.GREEN}Mode: EXECUTE{Color.RESET}")
            print(
                f"{Color.RED}WARNING: This will modify files and push to remote!{Color.RESET}\n"
            )

        print(f"Release version: {self.config.release_version}")
        print(f"Git remote: {self.config.git_remote or 'auto-detect'}")
        print(f"GPG key: {self.config.gpg_key_id or 'default'}\n")

        if not self.validate_version():
            return 1

        steps = [
            (Step.CHECK_PREREQUISITES, self.check_prerequisites),
            (Step.VALIDATE_GIT, self.validate_git_state),
            (Step.UPDATE_VERSION, self.update_version),
            (Step.BUILD_VERIFY, self.build_and_verify),
            (Step.DEPLOY, self.deploy),
            (Step.ASSEMBLE_ARTIFACTS, self.assemble_artifacts),
            (Step.GIT_RELEASE, self.git_release),
            (Step.POST_RELEASE, self.post_release_version),
        ]

        resume_enabled = self.config.resume_step is not None
        resume_step = None
        if resume_enabled:
            try:
                resume_step = Step[self.config.resume_step.upper()]
            except KeyError:
                self.log(f"Unknown step: {self.config.resume_step}", "ERROR")
                self.log(
                    f"Valid steps: {', '.join(s.name.lower() for s in Step)}", "ERROR"
                )
                return 1
            self.log(f"Resuming from step: {resume_step.name}", "WARN")

        executing = not resume_enabled

        try:
            for step_enum, step_fn in steps:
                if resume_enabled and step_enum == resume_step:
                    executing = True

                if not executing:
                    self.log(f"Skipping step: {step_enum.name}", "VERBOSE")
                    continue

                if not step_fn():
                    return 1

            self.config.release_completed = True

            print("\n========================================")
            if self.config.dry_run:
                print(f"{Color.GREEN}Dry-run completed successfully!{Color.RESET}")
                print("Run with --execute to perform actual release")
            else:
                print(
                    f"{Color.GREEN}Release {self.config.release_version} completed successfully!{Color.RESET}"
                )
            print("========================================\n")

            return 0
        except KeyboardInterrupt:
            print("\n")
            self.log("Release interrupted by user", "ERROR")
            if not self.config.dry_run:
                self.rollback()
            return 130
        except Exception as e:
            self.log(f"Release failed: {e}", "ERROR")
            if not self.config.dry_run and not self.config.release_completed:
                self.rollback()
            return 1


def parse_args() -> Config:
    parser = argparse.ArgumentParser(
        description="Release script for Prometheus JMX Exporter",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    %(prog)s 1.6.0                      # Dry-run with version 1.6.0
    %(prog)s 1.6.0 --execute            # Execute release 1.6.0
    %(prog)s 1.6.0 --execute --verbose  # Execute with verbose output
    %(prog)s 1.6.0 --gpg-key ABC123     # Use specific GPG key
    %(prog)s 1.6.0 --execute --resume deploy  # Resume from deploy step

Requirements:
    - Clean git working directory
    - ~/.m2/prometheus.settings.xml (for Maven Central deployment)
    - GPG configured for signing
    - Git remote with push access

Steps (for --resume):
    check_prerequisites, validate_git, update_version, build_verify,
    deploy, assemble_artifacts, git_release, post_release
""",
    )

    parser.add_argument("version", help="Release version (e.g., 1.6.0)")

    parser.add_argument(
        "--execute",
        action="store_true",
        help="Execute the release (default is dry-run mode)",
    )

    parser.add_argument(
        "--gpg-key",
        "--gpg-key-id",
        dest="gpg_key_id",
        help="GPG key ID for signing (uses default key if not specified)",
    )

    parser.add_argument(
        "--remote",
        dest="git_remote",
        help="Git remote name (auto-detected if not specified)",
    )

    parser.add_argument(
        "--resume",
        dest="resume_step",
        metavar="STEP",
        help="Resume from specific step (for debugging/interrupted releases)",
    )

    parser.add_argument(
        "-v", "--verbose", action="store_true", help="Enable verbose output"
    )

    parser.add_argument(
        "--no-color", action="store_true", help="Disable colored output"
    )

    args = parser.parse_args()

    config = Config(
        release_version=args.version,
        dry_run=not args.execute,
        verbose=args.verbose,
        gpg_key_id=args.gpg_key_id,
        git_remote=args.git_remote,
        resume_step=args.resume_step,
    )

    if args.no_color:
        Color.disable()

    return config


def main() -> int:
    config = parse_args()
    script = ReleaseScript(config)
    return script.run()


if __name__ == "__main__":
    sys.exit(main())
