# ABAP Chat Assistant

ABAP Chat Assistant is an Eclipse PDE plug-in for SAP ABAP Development Tools workflows. It adds a free-form chat view inside Eclipse that reads the developer's open ABAP/text editor working set, sends a privacy-aware prompt to the OpenAI Responses API, and returns explanations, defect analysis, test ideas or suggested ABAP snippets for manual review.

Project repository:

`https://github.com/Andresvelascofdez/AbapEclipsePlugin`

## Current Behaviour

- The Eclipse view is available at `Window > Show View > Other > ABAP Chat Assistant > ABAP Chat`.
- The user writes a natural-language question in `Question` and presses `Ask`.
- On each accepted question, the `Question` box clears immediately.
- The plug-in automatically reads every open Eclipse text editor tab, including background tabs that are not focused.
- The active editor is included first, followed by the other open text editors.
- The plug-in detects ABAP references such as `INCLUDE`, `SUBMIT`, `CALL FUNCTION`, `CALL TRANSACTION`, `PERFORM ... IN PROGRAM` and common class usages.
- The plug-in performs local ABAP dependency analysis before prompt construction, including includes, forms, programs, function modules, transactions, classes, methods and table references.
- The plug-in detects ABAP risk signals such as `SELECT` inside `LOOP`, `COMMIT WORK`, `ROLLBACK WORK`, BDC usage, update task usage, database writes, custom table access, lock handling, authority checks and hardcoded sensitive values.
- When those references match text files already present in the Eclipse workspace, the plug-in adds those related workspace sources to the prompt automatically.
- References that cannot be resolved from the open editors or local workspace are still listed in the prompt as TODO/TBC context.
- A compact dependency/context summary panel is shown in the view, including editor count, related source count, detected references, unresolved references, custom/Z objects, risk signals, character count and history turns.
- Recent questions and answers in the same view session are included as conversation history, bounded locally to keep prompts controlled.
- There is no visible context box and no manual context-loading button.
- Suggested code is returned as text and, when a fenced ABAP block is detected, copied into a `Suggested change review` panel with a manual-review header.
- The `Copy suggestion` button copies the suggested block for manual review only. The plug-in does not write to SAP, activate objects, or apply repository changes.
- OpenAI-style API keys, ticket references, handover references, invoice references, email addresses and SAP client numbers are redacted before sending prompts.

## Limits

- The plug-in reads Eclipse text editors and matching text resources that are already available in the local Eclipse workspace.
- It does not log into SAP or fetch remote repository objects that are not materialised in Eclipse/workspace resources.
- SAP GUI windows outside Eclipse are not read.
- Large context is truncated locally to keep requests bounded.
- Local dependency and risk detection is static and conservative; it supports review but does not replace ABAP runtime testing.
- Real SAP changes must be made manually by the user after reviewing any suggested code.
- Do not use real client secrets, production data, ticket IDs, invoices, credentials or confidential examples in prompts.

## Secrets

Do not commit real API keys. `.env` is ignored by git. Use `.env.example` as a template:

```powershell
Copy-Item .env.example .env
notepad .env
```

Expected local values:

```text
OPENAI_API_KEY=replace-with-your-new-openai-api-key
OPENAI_MODEL=gpt-5-mini
OPENAI_BASE_URL=https://api.openai.com/v1/responses
```

Inside Eclipse, the plug-in checks `.env` in this order:

- explicit `ABAP_ECLIPSE_ASSISTANT_ENV_FILE`
- imported `com.abap.assistant` project
- other workspace projects
- optional `ABAP_ECLIPSE_ASSISTANT_ENV_DIR`
- loaded bundle/code location
- workspace root
- Eclipse process working directory

## Validation

Run local validation:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test.ps1
```

Run the Eclipse runtime smoke test:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse"
```

Run the Eclipse import/build smoke test:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse"
```

Run the live OpenAI smoke test after configuring `.env`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-openai.ps1 -Prompt "Respond with exactly: OK"
```

## Manual Test

1. Install or launch the updated plug-in.
2. Open one ABAP program in Eclipse ADT.
3. Open optional related objects or ensure matching source files already exist in the local workspace.
4. Open `ABAP Chat`.
5. Ask a free-form question, for example:

```text
Explain this program, inspect related context, and list likely defects. If something is missing, mark it TODO/TBC.
```

6. Confirm the `Question` box clears after pressing `Ask`.
7. Confirm the context summary shows the number of editors, related sources, references, unresolved references, custom/Z objects, risk signals and history turns.
8. Confirm a second question can refer back to the previous answer.
9. If the response contains a fenced ABAP suggestion, confirm the review panel shows a manual-review header and `Copy suggestion` copies text only.
10. Confirm the response refers to the opened code and does not claim to apply changes.

## Installation Guide

See [docs/INSTALL_ECLIPSE_AND_TEST.md](docs/INSTALL_ECLIPSE_AND_TEST.md) and [docs/ECLIPSE_TEST_PLAN.md](docs/ECLIPSE_TEST_PLAN.md).

## Suggested Next Improvements

- Add a visual dependency graph showing detected includes, programs, function modules and classes with loaded/unresolved status.
- Add an explicit "clear chat history" command for long Eclipse sessions.
- Add optional ADT-aware remote object lookup after a deliberate user action, keeping it read-only.
- Add automated SWTBot-style UI interaction tests for pressing `Ask`, clearing `Question`, and verifying the context summary.
- Add local prompt-size controls per workspace to tune max editors, related files and history length.
- Enhance the suggested change review panel into a richer side-by-side diff view.
- Add privacy-preserving local usage/evidence logging outside committed source by default.

## OpenAI API

The implementation uses the OpenAI Responses API endpoint `/v1/responses`, with `store` set to `false` in requests. The default model is `gpt-5-mini` and can be changed with `OPENAI_MODEL`.

Reference: https://platform.openai.com/docs/api-reference/responses

## Disclaimer

This repository contains technical development material. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.
