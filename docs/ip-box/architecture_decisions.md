# Architecture Decisions

This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.

## ADR-001 - Eclipse PDE Plug-in With Separated Core Services

- Context: the project needs to run inside Eclipse/ADT while remaining testable outside Eclipse.
- Options considered: a pure Eclipse UI implementation, a separate command line tool, or an Eclipse plug-in with a Java core.
- Selected option: Eclipse PDE plug-in with core services under `com.anvel.abapeclipseassistant.core` and Eclipse UI code under `com.anvel.abapeclipseassistant.ui`.
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
