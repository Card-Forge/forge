# Test Logs Archive

This directory contains archived test artifacts from comprehensive test runs. These files are committed to GitHub for verification and audit purposes.

## Contents

When comprehensive tests are run and results are documented in `.documentation/Testing.md`, the following artifacts are archived here:

- `comprehensive-test-results-YYYYMMDD-HHMMSS.md` - Analysis report with aggregated metrics
- `network-debug-runYYYYMMDD-HHMMSS-gameN-Pp-test.log` - Individual game logs

## Purpose

These files provide:
1. **Verification** - Anyone can verify that documented test metrics match actual results
2. **Reproducibility** - Log files allow debugging of any reported issues
3. **Audit Trail** - Historical record of test runs that informed documentation

## Naming Convention

- **Batch ID**: `runYYYYMMDD-HHMMSS` - All logs from the same test run share this prefix
- **Game ID**: `gameN-Pp` - Game index and player count (e.g., `game5-3p` = game 5, 3-player)

## Policy

Test artifacts MUST be archived here BEFORE updating `.documentation/Testing.md` with any test metrics. This ensures all documented numbers can be independently verified.
