# ABAP Chat Assistant

ABAP Chat Assistant is an Eclipse PDE plug-in for SAP ABAP Development Tools workflows. It provides a small chat view inside Eclipse that reads every open text editor tab as context when the user presses `Ask`, sends a privacy-aware prompt to the OpenAI Responses API, and returns analysis or suggested ABAP code for the user to review.

The project is separate from `SapIsuAssistant`. The intended GitHub repository is:

`https://github.com/Andresvelascofdez/AbapEclipsePlugin`

## Current Behaviour

- The Eclipse view is available at `Window > Show View > Other > ABAP Chat Assistant > ABAP Chat`.
- The user writes a free-form question in `Question` and presses `Ask`.
- On each accepted question, the `Question` box is cleared immediately.
- The plug-in automatically reads every open Eclipse text editor tab, including background tabs that are not focused.
- The active editor is included first, followed by the other open text editors.
- There is no visible context box and no manual context-loading buttons.
- The assistant can explain ABAP, find possible defects, suggest tests, propose safe refactorings, answer general ADT questions, and suggest ABAP snippets.
- Suggested code is only returned as text. The plug-in does not write to SAP or modify repository objects.
- The prompt detects related ABAP references such as `INCLUDE`, `SUBMIT`, `CALL FUNCTION`, `CALL TRANSACTION`, and `PERFORM ... IN PROGRAM` in the opened context.
- OpenAI-style API keys, ticket references, handover references, invoice references, email addresses and SAP client numbers are redacted before sending prompts.

## Limits

- The plug-in reads open Eclipse text editors only.
- It does not yet fetch unopened ABAP includes, classes, function modules or programs from the SAP repository.
- To include related objects, open them in Eclipse editor tabs before pressing `Ask`.
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
3. Open any includes or related ABAP objects you want included as separate editor tabs.
4. Open `ABAP Chat`.
5. Ask a free-form question, for example:

```text
Explain this code and list likely defects. If related includes are missing, say so.
```

6. Confirm the `Question` box clears after pressing `Ask`.
7. Confirm the status says how many open editors were sent.
8. Confirm the response refers to the opened code and does not claim to apply changes.

## Installation Guide

See [docs/INSTALL_ECLIPSE_AND_TEST.md](docs/INSTALL_ECLIPSE_AND_TEST.md) and [docs/ECLIPSE_TEST_PLAN.md](docs/ECLIPSE_TEST_PLAN.md).

## OpenAI API

The implementation uses the OpenAI Responses API endpoint `/v1/responses`, with `store` set to `false` in requests. The default model is `gpt-5-mini` and can be changed with `OPENAI_MODEL`.

Reference: https://platform.openai.com/docs/api-reference/responses

## Disclaimer

This repository contains technical development material. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.
