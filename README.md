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
- When those references match text files already present in the Eclipse workspace, the plug-in adds those related workspace sources to the prompt automatically.
- References that cannot be resolved from the open editors or local workspace are still listed in the prompt as TODO/TBC context.
- A compact context summary is shown in the view, for example editor count, related source count, detected references, character count and history turns.
- Recent questions and answers in the same view session are included as conversation history, bounded locally to keep prompts controlled.
- There is no visible context box and no manual context-loading button.
- Suggested code is returned only as text. The plug-in does not write to SAP, activate objects, or apply repository changes.
- OpenAI-style API keys, ticket references, handover references, invoice references, email addresses and SAP client numbers are redacted before sending prompts.

## Limits

- The plug-in reads Eclipse text editors and matching text resources that are already available in the local Eclipse workspace.
- It does not log into SAP or fetch remote repository objects that are not materialised in Eclipse/workspace resources.
- SAP GUI windows outside Eclipse are not read.
- Large context is truncated locally to keep requests bounded.
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
7. Confirm the context summary shows the number of editors, related sources, references and history turns.
8. Confirm a second question can refer back to the previous answer.
9. Confirm the response refers to the opened code and does not claim to apply changes.

## Installation Guide

See [docs/INSTALL_ECLIPSE_AND_TEST.md](docs/INSTALL_ECLIPSE_AND_TEST.md) and [docs/ECLIPSE_TEST_PLAN.md](docs/ECLIPSE_TEST_PLAN.md).

## Suggested Next Improvements

- Add a read-only dependency graph panel showing detected includes, programs, function modules and classes with loaded/unresolved status.
- Add an explicit "clear chat history" command for long Eclipse sessions.
- Add optional ADT-aware remote object lookup after a deliberate user action, keeping it read-only.
- Add automated SWTBot-style UI interaction tests for pressing `Ask`, clearing `Question`, and verifying the context summary.
- Add local prompt-size controls per workspace to tune max editors, related files and history length.

## OpenAI API

The implementation uses the OpenAI Responses API endpoint `/v1/responses`, with `store` set to `false` in requests. The default model is `gpt-5-mini` and can be changed with `OPENAI_MODEL`.

Reference: https://platform.openai.com/docs/api-reference/responses

## Disclaimer

This repository contains technical development material. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.
