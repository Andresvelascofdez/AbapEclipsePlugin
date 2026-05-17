# Test And Validation Log

This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.

## 2026-05-11 - Automated Core And Metadata Validation

- Tested feature: core assistant services, CLI compilation path, Eclipse plug-in metadata and secret scanning.
- Test scenario: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1` from the project root.
- Expected result: Java core and CLI classes compile; core tests pass; `plugin.xml` exposes the expected view; `MANIFEST.MF` contains the expected bundle symbolic name; no OpenAI-style API key is found in project files except ignored `.env`.
- Actual result: `All core tests passed.` and `Validation completed successfully.`
- Status: Passed.
- Issues found: initial script BOM issue and BAPI classification regex issue were found and fixed before final validation.
- Follow-up required: run Eclipse PDE runtime test and optional live integration smoke test with a local API key.
- Reviewer/validator: local automated validation; manual validation TODO/TBC.

## TODO/TBC - Eclipse Runtime Validation

- Tested feature: ABAP Chat view inside Eclipse/ADT.
- Test scenario: import the project into Eclipse PDE, run as an Eclipse Application, open `ABAP Chat`, load a selected anonymised ABAP snippet and call `Ask`.
- Expected result: the view loads, the selection appears in the input area, the assistant returns output, and no real client data is used.
- Actual result: TODO/TBC.
- Status: TODO/TBC.
- Follow-up required: manual runtime validation should record results.
- Reviewer/validator: TODO/TBC.

## 2026-05-12 - Java 11 Compatibility And Import Diagnostics

- Tested feature: Java 11-compatible core assistant code and updated validation scripts.
- Test scenario: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1` after replacing Java 17-only language constructs and changing validation to `javac --release 11`.
- Expected result: Java core and CLI classes compile with Java 11; core tests pass; metadata validation and secret scan pass.
- Actual result: `All core tests passed.` and `Validation completed successfully.`
- Status: Passed.
- Issues found: Eclipse import issue is caused by missing/unresolved PDE/SWT/JFace dependencies in the Eclipse workspace, based on the reported error pattern.
- Follow-up required: install PDE or activate `Running Platform` target platform, then reimport or clean the project.
- Reviewer/validator: local automated validation; Eclipse workspace validation TODO/TBC.

## 2026-05-12 - Eclipse Runtime Smoke Tests

- Tested feature: ABAP Chat Eclipse runtime view creation, bundle identity, icon resolution and persisted-workspace compatibility.
- Test scenario 1: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1`.
- Test scenario 2: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120`.
- Test scenario 3: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -WorkspaceTemplate "C:\Users\Admin\runtime-EclipseApplication" -KeepPersistedState -TimeoutSeconds 120`.
- Expected result: core tests pass; full UI compiles against the real Eclipse installation; temporary Eclipse runtime starts; the smoke-test plugin opens `com.abap.assistant.ui.ChatView`, verifies the returned view class name, verifies the view site id, confirms `icons/abap_icon.png` resolves, and finds no ABAP Assistant view creation or bundle resolution errors in the fresh runtime log.
- Actual result: all three commands completed successfully. The Eclipse smoke marker contained `PASS`.
- Status: Passed.
- Issues found: the previous implementation used a new bundle/view identity that did not match the persisted runtime workspace. The runtime expected `com.abap.assistant` and `com.abap.assistant.ui.ChatView`.
- Follow-up required: pull the latest commit and reimport/clean the project or launch a fresh Eclipse Application.
- Reviewer/validator: local automated validation using the Eclipse installation at `C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse`.

## 2026-05-13 - OpenAI `.env` Loading Validation

- Tested feature: `.env` discovery from Eclipse workspace project, loaded plug-in bundle/code location, and live OpenAI client configuration.
- Test scenario 1: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120`.
- Test scenario 2: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -WorkspaceTemplate "C:\Users\Admin\runtime-EclipseApplication" -KeepPersistedState -TimeoutSeconds 120`.
- Test scenario 3: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -UseBundleEnv -TimeoutSeconds 120`.
- Test scenario 4: run `powershell -ExecutionPolicy Bypass -File scripts/smoke-openai.ps1 -Prompt "Respond with exactly: OK"` from the project root with a local `.env`.
- Expected result: Eclipse smoke tests read a test `.env` from both a temporary workspace project and the loaded bundle/code location, open `ChatView`, and exit successfully; live smoke test reads the local `.env` and receives a model response.
- Actual result: all scenarios passed. The live smoke test returned a model response and did not expose the API key.
- Status: Passed.
- Issues found: prior implementation did not cover PDE launches where `C:\Users\Admin\runtime-EclipseApplication` does not contain the development project.
- Follow-up required: pull the latest version and relaunch/clean the Eclipse runtime.
- Reviewer/validator: local automated validation using Eclipse and local `.env`.

## 2026-05-14 - Eclipse JavaSE-11 Classpath Validation

- Tested feature: Eclipse project classpath and Java compiler compatibility.
- Test scenario 1: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1`.
- Test scenario 2: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120`.
- Test scenario 3: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120`.
- Expected result: project compiles with Java 11 settings, the Eclipse runtime opens `com.abap.assistant.ui.ChatView`, and a clean Eclipse workspace can import/build the project without Java or PDE error markers.
- Actual result: all three commands passed.
- Status: Passed.
- Issues found: the generic JRE container and PDE build/export settings could inherit a Java 21 target from the workspace while project compliance remained Java 11.
- Follow-up required: refresh `.classpath` in Eclipse and run `Project > Clean`.
- Reviewer/validator: local automated validation using Eclipse.

## 2026-05-14 - Free-Form Editor Context Chat Validation

- Tested feature: free chat prompt mode, automatic open-editor context collection, related ABAP reference extraction, text-editor UI build dependencies and OpenAI request path.
- Test scenario 1: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1`.
- Test scenario 2: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120`.
- Test scenario 3: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120`.
- Test scenario 4: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -UseBundleEnv -TimeoutSeconds 120`.
- Test scenario 5: run `powershell -ExecutionPolicy Bypass -File scripts/smoke-openai.ps1 -Prompt "Respond with exactly: OK"`.
- Expected result: core tests cover free-chat prompt rules and related-reference extraction; Eclipse imports/builds the project with text-editor dependencies; runtime opens the simplified ABAP Chat view; live OpenAI call succeeds.
- Actual result: all scenarios passed.
- Status: Passed.
- Issues found: the Eclipse project build smoke test caught a missing `org.eclipse.ui.workbench.texteditor` bundle dependency before final validation; the dependency was added to `MANIFEST.MF`.
- Follow-up required: install the updated bundle and manually test `Ask` with one or more open anonymised/non-confidential ABAP editors.
- Reviewer/validator: local automated validation using Eclipse and local non-committed `.env`.

## 2026-05-14 - Question Clearing And Documentation Refresh Validation

- Tested feature: question clearing after ask, documentation consistency, Eclipse build/runtime compatibility and OpenAI request path.
- Test scenario 1: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1`.
- Test scenario 2: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120`.
- Test scenario 3: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120`.
- Test scenario 4: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -UseBundleEnv -TimeoutSeconds 120`.
- Test scenario 5: run `powershell -ExecutionPolicy Bypass -File scripts/smoke-openai.ps1 -Prompt "Respond with exactly: OK"`.
- Expected result: core tests pass; Eclipse imports/builds the project; runtime opens `ABAP Chat`; live OpenAI request succeeds; manual UI testing can confirm the `Question` box clears after a valid request.
- Actual result: all automated scenarios passed. One parallel smoke-test attempt collided on a shared temporary jar path, then the same smoke test passed when rerun sequentially.
- Status: Passed.
- Issues found: no product issue found. The parallel test collision is a test harness limitation because two `test-eclipse.ps1` runs share `build\eclipse-smoke`.
- Follow-up required: install the updated bundle and manually confirm the `Question` field clears after pressing `Ask`.
- Reviewer/validator: local automated validation; manual UI validation TODO/TBC.

## 2026-05-14 - Automatic Context Snapshot And History Validation

- Tested feature: open-editor context snapshot, related local workspace source lookup, ABAP class/reference extraction, conversation history and context summary UI.
- Test scenario 1: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1`.
- Test scenario 2: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 180`.
- Test scenario 3: run clean Eclipse runtime smoke test.
- Test scenario 4: run runtime `.env` smoke test.
- Test scenario 5: run persisted-workspace Eclipse runtime smoke test.
- Test scenario 6: run live OpenAI smoke test with the local non-committed `.env`.
- Expected result: core tests pass, reference extraction includes class references and raw names for lookup, Eclipse imports/builds the project without markers, the runtime opens `ABAP Chat`, `.env` discovery remains valid, persisted workspace state does not break the view, and live OpenAI request returns a response.
- Actual result: all scenarios passed. One interim Eclipse import/build run caught an unavailable `IFileEditorInput` dependency; the import was removed and the full suite passed after the fix.
- Status: Passed.
- Issues found: `IFileEditorInput` was not available to the PDE project with current dependencies; fixed by using the editor input adapter path already supported by the bundle dependencies.
- Follow-up required: manual UI validation with real ADT open editors remains TODO/TBC.
- Reviewer/validator: local automated validation; manual UI validation TODO/TBC.

## 2026-05-15 - ABAP Dependency, Risk And Review Validation

- Tested feature: ABAP dependency analyzer, ABAP risk signal analyzer, prompt dependency/risk summary, suggested change review model and Eclipse review panel compilation.
- Test scenario 1: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1`.
- Test scenario 2: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 180`.
- Test scenario 3: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 180`.
- Expected result: core tests detect ABAP references, custom/Z objects, risk signals, prompt summary content and safe suggested-change headers; Eclipse imports/builds the project without Java or PDE markers; clean Eclipse runtime opens `ABAP Chat`.
- Actual result: all three commands passed.
- Status: Passed.
- Issues found: initial risk detection did not catch variable names such as `lv_password`; the pattern was corrected and tests passed.
- Follow-up required: manual Eclipse UI validation of the simplified view and copy-only suggested change panel remains TODO/TBC.
- Reviewer/validator: local automated validation.

## 2026-05-17 - Simplified Chat View Validation

- Tested feature: Eclipse chat view layout after removing the visible dependency/context summary panel, while retaining internal editor-context and ABAP dependency/risk prompt analysis.
- Test scenario 1: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1`.
- Test scenario 2: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 180`.
- Test scenario 3: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 180`.
- Expected result: core tests pass; clean Eclipse workspace imports/builds the project without Java or PDE markers; clean Eclipse runtime opens `ABAP Chat` without view creation or bundle resolution errors.
- Actual result: all three commands passed.
- Status: Passed.
- Issues found: none in automated validation.
- Follow-up required: manual screenshot evidence of the simplified view remains TODO/TBC after installing the updated build.
- Reviewer/validator: local automated validation.

## 2026-05-17 - Unified Conversational UI Validation

- Tested feature: native SWT conversational chat layout, transcript rendering, bottom composer, integrated suggested-change section, per-response copy controls, smoke-test UI structure checks and unchanged Eclipse runtime creation.
- Test scenario 1: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1`.
- Test scenario 2: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 180`.
- Test scenario 3: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 180`.
- Test scenario 4: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -UseBundleEnv -TimeoutSeconds 180`.
- Test scenario 5: run `powershell -ExecutionPolicy Bypass -File scripts/smoke-openai.ps1 -Prompt "Respond with exactly: OK"`.
- Expected result: core tests pass; clean Eclipse workspace imports/builds the project without Java or PDE markers; clean Eclipse runtime opens `ABAP Chat`, verifies the transcript, composer, `Ask`, `Clear chat`, status label and welcome/safety message, and reports no view creation or bundle resolution errors.
- Actual result: all five commands passed. The live smoke test returned `OK` and did not expose the API key.
- Status: Passed.
- Issues found: the first enhanced runtime smoke test attempt resolved an older installed bundle and failed its new UI-structure assertion. The test harness was corrected to require the current bundle version range; the final runtime smoke tests passed.
- Follow-up required: capture manual screenshot evidence after installing the updated build.
- Reviewer/validator: local automated validation.

## 2026-05-17 - Single Transcript Inline Code Validation

- Tested feature: single read-only SWT `StyledText` transcript, inline ABAP code block rendering, latest-response copy controls, smoke-test UI structure checks and unchanged Eclipse runtime creation.
- Test scenario 1: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1`.
- Test scenario 2: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 180`.
- Test scenario 3: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 180`.
- Test scenario 4: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -UseBundleEnv -TimeoutSeconds 180`.
- Test scenario 5: run `powershell -ExecutionPolicy Bypass -File scripts/smoke-openai.ps1 -Prompt "Respond with exactly: OK"`.
- Expected result: core tests pass; clean Eclipse workspace imports/builds the project without Java or PDE markers; clean Eclipse runtime opens `ABAP Chat`, verifies the single transcript, composer, `Ask`, `Clear chat`, `Copy response`, `Copy ABAP code`, status label and welcome/safety text, and reports no view creation or bundle resolution errors.
- Actual result: all five commands passed. The live smoke test returned `OK` and did not expose the API key.
- Status: Passed.
- Issues found: none in final automated validation.
- Follow-up required: capture manual screenshot evidence after installing the updated build.
- Reviewer/validator: local automated validation.
