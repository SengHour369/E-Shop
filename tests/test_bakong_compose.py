"""Validate payment configuration gates without starting payment workers."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import pytest

ROOT = Path(__file__).resolve().parents[1]
pytestmark = pytest.mark.skipif(shutil.which('docker') is None, reason='Docker Compose is required')


def config(tmp_path, extra, automation=False):
    path = tmp_path / 'test.env'
    path.write_text('BAKONG_ACCOUNT_ID=registered-test@bank\nBAKONG_EMAIL=registered@example.com\n' + extra)
    environment = {k: v for k, v in os.environ.items()
                   if not k.startswith(('BAKONG_', 'KHQR_', 'COMPOSE_'))}
    args = ['docker', 'compose', '--env-file', str(path)]
    for name in ['compose.yaml', 'compose.ai.yaml', 'compose.local.yaml']:
        args += ['-f', str(ROOT / name)]
    if automation:
        args += ['-f', str(ROOT / 'compose.automation.yaml')]
    return subprocess.run(args + ['config', '--format', 'json'], env=environment,
                          capture_output=True, text=True, timeout=15)


def test_basic_provider_configuration_does_not_enable_payment_workers(tmp_path):
    result = config(tmp_path, '')
    assert result.returncode == 0
    service = json.loads(result.stdout)['services']['payment-service']['environment']
    assert service['BAKONG_ACCOUNT_ID'] == 'registered-test@bank'
    assert service['BAKONG_EMAIL'] == 'registered@example.com'
    assert service.get('COMMERCE_AUTOMATION_ENABLED') != 'true'


def test_individual_mode_wires_account_without_merchant_fields(tmp_path):
    result = config(tmp_path, 'BAKONG_KHQR_TYPE=INDIVIDUAL\n', automation=True)
    assert result.returncode == 0
    service = json.loads(result.stdout)['services']['payment-service']['environment']
    assert service['BAKONG_KHQR_TYPE'] == 'INDIVIDUAL'
    assert service['KHQR_MERCHANT_ID'] == ''
    assert service['KHQR_ACQUIRING_BANK'] == ''
    # Java configuration validation still rejects incomplete MERCHANT mode.



def test_complete_explicit_overlay_enables_both_sides_and_kafka(tmp_path):
    result = config(tmp_path, 'KHQR_MERCHANT_NAME=Test Merchant\nKHQR_MERCHANT_ID=test-merchant\nKHQR_ACQUIRING_BANK=test-bank\n', automation=True)
    assert result.returncode == 0
    services = json.loads(result.stdout)['services']
    for name in ['payment-service', 'order-service']:
        assert services[name]['environment']['COMMERCE_AUTOMATION_ENABLED'] == 'true'
        assert services[name]['environment']['SPRING_KAFKA_BOOTSTRAP_SERVERS'] == 'kafka:19092'
