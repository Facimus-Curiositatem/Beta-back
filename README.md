# Beta-back — Modulación de tareas pendientes
>
> **Alcance:** únicamente backend. El frontend queda fuera de esta etapa y el trabajo de CI/CD se presenta en un módulo independiente, asignado a la persona que ya está encargada de esa área.

## Objetivo

Completar la evolución del backend actual hacia una API REST versionada, segura y stateless, manteniendo temporalmente la aplicación MVC/Thymeleaf existente y reutilizando la misma capa de servicios y repositorios.

La arquitectura objetivo es una migración incremental:

```text
MVC / Thymeleaf existente ──┐
                            ├── Services ── Repositories ── Base de datos
API REST /api/v1/** ────────┘
```

No se debe duplicar lógica de negocio ni exponer entidades JPA directamente desde los controladores REST.

## Distribución principal — 5 personas

| Persona | Módulo | Responsabilidad principal | Entregable |
|---|---|---|---|
| **Persona 1** | Seguridad REST + JWT | Implementar autenticación stateless para `/api/v1/**` | Login REST, emisión y validación de JWT, principal autenticado y respuestas 401/403 |
| **Persona 2** | Normalización RESTful y contrato API | Definir y aplicar las convenciones HTTP transversales | API `/api/v1`, DTOs comunes, `ProblemDetail`, `PageResponse`, códigos y headers consistentes |
| **Persona 3** | Multitenancy y autorización | Garantizar aislamiento entre empresas y permisos por rol | Tenant derivado del principal, controles anti-IDOR y autorización centralizada |
| **Persona 4** | REST de Gestión | Exponer y completar los recursos del dominio de gestión | API de usuarios, procesos, roles, empresa e historial, alineada con el contrato común |
| **Persona 5** | REST de Modelado BPMN | Exponer y completar los recursos del dominio de modelado | API de pools, lanes, actividades, gateways, arcos, mensajes y correlaciones |

## Persona 1 — Seguridad REST, JWT y API stateless

### Objetivo

Reemplazar el uso de `HttpSession` como mecanismo de autenticación de la nueva API por Spring Security y Bearer JWT. La sesión actual puede mantenerse únicamente para los controladores MVC/Thymeleaf mientras dure la migración.

### Tareas

- Configurar `SecurityFilterChain` para `/api/v1/**`.
- Aplicar `SessionCreationPolicy.STATELESS` a la API REST.
- Implementar `ApiPrincipal` con los datos mínimos de identidad:
  - `usuarioId`;
  - `empresaId`;
  - `rol`.
- Implementar el servicio de creación y validación de JWT.
- Implementar el filtro de autenticación Bearer.
- Crear `POST /api/v1/auth/login`.
- Mantener BCrypt para la validación de contraseñas.
- Configurar secreto y expiración del JWT mediante variables de entorno.
- Diferenciar correctamente:
  - token ausente, inválido o expirado: `401 Unauthorized`;
  - usuario autenticado sin permisos: `403 Forbidden`.
- No implementar refresh tokens en esta etapa, salvo que aparezca una necesidad técnica justificada.
- Agregar pruebas de login, token válido, token inválido, token expirado, token ausente y permisos insuficientes.

### Resultado esperado

```text
POST /api/v1/auth/login
          ↓
         JWT
          ↓
Authorization: Bearer <token>
          ↓
Spring Security
          ↓
ApiPrincipal
```

La nueva API no depende de la sesión web y no contiene secretos hardcodeados.

## Persona 2 — Normalización RESTful y contrato `/api/v1`

### Objetivo

Definir el contrato HTTP común que deberán respetar los módulos de Gestión y Modelado.

### Tareas

- Versionar todos los endpoints REST bajo `/api/v1/**`.
- Usar sustantivos plurales y evitar rutas verbales cuando una modificación de recurso o subrecurso represente mejor la operación.
- Definir DTOs explícitos de request y response; no serializar entidades JPA.
- Incorporar Jakarta Bean Validation para validaciones sintácticas.
- Estandarizar los códigos de respuesta:
  - `200 OK` para consultas y actualizaciones con cuerpo;
  - `201 Created` y header `Location` para creaciones;
  - `204 No Content` para eliminaciones correctas;
  - `400 Bad Request` para solicitudes inválidas;
  - `401 Unauthorized` para autenticación faltante o inválida;
  - `403 Forbidden` para falta de permisos;
  - `404 Not Found` para recursos inexistentes o no visibles;
  - `409 Conflict` para conflictos de negocio.
- Adaptar el manejo de errores REST a `ProblemDetail`, compatible con RFC 9457.
- Crear un contrato estable `PageResponse<T>` en lugar de exponer `Page<T>` de Spring.
- Revisar la publicación de procesos y decidir entre:
  - `PATCH /api/v1/procesos/{id}` para modificar su estado; o
  - un subrecurso `publicacion`, si las reglas del dominio lo justifican.
- Revisar que `GET` sea seguro, `PUT` y `DELETE` sean idempotentes, y `PATCH` represente cambios parciales.
- Si OpenAPI es compatible con la versión real de Spring Boot, documentar el contrato sin bloquear la migración por esta dependencia.

### Entregables compartidos

- Convenciones de rutas y nombres.
- DTO común de paginación.
- Formato común de errores.
- Matriz de códigos HTTP.
- Criterios de validación de requests.
- Contrato base que usarán Personas 4 y 5.

## Persona 3 — Multitenancy, autorización e IDOR

### Objetivo

Garantizar que cada usuario solo pueda consultar o modificar información de su propia empresa y dentro de los permisos de su rol.

### Tareas

- Obtener siempre `empresaId` desde `ApiPrincipal`.
- Prohibir que el cliente seleccione el tenant mediante JSON, query params o URL.
- Reutilizar el patrón multiempresa existente y `RepositorioTenant<T>`.
- Auditar todos los accesos por identificador, incluidos:
  - usuarios;
  - procesos;
  - roles;
  - pools;
  - lanes;
  - actividades;
  - gateways;
  - arcos;
  - mensajes;
  - correlaciones.
- Validar pertenencia al tenant tanto del recurso solicitado como de los recursos relacionados.
- Evitar ataques IDOR: conocer un ID de otra empresa nunca debe permitir acceder a sus datos.
- Centralizar la autorización con Spring Security, según los roles reales del dominio, evitando validaciones dispersas en controladores.
- Definir una respuesta consistente para recursos de otro tenant sin revelar su existencia.
- Agregar pruebas de aislamiento Empresa A/Empresa B para lectura, creación, actualización y eliminación.
- Actualizar las reglas ArchUnit que correspondan sin retirar las protecciones arquitectónicas actuales.

### Resultado esperado

```text
JWT
 ↓
ApiPrincipal
 ↓
empresaId / usuarioId / rol
 ↓
Service
 ↓
RepositorioTenant
 ↓
Base de datos
```

Ninguna operación puede cambiar de empresa manipulando un identificador o el cuerpo de la petición.

## Persona 4 — REST del módulo Gestión

### Objetivo

Exponer el dominio de Gestión mediante la nueva API reutilizando los servicios existentes y el contrato definido por Persona 2.

### Alcance

- `Empresa`
- `Usuario`
- `Proceso`
- `RolProceso`
- `HistorialCambio`

### Tareas

- Crear o completar controladores REST, DTOs y mappers del módulo.
- Implementar las operaciones necesarias de usuarios: listar, crear, consultar, actualizar rol/estado y desactivar.
- Implementar las operaciones de procesos: listar, filtrar, paginar, crear, consultar, actualizar, publicar/cambiar estado y eliminar lógicamente.
- Mantener la trazabilidad mediante `HistorialCambio`.
- Implementar las operaciones necesarias de roles: listar, crear, consultar, actualizar y eliminar lógicamente.
- Exponer únicamente las operaciones de empresa necesarias para el dominio.
- Aplicar permisos, aislamiento por tenant, validación, códigos HTTP, `Location`, `ProblemDetail` y `PageResponse` definidos transversalmente.
- Mantener la separación:

```text
Controller → Service → Repository
```

- No acceder a repositorios desde controladores.
- No duplicar reglas existentes de los servicios.
- Agregar pruebas REST funcionales del módulo, incluidos filtros, paginación, soft delete y conflictos de negocio.

### Resultado esperado

El módulo Gestión queda disponible bajo `/api/v1/**`, conserva el comportamiento del dominio actual y puede coexistir con los controladores Thymeleaf.

## Persona 5 — REST del módulo Modelado BPMN

### Objetivo

Exponer todo el modelo BPMN mediante una API coherente, aprovechando las entidades, repositorios y servicios ya existentes.

### Alcance

- `Pool`
- `Lane`
- `Actividad`
- `Gateway`
- `Arco`
- `Mensaje`
- `Correlacion`

### Tareas

- Crear controladores REST, DTOs y mappers para todos los recursos del módulo.
- Definir las relaciones jerárquicas naturales sin usar nesting excesivo.
- Implementar operaciones de pools vinculadas a procesos.
- Implementar operaciones de lanes vinculadas a pools.
- Implementar actividades y gateways vinculados a lanes, incluidos `posX`, `posY` y tipo cuando corresponda.
- Implementar arcos con origen, destino, condición y etiqueta.
- Validar que origen y destino de un arco pertenezcan al proceso y tenant correctos.
- Implementar mensajes y determinar su relación correcta con procesos y pools según el modelo real.
- Implementar correlaciones y validar su cardinalidad con mensajes.
- Aplicar el contrato común de DTOs, errores, códigos HTTP y validaciones.
- Agregar pruebas REST funcionales y casos negativos de relaciones cruzadas o inválidas.

### Guía inicial de rutas

```text
GET  /api/v1/procesos/{procesoId}/pools
POST /api/v1/procesos/{procesoId}/pools
GET  /api/v1/pools/{poolId}

GET  /api/v1/pools/{poolId}/lanes
POST /api/v1/pools/{poolId}/lanes
GET  /api/v1/lanes/{laneId}

POST /api/v1/lanes/{laneId}/actividades
GET  /api/v1/actividades/{id}

POST /api/v1/lanes/{laneId}/gateways
GET  /api/v1/gateways/{id}
```

Las rutas de arcos, mensajes y correlaciones deben decidirse después de validar el padre natural en el modelo real; esta guía no sustituye esa revisión.

## Coordinación y dependencias

```text
Persona 1: identidad y autenticación
                 │
                 ▼
Persona 3: tenant y autorización

Persona 2: contrato REST transversal
        ├───────────────┐
        ▼               ▼
Persona 4           Persona 5
Gestión             Modelado BPMN
```

- Persona 1 debe estabilizar `ApiPrincipal` pronto para desbloquear a Persona 3.
- Persona 2 define el contrato común y lo coordina con Personas 4 y 5, pero no implementa sus reglas de negocio.
- Personas 4 y 5 pueden avanzar en paralelo usando dobles de autenticación mientras se estabiliza la seguridad.
- Persona 3 revisa transversalmente los endpoints construidos por Personas 4 y 5.
- Cada persona debe mantener pruebas de su módulo; la persona de CI/CD integra su ejecución y sus reportes en el pipeline.

## Criterios comunes de terminado

Una tarea de backend se considera terminada cuando:

- reutiliza servicios y repositorios existentes;
- no expone entidades JPA directamente;
- respeta `/api/v1/**` y las convenciones HTTP acordadas;
- deriva el tenant del usuario autenticado;
- tiene pruebas positivas, negativas y de autorización aplicables;
- no rompe el flujo MVC/Thymeleaf existente;
- mantiene las reglas ArchUnit;
- no introduce secretos en Git;
- pasa `mvn test` y `mvn clean verify`;
- actualiza la documentación solo con comportamiento ya implementado y verificado.

---

# Módulo separado — CI/CD y calidad

> Este módulo **no forma parte del reparto entre las cinco personas**. Pertenece exclusivamente a la persona que ya está encargada de CI/CD.

## Objetivo

Evolucionar el pipeline actual para integrar análisis SonarQube y un Quality Gate bloqueante, conservando JaCoCo, JUnit, ArchUnit y el Dockerfile existente.

## Principio de calidad

SonarQube no reemplaza a JaCoCo. La cadena esperada es:

```text
JUnit
  ↓
JaCoCo
  ↓
target/site/jacoco/jacoco.xml
  ↓
SonarQube
  ↓
Quality Gate
```

JaCoCo mide y genera la cobertura; SonarQube importa el reporte XML y lo combina con análisis de bugs, vulnerabilidades, duplicación y mantenibilidad.

## Trabajo pendiente

### 1. Auditar el pipeline existente

- Revisar `.github/workflows/ci.yml` antes de modificarlo.
- Conservar los jobs actuales de build, tests, guardas de arquitectura y Docker.
- Identificar dependencias, condiciones y matrices Linux/Windows existentes.
- Evitar duplicar innecesariamente el mismo análisis Sonar en cada sistema operativo.

### 2. Preparar Maven y JaCoCo

- Mantener `jacoco-maven-plugin`.
- Garantizar que `mvn clean verify` genere:

```text
target/site/jacoco/jacoco.xml
```

- Verificar que el reporte XML incluya los módulos y paquetes relevantes.
- Mantener JUnit y ArchUnit como controles independientes.
- Configurar SonarScanner for Maven con una versión compatible con el proyecto.
- No fijar versiones arbitrarias sin revisar primero el `pom.xml` y la compatibilidad real.

### 3. Integrar SonarQube

- Configurar el identificador y nombre del proyecto Sonar.
- Configurar la ruta del reporte XML de JaCoCo.
- Ejecutar el análisis después de compilar y probar el código.
- Usar exclusivamente secretos del repositorio:
  - `SONAR_TOKEN`;
  - `SONAR_HOST_URL`.
- No guardar tokens, URLs sensibles ni credenciales en el repositorio o Dockerfile.

### 4. Configurar el Quality Gate

Aplicar el control principalmente sobre **código nuevo**, con objetivos recomendados:

| Métrica de código nuevo | Umbral recomendado |
|---|---:|
| Bugs | 0 |
| Vulnerabilidades | 0 |
| Cobertura | ≥ 80 % |
| Líneas duplicadas | < 3 % |

No imponer inicialmente un umbral global artificial que bloquee el proyecto por deuda heredada. Los security hotspots deben revisarse según la política acordada en SonarQube.

### 5. Orden y dependencias del pipeline

Evolucionar el flujo hacia:

```text
Build & Test
      ↓
Architecture Guard
      ↓
JaCoCo XML
      ↓
SonarQube Analysis
      ↓
Quality Gate
      ↓
Docker Build
```

- El Docker build debe ejecutarse solo cuando las verificaciones anteriores sean satisfactorias.
- El job del Quality Gate debe esperar el resultado real del servidor SonarQube.
- Un Quality Gate fallido debe marcar el workflow como fallido.
- Mantener el Dockerfile multi-stage y verificar que el contenedor siga ejecutándose con usuario no root.

### 6. Triggers y protección de ramas

- Revisar los triggers de `push` y `pull_request`.
- Incluir la rama real de migración REST, prevista como `feature/migracion-rest`, si ese es finalmente el nombre usado en el repositorio.
- Alinear el workflow con las ramas que realmente existan; no asumir que `develop` existe.
- Configurar el check de CI/Quality Gate como obligatorio para merge cuando la administración del repositorio lo permita.
- No hacer merge automático ni force push.

### 7. Verificación final

Comprobar y registrar:

- cantidad de tests ejecutados y fallos;
- resultado de `mvn test`;
- resultado de `mvn clean verify`;
- ejecución correcta de ArchUnit;
- generación de `target/site/jacoco/jacoco.xml`;
- importación de cobertura en SonarQube;
- resultado real del Quality Gate;
- comportamiento del pipeline ante un Quality Gate fallido;
- ejecución condicional del Docker build;
- resultado de `docker build -t facimus/procesos-back:test .`;
- ausencia de secretos en archivos versionados;
- ejecución del workflow en la rama de migración y en un pull request.

No se debe afirmar que SonarQube o GitHub Actions están en verde sin evidencia de una ejecución real.

## Entregables de CI/CD

- `pom.xml` ajustado para JaCoCo XML y SonarScanner, si corresponde.
- `.github/workflows/ci.yml` actualizado sin eliminar las verificaciones actuales.
- Quality Gate creado y asociado al proyecto SonarQube.
- Secrets requeridos documentados y configurados fuera de Git.
- Evidencia de una ejecución exitosa y de una ejecución bloqueada deliberadamente por el Quality Gate.
- Documentación de comandos locales de prueba, cobertura, análisis y Docker.

## Criterios de aceptación de CI/CD

- `mvn clean verify` genera un XML de cobertura válido.
- SonarQube muestra la cobertura importada desde JaCoCo.
- El Quality Gate bloquea el pipeline cuando no se cumple.
- El Docker build no se ejecuta si falla una verificación previa.
- El pipeline se activa en las ramas y eventos acordados.
- JUnit, ArchUnit y JaCoCo continúan funcionando.
- No hay secretos hardcodeados.
- Existe evidencia reproducible del pipeline completo.

## Fuera de alcance

- Implementación de frontend o Angular.
- Integración visual o consumo de la API desde frontend.
- Docker o despliegue del frontend.
- Pruebas E2E de interfaz.
- Sustitución de JaCoCo o ArchUnit por SonarQube.
- Refresh tokens, salvo decisión técnica posterior justificada.

## Nota de validación

Esta modulación se basa en el mapeo previo de Beta-back. Antes de iniciar cada módulo se deben contrastar rutas, paquetes, ramas y versiones con el estado actual del repositorio, ya que pueden haber cambiado desde el último levantamiento.
