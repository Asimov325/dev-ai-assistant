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

**Estado:** Implementado; pendiente de validación local  
**Rama:** `desarrollo`

### Objetivo
Transformar la interfaz técnica inicial en la estructura visual del MVP orientada al equipo de desarrollo, separando las herramientas internas de diagnóstico del flujo funcional del producto.

### Implementado
- Nueva página de Inicio con acceso principal a generación documental y estado del entorno.
- Sidebar vertical responsive con Nuevo análisis, Inicio, Documentación, Historial y Configuración.
- Nueva pantalla de Nueva documentación preparada para el flujo Requerimiento → Código fuente → Documento Técnico.
- Nueva pantalla de Configuración para centralizar información reutilizable de Git e IA y preparar Jira/Confluence.
- Pantallas iniciales de Documentación e Historial con estados vacíos coherentes.
- Estilos reutilizables en `static/css/app.css` para mantener consistencia visual.
- La antigua pantalla de pruebas queda fuera del menú del MVP y disponible únicamente en `/technical-validation` como herramienta interna temporal.
- Eliminado el comentario visible “El análisis Git se ha conservado.” de la validación de IA.

### Decisiones UX/UI
- La ruta de repositorio y demás datos repetitivos pertenecen a Configuración, no al flujo diario.
- La rama del requerimiento sigue siendo una selección propia de cada análisis.
- Los diffs y pruebas directas de proveedor son herramientas técnicas y no forman parte de la navegación funcional.
- La validación técnica no es una dependencia del MVP y podrá deshabilitarse o eliminarse posteriormente.

### No incluido en este bloque
- Persistencia real de Configuración.
- Integración Jira.
- Generación real del DT desde el nuevo flujo.
- Persistencia de Documentación/Historial.
- Publicación Confluence.

### Pendiente de validación
- Ejecutar `mvn clean test` localmente.
- Levantar Spring Boot y revisar navegación, responsive y las rutas principales.
- Confirmar acceso manual a `/technical-validation` y que no aparece en el menú del MVP.

---

## Forma de trabajo acordada

Antes de cada nuevo bloque de desarrollo se explicará el alcance, archivos y motivo; se esperará aprobación explícita; después se aplicará un commit lógico y se actualizará este historial. Un bloque no se considera estable únicamente por estar implementado: debe compilar, ejecutar sus pruebas aplicables y registrar cualquier validación externa pendiente.
