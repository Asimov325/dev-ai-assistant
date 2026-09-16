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

### Resultado
Development AI Assistant queda preparado para comprobar una conexión real con Gemini y posteriormente sustituirlo por otro proveedor sin modificar la lógica de Git, Jira o generación documental.

---

## Cambio #007 — Estabilización técnica y de la pantalla de validación

**Estado:** Implementado; validación local en curso  
**Rama:** `desarrollo`

### Implementado
- Corregido el import de JGit `FileHeader`.
- Cerrado correctamente `ObjectReader`.
- Contenido el diff dentro de su propia área de scroll.
- Conservado el estado Git durante la prueba temporal de IA.
- Manejo controlado de errores del proveedor IA.

### Validado
- `mvn clean test` obtuvo `BUILD SUCCESS` después de la corrección JGit.
- Aplicación Spring Boot levantada localmente.
- Inspección y comparación Git verificadas.

### Pendiente externo
- Validación real de Gemini con API key y contenido no corporativo.

---

## Cambio #008 — UX/UI, navegación y separación del MVP

**Estado:** Validación local parcial satisfactoria  
**Rama:** `desarrollo`

### Objetivo
Transformar la interfaz técnica inicial en la estructura visual del MVP orientada al equipo de desarrollo, separando las herramientas internas de diagnóstico del flujo funcional del producto.

### Implementado
- Inicio, sidebar, Nuevo análisis, Documentación, Historial y Configuración.
- Configuración Git reutilizable por el nuevo análisis.
- Herramienta técnica separada en `/technical-validation`.

### Validado
- `mvn clean test`: BUILD SUCCESS.
- Configuración Git local comprobada funcionalmente.

---

## Cambio #008.1 — Conectividad real y fuentes Git LOCAL/REMOTE

**Estado:** Implementado; pendiente de validación local y externa  
**Rama:** `desarrollo`

### Objetivo
Completar la infraestructura del POC para comprobar las fuentes reales antes de implementar el análisis Jira + Git y la generación del DT.

### Implementado
- Se conserva Git LOCAL mediante JGit.
- Se incorpora Git REMOTE mediante JGit, con clonación temporal, `fetch`, autenticación usuario/token y detección de ramas remotas.
- La configuración Git permite seleccionar LOCAL o REMOTE y comprobar el acceso antes de guardar.
- Jira realiza una prueba HTTP real contra el proyecto configurado mediante autenticación Basic usuario/token.
- Confluence realiza una prueba HTTP real contra el Space configurado, sin crear ni modificar páginas.
- Se mantienen respuestas controladas para autenticación, permisos, recurso inexistente y errores de conectividad.
- Gemini conserva su prueba real mediante `AiProvider`.
- Nuevo análisis identifica repositorios LOCAL/REMOTE y presenta sus ramas.
- Las credenciales no se incorporan a `application.yml` ni se registran en el historial del repositorio.

### Pendiente de validación
- Ejecutar `mvn clean test` después de actualizar la rama local.
- Probar Git REMOTE contra el repositorio personal del POC.
- Probar Jira corporativo con VPN/acceso correspondiente.
- Probar Gemini con credencial válida y contenido no corporativo.
- Probar Confluence y verificar acceso al Space configurado.

### Fuera de alcance
- Consulta funcional del Jira desde Nuevo análisis (#009).
- Generación del Documento Técnico.
- Publicación de páginas en Confluence.
- Adaptación de autenticación específica del Git corporativo.

---

## Forma de trabajo acordada

Antes de cada nuevo bloque de desarrollo se explicará el alcance, archivos y motivo; se esperará aprobación explícita; se implementará sin commit; después de la revisión se esperará aprobación explícita para el commit. Las ramas temporales, cuando sean necesarias, se nombrarán con el identificador del cambio (por ejemplo `tmp-008-1`) para conservar trazabilidad y evitar nombres ambiguos como `tmp-ignore`.
