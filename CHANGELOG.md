# Changelog

## 0.1.0 - 2026-05-11

- Created the initial Eclipse PDE plug-in scaffold for ABAP Chat Assistant.
- Added the ABAP Chat view contribution and UI shell.
- Added core assistant services for prompt building, privacy classification, sensitive data redaction, dotenv loading, OpenAI Responses API calls, and response text extraction.
- Added a CLI smoke-test entry point for live OpenAI validation when a local `.env` is configured.
- Added automated core tests, plug-in metadata validation, and secret scanning through `scripts/test.ps1`.
- Added README, Eclipse installation/testing guide, and IP Box documentation records.
- Lowered source compatibility to Java 11 and added Eclipse PDE prerequisite diagnostics for import errors where `org.eclipse.*`, SWT, JFace or PDE classes are not resolved.
- Renamed the runtime bundle/view to `com.abap.assistant` / `com.abap.assistant.ui.ChatView`, added the ABAP Chat icon expected by Eclipse persisted state, and added real Eclipse smoke tests.
- Added Eclipse workspace `.env` discovery so the view can read `OPENAI_API_KEY` from the imported project location, not only from Eclipse's process working directory.
- Added bundle/code-location `.env` discovery for PDE runtime launches where the Eclipse Application workspace does not contain the development project, and bumped the bundle to `0.1.1.qualifier`.
- Fixed the Eclipse project classpath to use the JavaSE-11 execution environment explicitly, avoiding mixed compiler settings where compliance is 11 but the workspace target defaults to 21.
- Fixed PDE build/export Java settings by adding explicit `javacSource = 11` and `javacTarget = 11`, and added an Eclipse project import/build smoke test.
