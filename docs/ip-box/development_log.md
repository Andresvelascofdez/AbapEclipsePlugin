# Development Log

This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.

## 2026-05-11 - Initial Eclipse Plug-in Scaffold

- Feature/module worked on: ABAP Chat Assistant initial plug-in, core assistant services, OpenAI integration layer, validation scripts and documentation.
- Technical objective: create a separated Eclipse PDE project for `Andresvelascofdez/AbapEclipsePlugin` without mixing it with `SapIsuAssistant`.
- Implementation summary: created Eclipse plug-in metadata, ABAP Chat view, core prompt builder, privacy classifier, redactor, dotenv loader, OpenAI Responses API client, CLI smoke test, automated tests, README, changelog and installation guide.
- Files changed: project metadata files, `src/com/abap/assistant/**`, `test/com/abap/assistant/**`, `scripts/**`, `README.md`, `CHANGELOG.md`, `docs/**`.
- User/business reason: the project owner requested development of the tasks defined in `instrucciones.md`, recurring GitHub publishing discipline, a guide to install and test in Eclipse, and validation before confirming functionality.
- Validation status: automated validation passed locally with `scripts/test.ps1` on 2026-05-11.
- GitHub commit reference: `09793a4645a734b7d8e60751a199a4facf5400a6` pushed to `Andresvelascofdez/AbapEclipsePlugin`.
- Open limitations: live OpenAI smoke testing requires a local non-committed `.env` with a valid API key. Eclipse runtime validation must be performed in an Eclipse PDE/ADT installation.
- AI assistance used: yes, Codex generated the initial implementation and documentation under owner direction.
- Human decision/review notes: the project owner confirmed the GitHub destination as `Andresvelascofdez/AbapEclipsePlugin` and explicitly instructed that it must not be mixed with `Andresvelascofdez/SapIsuAssistant`.

## 2026-05-12 - Eclipse Import Error Handling

- Feature/module worked on: Eclipse import prerequisites, Java compatibility and troubleshooting documentation.
- Technical objective: address Eclipse workspace errors where SWT/JFace/PDE classes are not resolved during import.
- Implementation summary: changed compiler and bundle execution environment to Java 11, removed Java 17-only language constructs from core/test code, added `scripts/check-eclipse-prereqs.ps1`, and documented the PDE/Target Platform fix.
- Files changed: `.settings/org.eclipse.jdt.core.prefs`, `META-INF/MANIFEST.MF`, core Java files, tests, scripts, README, changelog, installation guide and IP Box logs.
- User/business reason: the project owner reported Eclipse import errors from the Problems view.
- Validation status: automated validation passed locally with `scripts/test.ps1` on 2026-05-12.
- Open limitations: local environment still does not include a detectable Eclipse installation, so runtime UI validation remains TODO/TBC.
- AI assistance used: yes.
- Human decision/review notes: owner provided screenshot evidence of the Eclipse import issue.

## 2026-05-12 - Eclipse Runtime Validation And Bundle Identity Fix

- Feature/module worked on: Eclipse runtime identity, ABAP Chat view contribution, icon packaging and Eclipse smoke tests.
- Technical objective: make the plugin match the persisted Eclipse runtime identity `com.abap.assistant` / `com.abap.assistant.ui.ChatView` and prove it opens in Eclipse.
- Implementation summary: renamed Java packages and bundle metadata to `com.abap.assistant`, set the view id/class to `com.abap.assistant.ui.ChatView`, added `icons/abap_icon.png`, added `scripts/test-eclipse.ps1`, and documented the Eclipse test plan.
- Files changed: `.project`, `META-INF/MANIFEST.MF`, `plugin.xml`, `build.properties`, `src/com/abap/assistant/**`, `test/com/abap/assistant/**`, `icons/abap_icon.png`, `scripts/**`, `docs/**`, `README.md`, `CHANGELOG.md`.
- User/business reason: the project owner reported that previous validation did not actually run inside Eclipse and provided Eclipse runtime errors.
- Validation status: `scripts/test.ps1`, clean Eclipse runtime smoke test, and persisted workspace Eclipse smoke test passed on 2026-05-12.
- Open limitations: live OpenAI API call still requires a local valid `.env`; no real client data should be used in smoke tests.
- AI assistance used: yes.
- Human decision/review notes: owner challenged the insufficient validation and required Eclipse-based testing before confirmation.

## 2026-05-13 - Eclipse `.env` Discovery Fix

- Feature/module worked on: OpenAI configuration loading inside Eclipse runtime.
- Technical objective: allow the ABAP Chat view to read `OPENAI_API_KEY` from the imported project `.env`, not only from Eclipse's process working directory.
- Implementation summary: added `EclipseDotEnvLocator`, exported the core and Eclipse helper packages, updated `ChatView` to pass Eclipse workspace `.env` candidates to `OpenAiSettings`, and expanded the Eclipse smoke test to validate workspace project `.env` discovery.
- Files changed: `META-INF/MANIFEST.MF`, `src/com/abap/assistant/core/OpenAiSettings.java`, `src/com/abap/assistant/eclipse/EclipseDotEnvLocator.java`, `src/com/abap/assistant/ui/ChatView.java`, `scripts/test-eclipse.ps1`, documentation and IP Box records.
- User/business reason: the project owner reported that `.env` existed but the Eclipse view still showed `OPENAI_API_KEY is required`.
- Validation status: local validation, clean Eclipse smoke test, persisted workspace Eclipse smoke test and live OpenAI smoke test passed on 2026-05-13.
- Open limitations: live usage still depends on a valid non-committed OpenAI API key and network access.
- AI assistance used: yes.
- Human decision/review notes: owner confirmed `.env` had been created and provided the Eclipse UI error screenshot.
