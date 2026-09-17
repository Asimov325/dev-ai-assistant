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
**Estado:** Implementado en rama temporal `tmp-010`; pendiente de validación local antes de promover a `desarrollo`.

### Objetivo
Convertir la evidencia reunida por #009 en un documento preliminar revisable, manteniendo a la persona como responsable de validar el contenido antes de cualquier publicación.

### Implementado
- Selección independiente de Documento Técnico (DT) o Documento de Propuesta de Cambio (DPC).
- Recuperación del contexto del Jira seleccionado para la generación: clave, título, estado, tipo y descripción.
- Generación real mediante la abstracción `AiProvider`, actualmente Gemini.
- Contexto de IA compuesto por Jira + inventario de cambios Git + selección limitada de diferencias relevantes.
- Límites de cantidad/tamaño de diff para evitar enviar indiscriminadamente todo el cambio a la IA.
- Reglas explícitas contra invención de procesos, objetos, scripts, impactos y datos no sustentados.
- Uso de `No Aplica` y `Requiere validación` cuando la evidencia no permite completar una sección.
- Estructura DT alineada con el DT corporativo de referencia.
- Estructura DPC separada y alineada con el DPC corporativo de referencia: Información General, Objetivo del Documento, Requisitos, Scripts de Base de Datos (Creación/Reversión), Scripts MQ, reglas de acceso, opciones/perfiles, Procedimiento del Pase y Proceso del Plan de Ejecución.
- Indicador de progreso durante la generación IA.
- Vista preliminar editable dentro de Development AI Assistant.
- El documento permanece como `Pendiente de revisión`; #010 no publica ni modifica Confluence.
- Ningún Jira o rama de prueba está hardcodeado en la implementación.

### Pendiente de validación
- Ejecutar `mvn clean test` localmente.
- Levantar la aplicación y verificar que DT y DPC aparezcan en el selector.
- Ejecutar un análisis real con un Jira y ramas seleccionadas por el usuario.
- Generar DT y revisar que Gemini use contexto Jira + Git sin inventar información.
- Generar DPC y revisar que use su estructura propia, distinta al DT.
- Confirmar que el contenido preliminar pueda editarse en pantalla y que no exista publicación a Confluence.

### Fuera de alcance
- Guardado/publicación en Confluence.
- Configuración de página padre de DT y DPC en Confluence.
- Estado `Ready for review` en Confluence.
- Aprobación/publicación definitiva del documento.

---

## Forma de trabajo acordada
Antes de cada nuevo bloque de desarrollo se explicará el alcance, archivos y motivo; se esperará aprobación explícita; después de la implementación se realizará validación local antes de considerar estable el cambio.
