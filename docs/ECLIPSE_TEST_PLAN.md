# Eclipse Test Plan

This plan defines the validation required before confirming that ABAP Chat Assistant works in Eclipse.

## Test Environment

- Eclipse executable: `C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse\eclipse.exe`
- Eclipse package: Eclipse Java 2026-03 with PDE available.
- Test workspace: generated under `build/eclipse-smoke/workspace`.
- Test configuration: generated under `build/eclipse-smoke/configuration`.
- Runtime plugin id: `com.abap.assistant`.
- Runtime view id/class: `com.abap.assistant.ui.ChatView`.

## Automated Tests

1. Core Java validation:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test.ps1
```

Expected result:

- Core and CLI Java classes compile with Java 11.
- Core unit-style tests pass.
- `plugin.xml` exposes `com.abap.assistant.ui.ChatView`.
- `MANIFEST.MF` exposes `com.abap.assistant`.
- No Java source contains UTF-8 BOM.
- No OpenAI-style API key is committed.
- The ABAP Chat icon exists and has a PNG header.

2. Eclipse runtime smoke test:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse"
```

Expected result:

- The full plugin, including SWT/JFace/PDE UI code, compiles against the real Eclipse installation.
- A temporary plugin jar is created.
- A temporary smoke-test plugin is created.
- Eclipse starts against a temporary workspace/configuration.
- The smoke-test plugin opens `com.abap.assistant.ui.ChatView`.
- The smoke-test plugin verifies that the returned view class is `com.abap.assistant.ui.ChatView` and that the Eclipse view site id matches `com.abap.assistant.ui.ChatView`.
- The smoke-test plugin creates a temporary workspace project named `com.abap.assistant` with a test `.env` and verifies that `OpenAiSettings` reads `OPENAI_API_KEY` from that project location.
- The view opens after the free-chat UI changes and declares all required text-editor dependencies.
- Eclipse exits automatically.
- The workspace log contains no ABAP Assistant view creation, icon or bundle resolution errors.

3. Eclipse project import/build smoke test:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120
```

Expected result:

- A clean Eclipse workspace imports a copy of the project metadata.
- Eclipse runs a full workspace build using `.project`, `.classpath`, `.settings/org.eclipse.jdt.core.prefs`, `MANIFEST.MF` and `build.properties`.
- No Java, PDE or compiler-level error markers are produced.
- This catches mixed compiler settings such as compliance `11` with target `21`.

4. Runtime-workspace `.env` smoke test without imported project:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -UseBundleEnv
```

Expected result:

- Eclipse starts from the Eclipse installation directory, not the project root.
- The smoke test does not create a `com.abap.assistant` workspace project.
- `OpenAiSettings` still resolves `OPENAI_API_KEY` from a `.env` located in the runtime workspace or configured environment path.
- This covers Eclipse launches where no imported `com.abap.assistant` project is present in the runtime workspace.

5. Persisted workspace runtime test:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -WorkspaceTemplate "C:\Users\Admin\runtime-EclipseApplication" -KeepPersistedState
```

Expected result:

- A copy of the previous runtime workspace is used.
- Old `.log` files are removed before launch so only new errors are assessed.
- Persisted workbench state is kept.
- The plugin id `com.abap.assistant`, view id `com.abap.assistant.ui.ChatView`, and icon `icons/abap_icon.png` resolve correctly.
- The smoke-test plugin opens the persisted/current `ABAP Chat` view, verifies the `ChatView` instance and exits successfully.

6. Live OpenAI smoke test:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-openai.ps1 -Prompt "Respond with exactly: OK"
```

Expected result:

- The script reads the local, non-committed `.env`.
- The OpenAI client sends a request without exposing the API key.
- The model returns a response.

## Manual Follow-Up Tests

After automated tests pass:

- Import the project into Eclipse.
- Confirm `Plug-in Dependencies` appears in the project.
- Run `Project > Clean`.
- Launch `Run As > Eclipse Application`.
- Confirm `Window > Show View > Other > ABAP Chat Assistant > ABAP Chat` opens.
- Test `Ask` with one anonymised ABAP editor open and confirm the response refers to that code.
- Test `Ask` with a main program plus includes or related objects opened in separate editor tabs and confirm the response uses the open-tab context.
- Test a free-form question that asks for ABAP code suggestions and confirm the response provides suggested snippets only.
- Test `Ask` only after configuring a local `.env` with a valid, non-committed OpenAI API key.
