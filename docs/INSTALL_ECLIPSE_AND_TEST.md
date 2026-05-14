# Instalacion Y Prueba En Eclipse

Esta guia explica como instalar y probar ABAP Chat Assistant sin mezclarlo con `SapIsuAssistant`.

## 1. Requisitos

- Java 11 o superior.
- Eclipse IDE con Plug-in Development Environment (PDE). Un Eclipse solo para ABAP/ADT puede no traer PDE instalado.
- SAP ABAP Development Tools si se quiere probar con editores ABAP reales.
- Acceso al repositorio `https://github.com/Andresvelascofdez/AbapEclipsePlugin`.
- Una API key de OpenAI nueva y no expuesta en chats, logs, commits ni capturas.

No hace falta cerrar nada para que Codex cree o modifique los archivos del proyecto. Antes de importar o exportar el plugin, cierra cualquier Eclipse que tenga este mismo proyecto abierto si ves bloqueos de workspace o de compilacion.

## 2. Preparar El Proyecto

Desde PowerShell:

```powershell
cd C:\Users\Admin\SapAssistant\AbapEclipseAssistant
powershell -ExecutionPolicy Bypass -File scripts/test.ps1
```

El test debe terminar con:

```text
All core tests passed.
Validation completed successfully.
```

Ejecuta tambien la prueba runtime real con Eclipse:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse"
```

Ejecuta tambien la prueba de importacion/build real del proyecto en Eclipse:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse-project-build.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse"
```

Si vienes de un workspace que ya tenia la vista `ABAP Chat` persistida y dio errores como `Unable to resolve plug-in "com.abap.assistant"` o `platform:/plugin/com.abap.assistant/icons/abap_icon.png`, ejecuta:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-eclipse.ps1 -EclipseHome "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse" -WorkspaceTemplate "C:\Users\Admin\runtime-EclipseApplication" -KeepPersistedState
```

## 2.1. Si Eclipse Muestra `org.eclipse Cannot Be Resolved`

Los errores de la captura, por ejemplo `Button cannot be resolved to a type`, `SWT cannot be resolved to a variable` o `The import org.eclipse cannot be resolved`, indican que Eclipse no ha cargado las dependencias de plug-in. No es un error del codigo Java de `ChatView`; falta PDE o no esta activa la Target Platform.

Comprueba tu instalacion:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-eclipse-prereqs.ps1 -EclipseHome "C:\ruta\a\eclipse"
```

Para corregirlo en Eclipse:

1. Instala PDE desde `Help > Install New Software`.
2. Busca e instala `Eclipse Plug-in Development Environment`.
3. Reinicia Eclipse.
4. Abre `Window > Preferences > Plug-in Development > Target Platform`.
5. Activa `Running Platform`.
6. Aplica cambios.
7. Si habias importado el proyecto antes de instalar PDE, borralo del workspace sin borrar del disco y vuelve a importarlo.
8. Ejecuta `Project > Clean`.

Cuando este bien, el proyecto debe mostrar `Plug-in Dependencies` en el build path y desapareceran los errores de SWT/JFace/PDE.

## 3. Configurar OpenAI

No guardes claves reales en git. Crea un `.env` local desde la plantilla:

```powershell
Copy-Item .env.example .env
notepad .env
```

Rellena:

```text
OPENAI_API_KEY=replace-with-your-new-openai-api-key
OPENAI_MODEL=gpt-5-mini
OPENAI_BASE_URL=https://api.openai.com/v1/responses
```

Si una clave ya se pego en un chat o documento, revocala y crea una nueva antes de usarla.

Dentro de Eclipse, el plugin busca `.env` primero en el proyecto importado `com.abap.assistant`, despues en otros proyectos del workspace, despues en `ABAP_ECLIPSE_ASSISTANT_ENV_DIR` si lo defines, despues cerca del bundle/codigo cargado del plugin, despues en la raiz del workspace runtime y finalmente en el directorio de arranque de Eclipse. Esto cubre lanzamientos `Run As > Eclipse Application`, donde el workspace runtime puede ser `C:\Users\Admin\runtime-EclipseApplication` aunque el `.env` este en `C:\Users\Admin\SapAssistant\AbapEclipseAssistant`.

Si quieres fijar la ruta exacta, define la variable de entorno o propiedad Java `ABAP_ECLIPSE_ASSISTANT_ENV_FILE` con la ruta completa al archivo `.env`. Para una configuracion de lanzamiento PDE, puedes anadir este VM argument:

```text
-DABAP_ECLIPSE_ASSISTANT_ENV_FILE=C:\Users\Admin\SapAssistant\AbapEclipseAssistant\.env
```

## 4. Probar La Llamada Real A OpenAI

Con `.env` configurado:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-openai.ps1 -Prompt "Explain SELECT SINGLE in SAP ABAP using public SAP standard knowledge only."
```

La salida debe mostrar el `Privacy scope` y una respuesta del modelo. Esta prueba usa el mismo cliente OpenAI que el plugin, pero no requiere arrancar Eclipse.

## 5. Importar En Eclipse

1. Abre Eclipse.
2. Ve a `File > Import`.
3. Selecciona `General > Existing Projects into Workspace`.
4. En `Select root directory`, elige:

```text
C:\Users\Admin\SapAssistant\AbapEclipseAssistant
```

5. Importa el proyecto `com.abap.assistant`.
6. Comprueba que Eclipse no muestra errores de PDE o JDT.

## 6. Ejecutar En Un Eclipse De Prueba

1. En Eclipse, haz clic derecho sobre el proyecto.
2. Selecciona `Run As > Eclipse Application`.
3. En el Eclipse runtime, abre:

```text
Window > Show View > Other > ABAP Chat Assistant > ABAP Chat
```

4. Abre uno o varios editores ABAP.
5. Escribe una pregunta libre en `Question`.
6. Pulsa `Ask`.
7. El plugin leera automaticamente todos los editores de texto abiertos, incluida la pestana visible y las pestanas abiertas en segundo plano.
8. Verifica que la respuesta aparece en el panel inferior y que el estado termina en `Done`.

## 7. Exportar E Instalar En Eclipse

Desde el Eclipse de desarrollo:

1. Ve a `File > Export`.
2. Selecciona `Plug-in Development > Deployable plug-ins and fragments`.
3. Selecciona `com.abap.assistant`.
4. Elige una carpeta de destino.
5. Copia el `.jar` generado a la carpeta `dropins` de tu instalacion Eclipse.
6. Reinicia Eclipse.
7. Abre la vista `ABAP Chat` desde `Window > Show View > Other`.

## 8. Pruebas Recomendadas

- Ejecutar `scripts/test.ps1` antes de cada commit relevante.
- Ejecutar `scripts/smoke-openai.ps1` despues de configurar una API key nueva.
- Probar `Ask` con un programa Z de prueba abierto.
- Probar `Ask` con un programa principal y varios includes abiertos en pestanas distintas.
- Probar cada modo de asistente con ejemplos sin datos reales de cliente.
- Confirmar que referencias tipo `TCK12345`, `HND12345`, emails y clientes numericos se anonimicen antes de enviarse.

## 9. Problemas Frecuentes

- `OPENAI_API_KEY is required.`: revisa las rutas que aparecen en el propio mensaje. Si no aparece `C:\Users\Admin\SapAssistant\AbapEclipseAssistant\.env`, actualiza el plugin, limpia el runtime workspace o usa `-DABAP_ECLIPSE_ASSISTANT_ENV_FILE=C:\Users\Admin\SapAssistant\AbapEclipseAssistant\.env`.
- `Compliance level '11' is incompatible with target level '21'`: actualiza el proyecto y refresca `.classpath` y `build.properties`; el contenedor JRE debe ser `JavaSE-11`, y `build.properties` debe tener `javacSource = 11` y `javacTarget = 11`. Despues ejecuta `Project > Clean`.
- `javac is not recognized`: instala Java 17+ y comprueba el `PATH`.
- La vista no aparece: revisa que PDE haya reconocido `plugin.xml` y que el proyecto no tenga errores.
- Error HTTP de OpenAI: revisa la clave, el modelo configurado y la conectividad.
