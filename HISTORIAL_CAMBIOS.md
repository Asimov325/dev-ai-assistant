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
**Estado:** Implementado; pendiente de compilación y validación funcional local.

### Objetivo
Integrar las fuentes ya configuradas dentro del flujo principal de Nuevo análisis sin convertir la pantalla documental en una herramienta de revisión Git.

### Implementado
- Búsqueda libre de requerimientos Jira, sin prefijo de proyecto hardcodeado.
- Autocompletado Jira con máximo 5 coincidencias y selección explícita del requerimiento.
- Rama origen y Rama del requerimiento reemplazadas por búsqueda predictiva con máximo 5 coincidencias.
- La rama del requerimiento usa el Jira seleccionado como texto inicial de búsqueda, sin seleccionarla automáticamente.
- El botón Analizar ejecuta la comparación real entre Rama origen y Rama del requerimiento.
- Comparación disponible tanto para repositorios LOCAL como REMOTE configurados.
- Resultado principal compacto: requerimiento, repositorio, ramas y cantidades de archivos modificados/nuevos/eliminados/renombrados.
- Evidencia técnica secundaria y colapsada para evitar saturar la vista cuando existen muchos cambios.
- El detalle de archivos muestra tipo, ruta y líneas agregadas/eliminadas.
- Generar Documento Técnico permanece deshabilitado hasta #010.

### Pendiente de validación
- `mvn clean test`.
- Búsqueda Jira por código y por texto.
- Confirmar máximo 5 resultados en Jira y ramas.
- Probar repositorio con gran cantidad de ramas.
- Ejecutar análisis LOCAL y/o REMOTE entre dos ramas reales.
- Confirmar que el resumen permanece compacto con muchos archivos modificados.
- Revisar evidencia técnica colapsada.

### Fuera de alcance
- Generación, visualización y edición del DT (#010).
- Publicación del documento en Confluence.

---

## Forma de trabajo acordada
Antes de cada nuevo bloque de desarrollo se explicará el alcance, archivos y motivo; se esperará aprobación explícita; después de la implementación se realizará validación local antes de considerar estable el cambio.
