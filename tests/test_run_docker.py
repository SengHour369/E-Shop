"""Launcher regression checks use fake Docker/curl and never start services."""
import json
import os
from pathlib import Path
import shutil
import subprocess

LAUNCHER = Path(__file__).resolve().parents[1] / 'run-docker.sh'


def launch(tmp_path, files, explicit=None, automation=False):
    shutil.copy(LAUNCHER, tmp_path / 'run-docker.sh')
    for filename in files:
        (tmp_path / filename).write_text('AI_PROVIDER=GEMINI\n')
    bin_dir = tmp_path / 'bin'
    bin_dir.mkdir()
    docker = bin_dir / 'docker'
    docker.write_text('#!/usr/bin/env python3\nimport json,sys,os\nif \"--environment\" in sys.argv: print(\"COMMERCE_AUTOMATION_ENABLED=\"+os.environ.get(\"LAUNCHER_TEST_AUTOMATION\",\"false\"))\nfrom pathlib import Path\nPath("docker-args.json").write_text(json.dumps(sys.argv[1:]))\n')
    curl = bin_dir / 'curl'
    curl.write_text('#!/bin/sh\nexit 0\n')
    docker.chmod(0o755)
    curl.chmod(0o755)
    environment = {**os.environ, 'PATH': str(bin_dir) + os.pathsep + os.environ['PATH']}
    environment.pop('ESHOP_ENV_FILE', None)
    environment['LAUNCHER_TEST_AUTOMATION'] = 'true' if automation else 'false'
    if explicit is not None:
        environment['ESHOP_ENV_FILE'] = explicit
    result = subprocess.run(['bash', 'run-docker.sh', 'ai-inference'], cwd=tmp_path,
                            env=environment, capture_output=True, text=True, timeout=10)
    args_file = tmp_path / 'docker-args.json'
    return result, json.loads(args_file.read_text()) if args_file.exists() else []


def test_legacy_env_is_loaded_without_missing_optional_compose(tmp_path):
    result, args = launch(tmp_path, ['env'])
    assert result.returncode == 0
    assert args[:3] == ['compose', '--env-file', 'env']
    assert 'compose.docker-local.yaml' not in args
    assert 'GEMINI' not in result.stdout


def test_standard_env_and_private_override_order(tmp_path):
    result, args = launch(tmp_path, ['env', '.env', '.env.local', 'compose.docker-local.yaml'])
    assert result.returncode == 0
    assert args[:5] == ['compose', '--env-file', '.env', '--env-file', '.env.local']
    assert 'env' not in args
    assert 'compose.docker-local.yaml' in args


def test_explicit_env_is_used_alone(tmp_path):
    result, args = launch(tmp_path, ['.env', '.env.local', 'custom.env'], 'custom.env')
    assert result.returncode == 0
    assert args[:3] == ['compose', '--env-file', 'custom.env']
    assert '.env' not in args and '.env.local' not in args


def test_missing_explicit_env_fails_before_docker(tmp_path):
    result, args = launch(tmp_path, [], 'missing.env')
    assert result.returncode != 0
    assert not args
    assert 'does not exist' in result.stderr


def test_commerce_flag_loads_both_payment_and_order_automation_config(tmp_path):
    result, args = launch(tmp_path, ['env'], automation=True)
    assert result.returncode == 0
    assert 'compose.automation.yaml' in args
