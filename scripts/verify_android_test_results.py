#!/usr/bin/env python3
"""Reject missing, empty, skipped-only, or failing connected Android test runs."""
import argparse
from collections import Counter
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def verify(results: Path, required_classes=()):
    files = sorted(results.rglob('TEST-*.xml')) if results.is_dir() else []
    errors, executed = [], Counter()
    skipped = total = 0
    if not files:
        errors.append('No connected Android JUnit XML reports found; assembling a test APK is not execution.')
    for path in files:
        try:
            root = ET.parse(path).getroot()
            cases = list(root.iter('testcase'))
            total += len(cases)
            # Some runners report a suite failure without a matching testcase node.
            for suite in root.iter('testsuite'):
                if int(suite.get('failures', '0')) or int(suite.get('errors', '0')):
                    errors.append(f'{path.name}: testsuite reports failures or errors')
            for case in cases:
                name = f"{case.get('classname', '')}.{case.get('name', '')}"
                if case.find('failure') is not None or case.find('error') is not None:
                    errors.append(f'{path.name}: failed {name}')
                elif case.find('skipped') is not None:
                    skipped += 1
                else:
                    executed[case.get('classname', '')] += 1
        except (OSError, ET.ParseError, ValueError) as exc:
            errors.append(f'{path.name}: unreadable test report: {exc}')
    if not sum(executed.values()):
        errors.append('No successful, non-skipped Android testcases were executed.')
    for class_name in required_classes:
        if not executed[class_name]:
            errors.append(f'Required test class did not execute successfully: {class_name}')
    return {
        'status': 'FAIL' if errors else 'PASS',
        'scope': 'CONNECTED_ANDROID_TEST_RESULTS; does not certify semantic or image acceptance',
        'reportFiles': [str(path) for path in files],
        'testcases': total,
        'successful': sum(executed.values()),
        'skipped': skipped,
        'successfulByClass': dict(executed),
        'errors': errors,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--results', type=Path, required=True)
    parser.add_argument('--require-class', action='append', default=[])
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    result = verify(args.results, args.require_class)
    encoded = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(encoded, encoding='utf-8')
    print(encoded, end='')
    return int(bool(result['errors']))


if __name__ == '__main__':
    raise SystemExit(main())
