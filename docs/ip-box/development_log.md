# Development Log

This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.

## 2026-05-11 - Initial Eclipse Plug-in Scaffold

- Feature/module worked on: ABAP Chat Assistant initial plug-in, core assistant services, OpenAI integration layer, validation scripts and documentation.
- Technical objective: create a dedicated Eclipse PDE project for `Andresvelascofdez/AbapEclipsePlugin`.
- Implementation summary: created Eclipse plug-in metadata, ABAP Chat view, core prompt builder, privacy classifier, redactor, dotenv loader, OpenAI Responses API client, CLI smoke test, automated tests, README, changelog and installation guide.
- Files changed: project metadata files, `src/com/abap/assistant/**`, `test/com/abap/assistant/**`, `scripts/**`, `README.md`, `CHANGELOG.md`, `docs/**`.
- User/business reason: provide an Eclipse/ADT-based ABAP development assistant that reduces context switching, supports code review workflows and keeps suggested changes under developer control.
- Validation status: automated validation passed locally with `scripts/test.ps1` on 2026-05-11.
- GitHub commit reference: `09793a4645a734b7d8e60751a199a4facf5400a6` pushed to `Andresvelascofdez/AbapEclipsePlugin`.
- Open limitations: live OpenAI smoke testing requires a local non-committed `.env` with a valid API key. Eclipse runtime validation must be performed in an Eclipse PDE/ADT installation.
- Product decision/review notes: repository destination, product boundary and initial validation requirements were set for a dedicated ABAP Eclipse plug-in project.

## 2026-05-12 - Eclipse Import Error Handling

- Feature/module worked on: Eclipse import prerequisites, Java compatibility and troubleshooting documentation.
- Technical objective: address Eclipse workspace errors where SWT/JFace/PDE classes are not resolved during import.
- Implementation summary: changed compiler and bundle execution environment to Java 11, removed Java 17-only language constructs from core/test code, added `scripts/check-eclipse-prereqs.ps1`, and documented the PDE/Target Platform fix.
- Files changed: `.settings/org.eclipse.jdt.core.prefs`, `META-INF/MANIFEST.MF`, core Java files, tests, scripts, README, changelog, installation guide and IP Box logs.
- User/business reason: the project must be importable and buildable in a real Eclipse/PDE environment used for ADT development.
- Validation status: automated validation passed locally with `scripts/test.ps1` on 2026-05-12.
- Open limitations: local environment still does not include a detectable Eclipse installation, so runtime UI validation remains TODO/TBC.
- Product decision/review notes: Eclipse import/build compatibility was treated as a release requirement, not an optional setup issue.

## 2026-05-12 - Eclipse Runtime Validation And Bundle Identity Fix

- Feature/module worked on: Eclipse runtime identity, ABAP Chat view contribution, icon packaging and Eclipse smoke tests.
- Technical objective: make the plugin match the persisted Eclipse runtime identity `com.abap.assistant` / `com.abap.assistant.ui.ChatView` and prove it opens in Eclipse.
- Implementation summary: renamed Java packages and bundle metadata to `com.abap.assistant`, set the view id/class to `com.abap.assistant.ui.ChatView`, added `icons/abap_icon.png`, added `scripts/test-eclipse.ps1`, and documented the Eclipse test plan.
- Files changed: `.project`, `META-INF/MANIFEST.MF`, `plugin.xml`, `build.properties`, `src/com/abap/assistant/**`, `test/com/abap/assistant/**`, `icons/abap_icon.png`, `scripts/**`, `docs/**`, `README.md`, `CHANGELOG.md`.
- User/business reason: the plug-in must run inside Eclipse, open the expected view and remain compatible with persisted Eclipse workbench state.
- Validation status: `scripts/test.ps1`, clean Eclipse runtime smoke test, and persisted workspace Eclipse smoke test passed on 2026-05-12.
- Open limitations: live OpenAI API call still requires a local valid `.env`; no real client data should be used in smoke tests.
- Product decision/review notes: Eclipse runtime validation became part of the standard acceptance path for this product.

## 2026-05-13 - Eclipse `.env` Discovery Fix

- Feature/module worked on: OpenAI configuration loading inside Eclipse runtime.
- Technical objective: allow the ABAP Chat view to read `OPENAI_API_KEY` from the development project `.env`, not only from Eclipse's process working directory or runtime workspace.
- Implementation summary: added `EclipseDotEnvLocator`, exported the core and Eclipse helper packages, updated `ChatView` to pass Eclipse `.env` candidates to `OpenAiSettings`, added workspace-project and bundle/code-location discovery, and expanded the Eclipse smoke test to validate both cases.
- Files changed: `META-INF/MANIFEST.MF`, `src/com/abap/assistant/core/OpenAiSettings.java`, `src/com/abap/assistant/eclipse/EclipseDotEnvLocator.java`, `src/com/abap/assistant/ui/ChatView.java`, `scripts/test-eclipse.ps1`, documentation and IP Box records.
- User/business reason: the product needs practical local configuration in realistic Eclipse/PDE launch paths while keeping secrets outside the repository.
- Validation status: local validation, clean Eclipse smoke test, persisted workspace Eclipse smoke test, bundle-location Eclipse smoke test and live OpenAI smoke test passed on 2026-05-13.
- Open limitations: live usage still depends on a valid non-committed OpenAI API key and network access.
- Product decision/review notes: project-local `.env` discovery was selected as the preferred local testing workflow.

## 2026-05-14 - Eclipse Java Execution Environment Alignment

- Feature/module worked on: Eclipse project compiler and classpath configuration.
- Technical objective: remove mixed Java settings where Eclipse reported compliance level `11` with target level `21`.
- Implementation summary: changed `.classpath` to use the explicit `JavaSE-11` execution environment container, added `javacSource = 11` and `javacTarget = 11` to `build.properties`, and added an Eclipse project import/build smoke test.
- Files changed: `.classpath`, `build.properties`, `scripts/test.ps1`, `scripts/test-eclipse-project-build.ps1`, `CHANGELOG.md`, `README.md`, `docs/INSTALL_ECLIPSE_AND_TEST.md`, `docs/ECLIPSE_TEST_PLAN.md`, IP Box records.
- User/business reason: prevent Eclipse workspace compiler inconsistencies and keep the plug-in usable in conservative ADT/Eclipse installations.
- Validation status: local validation, clean Eclipse smoke test, and Eclipse project import/build smoke test passed on 2026-05-14.
- Open limitations: if Eclipse keeps stale project metadata, the owner should refresh, clean, or reimport the project after pulling.
- Product decision/review notes: Java 11 was maintained as the compatibility baseline for the plug-in.

## 2026-05-14 - Free-Form Editor Context Chat

- Feature/module worked on: ABAP Chat Eclipse UI, prompt construction and editor context loading.
- Technical objective: make the assistant work more like a free-form development chat that automatically reads every open Eclipse text editor tab as context.
- Implementation summary: added `Free chat` mode, simplified the view to a question/answer flow without manual context-loading controls, made `Ask` collect all open text editor tabs automatically, added ABAP related-reference extraction, text-editor bundle dependency declaration and prompt rules for code suggestions that must be confirmed or copied by the user.
- Files changed: `ChatView.java`, `AssistantMode.java`, `AssistantPromptBuilder.java`, `AbapReferenceExtractor.java`, tests, manifest, scripts and documentation.
- User/business reason: support a natural ABAP development workflow where the developer asks free-form questions over the currently opened ADT working set.
- Validation status: local validation, clean Eclipse runtime smoke test, Eclipse project import/build smoke test, runtime-workspace `.env` smoke test and live OpenAI smoke test passed on 2026-05-14.
- Open limitations: the plug-in reads open Eclipse editors, including tabs in the background. It does not yet fetch unopened nested ABAP includes/programs directly from the SAP repository; it detects references and can use them when opened as editor tabs.
- Product decision/review notes: suggested code remains text-only for manual review; the plug-in does not apply SAP changes automatically.

## 2026-05-14 - Question Clearing And Documentation Refresh

- Feature/module worked on: ABAP Chat Eclipse UI and project documentation.
- Technical objective: clear the question input after each accepted request and align all user-facing and IP Box documentation with the current automatic open-editor context workflow.
- Implementation summary: updated `ChatView` so the `Question` box is cleared immediately after a valid ask request is queued, bumped the bundle version, rewrote the README, installation guide and Eclipse test plan around the current free-form workflow, and refreshed IP Box records.
- Files changed: `ChatView.java`, `META-INF/MANIFEST.MF`, `scripts/test-eclipse.ps1`, `README.md`, `docs/INSTALL_ECLIPSE_AND_TEST.md`, `docs/ECLIPSE_TEST_PLAN.md`, `CHANGELOG.md`, IP Box records.
- User/business reason: improve usability during iterative ABAP analysis and keep documentation aligned with actual runtime behaviour.
- Validation status: local validation, clean Eclipse smoke test, Eclipse import/build smoke test, runtime `.env` smoke test and live OpenAI smoke test passed on 2026-05-14.
- Open limitations: question clearing is exercised through the Eclipse UI; automated smoke tests validate view creation and build compatibility but do not yet click the UI button.
- Product decision/review notes: documentation was updated to describe the actual product workflow rather than a previous mode-based workflow.

## 2026-05-14 - Automatic Context Snapshot, History And Documentation Separation

- Feature/module worked on: ABAP Chat Eclipse UI context collection, ABAP reference extraction, conversation continuity and documentation records.
- Technical objective: implement the first three planned improvements: automatic related-context discovery from the Eclipse workspace, bounded conversational history, and a visible compact summary of the context sent to the assistant.
- Implementation summary: updated `ChatView` to build a context snapshot from all open text editors, resolve detected references against matching local workspace text files, include unresolved references as TODO/TBC, add bounded in-memory Q/A history, and display editor/source/reference/history counts. Extended `AbapReferenceExtractor` to expose raw reference names and detect class usages. Updated documentation and removed references to unrelated tools/projects.
- Files changed: `src/com/abap/assistant/ui/ChatView.java`, `src/com/abap/assistant/core/AbapReferenceExtractor.java`, `src/com/abap/assistant/core/AbapContextClassifier.java`, `test/com/abap/assistant/core/AssistantCoreTest.java`, `META-INF/MANIFEST.MF`, `README.md`, `CHANGELOG.md`, `docs/**`, `instrucciones.md`.
- User/business reason: reduce manual context preparation, support free follow-up questions and keep the project documentation independent and technically auditable.
- Validation status: core validation, clean Eclipse runtime smoke test, Eclipse import/build smoke test, runtime `.env` smoke test, persisted-workspace smoke test and live OpenAI smoke test passed on 2026-05-14.
- Open limitations: related-source lookup is read-only and limited to text resources already available in the Eclipse workspace. It does not fetch remote SAP repository objects that are not open or materialised locally.
- Product decision/review notes: the product record now focuses on technical evolution, architecture, validation and business purpose, without separate authorship/tooling narratives.

## 2026-05-15 - ABAP Dependency, Risk And Review Layer

- Feature/module worked on: technical IP dossier, ABAP dependency analyzer, ABAP risk analyzer, prompt construction, Eclipse context summary panel and suggested change review panel.
- Technical objective: strengthen the product's proprietary ABAP/Eclipse context layer before model calls, provide review-only suggested change handling and create a clearer technical IP evidence record.
- Implementation summary: added `technical_ip_dossier.md`; added structured ABAP object/risk model classes; implemented local dependency detection for includes, forms, programs, function modules, transactions, classes, methods and table references; implemented local risk signal detection; added dependency/risk summary to prompt construction; replaced the one-line context label with a read-only dependency/context summary panel; added a copy-only suggested change review panel with manual-review header generation.
- Files changed: `src/com/abap/assistant/core/AbapDependencyAnalyzer.java`, `AbapAnalysisResult.java`, `AbapObjectReference.java`, `AbapObjectType.java`, `AbapRiskAnalyzer.java`, `AbapRiskSignal.java`, `SafeChangeRules.java`, `SuggestedChangeReview.java`, `SuggestedChangeReviewBuilder.java`, `AssistantPromptBuilder.java`, `src/com/abap/assistant/ui/ChatView.java`, `test/com/abap/assistant/core/AssistantCoreTest.java`, `README.md`, `CHANGELOG.md`, `docs/ip-box/**`.
- User/business reason: evolve the plug-in from a simple chat wrapper into a more defensible ABAP/Eclipse productivity product with its own context engine, safety controls and validation evidence.
- Validation status: local automated validation passed with `scripts/test.ps1`; Eclipse import/build smoke test passed with `scripts/test-eclipse-project-build.ps1`; clean Eclipse runtime smoke test passed with `scripts/test-eclipse.ps1`.
- Open limitations: dependency/risk analysis is static and conservative; review panel extracts only the first fenced ABAP code block; remote ADT object lookup, richer side-by-side diff view and local usage/evidence logging remain planned/TBC.
- Product decision/review notes: the local ABAP analysis layer remains read-only, performs no SAP writes and supports human review of suggested changes.

## 2026-05-17 - Simplified Chat View Layout

- Feature/module worked on: ABAP Chat Eclipse UI layout and product documentation.
- Technical objective: remove the secondary visible dependency/context panel from the plug-in view while preserving automatic editor-context reading, local ABAP dependency/risk analysis and suggested-change review.
- Implementation summary: removed the read-only context summary widget from `ChatView`, removed UI-only summary rendering methods, expanded the response panel area, retained internal context snapshot construction and retained dependency/risk summary inclusion in prompt context.
- Files changed: `src/com/abap/assistant/ui/ChatView.java`, `META-INF/MANIFEST.MF`, `README.md`, `CHANGELOG.md`, `docs/INSTALL_ECLIPSE_AND_TEST.md`, `docs/ECLIPSE_TEST_PLAN.md`, `docs/ip-box/**`.
- User/business reason: keep the Eclipse assistant compact and easier to use once automatic context loading has been validated in manual testing.
- Validation status: local automated validation passed with `scripts/test.ps1`; Eclipse import/build smoke test passed with `scripts/test-eclipse-project-build.ps1`; clean Eclipse runtime smoke test passed with `scripts/test-eclipse.ps1`.
- Open limitations: richer visual dependency graphs remain planned/TBC and should be optional or separately opened if implemented later.
- Product decision/review notes: internal ABAP analysis remains part of the product architecture; only the permanent visual diagnostic panel was removed from the normal workflow.
