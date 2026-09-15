# Historial de cambios — Development AI Assistant

Este archivo registra los cambios funcionales y técnicos aprobados durante el desarrollo del proyecto.

## Cambios #001 a #004
Los cambios anteriores permanecen registrados en el historial Git del proyecto. El Cambio #004 quedó aplicado en el commit `867b69338d82ac0025046c83ee42184156bd9152` y estableció la comparación de ramas y la evidencia técnica Git, utilizando `MEWARI-1455` como caso patrón.

## Cambio #005 — Pantalla de prueba del análisis Git

**Estado:** Aplicado  
**Rama:** `desarrollo`  
**Commit:** `0334afbd9e11afd08ab2b9cfc17bb5621c40b2ca`

### Objetivo
Incorporar una interfaz web mínima para inspeccionar un repositorio local, seleccionar explícitamente rama base y rama del requerimiento, comparar ambas y visualizar la evidencia técnica antes de integrar Jira e IA.

### Resultado
Se incorporó la pantalla Thymeleaf de análisis Git y el controlador web. La comparación muestra resumen, archivos afectados, líneas agregadas/eliminadas y diff individual bajo demanda.

---

## Cambio #006 — Proveedor de IA desacoplado y Gemini

**Estado:** Aplicado  
**Rama:** `desarrollo`

### Objetivo
Incorporar una abstracción de proveedor de inteligencia artificial que permita utilizar Gemini durante el MVP sin acoplar el resto de Development AI Assistant a un motor específico.

### Archivos
- Creado: `src/main/java/com/dev/aiassistant/ai/AiProvider.java`
- Creado: `src/main/java/com/dev/aiassistant/ai/AiService.java`
- Creado: `src/main/java/com/dev/aiassistant/ai/gemini/GeminiAiProvider.java`
- Creado: `src/main/java/com/dev/aiassistant/ai/web/AiTestController.java`
- Modificado: `src/main/resources/application.yml`
- Modificado: `src/main/resources/templates/git-analysis.html`
- Actualizado: `HISTORIAL_CAMBIOS.md`

### Decisión de diseño
`AiProvider` constituye el contrato independiente del motor. `GeminiAiProvider` implementa temporalmente ese contrato para el MVP y `AiService` es el punto de entrada para las capacidades que posteriormente utilizarán el agente documental y otros agentes.

La API key no se almacena en el repositorio. Se obtiene mediante la variable de entorno `GEMINI_API_KEY`. El modelo y la URL base también pueden configurarse externamente mediante `GEMINI_MODEL` y `GEMINI_BASE_URL`.

La integración utiliza la API REST de Gemini y mantiene los detalles específicos del proveedor encapsulados dentro de `GeminiAiProvider`.

### Funcionalidad disponible
- Proveedor IA desacoplado mediante `AiProvider`.
- Implementación inicial con Gemini.
- Configuración externa de credenciales y modelo.
- Validación de prompt vacío.
- Manejo entendible de ausencia de API key, errores HTTP y respuestas vacías.
- Prueba manual desde la interfaz web con contenido controlado.

### Seguridad para el MVP
La pantalla advierte que la prueba temporal no debe utilizar código ni información corporativa sensible. La integración con Gemini gratuito se validará inicialmente con contenido no corporativo.

### Resultado
Development AI Assistant queda preparado para comprobar una conexión real con Gemini y, posteriormente, sustituirlo por otro proveedor o por el motor corporativo sin modificar la lógica de Git, Jira o generación documental.

---

## Forma de trabajo acordada

Antes de cada nuevo bloque de desarrollo se explicará el alcance, archivos y motivo; se esperará aprobación explícita; después se aplicará un commit lógico y se actualizará este historial.
