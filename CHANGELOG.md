# Changelog

## Documentation - 2026-05-15

- Removed obsolete IP Box contribution/disclosure records from the documentation set.
- Refocused the IP Box folder on product evidence: development history, architecture decisions, feature register, validation log and evidence register.
- Updated `instrucciones.md`, development log, evidence register and validation log so the record reads as a technical product dossier rather than an authorship/tooling dossier.

## 0.1.5 - 2026-05-14

- Added automatic related workspace source loading for detected ABAP references when matching text resources already exist in the Eclipse workspace.
- Extended ABAP reference detection to include common class usages in addition to includes, programs, function modules and transactions.
- Added bounded conversation history so follow-up questions in the same view can use recent Q/A context.
- Added a compact context summary showing editor count, related source count, reference count, prompt character count and history turns.
- Updated README, Eclipse installation/testing guide, Eclipse test plan and IP Box documentation for the new context workflow.
- Removed documentation references to unrelated tools/projects so the project record remains independent.

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
- Added a freer chat workflow with separate question/context fields, active-editor loading, open-editor context loading, automatic active-editor refresh on ask, related ABAP reference hints, and code-suggestion prompting that does not claim to apply SAP changes.
- Simplified the chat workflow so `Ask` automatically reads every open Eclipse text editor tab by default, without manual context-loading buttons or a visible context panel.
- Cleared the `Question` box immediately after each accepted ask request and refreshed user/IP Box documentation to match the current open-editor chat behaviour.
