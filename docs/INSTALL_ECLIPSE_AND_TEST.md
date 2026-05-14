# Instalacion Y Prueba En Eclipse

Esta guia explica como instalar y probar ABAP Chat Assistant sin mezclarlo con `SapIsuAssistant`.

## 1. Requisitos

- Java 11 o superior.
- Eclipse IDE con Plug-in Development Environment (PDE).
- SAP ABAP Development Tools (ADT) si se va a probar con objetos ABAP reales.
- Acceso al repositorio `https://github.com/Andresvelascofdez/AbapEclipsePlugin`.
- Una API key de OpenAI guardada solo en `.env` local.

No guardes claves, usuarios SAP, tickets reales, facturas ni datos de cliente en git, capturas o prompts. Usa ejemplos anonimizados.

## 2. Obtener El Proyecto

```powershell
cd C:\Users\Admin\SapAssistant
git clone https://github.com/Andresvelascofdez/AbapEclipsePlugin.git AbapEclipseAssistant
cd C:\Users\Admin\SapAssistant\AbapEclipseAssistant
```

Si ya existe:

```powershell
cd C:\Users\Admin\SapAssistant\AbapEclipseAssistant
git pull
```

## 3. Configurar OpenAI

Crea `.env` desde la plantilla:

```powershell
Copy-Item .env.example .env
notepad .env
```

Contenido esperado:

```text
OPENAI_API_KEY=replace-with-your-new-openai-api-key
OPENAI_MODEL=gpt-5-mini
OPENAI_BASE_URL=https://api.openai.com/v1/responses
```

Dentro de Eclipse, el plug-in busca `.env` en este orden:

- `ABAP_ECLIPSE_ASSISTANT_ENV_FILE`, si se define con la ruta completa al fichero.
- Proyecto importado `com.abap.assistant`.
- Otros proyectos del workspace.
- `ABAP_ECLIPSE_ASSISTANT_ENV_DIR`, si se define con una carpeta.
- Ubicacion del bundle/codigo cargado.
- Raiz del workspace.
- Directorio de arranque de Eclipse.

Para fijar una ruta exacta en una configuracion PDE, usa este VM argument:

```text
-DABAP_ECLIPSE_ASSISTANT_ENV_FILE=C:\Users\Admin\SapAssistant\AbapEclipseAssistant\.env
```

## 4. Validar Sin Eclipse

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test.ps1
```

Resultado esperado:

```text
All core tests passed.
Validation completed successfully.
```

## 5. Validar Con Eclipse Real

Prueba runtime:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120
```

Prueba de importacion/build del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -TimeoutSeconds 120
```

Prueba con `.env` de runtime:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -UseBundleEnv -TimeoutSeconds 120
```

Prueba viva contra OpenAI:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-openai.ps1 -Prompt "Respond with exactly: OK"
```

## 6. Importar En Eclipse

1. Abre Eclipse.
2. Ve a `File > Import`.
3. Selecciona `General > Existing Projects into Workspace`.
4. En `Select root directory`, elige:

```text
C:\Users\Admin\SapAssistant\AbapEclipseAssistant
```

5. Importa `com.abap.assistant`.
6. Confirma que `Plug-in Dependencies` aparece en el build path.
7. Ejecuta `Project > Clean`.

Si aparecen errores `org.eclipse cannot be resolved`, `SWT cannot be resolved`, `Button cannot be resolved` o similares, instala PDE y activa `Window > Preferences > Plug-in Development > Target Platform > Running Platform`.

## 7. Ejecutar En Eclipse

Desde el Eclipse de desarrollo:

1. Click derecho en el proyecto.
2. `Run As > Eclipse Application`.
3. En el Eclipse runtime abre:

```text
Window > Show View > Other > ABAP Chat Assistant > ABAP Chat
```

Si instalas el jar en un Eclipse normal, exporta el plug-in desde PDE y copia el jar a `dropins`; despues reinicia Eclipse con `-clean` si ves una version antigua.

## 8. Uso Actual Del Plug-in

La vista actual tiene:

- selector de modo
- boton `Ask`
- caja `Question`
- panel de respuesta
- estado inferior

No hay botones de carga manual ni caja visible de contexto.

Flujo:

1. Abre uno o varios editores ABAP en Eclipse.
2. Abre tambien includes, clases, funciones u otros objetos relacionados si quieres que formen parte del contexto.
3. Escribe una pregunta libre en `Question`.
4. Pulsa `Ask`.
5. La caja `Question` se limpia inmediatamente.
6. El plug-in lee automaticamente todos los editores de texto abiertos, tanto la pestana visible como las pestanas abiertas en segundo plano.
7. La respuesta aparece en el panel inferior.

Ejemplos de preguntas:

```text
Explica este programa y dime posibles defectos reales.
```

```text
Propón una refactorizacion segura. Dame el codigo sugerido, pero no asumas que lo has aplicado.
```

```text
Analiza el flujo usando todos los editores abiertos. Si falta algun include, indicalo como TODO/TBC.
```

## 9. Que Contexto Lee

Lee automaticamente:

- editor activo
- otros editores de texto abiertos
- programas/includes/clases abiertos en pestanas de Eclipse

No lee automaticamente:

- includes no abiertos
- programas no abiertos
- objetos SAP que solo existan en el repositorio pero no esten en pestanas
- SAP GUI fuera de Eclipse

Para que un include u objeto relacionado entre en contexto, abrelo en una pestana antes de pulsar `Ask`.

## 10. Problemas Frecuentes

- `OPENAI_API_KEY is required.`: revisa las rutas que aparecen en el error y confirma que `.env` existe en una de ellas.
- La UI antigua sigue apareciendo: reinstala/exporta el jar nuevo y reinicia Eclipse con `-clean`.
- `Compliance level '11' is incompatible with target level '21'`: confirma `JavaSE-11` en `.classpath` y `javacSource = 11` / `javacTarget = 11` en `build.properties`; luego ejecuta `Project > Clean`.
- La respuesta ignora un include: abre ese include como pestana de Eclipse y vuelve a preguntar.
- Error HTTP de OpenAI: revisa la clave, el modelo y conectividad.

## 11. Pruebas Manuales Recomendadas

- Preguntar con un solo programa Z de prueba abierto.
- Preguntar con programa principal e includes abiertos.
- Confirmar que la caja `Question` se borra tras pulsar `Ask`.
- Confirmar que el estado indica el numero de editores abiertos enviados.
- Pedir codigo sugerido y verificar que no afirma haber modificado SAP.
- Usar solo codigo anonimizado o no confidencial.
