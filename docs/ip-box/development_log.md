# Development Log

This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.

## 2026-05-11 - Initial Eclipse Plug-in Scaffold

- Feature/module worked on: ABAP Chat Assistant initial plug-in, core assistant services, OpenAI integration layer, validation scripts and documentation.
- Technical objective: create a dedicated Eclipse PDE project for `Andresvelascofdez/AbapEclipsePlugin`.
- Implementation summary: created Eclipse plug-in metadata, ABAP Chat view, core prompt builder, privacy classifier, redactor, dotenv loader, OpenAI Responses API client, CLI smoke test, automated tests, README, changelog and installation guide.
- Files changed: project metadata files, `src/com/abap/assistant/**`, `test/com/abap/assistant/**`, `scripts/**`, `README.md`, `CHANGELOG.md`, `docs/**`.
- User/business reason: the project owner requested development of the tasks defined in `instrucciones.md`, recurring GitHub publishing discipline, a guide to install and test in Eclipse, and validation before confirming functionality.
- Validation status: automated validation passed locally with `scripts/test.ps1` on 2026-05-11.
- GitHub commit reference: `09793a4645a734b7d8e60751a199a4facf5400a6` pushed to `Andresvelascofdez/AbapEclipsePlugin`.
- Open limitations: live OpenAI smoke testing requires a local non-committed `.env` with a valid API key. Eclipse runtime validation must be performed in an Eclipse PDE/ADT installation.
- Development tooling note: implementation and documentation work was performed under project owner direction, using development tooling as support.
- Human decision/review notes: the project owner defined the product direction and confirmed the GitHub destination as `Andresvelascofdez/AbapEclipsePlugin`.

## 2026-05-12 - Eclipse Import Error Handling

- Feature/module worked on: Eclipse import prerequisites, Java compatibility and troubleshooting documentation.
- Technical objective: address Eclipse workspace errors where SWT/JFace/PDE classes are not resolved during import.
- Implementation summary: changed compiler and bundle execution environment to Java 11, removed Java 17-only language constructs from core/test code, added `scripts/check-eclipse-prereqs.ps1`, and documented the PDE/Target Platform fix.
- Files changed: `.settings/org.eclipse.jdt.core.prefs`, `META-INF/MANIFEST.MF`, core Java files, tests, scripts, README, changelog, installation guide and IP Box logs.
- User/business reason: the project owner reported Eclipse import errors from the Problems view.
- Validation status: automated validation passed locally with `scripts/test.ps1` on 2026-05-12.
- Open limitations: local environment still does not include a detectable Eclipse installation, so runtime UI validation remains TODO/TBC.
- Development tooling note: development tooling was used as implementation support under owner direction.
- Human decision/review notes: owner provided screenshot evidence of the Eclipse import issue and set the requirement that Eclipse import/build must work.

## 2026-05-12 - Eclipse Runtime Validation And Bundle Identity Fix

- Feature/module worked on: Eclipse runtime identity, ABAP Chat view contribution, icon packaging and Eclipse smoke tests.
- Technical objective: make the plugin match the persisted Eclipse runtime identity `com.abap.assistant` / `com.abap.assistant.ui.ChatView` and prove it opens in Eclipse.
- Implementation summary: renamed Java packages and bundle metadata to `com.abap.assistant`, set the view id/class to `com.abap.assistant.ui.ChatView`, added `icons/abap_icon.png`, added `scripts/test-eclipse.ps1`, and documented the Eclipse test plan.
- Files changed: `.project`, `META-INF/MANIFEST.MF`, `plugin.xml`, `build.properties`, `src/com/abap/assistant/**`, `test/com/abap/assistant/**`, `icons/abap_icon.png`, `scripts/**`, `docs/**`, `README.md`, `CHANGELOG.md`.
- User/business reason: the project owner reported that previous validation did not actually run inside Eclipse and provided Eclipse runtime errors.
- Validation status: `scripts/test.ps1`, clean Eclipse runtime smoke test, and persisted workspace Eclipse smoke test passed on 2026-05-12.
- Open limitations: live OpenAI API call still requires a local valid `.env`; no real client data should be used in smoke tests.
- Development tooling note: development tooling was used as implementation support under owner direction.
- Human decision/review notes: owner challenged the insufficient validation and required Eclipse-based testing before confirmation.

## 2026-05-13 - Eclipse `.env` Discovery Fix

- Feature/module worked on: OpenAI configuration loading inside Eclipse runtime.
- Technical objective: allow the ABAP Chat view to read `OPENAI_API_KEY` from the development project `.env`, not only from Eclipse's process working directory or runtime workspace.
- Implementation summary: added `EclipseDotEnvLocator`, exported the core and Eclipse helper packages, updated `ChatView` to pass Eclipse `.env` candidates to `OpenAiSettings`, added workspace-project and bundle/code-location discovery, and expanded the Eclipse smoke test to validate both cases.
- Files changed: `META-INF/MANIFEST.MF`, `src/com/abap/assistant/core/OpenAiSettings.java`, `src/com/abap/assistant/eclipse/EclipseDotEnvLocator.java`, `src/com/abap/assistant/ui/ChatView.java`, `scripts/test-eclipse.ps1`, documentation and IP Box records.
- User/business reason: the project owner reported that `.env` existed but the Eclipse view still showed `OPENAI_API_KEY is required`, and later provided evidence that only the runtime workspace and Eclipse installation directories were checked.
- Validation status: local validation, clean Eclipse smoke test, persisted workspace Eclipse smoke test, bundle-location Eclipse smoke test and live OpenAI smoke test passed on 2026-05-13.
- Open limitations: live usage still depends on a valid non-committed OpenAI API key and network access.
- Development tooling note: development tooling was used as implementation support under owner direction.
- Human decision/review notes: owner confirmed `.env` had been created, provided the Eclipse UI error screenshot, and required practical runtime configuration.

## 2026-05-14 - Eclipse Java Execution Environment Alignment

- Feature/module worked on: Eclipse project compiler and classpath configuration.
- Technical objective: remove mixed Java settings where Eclipse reported compliance level `11` with target level `21`.
- Implementation summary: changed `.classpath` to use the explicit `JavaSE-11` execution environment container, added `javacSource = 11` and `javacTarget = 11` to `build.properties`, and added an Eclipse project import/build smoke test.
- Files changed: `.classpath`, `build.properties`, `scripts/test.ps1`, `scripts/test-eclipse-project-build.ps1`, `CHANGELOG.md`, `README.md`, `docs/INSTALL_ECLIPSE_AND_TEST.md`, `docs/ECLIPSE_TEST_PLAN.md`, IP Box records.
- User/business reason: the project owner reported the Eclipse compiler error on 2026-05-14 after reimporting the project.
- Validation status: local validation, clean Eclipse smoke test, and Eclipse project import/build smoke test passed on 2026-05-14.
- Open limitations: if Eclipse keeps stale project metadata, the owner should refresh, clean, or reimport the project after pulling.
- Development tooling note: development tooling was used as implementation support under owner direction.
- Human decision/review notes: owner provided the exact Eclipse compiler diagnostic and required the project to reimport cleanly.

## 2026-05-14 - Free-Form Editor Context Chat

- Feature/module worked on: ABAP Chat Eclipse UI, prompt construction and editor context loading.
- Technical objective: make the assistant work more like a free-form development chat that automatically reads every open Eclipse text editor tab as context.
- Implementation summary: added `Free chat` mode, simplified the view to a question/answer flow without manual context-loading controls, made `Ask` collect all open text editor tabs automatically, added ABAP related-reference extraction, text-editor bundle dependency declaration and prompt rules for code suggestions that must be confirmed or copied by the user.
- Files changed: `ChatView.java`, `AssistantMode.java`, `AssistantPromptBuilder.java`, `AbapReferenceExtractor.java`, tests, manifest, scripts and documentation.
- User/business reason: the project owner requested a less rigid mode-based assistant that can read opened Z programs and answer free-text questions, including suggested ABAP code.
- Validation status: local validation, clean Eclipse runtime smoke test, Eclipse project import/build smoke test, runtime-workspace `.env` smoke test and live OpenAI smoke test passed on 2026-05-14.
- Open limitations: the plug-in reads open Eclipse editors, including tabs in the background. It does not yet fetch unopened nested ABAP includes/programs directly from the SAP repository; it detects references and can use them when opened as editor tabs.
- Development tooling note: development tooling was used as implementation support under owner direction.
- Human decision/review notes: owner provided a screenshot showing a loaded Z program, tested the workflow, and requested the more automatic chat behaviour.

## 2026-05-14 - Question Clearing And Documentation Refresh

- Feature/module worked on: ABAP Chat Eclipse UI and project documentation.
- Technical objective: clear the question input after each accepted request and align all user-facing and IP Box documentation with the current automatic open-editor context workflow.
- Implementation summary: updated `ChatView` so the `Question` box is cleared immediately after a valid ask request is queued, bumped the bundle version, rewrote the README, installation guide and Eclipse test plan around the current free-form workflow, and refreshed IP Box records.
- Files changed: `ChatView.java`, `META-INF/MANIFEST.MF`, `scripts/test-eclipse.ps1`, `README.md`, `docs/INSTALL_ECLIPSE_AND_TEST.md`, `docs/ECLIPSE_TEST_PLAN.md`, `CHANGELOG.md`, IP Box records.
- User/business reason: the project owner reported that the question text remained after asking and requested a full documentation refresh plus next-improvement suggestions.
- Validation status: local validation, clean Eclipse smoke test, Eclipse project import/build smoke test, runtime `.env` smoke test and live OpenAI smoke test passed on 2026-05-14.
- Open limitations: question clearing is exercised through the Eclipse UI; automated smoke tests validate view creation and build compatibility but do not yet click the UI button.
- Development tooling note: development tooling was used as implementation support under owner direction.
- Human decision/review notes: owner accepted the automatic open-editor context direction as reasonably working and requested UX polish.

## 2026-05-14 - Automatic Context Snapshot, History And Documentation Separation

- Feature/module worked on: ABAP Chat Eclipse UI context collection, ABAP reference extraction, conversation continuity and documentation records.
- Technical objective: implement the first three planned improvements: automatic related-context discovery from the Eclipse workspace, bounded conversational history, and a visible compact summary of the context sent to the assistant.
- Implementation summary: updated `ChatView` to build a context snapshot from all open text editors, resolve detected references against matching local workspace text files, include unresolved references as TODO/TBC, add bounded in-memory Q/A history, and display editor/source/reference/history counts. Extended `AbapReferenceExtractor` to expose raw reference names and detect class usages. Updated documentation and removed references to unrelated tools/projects.
- Files changed: `src/com/abap/assistant/ui/ChatView.java`, `src/com/abap/assistant/core/AbapReferenceExtractor.java`, `src/com/abap/assistant/core/AbapContextClassifier.java`, `test/com/abap/assistant/core/AssistantCoreTest.java`, `META-INF/MANIFEST.MF`, `README.md`, `CHANGELOG.md`, `docs/**`, `instrucciones.md`.
- User/business reason: the project owner requested a more automatic chat experience that reads the Eclipse working context, supports free follow-up questions and documents the tool independently for IP Box purposes.
- Validation status: core validation, clean Eclipse runtime smoke test, Eclipse import/build smoke test, runtime `.env` smoke test, persisted-workspace smoke test and live OpenAI smoke test passed on 2026-05-14.
- Open limitations: related-source lookup is read-only and limited to text resources already available in the Eclipse workspace. It does not fetch remote SAP repository objects that are not open or materialised locally.
- Development tooling note: development tooling was used as implementation support under owner direction.
- Human decision/review notes: owner selected these improvements from the suggested next-step list, required independent IP Box documentation, and required validation before publication.
