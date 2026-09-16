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

### Observaciones trasladadas a #008.2
- La prueba Git REMOTE resultaba lenta porque clonaba el repositorio completo.
- Las configuraciones se mantenían solo durante la ejecución y podían perder estado visual.
- Inicio no reflejaba correctamente todas las integraciones configuradas.
- Gemini dependía de variable de entorno.
- Faltaba un flujo uniforme de probar → guardar → mostrar configuración.

---

## Cambio #008.2 — Estabilización de configuración e integraciones
**Estado:** Implementado en `tmp-008-2`; pendiente de compilación y validación funcional local

### Objetivo
Dejar Configuración como base estable del MVP antes de incorporar el análisis Jira + Git, evitando pérdida de configuración, exposición de credenciales y operaciones Git REMOTE innecesariamente pesadas.

### Implementado
- Git REMOTE valida URL/autenticación/ramas con `ls-remote`, sin clonar el repositorio durante `Probar conexión`.
- Configuración persistente fuera del repositorio en `~/.development-ai-assistant`.
- Credenciales locales protegidas con AES/GCM y clave local separada.
- Git, Jira, Gemini y Confluence mantienen configuración independiente.
- Flujo uniforme: probar conexión → habilitar Guardar → configuración activa.
- La operación Guardar no repite la conexión externa ya validada.
- Las configuraciones activas se muestran mediante cards y dejan de exponer formularios editables.
- Para cambiar una configuración se elimina/desactiva la actual y se vuelve a validar la nueva antes de guardarla.
- Los tokens nunca se devuelven como campos ocultos al navegador.
- Gemini puede recibir la API key desde Configuración y conserva como fallback la configuración externa existente.
- Confluence puede reutilizar URL, usuario y token de Jira cuando ambos pertenecen al mismo sitio Atlassian, manteniendo Space independiente.
- Inicio refleja el estado real de Git, Jira, Gemini y Confluence.

### Pendiente de validación antes de aprobar commit
- `mvn clean test`.
- Reiniciar la aplicación y comprobar que las configuraciones persisten.
- Git LOCAL: probar, guardar y visualizar card.
- Git REMOTE: confirmar que `Probar conexión` es sensiblemente más rápido y guardar no vuelve a consultar la red.
- Jira: probar, guardar, reiniciar y verificar estado/card.
- Gemini: ingresar API key desde UI, probar, guardar y reiniciar.
- Confluence: validar tanto credencial Atlassian compartida como configuración independiente según disponibilidad.
- Confirmar que Inicio refleja inmediatamente los cuatro estados.

### Fuera de alcance
- Consulta funcional del Jira desde Nuevo análisis (#009).
- Generación del Documento Técnico (#010).
- Publicación real en Confluence.

---

## Forma de trabajo acordada
Antes de cada nuevo bloque de desarrollo se explicará el alcance, archivos y motivo; se esperará aprobación explícita; se implementará en una rama temporal identificada con el cambio; después de la validación se esperará aprobación explícita para integrar el cambio definitivo en `desarrollo`.
