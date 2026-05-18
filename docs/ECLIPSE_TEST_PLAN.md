# Eclipse Test Plan

This plan defines the validation required before confirming that ABAP Chat Assistant works in Eclipse.

## Runtime Scope

- Runtime bundle id: `com.abap.assistant`.
- Runtime view id/class: `com.abap.assistant.ui.ChatView`.
- Current UI model: unified conversational view with compact header, one read-only scrollable transcript, bottom question composer, latest-response copy actions and status line.
- Current visual model: high-contrast dark transcript with user messages right-aligned and assistant/system/error messages left-aligned.
- Current context model: every open Eclipse text editor tab is read automatically when `Ask` is pressed.
- Related context model: detected ABAP references are matched against text resources already present in the local Eclipse workspace and included when found.
- Local analysis model: dependency and risk analyzers inspect ABAP text before prompt construction.
- Conversation model: recent Q/A turns in the same view session are sent as bounded history.
- Current write model: suggested ABAP code is text only; the plug-in does not write to SAP.
- Review model: fenced ABAP suggestions are rendered inline in the transcript as differentiated `ABAP code` blocks; the latest code block can be copied with a manual-review header.

## Automated Tests

### 1. Core Validation

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test.ps1
```

Expected result:

- Java core and CLI classes compile with Java 11.
- Core tests pass.
- Free-chat prompt rules are validated.
- Related ABAP references are detected, including includes, programs, function modules, transactions and classes.
- ABAP risk signals are detected, including `SELECT` inside `LOOP`, commits/rollbacks, BDC, update task usage, database writes, custom table access and hardcoded sensitive values.
- Raw reference names are available for workspace lookup.
- `plugin.xml` exposes `com.abap.assistant.ui.ChatView`.
- `MANIFEST.MF` exposes `com.abap.assistant`.
- No OpenAI-style API key is committed.
- `build.properties` keeps Java 11 PDE settings.

### 2. Eclipse Runtime Smoke Test

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120
```

Expected result:

- The full plug-in compiles against the real Eclipse installation.
- A temporary plug-in jar is created.
- Eclipse starts with a temporary workspace/configuration.
- The smoke plug-in opens `ABAP Chat`.
- The view class and site id are `com.abap.assistant.ui.ChatView`.
- The view opens with the unified conversational UI.
- The smoke test finds the single transcript, bottom composer, `Ask`, `Clear chat`, `Copy response`, `Copy ABAP code`, status label and welcome/safety text.
- The smoke test verifies the transcript uses readable light foreground text on a dark background.
- The smoke test injects a local user message and verifies it is right-aligned in the transcript.
- No ABAP Assistant view creation, icon or bundle resolution errors appear in the workspace log.

### 3. Eclipse Import/Build Smoke Test

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120
```

Expected result:

- A clean Eclipse workspace imports a copy of the project.
- Eclipse runs a full build using `.project`, `.classpath`, `.settings`, `MANIFEST.MF` and `build.properties`.
- No Java, PDE or compiler error markers are produced.

### 4. Runtime `.env` Smoke Test

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -UseBundleEnv -TimeoutSeconds 120
```

Expected result:

- Eclipse starts from the Eclipse installation directory.
- No imported `com.abap.assistant` project is required in the runtime workspace.
- `OpenAiSettings` resolves `OPENAI_API_KEY` from the configured runtime `.env`.

### 5. Persisted Workspace Runtime Test

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -WorkspaceTemplate "C:\Users\Admin\runtime-EclipseApplication" -KeepPersistedState -TimeoutSeconds 120
```

Expected result:

- A copy of the previous runtime workspace is used.
- Persisted workbench state is kept.
- The view contribution still resolves.
- The smoke plug-in opens `ABAP Chat` successfully.

### 6. Live OpenAI Smoke Test

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-openai.ps1 -Prompt "Respond with exactly: OK"
```

Expected result:

- The script reads the local non-committed `.env`.
- The OpenAI client sends a request without exposing the API key.
- The model returns a response.

## Manual Eclipse Tests

### Single Open Editor

1. Open one non-confidential ABAP program in ADT.
2. Open `ABAP Chat`.
3. Ask:

```text
Explain this program and list likely defects.
```

Expected result:

- The composer clears after pressing `Ask`.
- The user question appears in the transcript with a compact context line and is visually right-aligned.
- User, assistant and code text are readable against the dark transcript background.
- The response refers to the opened program.
- The response does not claim to modify SAP.

### Multiple Open Editors

1. Open a main ABAP program.
2. Open one or more includes, classes or related objects in additional tabs.
3. Ask:

```text
Analyze the flow using all open editors. If a related object is missing, mark it TODO/TBC.
```

Expected result:

- The composer clears after pressing `Ask`.
- The transcript contains user and assistant messages.
- The response uses the main program and open related objects.
- Missing references are treated as TODO/TBC.

### Related Workspace Sources

1. Open a program that references a local include, program or class name.
2. Ensure the referenced source file exists as a text resource in the same Eclipse workspace/project.
3. Ask:

```text
Use all loaded and related workspace context to explain the flow.
```

Expected result:

- The response distinguishes loaded context from unresolved references.
- No remote SAP object is fetched without being opened or available locally.

### Conversation History

1. Ask a first question and wait for the response.
2. Ask a follow-up:

```text
Now summarize only the defects from your previous answer.
```

Expected result:

- The response can use the previous answer.
- History remains bounded; very long sessions are truncated locally.
- `Clear chat` clears the visible transcript and resets the in-memory conversation history for the current view.

### Suggested Code

Ask:

```text
Suggest a safe ABAP change for this code. Return only code I can review manually.
```

Expected result:

- The response may include fenced ABAP code.
- The transcript contains an inline `ABAP code` block when fenced ABAP code is present.
- The inline code block remains readable and visually separated inside the same transcript.
- `Copy response` copies the complete answer text.
- `Copy ABAP code` copies text with a manual-review header.
- The response and inline code block do not say the change was applied.
- The user remains responsible for copying, reviewing and activating code in SAP.

### Privacy

Use anonymised placeholders only. If a test snippet contains ticket-like values or emails, confirm the response uses anonymised placeholders such as `TCKXXXXX` or `user@example.invalid`.

### Dependency/Risk Prompt Context

1. Open a non-confidential ABAP sample containing an include, a function module call, a custom table select and one safe test risk signal such as `COMMIT WORK`.
2. Ask:

```text
Summarize the local dependency and risk findings before giving advice.
```

Expected result:

- The response can summarize the detected reference and risk findings because the local analysis is included in the prompt.
- Custom/Z objects are discussed when present.
- Unresolved references are treated as TODO/TBC context unless already open or available as local workspace text resources.
- The plug-in does not create a visual context panel and does not write full ABAP source to logs during this check.

## Known Limits

- Unopened remote SAP repository objects are not fetched automatically.
- Local related-source lookup only uses text resources available in the Eclipse workspace.
- SAP GUI windows outside Eclipse are not read.
- Large editor sets, related sources and history are truncated locally to keep requests bounded.
- Real SAP code changes are never applied by the plug-in.
