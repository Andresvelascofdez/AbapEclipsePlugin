# Architecture Decisions

This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.

## ADR-001 - Eclipse PDE Plug-in With Separated Core Services

- Context: the project needs to run inside Eclipse/ADT while remaining testable outside Eclipse.
- Options considered: a pure Eclipse UI implementation, a separate command line tool, or an Eclipse plug-in with a Java core.
- Selected option: Eclipse PDE plug-in with core services under `com.abap.assistant.core` and Eclipse UI code under `com.abap.assistant.ui`.
- Reason for selection: the UI can integrate with Eclipse while core prompt, privacy, and OpenAI logic can be compiled and tested locally without an Eclipse target platform.
- Expected benefit: faster automated validation and lower risk when changing assistant logic.
- Risks/limitations: full UI validation still requires Eclipse PDE/ADT.
- Relation to SAP/ABAP/Eclipse/ADT use case: the view can load ABAP editor selections and provide assistant output inside the developer workflow.
- Project owner decision: direction came from the owner request to build the Eclipse project and provide install/test instructions.

## ADR-002 - Environment-Based API Key Handling

- Context: the project needs OpenAI access, but real API keys must not be committed or hardcoded.
- Options considered: hardcoded key, committed configuration file, environment variables, or ignored local `.env`.
- Selected option: read `OPENAI_API_KEY` from system properties, environment variables, or a local ignored `.env`.
- Reason for selection: protects secrets while supporting local development and Eclipse runtime usage.
- Expected benefit: safer repository history and simpler setup.
- Risks/limitations: each developer must configure their own local key.
- Relation to SAP/ABAP/Eclipse/ADT use case: assistant requests can be tested locally without exposing credentials.
- Project owner decision: the owner provided a key for the project; Codex did not persist it because project instructions prohibit committing real API keys.

## ADR-003 - OpenAI Responses API Client

- Context: the assistant requires a text-generation API integration.
- Options considered: OpenAI Chat Completions, OpenAI Responses API, or a mock-only integration.
- Selected option: OpenAI Responses API over Java `HttpClient`.
- Reason for selection: official OpenAI documentation describes `/v1/responses` as the current advanced interface for model responses and supports direct text inputs.
- Expected benefit: minimal dependencies and direct control over request privacy settings such as `store: false`.
- Risks/limitations: API behavior and model availability can change; `OPENAI_MODEL` remains configurable.
- Relation to SAP/ABAP/Eclipse/ADT use case: enables ABAP/ADT assistant responses from selected code or pasted context.
- Project owner decision: owner requested OpenAI API usage for this project.

## ADR-004 - Conservative Privacy Classification And Redaction

- Context: SAP ABAP snippets may contain a mix of SAP standard/public references and client-specific custom objects or ticket references.
- Options considered: send raw input, require manual anonymisation only, or implement automatic redaction and classification.
- Selected option: automatic redaction plus conservative classification before prompt submission.
- Reason for selection: reduces accidental leakage and reinforces the project rule to keep SAP standard/public knowledge separate from client-specific Z/private knowledge.
- Expected benefit: safer assistant usage in SAP consulting contexts.
- Risks/limitations: regex-based redaction is not a complete data-loss-prevention system and must not replace human review.
- Relation to SAP/ABAP/Eclipse/ADT use case: ABAP custom objects often use Z/Y naming and can indicate client-specific scope.
- Project owner decision: aligned with the owner's project instructions.

## ADR-005 - Java 11 Baseline For Eclipse Compatibility

- Context: the owner reported Eclipse import errors while validating the plug-in project.
- Options considered: keep Java 17, move to Java 11, or add build-tool-specific dependency handling only.
- Selected option: move the plug-in code and validation scripts to Java 11 compatibility.
- Reason for selection: Java 11 is broadly compatible with Eclipse/ADT installations and still supports the required `HttpClient` API.
- Expected benefit: fewer import/build issues in Eclipse workspaces that are not configured for Java 17.
- Risks/limitations: future features must avoid newer Java language constructs unless the baseline is deliberately raised.
- Relation to SAP/ABAP/Eclipse/ADT use case: ADT users may run Eclipse packages with conservative Java baselines.
- Project owner decision: change made in response to owner-reported Eclipse import errors.

## ADR-006 - Runtime Bundle Identity Aligned With Persisted Eclipse View

- Context: the Eclipse runtime workspace reported failures for `com.abap.assistant.ui.ChatView` and `platform:/plugin/com.abap.assistant/icons/abap_icon.png`.
- Options considered: ask the owner to clear persisted workspace state, keep the new `com.anvel.abapeclipseassistant` identity, or align the plugin with the persisted runtime identity.
- Selected option: align the bundle id, Java packages, view id and icon resource to `com.abap.assistant`.
- Reason for selection: this directly resolves the runtime identity that Eclipse is already trying to load.
- Expected benefit: existing runtime workspaces and fresh workspaces can resolve the same view contribution.
- Risks/limitations: repository documentation must remain clear that the GitHub project is `AbapEclipsePlugin` even though the runtime bundle id is `com.abap.assistant`.
- Relation to SAP/ABAP/Eclipse/ADT use case: Eclipse view ids are persisted by the workbench and must remain stable for reliable ADT usage.
- Project owner decision: change made in response to owner-provided runtime screenshots and log file.

## ADR-007 - Eclipse Workspace `.env` Discovery

- Context: the ABAP Chat view ran inside Eclipse, but `OPENAI_API_KEY` was not found even though the project root contained a local `.env`.
- Options considered: require users to launch Eclipse from the project folder, require an OS-level environment variable only, require `ABAP_ECLIPSE_ASSISTANT_ENV_FILE`, discover `.env` from the imported Eclipse project, discover `.env` from a configured directory, or discover `.env` near the loaded bundle/code location.
- Selected option: discover `.env` from the imported `com.abap.assistant` workspace project first, then other workspace projects, then optional `ABAP_ECLIPSE_ASSISTANT_ENV_DIR`, the loaded bundle/code location, workspace root and process working directory, while still supporting explicit `ABAP_ECLIPSE_ASSISTANT_ENV_FILE`.
- Reason for selection: Eclipse normally runs with a working directory unrelated to the imported project, and PDE `Run As > Eclipse Application` uses a separate runtime workspace that may not contain the development project.
- Expected benefit: simpler local setup while keeping API keys out of git.
- Risks/limitations: if several locations contain `.env`, the primary `com.abap.assistant` project is intentionally preferred. Explicit `ABAP_ECLIPSE_ASSISTANT_ENV_FILE` remains the deterministic override.
- Relation to SAP/ABAP/Eclipse/ADT use case: ADT users typically import projects into an Eclipse workspace and run/debug from there.
- Project owner decision: change made in response to owner report that `.env` existed but the view still showed `OPENAI_API_KEY is required`.

## ADR-008 - Free-Form Chat With Editor Context Instead Of Direct SAP Writes

- Context: the owner requested a more automatic, natural chat workflow that can read opened ABAP code and propose code changes.
- Options considered: keep mode-only analysis, add free-form chat over copied text, require manual context-loading buttons, automatically read all open Eclipse editors, or attempt direct SAP repository edits.
- Selected option: support free-form questions that automatically include all open Eclipse text editor tabs, clear the question input after each accepted request, and provide code suggestions only, leaving all SAP changes for explicit user confirmation/copying.
- Reason for selection: this improves usability while avoiding unapproved writes to SAP systems and keeping sensitive/custom code boundaries visible.
- Expected benefit: developers can ask natural questions against the currently opened ABAP working set and receive suggested snippets with less manual copy/paste.
- Risks/limitations: unopened nested includes/programs are not automatically fetched from SAP. The plug-in detects references and can use them when the user opens the related objects as editor tabs.
- Relation to SAP/ABAP/Eclipse/ADT use case: ADT developers commonly work across a main report and includes or related objects in open editor tabs.
- Project owner decision: change made in response to owner feedback after testing with an opened Z report.

## ADR-009 - Read-Only Context Snapshot With Local Workspace Related Sources

- Context: the owner requested a more automatic assistant that behaves like a development chat over the available Eclipse working set, including related code where possible.
- Options considered: keep open-editor-only context, add manual context buttons again, directly fetch remote SAP objects, or build a read-only context snapshot from open editors plus local workspace sources matching detected ABAP references.
- Selected option: build a context snapshot on each accepted ask request. The snapshot reads open text editors, detects ABAP references, attempts read-only lookup of matching text resources already present in the Eclipse workspace, records unresolved references as TODO/TBC, includes bounded conversation history, and shows a compact summary in the view.
- Reason for selection: this increases automation without introducing unapproved SAP writes or hidden remote repository access.
- Expected benefit: fewer manual copy/paste steps, better follow-up questions, clearer visibility of what was sent to the assistant, and safer handling of missing dependencies.
- Risks/limitations: workspace filename/path matching cannot guarantee complete SAP dependency resolution. Remote objects not open or materialised locally remain unresolved until the user opens or imports them.
- Relation to SAP/ABAP/Eclipse/ADT use case: ABAP developers often inspect a main object together with includes, function modules, classes or related programs in the same Eclipse workspace.
- Project owner decision: selected after the owner requested implementation of the first three proposed improvements.
