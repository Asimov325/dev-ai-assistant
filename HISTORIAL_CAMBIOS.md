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
**Estado:** Promovido a `desarrollo`; validación funcional de Git correcta para MEWARI-1679.

### Implementado y validado
- Evidencia calculada desde `merge-base` hasta la rama del requerimiento.
- `Analizar` calcula por separado commits de la rama origen no incorporados en la HU.
- MEWARI-1679 reportó 18 archivos y 6 commits propios, coincidiendo con el comparador de referencia.
- Se detectaron 43 commits actuales de `produccion` no incorporados en MEWARI-1679 y se mostró la advertencia sin contaminar la evidencia documental.
- Logs incluyen SHA origen, SHA requerimiento, merge-base, archivos, commits propios y alineamiento.
- Markdown permanece como formato canónico y se renderiza como documento tipo Word/Confluence.

---

## Cambio #010.3.1 — Selección dinámica y resiliencia Gemini
**Estado:** Promovido a `desarrollo`; generación funcional confirmada con `gemini-flash-lite-latest`.

### Implementado
- Selección dinámica desde el catálogo real de modelos Gemini con `generateContent`.
- HTTP 404 descarta el modelo; HTTP 503/429 reintenta y luego continúa con otro candidato.
- Los modelos descartados durante la sesión no vuelven a ser candidatos inmediatos.
- Logs registran modelo, intento, código HTTP y fallback sin exponer credenciales.

---

## Cambio #010.4 — Estructura corporativa DT/DPC y edición documental
**Estado:** Promovido a `desarrollo`; DT y DPC revisados funcionalmente en la vista preliminar.

### Implementado
- Gemini debe devolver Markdown GFM estructurado, con títulos, subtítulos, tablas, listas, negritas y separación explícita entre secciones.
- DT alineado al patrón corporativo: Información general en tabla; Descripción funcional; Modelo conceptual; Descripción técnica con Nivel BD y Nivel Desarrollo; Objetos Relacionados con tablas separadas de aplicación y BD.
- DPC alineado al patrón corporativo: Información General; Objetivo; Requisitos; Scripts BD separados en CREACIÓN/REVERSIÓN y tablas; Scripts MQ; reglas de acceso; opciones/perfiles; Procedimiento del Pase en tabla; Plan de Ejecución.
- `No Aplica` y `Requiere validación` se presentan separados del título correspondiente.
- Se prohíbe inventar ZIP, LDAP, comandos, pipelines, despliegues o datos del documento de referencia que no estén demostrados por Jira/Git.
- El prompt recibe el nombre lógico del repositorio configurado y no el directorio temporal interno `dev-ai-analysis-*`.
- El editor Markdown ocupa el ancho disponible y aproximadamente 72% de la altura de la ventana, con redimensionamiento vertical.
- La vista HTML conserva Markdown como fuente canónica y mantiene renderizado seguro mediante CommonMark/GFM Tables.

---

## Cambio #010.5 — Ajustes finales de presentación DT/DPC
**Estado:** Promovido a `desarrollo`; validación funcional visual realizada con DT y DPC de MEWARI-1679.

### Implementado y validado
- Las tablas documentales respetan el ancho de la hoja y las rutas/nombres técnicos largos se dividen en varias líneas sin desbordar el DT/DPC.
- Los datos que no pueden confirmarse con Jira/Git se muestran como `[Requiere validación]`, dejando explícito que son campos pendientes de completar o confirmar durante la revisión humana.
- Se normalizan respuestas de IA que todavía devuelvan `Requiere validación` sin corchetes para mantener un formato consistente.
- DT y DPC incorporan al final una firma generada por la aplicación con Development AI Assistant, fuentes Jira + Git, requerimiento, proveedor/modelo IA realmente utilizado y estado `Pendiente de revisión humana`.
- La firma se agrega al Markdown canónico y no depende de que la IA la redacte, de modo que podrá conservarse en la posterior publicación a Confluence.

---

## Cambio #010.6 — Tablas DPC y procedimiento de pase por actividades
**Estado:** Implementado en `tmp-010-6`; pendiente de promoción y validación local.

### Implementado
- Las tablas DPC reciben una clasificación semántica al renderizar Markdown, sin modificar el Markdown canónico que posteriormente se publicará en Confluence.
- `Requisitos` reserva aproximadamente 28% para Item y 72% para Descripción.
- Las tablas de scripts reservan solo 5% para `#`, 55% para nombre/ruta del script y 40% para consideraciones.
- `Procedimiento del Pase` reserva solo 4% para `#` y prioriza el espacio de instrucciones de ejecución, validación y reversión.
- El prompt DPC define nombres cortos y estables para requisitos, evitando etiquetas redundantes como `Scripts de Base de Scripts`.
- El Procedimiento del Pase se genera como secuencia de actividades de despliegue y no como inventario de archivos.
- Los archivos Java/fuentes del mismo despliegue se consolidan en un único paso de Fuentes/Aplicación; no se crea una fila por clase Java.
- Scripts BD, fuentes/branch, opciones LDAP/LDIF, asociaciones de perfiles, MQ u otros recursos se convierten en pasos independientes solo cuando exista evidencia Jira/Git.
- El orden de pasos se deriva de las dependencias/evidencia del requerimiento y no se fuerza a una numeración fija.
- Cuando una actividad está demostrada pero faltan instrucciones concretas de ejecución, validación o reversión, solo esas celdas quedan como `[Requiere validación]`.
- Se mantiene la prohibición de inventar comandos, herramientas de compilación, pipelines, servidores, ambientes, CN LDAP o perfiles no sustentados.

### Validación requerida
- Regenerar el DPC de MEWARI-1679 y verificar proporciones de `Requisitos`, scripts y `Procedimiento del Pase`.
- Confirmar que los cuatro archivos Java del caso se consoliden en un único paso de fuentes/aplicación.
- Confirmar que los scripts BD se consoliden como actividad de BD y que los detalles desconocidos queden en `[Requiere validación]`.
- Verificar que no se inventen pasos de opciones/perfiles si la evidencia de MEWARI-1679 no los contiene.

---

## Forma de trabajo acordada
Antes de cada nuevo bloque de desarrollo se explicará el alcance, archivos y motivo; se esperará aprobación explícita; después de la implementación se realizará validación local antes de considerar estable el cambio.