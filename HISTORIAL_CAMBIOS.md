# Historial de cambios — Development AI Assistant

Este archivo registra los cambios funcionales y técnicos aprobados durante el desarrollo del proyecto.

## Cambio #001 — Modelo de información de fuente Git

**Estado:** Aplicado  
**Rama:** `desarrollo`  
**Commit de implementación:** `966798c2aafde2b051fcf68136775f7aa77206eb`

### Objetivo
Representar la información obtenida de una fuente Git seleccionada para análisis sin confundir este concepto con la creación de un repositorio o proyecto.

### Archivos
- Creado originalmente: `src/main/java/com/asimov/devaia/git/model/GitSourceInfo.java`
- Ruta actual después del Cambio #002: `src/main/java/com/dev/aiassistant/git/model/GitSourceInfo.java`

### Decisión de diseño
Se utiliza el nombre `GitSourceInfo` porque el objeto contiene información descubierta desde una fuente Git: ruta, nombre y ramas disponibles.

Se evita `GitRepositoryInfo` y `GitProjectInfo` porque esos nombres podían interpretarse como la creación o configuración de otro repositorio/proyecto.

### Resultado
Se establece el primer modelo del módulo Git. Este modelo será utilizado posteriormente por el servicio encargado de inspeccionar una fuente Git mediante JGit.

---

## Cambio #002 — Neutralización del package Java

**Estado:** Aplicado  
**Rama:** `desarrollo`

### Objetivo
Eliminar el nickname personal de las rutas y packages Java del proyecto y utilizar una nomenclatura neutral, entendible y reutilizable por el equipo.

### Archivos
- Movido: `src/main/java/com/asimov/devaia/DevAiAssistantApplication.java` → `src/main/java/com/dev/aiassistant/DevAiAssistantApplication.java`
- Movido: `src/main/java/com/asimov/devaia/git/model/GitSourceInfo.java` → `src/main/java/com/dev/aiassistant/git/model/GitSourceInfo.java`
- Actualizado: `HISTORIAL_CAMBIOS.md`

### Decisión de diseño
El package base queda establecido como `com.dev.aiassistant`. El nombre representa un asistente de inteligencia artificial orientado al desarrollo y evita relacionar la estructura interna del producto con una cuenta personal.

### Resultado
La aplicación y el módulo Git quedan bajo el package neutral `com.dev.aiassistant`. No se modifica comportamiento funcional.

### Commits del bloque aprobado
- `5d40e5c094de3f5de9032e422371160cffef06d4` — creación de la clase principal en el package neutral.
- `0fe43bfa6a503d7d45d86fc1e00f7b83f9d35715` — creación de `GitSourceInfo` en el package neutral.
- `267abd56b4eae6f108c1b020fa9106a535a6b103` — eliminación de la ruta personal anterior de la clase principal.
- `749cc2e631b0e7734edb941e4ba017d9ed3b26c8` — eliminación de la ruta personal anterior del modelo Git.

---

## Forma de trabajo acordada

Antes de cada nuevo bloque de desarrollo:

1. Se explicará el cambio propuesto, los archivos que se crearán o modificarán y el motivo.
2. Se esperará la aprobación del usuario.
3. Solo después de la aprobación se aplicarán los cambios y commits.
4. Se actualizará este historial con el resultado y los commits correspondientes.
