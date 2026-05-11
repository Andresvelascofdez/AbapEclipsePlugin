# Instalacion Y Prueba En Eclipse

Esta guia explica como instalar y probar ABAP Eclipse Assistant sin mezclarlo con `SapIsuAssistant`.

## 1. Requisitos

- Java 17 o superior.
- Eclipse IDE con Plug-in Development Environment (PDE).
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

5. Importa el proyecto `com.anvel.abapeclipseassistant`.
6. Comprueba que Eclipse no muestra errores de PDE o JDT.

## 6. Ejecutar En Un Eclipse De Prueba

1. En Eclipse, haz clic derecho sobre el proyecto.
2. Selecciona `Run As > Eclipse Application`.
3. En el Eclipse runtime, abre:

```text
Window > Show View > Other > ABAP Eclipse Assistant > ABAP Assistant
```

4. Abre un editor ABAP o pega texto ABAP en la vista.
5. Usa `Load Selection` si tienes texto seleccionado en el editor.
6. Elige un modo y pulsa `Ask`.
7. Verifica que la respuesta aparece en el panel inferior y que el estado termina en `Done`.

## 7. Exportar E Instalar En Eclipse

Desde el Eclipse de desarrollo:

1. Ve a `File > Export`.
2. Selecciona `Plug-in Development > Deployable plug-ins and fragments`.
3. Selecciona `com.anvel.abapeclipseassistant`.
4. Elige una carpeta de destino.
5. Copia el `.jar` generado a la carpeta `dropins` de tu instalacion Eclipse.
6. Reinicia Eclipse.
7. Abre la vista `ABAP Assistant` desde `Window > Show View > Other`.

## 8. Pruebas Recomendadas

- Ejecutar `scripts/test.ps1` antes de cada commit relevante.
- Ejecutar `scripts/smoke-openai.ps1` despues de configurar una API key nueva.
- Probar `Load Selection` con un snippet ABAP publico o anonimizado.
- Probar cada modo de asistente con ejemplos sin datos reales de cliente.
- Confirmar que referencias tipo `TCK12345`, `HND12345`, emails y clientes numericos se anonimicen antes de enviarse.

## 9. Problemas Frecuentes

- `OPENAI_API_KEY is required.`: falta `.env` o la variable de entorno.
- `javac is not recognized`: instala Java 17+ y comprueba el `PATH`.
- La vista no aparece: revisa que PDE haya reconocido `plugin.xml` y que el proyecto no tenga errores.
- Error HTTP de OpenAI: revisa la clave, el modelo configurado y la conectividad.

