# Historial de cambios — Development AI Assistant

Este archivo registra los cambios funcionales y técnicos aprobados durante el desarrollo del proyecto.

## Cambios #001 a #004
Los cambios anteriores permanecen registrados en el historial Git del proyecto. El Cambio #004 quedó aplicado en el commit `867b69338d82ac0025046c83ee42184156bd9152` y estableció la comparación de ramas y la evidencia técnica Git, utilizando `MEWARI-1455` como caso patrón.

## Cambio #005 — Pantalla de prueba del análisis Git
**Estado:** Aplicado  
**Commit:** `0334afbd9e11afd08ab2b9cfc17bb5621c40b2ca`

## Cambio #006 — Proveedor de IA desacoplado y Gemini
**Estado:** Aplicado

## Cambio #007 — Estabilización técnica y de la pantalla de validación
**Estado:** Aplicado y validado localmente

### Validado
- `mvn clean test`: BUILD SUCCESS.
- Aplicación Spring Boot levantada localmente.
- Inspección y comparación Git verificadas.

## Cambio #008 — UX/UI, navegación y separación del MVP
**Estado:** Aplicado

### Implementado
- Inicio, sidebar, Nuevo análisis, Documentación, Historial y Configuración.
- Configuración Git reutilizable por el nuevo análisis.
- Herramienta técnica separada del flujo funcional.

## Cambio #008.1 — Conectividad real y fuentes Git LOCAL/REMOTE
**Estado:** Aplicado; validación funcional identificó ajustes para #008.2

### Validado
- `mvn clean test`: BUILD SUCCESS reportado por validación local.
- Git LOCAL funcional.
- Git REMOTE logró autenticarse contra repositorio privado personal.
- Jira logró realizar la prueba de conectividad.

## Cambio #008.2 — Estabilización de configuración e integraciones
**Estado:** Aplicado; validación funcional realizada y ajustes de Confluence incorporados.

### Implementado
- Git REMOTE valida URL/autenticación/ramas con `ls-remote`, sin clonar durante `Probar conexión`.
- Configuración persistente fuera del repositorio en `~/.development-ai-assistant`.
- Credenciales locales protegidas con AES/GCM y clave local separada.
- Git, Jira, Gemini y Confluence mantienen configuración independiente.
- Configuraciones activas mediante cards y opción de eliminación.
- Jira y Confluence usan configuraciones totalmente independientes.
- Confluence valida el Space Key mediante API V2.
- Inicio refleja el estado real de las integraciones.

---

## Cambio #009 — Nuevo análisis real Jira + Git
**Estado:** Validado localmente.

### Implementado y validado
- Búsqueda libre de requerimientos Jira, sin prefijo de proyecto hardcodeado.
- Autocompletado Jira con máximo 5 coincidencias y selección explícita.
- Búsqueda predictiva de Rama origen y Rama del requerimiento.
- Comparación real LOCAL/REMOTE y evidencia técnica compacta.
- Detalle colapsado de archivos con tipo, ruta y líneas agregadas/eliminadas.
- `mvn clean test`: BUILD SUCCESS reportado por validación local de #009.

## Cambio #009.1 — Rendimiento y progreso del análisis
**Estado:** Implementado y probado funcionalmente.

### Implementado
- REMOTE obtiene únicamente las dos ramas necesarias para el análisis en lugar de clonar todas las ramas.
- Indicador visual de progreso mientras se realiza la comparación Git.
- Prevención de envíos duplicados durante el análisis.

### Validación funcional
- Se reportó una mejora perceptible en el tiempo del análisis.

---

## Cambio #010 — Generación IA y vista preliminar DT / DPC
**Estado:** Promovido a `desarrollo`; compilación local validada con BUILD SUCCESS. La prueba funcional detectó incompatibilidad del modelo Gemini al ejecutar `generateContent`, tratada en #010.1 y #010.2.

### Implementado
- Selección independiente de DT o DPC.
- Contexto Jira + evidencia Git limitada y relevante.
- Generación mediante `AiProvider`, actualmente Gemini.
- Estructuras corporativas diferenciadas para DT y DPC.
- Reglas anti-invención y estados `No Aplica` / `Requiere validación`.
- Vista preliminar editable y sin publicación a Confluence.

---

## Cambio #010.1 — Estabilización Gemini y trazabilidad de generación
**Estado:** Promovido a `desarrollo`; compilación local reportada con BUILD SUCCESS. La prueba funcional mostró que `models.list` anunciaba `gemini-2.5-flash-lite` como compatible, pero `generateContent` devolvía HTTP 404.

### Implementado
- Descubrimiento dinámico del catálogo de modelos que Gemini declara compatibles con `generateContent`.
- Logging mediante SLF4J/Spring Boot para seguir análisis y generación sin exponer credenciales ni contenido completo del prompt/diff.
- La vista preliminar muestra proveedor IA y modelo utilizado.
- Detección explícita del HTTP 404 durante generación.

---

## Cambio #010.2 — Fallback real entre modelos Gemini
**Estado:** Implementado en rama temporal `tmp-010-2`; pendiente de promoción a `desarrollo` y validación local.

### Motivo
En la prueba funcional, Gemini listó `gemini-2.5-flash-lite` con soporte para `generateContent`, pero la llamada real devolvió HTTP 404. El reintento de #010.1 volvía a elegir el mismo modelo y no podía recuperarse.

### Implementado
- La autenticación REST de Gemini utiliza el header `x-goog-api-key` tanto para consultar modelos como para generar contenido.
- La generación obtiene el catálogo compatible y mantiene una lista de modelos descartados únicamente durante la solicitud actual.
- Si un modelo devuelve HTTP 404, se descarta para esa generación y se selecciona automáticamente otro candidato compatible.
- El fallback puede recorrer candidatos alternativos hasta obtener una generación válida o agotar los modelos anunciados por Gemini.
- Se mantiene el modelo preferido como primer intento cuando está disponible, sin permitir que bloquee los fallbacks posteriores.
- La selección dinámica favorece generaciones actuales y modelos Flash/Lite, evitando fijar en código un único identificador concreto de Gemini.
- Los logs muestran cada modelo descartado, el modelo alternativo elegido y el resultado de cada intento, sin registrar API keys ni el prompt completo.

### Pendiente de validación
- Promover #010.2 a `desarrollo` previa aprobación.
- Ejecutar `mvn clean test`.
- Repetir la generación DT con el caso de prueba seleccionado por el usuario.
- Confirmar en logs que un HTTP 404 descarta el modelo y utiliza un modelo alternativo.
- Confirmar que la vista preliminar muestre el modelo que finalmente generó el documento.
- Posteriormente validar generación DPC.

---

## Forma de trabajo acordada
Antes de cada nuevo bloque de desarrollo se explicará el alcance, archivos y motivo; se esperará aprobación explícita; después de la implementación se realizará validación local antes de considerar estable el cambio.
