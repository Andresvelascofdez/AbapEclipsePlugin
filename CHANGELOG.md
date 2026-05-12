# Changelog

## 0.1.0 - 2026-05-11

- Created the initial Eclipse PDE plug-in scaffold for ABAP Eclipse Assistant.
- Added the ABAP Assistant view contribution and UI shell.
- Added core assistant services for prompt building, privacy classification, sensitive data redaction, dotenv loading, OpenAI Responses API calls, and response text extraction.
- Added a CLI smoke-test entry point for live OpenAI validation when a local `.env` is configured.
- Added automated core tests, plug-in metadata validation, and secret scanning through `scripts/test.ps1`.
- Added README, Eclipse installation/testing guide, and IP Box documentation records.
- Lowered source compatibility to Java 11 and added Eclipse PDE prerequisite diagnostics for import errors where `org.eclipse.*`, SWT, JFace or PDE classes are not resolved.
