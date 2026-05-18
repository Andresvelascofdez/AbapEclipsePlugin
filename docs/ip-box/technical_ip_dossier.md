# Technical IP Dossier

This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.

## 1. Product Overview

ABAP Eclipse Assistant is a proprietary Eclipse PDE plug-in for SAP ABAP Development Tools workflows. It provides an ABAP-focused assistant view inside Eclipse that reads the developer's open editor working set, builds a controlled context snapshot, performs local ABAP-oriented analysis, and returns reviewable guidance or suggested code for manual review.

The intended user is an ABAP developer or SAP technical consultant working in Eclipse ADT. The product addresses repeated manual ABAP analysis, context switching between related objects, difficulty reviewing includes/classes/function calls together, and the risk that generated code suggestions could be used without sufficient review controls.

## 2. Qualifying Asset Under Review

The candidate software asset for advisor review is the original ABAP Eclipse Assistant code and documentation maintained in this repository, including:

- Eclipse plug-in source code and PDE metadata.
- ABAP Chat view, conversational UI and review workflows.
- ABAP context snapshot and local workspace source discovery.
- ABAP dependency/reference detection.
- ABAP risk signal detection.
- Sensitive-data redaction.
- SAP standard/custom/Z object classification.
- Controlled prompt/context construction.
- Suggested change review workflow and richer diff workflow when implemented.
- Validation scripts, test cases and product documentation.

## 3. Items Outside Owned IP Boundary

The following are outside the owned software boundary and should not be treated as proprietary product code:

- SAP software, SAP systems and SAP standard objects.
- SAP ABAP Development Tools.
- Eclipse platform and third-party Eclipse components.
- OpenAI models, APIs and hosted infrastructure.
- Client confidential ABAP code, data, tickets, credentials or production examples.
- Third-party libraries and runtime dependencies.
- Legal, tax, valuation or IP Box eligibility conclusions.

## 4. Current Implementation Status

### Implemented

- Eclipse PDE plug-in and ABAP Chat view.
- Unified conversational UI with compact header, single scrollable transcript and bottom composer.
- High-contrast transcript rendering with right-aligned user turns and left-aligned assistant/system turns.
- Free-form question workflow.
- Automatic open editor context reading.
- Related local workspace source lookup by detected reference names.
- Internal dependency/risk summary for prompt construction.
- Bounded conversation history.
- `.env` discovery for Eclipse runtime usage.
- Sensitive-data redaction.
- SAP standard/custom/Z classification.
- OpenAI Responses API client.
- Local ABAP dependency analyzer.
- ABAP risk signal analyzer.
- Copy-only inline ABAP code review blocks.
- Per-response copy controls and clear-chat workflow.
- Safe-change manual review header generation.
- Core tests and Eclipse smoke/build validation scripts.
- IP Box product evidence documentation folder.

### Partial

- Related source lookup is limited to text resources already available in the local Eclipse workspace.
- Risk signals are regex/static-analysis based and intentionally conservative.
- Inline ABAP code review is an MVP presentation, not yet a full side-by-side diff viewer.
- No dedicated visual dependency graph or secondary context panel is currently shown in the Eclipse view.

### Planned/TBC

- Read-only ADT object lookup for unopened remote objects.
- Visual dependency graph or richer dependency panel.
- Richer side-by-side suggested change diff view.
- Configurable safe-change insertion rules beyond the current manual-review header.
- Local usage/evidence logging without confidential code.
- Monthly usage/evidence report.
- Stronger UI automation coverage.
- Internal/private packaging workflow.

## 5. Proprietary Software Elements

The product is not merely a model wrapper. Its proprietary software layer includes Eclipse/ADT workflow integration and ABAP-specific safety/context logic before and after model calls:

- Reads the Eclipse editor working set instead of relying only on pasted text.
- Builds controlled context snapshots from open editors and local workspace sources.
- Detects ABAP references such as includes, forms, submitted programs, function modules, transactions, classes, methods and tables.
- Detects risk signals such as `SELECT` inside `LOOP`, `COMMIT WORK`, BDC usage, update task usage, database writes, custom table access and hardcoded sensitive values.
- Redacts sensitive values before prompt construction.
- Classifies SAP standard/public versus custom/Z/private scope.
- Keeps suggested changes review-only.
- Performs no automatic SAP writes, activation or repository modification.
- Adds a manual-review header to copied suggestions.
- Presents questions, answers, context metadata and ABAP code blocks in one readable developer-focused conversation.
- Supports repeatable ABAP analysis workflows inside Eclipse ADT.

## 6. Architecture

```text
User
  -> Eclipse View
  -> Context Snapshot
  -> ABAP Reference Extractor / Dependency Analyzer
  -> Risk Analyzer
  -> Redaction / Classification
  -> Prompt Builder
  -> Model Client
  -> Conversational Response / Suggested Change Review UI
```

The Eclipse UI gathers the working context and renders the conversation. The core package performs redaction, classification, dependency detection, risk detection and prompt construction. The model client is treated as an external provider dependency. The response is returned to the Eclipse view for developer review.

## 7. Safety Model

- No direct SAP object modification.
- No automatic SAP activation.
- No automatic repository write-back.
- Suggested code remains text for manual review.
- Fenced ABAP suggestions are shown inline in the transcript and copied with a manual-review header.
- The developer must decide whether to copy, adapt, test and activate any suggested change.
- Future diff/review functionality must preserve the no-write rule unless a separate safe write integration is explicitly designed, implemented and tested.

## 8. Evidence Model

Evidence that may support future review includes:

- Git commit history.
- Release tags when created.
- Local automated test logs.
- Eclipse runtime/build smoke test logs.
- Screenshots using anonymised or non-confidential examples.
- Demo scenarios.
- Build/export artifacts.
- Manual validation notes.
- Future local usage logs that avoid full confidential code by default.
- Future monthly usage/evidence reports.

## 9. Roadmap Relevant To IP Evidence

- ABAP dependency graph with loaded/unresolved status.
- Read-only ADT object lookup for unopened dependencies.
- Richer side-by-side suggested change diff window.
- Configurable safe-change insertion rules.
- Local privacy-preserving usage/evidence logging.
- Monthly usage report.
- Expanded core and Eclipse UI test suite.
- Internal/private product packaging and release process.

## 10. Advisor Questions

Questions for qualified advisors may include:

- Whether the plug-in qualifies as copyrighted software for the relevant IP Box regime.
- How to treat dependencies on SAP, Eclipse and OpenAI services.
- How much value can be attributed to the proprietary ABAP/Eclipse workflow layer versus external model/provider services.
- What usage evidence, release evidence or customer/internal deployment evidence is required.
- How to document qualifying profit, nexus fraction and attribution without mixing client confidential data into the technical record.
