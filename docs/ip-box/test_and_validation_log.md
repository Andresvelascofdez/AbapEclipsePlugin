# Test And Validation Log

This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.

## 2026-05-11 - Automated Core And Metadata Validation

- Tested feature: core assistant services, CLI compilation path, Eclipse plug-in metadata and secret scanning.
- Test scenario: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1` from the project root.
- Expected result: Java core and CLI classes compile with Java 17; core tests pass; `plugin.xml` exposes the expected view; `MANIFEST.MF` contains the expected bundle symbolic name; no OpenAI-style API key is found in project files except ignored `.env`.
- Actual result: `All core tests passed.` and `Validation completed successfully.`
- Status: Passed.
- Issues found: initial script BOM issue and BAPI classification regex issue were found and fixed before final validation.
- Follow-up required: run Eclipse PDE runtime test and optional live OpenAI smoke test with a new local API key.
- Reviewer/validator: Codex local automated validation; owner manual validation TODO/TBC.

## TODO/TBC - Eclipse Runtime Validation

- Tested feature: ABAP Chat view inside Eclipse/ADT.
- Test scenario: import the project into Eclipse PDE, run as an Eclipse Application, open `ABAP Chat`, load a selected anonymised ABAP snippet and call `Ask`.
- Expected result: the view loads, the selection appears in the input area, the assistant returns output, and no real client data is used.
- Actual result: TODO/TBC.
- Status: TODO/TBC.
- Follow-up required: project owner or Codex with an Eclipse runtime should record results.
- Reviewer/validator: TODO/TBC.

## 2026-05-12 - Java 11 Compatibility And Import Diagnostics

- Tested feature: Java 11-compatible core assistant code and updated validation scripts.
- Test scenario: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1` after replacing Java 17-only language constructs and changing validation to `javac --release 11`.
- Expected result: Java core and CLI classes compile with Java 11; core tests pass; metadata validation and secret scan pass.
- Actual result: `All core tests passed.` and `Validation completed successfully.`
- Status: Passed.
- Issues found: Eclipse import issue is caused by missing/unresolved PDE/SWT/JFace dependencies in the Eclipse workspace, based on the error pattern reported by the owner.
- Follow-up required: owner should install PDE or activate `Running Platform` target platform, then reimport or clean the project.
- Reviewer/validator: Codex local automated validation; Eclipse workspace validation by owner TODO/TBC.

## 2026-05-12 - Eclipse Runtime Smoke Tests

- Tested feature: ABAP Chat Eclipse runtime view creation, bundle identity, icon resolution and persisted-workspace compatibility.
- Test scenario 1: run `powershell -ExecutionPolicy Bypass -File scripts/test.ps1`.
- Test scenario 2: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120`.
- Test scenario 3: run `powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -WorkspaceTemplate "C:\Users\Admin\runtime-EclipseApplication" -KeepPersistedState -TimeoutSeconds 120`.
- Expected result: core tests pass; full UI compiles against the real Eclipse installation; temporary Eclipse runtime starts; the smoke-test plugin opens `com.abap.assistant.ui.ChatView`; `icons/abap_icon.png` resolves; no ABAP Assistant view creation or bundle resolution errors are written to the fresh runtime log.
- Actual result: all three commands completed successfully. The Eclipse smoke marker contained `PASS`.
- Status: Passed.
- Issues found: the previous implementation used a new bundle/view identity that did not match the persisted runtime workspace. The runtime expected `com.abap.assistant` and `com.abap.assistant.ui.ChatView`.
- Follow-up required: owner should pull the latest commit and reimport/clean the project or launch a fresh Eclipse Application.
- Reviewer/validator: Codex using the local Eclipse installation at `C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse`.
