# ABAP Chat Assistant

ABAP Chat Assistant is an Eclipse PDE plug-in prototype for SAP ABAP Development Tools workflows. It provides an Eclipse view that can load selected ABAP text, classify whether the context appears public SAP standard or custom/client-specific, redact sensitive values, and send a carefully constrained prompt to the OpenAI Responses API.

The project is designed, directed, reviewed and validated by the project owner, using AI-assisted development tools to accelerate implementation.

## Current Features

- Eclipse view contribution: `Window > Show View > Other > ABAP Chat Assistant > ABAP Chat`.
- Free-form ABAP chat with optional task modes for explaining ABAP, finding possible defects, suggesting tests, proposing safe refactoring ideas, and general ABAP/ADT help.
- Text selection, full active editor, and open-editor context loading from Eclipse.
- Related ABAP reference detection for includes, submitted programs, function modules and transactions present in the loaded context.
- Sensitive value redaction for OpenAI-style API keys, ticket references, handover references, invoice references, email addresses, and SAP client numbers.
- Context classification to keep SAP standard/public knowledge separate from client-specific Z/Y/private knowledge.
- OpenAI Responses API client using `OPENAI_API_KEY`, `OPENAI_MODEL`, and `OPENAI_BASE_URL`.
- Local core tests and metadata validation through `scripts/test.ps1`.
- Optional live OpenAI smoke test through `scripts/smoke-openai.ps1`.

## Repository And Secrets

The intended GitHub repository is:

`https://github.com/Andresvelascofdez/AbapEclipsePlugin`

Do not commit real API keys. `.env` is intentionally ignored by git. Use `.env.example` as a template:

```powershell
Copy-Item .env.example .env
notepad .env
```

If an API key was shared in a chat, log, screenshot, commit, or ticket, revoke it and create a new key before using the project.

When running inside Eclipse, the plug-in checks `.env` in the imported `com.abap.assistant` project first, then other workspace projects, then an optional `ABAP_ECLIPSE_ASSISTANT_ENV_DIR`, then the loaded plug-in bundle/code location, then the workspace root, then Eclipse's process working directory. This covers PDE "Run As > Eclipse Application" launches where the runtime workspace is different from the development workspace. You can also set `ABAP_ECLIPSE_ASSISTANT_ENV_FILE` to an explicit `.env` path.

## Validation

Run the automated validation from the project root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test.ps1
```

The validation compiles the core assistant and CLI classes with Java 11, runs core tests, validates Eclipse plug-in metadata, verifies the icon, and scans tracked project files for accidental OpenAI-style API keys.

Run the Eclipse runtime smoke test against a real Eclipse/PDE installation:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse"
```

Run the Eclipse import/build smoke test to validate `.project`, `.classpath`, `.settings` and `build.properties` inside Eclipse:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse"
```

To reproduce the previously reported persisted-workspace issue, run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -WorkspaceTemplate "C:\Users\Admin\runtime-EclipseApplication" -KeepPersistedState
```

Run a live OpenAI smoke test from the project root after creating a local `.env`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-openai.ps1 -Prompt "Respond with exactly: OK"
```

## Eclipse Import Prerequisites

This project is an Eclipse plug-in project. The Eclipse installation used for development must include Plug-in Development Environment (PDE). If `ChatView.java` shows errors such as `The import org.eclipse cannot be resolved`, `Button cannot be resolved to a type`, or `SWT cannot be resolved`, the Eclipse target platform is not resolving PDE/SWT/JFace dependencies.

Check a local Eclipse installation with:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-eclipse-prereqs.ps1 -EclipseHome "C:\path\to\eclipse"
```

## Installation And Testing

See [docs/INSTALL_ECLIPSE_AND_TEST.md](docs/INSTALL_ECLIPSE_AND_TEST.md) and [docs/ECLIPSE_TEST_PLAN.md](docs/ECLIPSE_TEST_PLAN.md).

## OpenAI API

The implementation uses the OpenAI Responses API endpoint `/v1/responses`, with `store` set to `false` in requests. The default model is `gpt-5-mini` and can be changed with `OPENAI_MODEL`.

Reference: https://platform.openai.com/docs/api-reference/responses

## Disclaimer

This repository contains technical development material. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.
