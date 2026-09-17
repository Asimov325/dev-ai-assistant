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

---

## Cambio #010 — Generación IA y vista preliminar DT / DPC
**Estado:** Promovido a `desarrollo`; compilación local validada con BUILD SUCCESS. Ajustes posteriores en #010.1, #010.2 y #010.3.

### Implementado
- Selección independiente de DT o DPC.
- Contexto Jira + evidencia Git limitada y relevante.
- Generación mediante `AiProvider`, actualmente Gemini.
- Estructuras corporativas diferenciadas para DT y DPC.
- Reglas anti-invención y estados `No Aplica` / `Requiere validación`.

## Cambio #010.1 — Estabilización Gemini y trazabilidad de generación
**Estado:** Promovido a `desarrollo`; compilación local reportada con BUILD SUCCESS.

### Implementado
- Descubrimiento dinámico de modelos Gemini compatibles con `generateContent`.
- Logging SLF4J/Spring Boot sin exponer credenciales ni prompt/diff completo.
- Vista preliminar muestra proveedor y modelo IA.

## Cambio #010.2 — Fallback real entre modelos Gemini
**Estado:** Promovido a `desarrollo`; prueba funcional confirmó generación usando modelo alternativo tras incompatibilidad del modelo inicial.

### Implementado
- Autenticación REST mediante `x-goog-api-key`.
- Descarte temporal de modelos que devuelven HTTP 404.
- Fallback entre modelos compatibles hasta generar o agotar candidatos.
- Logs de modelo descartado, alternativo y resultado.

---

## Cambio #010.3 — Evidencia Git de la HU, alineamiento y vista documental
**Estado:** Implementado en `tmp-010-3`; pendiente de promoción a `desarrollo` y validación local/funcional.

### Motivo
La comparación anterior utilizaba directamente el árbol del tip de la rama origen contra el tip de la rama del requerimiento. En MEWARI-1679 la aplicación reportaba 64 archivos mientras el comparador de referencia basado en tres puntos mostraba 18. Además, la vista preliminar se mostraba como un textarea y no como documento estructurado.

### Implementado
- La evidencia propia del requerimiento se calcula desde el `merge-base` hasta la rama del requerimiento, equivalente conceptualmente a una comparación de tres puntos.
- El botón `Analizar` calcula por separado cuántos commits actuales de la rama origen todavía no están incorporados en la rama del requerimiento.
- El desalineamiento no contamina la evidencia documental y no bloquea la generación; se muestra como advertencia antes de generar.
- El análisis registra SHA origen, SHA requerimiento, merge-base, archivos de la HU, commits propios, commits de origen no incorporados y estado de alineamiento.
- `Ver evidencia técnica` muestra también los SHA utilizados para facilitar auditoría de la comparación.
- Markdown permanece como formato canónico del DT/DPC.
- Se agregó renderizado server-side de Markdown con soporte para tablas GFM y escape de HTML/URLs inseguras.
- La vista preliminar se presenta como una hoja/documento amplio tipo Word/Confluence.
- `Editar documento` cambia a un editor Markdown amplio; la edición sigue siendo preliminar y todavía no se persiste ni publica en Confluence.

### Validación requerida
- Promover a `desarrollo` solo después de aprobación.
- Ejecutar `mvn clean test`.
- Para MEWARI-1679 ejecutar primero solo `Analizar` y contrastar archivos/commits con el comparador de referencia (esperado observado: 18 archivos y 6 commits).
- Verificar la advertencia de alineamiento/desalineamiento.
- Solo cuando la evidencia Git sea correcta, generar nuevamente el DT y validar la vista estructurada.

---

## Forma de trabajo acordada
Antes de cada nuevo bloque de desarrollo se explicará el alcance, archivos y motivo; se esperará aprobación explícita; después de la implementación se realizará validación local antes de considerar estable el cambio.
