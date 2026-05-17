# Instalacion Y Prueba En Eclipse

Esta guia explica como instalar, ejecutar y validar ABAP Chat Assistant en Eclipse/ADT.

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
- panel `Suggested change review`
- boton `Copy suggestion`
- estado inferior

No hay botones de carga manual, caja visible de contexto ni panel secundario de resumen. El analisis de dependencias/riesgos se mantiene internamente para construir el prompt.

Flujo:

1. Abre uno o varios editores ABAP en Eclipse.
2. Opcionalmente abre objetos relacionados o confirma que sus ficheros fuente ya existen en el workspace local.
3. Escribe una pregunta libre en `Question`.
4. Pulsa `Ask`.
5. La caja `Question` se limpia inmediatamente.
6. El plug-in lee automaticamente todos los editores de texto abiertos, tanto la pestana visible como las pestanas abiertas en segundo plano.
7. El plug-in detecta referencias ABAP, objetos Z/custom y senales de riesgo locales antes de construir el prompt.
8. El plug-in carga ficheros de texto relacionados cuando coinciden con recursos ya disponibles en el workspace.
9. La respuesta aparece en el panel inferior y puede usar el analisis interno de dependencias/riesgos.
10. Si la respuesta contiene un bloque ABAP, el panel `Suggested change review` muestra una propuesta con cabecera de revision manual.
11. `Copy suggestion` copia la propuesta; no escribe ni activa nada en SAP.
12. La siguiente pregunta puede apoyarse en el historial reciente de la conversacion.

Ejemplos de preguntas:

```text
Explica este programa y dime posibles defectos reales.
```

```text
Propon una refactorizacion segura. Dame el codigo sugerido, pero no asumas que lo has aplicado.
```

```text
Analiza el flujo usando todos los editores abiertos y cualquier fuente relacionada cargada del workspace. Si falta algo, indicalo como TODO/TBC.
```

## 9. Que Contexto Lee

Lee automaticamente:

- editor activo
- otros editores de texto abiertos
- programas/includes/clases abiertos en pestanas de Eclipse
- ficheros de texto relacionados ya presentes en el workspace local cuando su nombre/ruta coincide con referencias detectadas
- historial reciente de preguntas y respuestas de la misma vista
- resumen local interno de dependencias y senales de riesgo para el prompt

No lee automaticamente:

- objetos remotos que no esten abiertos ni materializados en el workspace local
- SAP GUI fuera de Eclipse
- datos de cliente externos a los editores/workspace

## 10. Pruebas Manuales Recomendadas

### Un Programa Abierto

1. Abre un programa ABAP de prueba.
2. Pregunta:

```text
Explica este programa y lista riesgos reales.
```

Resultado esperado:

- `Question` se borra.
- La respuesta habla del programa abierto.
- La respuesta no afirma que haya modificado SAP.

### Varios Editores Abiertos

1. Abre programa principal e includes/clases en pestanas separadas.
2. Pregunta:

```text
Analiza el flujo completo con todo lo abierto.
```

Resultado esperado:

- La respuesta usa contexto de todas las pestanas.
- Las referencias no disponibles aparecen como TODO/TBC.

### Fuente Relacionada En Workspace

1. Deja abierto un programa que contenga `INCLUDE zexample_forms.` o una referencia de clase.
2. Verifica que existe un fichero de texto con nombre/ruta compatible en el workspace.
3. Pregunta por el flujo.

Resultado esperado:

- La respuesta distingue lo cargado de lo no disponible.

### Historial

1. Haz una pregunta sobre un programa abierto.
2. Cuando responda, pregunta:

```text
Ahora dame una version mas corta centrada solo en defectos.
```

Resultado esperado:

- La respuesta entiende la continuidad de la conversacion.

### Codigo Sugerido

Pregunta:

```text
Sugiere un cambio ABAP seguro. Devuelve solo codigo para revisar manualmente.
```

Resultado esperado:

- Puede devolver codigo ABAP.
- El panel `Suggested change review` muestra el bloque si la respuesta lo devuelve en formato fenced code.
- `Copy suggestion` copia texto con cabecera de revision manual.
- No dice que lo haya aplicado.
- El usuario decide si copia, adapta y activa el cambio.

## 11. Problemas Frecuentes

- `OPENAI_API_KEY is required.`: revisa las rutas que aparecen en el error y confirma que `.env` existe en una de ellas.
- La UI antigua sigue apareciendo: reinstala/exporta el jar nuevo y reinicia Eclipse con `-clean`.
- `Compliance level '11' is incompatible with target level '21'`: confirma `JavaSE-11` en `.classpath` y `javacSource = 11` / `javacTarget = 11` en `build.properties`; luego ejecuta `Project > Clean`.
- La respuesta ignora una dependencia: abre esa dependencia como pestana o confirma que existe como fichero de texto en el workspace local.
- La respuesta muestra falsos positivos de analisis: recuerda que el analisis es estatico y conservador; usa el resultado como ayuda de revision, no como prueba final de comportamiento runtime.
- Error HTTP de OpenAI: revisa la clave, el modelo y conectividad.
