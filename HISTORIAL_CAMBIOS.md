# Historial de cambios — Development AI Assistant

Este archivo registra los cambios funcionales y técnicos aprobados durante el desarrollo del proyecto.

## Cambio #001 — Modelo de información de fuente Git

**Estado:** Aplicado  
**Rama:** `desarrollo`  
**Commit de implementación:** `966798c2aafde2b051fcf68136775f7aa77206eb`

### Objetivo
Representar la información obtenida de una fuente Git seleccionada para análisis sin confundir este concepto con la creación de un repositorio o proyecto.

### Archivos
- Creado: `src/main/java/com/asimov/devaia/git/model/GitSourceInfo.java`

### Decisión de diseño
Se utiliza el nombre `GitSourceInfo` porque el objeto contiene información descubierta desde una fuente Git: ruta, nombre y ramas disponibles.

Se evita `GitRepositoryInfo` y `GitProjectInfo` porque esos nombres podían interpretarse como la creación o configuración de otro repositorio/proyecto.

### Resultado
Se establece el primer modelo del módulo Git. Este modelo será utilizado posteriormente por el servicio encargado de inspeccionar una fuente Git mediante JGit.

---

## Forma de trabajo acordada

Antes de cada nuevo bloque de desarrollo:

1. Se explicará el cambio propuesto, los archivos que se crearán o modificarán y el motivo.
2. Se esperará la aprobación del usuario.
3. Solo después de la aprobación se aplicarán los cambios y commits.
4. Se actualizará este historial con el resultado y los commits correspondientes.
