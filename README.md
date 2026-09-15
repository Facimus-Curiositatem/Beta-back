<<<<<<< HEAD
# Beta-back — Plan de Migración RESTful

## 1. Objetivo

El objetivo de esta migración es convertir `Beta-back` en un backend exclusivamente RESTful.

La arquitectura final no utilizará:

- Thymeleaf;
- controllers MVC para renderizar HTML;
- templates;
- `HttpSession`;
- autenticación basada en sesión;
- interceptores destinados a navegación web;
- lógica de presentación en el backend.

Toda comunicación con clientes se realizará mediante:

```text
HTTP + JSON
```

a través de una API versionada:

```text
/api/v1/**
```

La arquitectura objetivo será:

```text
Cliente
   |
HTTP + JSON
   |
/api/v1/**
   |
Spring Security
   |
Bearer JWT
   |
ApiPrincipal
   |
REST Controllers
   |
Request / Response DTOs
   |
Services
   |
Repositories
   |
JPA
   |
PostgreSQL / H2
```

---

## 2. Decisiones arquitectónicas definitivas

| Área | Decisión |
|---|---|
| Arquitectura backend | Backend exclusivamente RESTful |
| API | `/api/v1/**` |
| Formato | HTTP + JSON |
| Presentación | No existe presentación HTML en backend |
| Thymeleaf | Se elimina |
| Controllers MVC | Se eliminan |
| HttpSession | Se elimina de la API |
| Seguridad | Spring Security |
| Autenticación | Bearer JWT |
| Estado | API stateless |
| Identidad | `ApiPrincipal` |
| Multitenancy | `empresaId` obtenido desde el principal autenticado |
| Contratos | Request DTO + Response DTO |
| Persistencia | JPA / Hibernate |
| Errores | `ProblemDetail` |
| Cobertura | JaCoCo |
| Análisis de calidad | SonarQube |
| Arquitectura | ArchUnit |

---

## 3. Componentes del backend que se conservan

La migración no implica reconstruir el dominio desde cero.

Se conservarán principalmente:

```text
Model
Repository
Service
```

Es decir:

```text
gestion/
├── model/
├── repository/
└── service/

modelado/
├── model/
├── repository/
└── service/
```

Estos componentes seguirán conteniendo:

- entidades JPA;
- repositorios;
- multitenancy;
- reglas de negocio;
- operaciones de dominio;
- persistencia.

---

## 4. Componentes que reemplazan la arquitectura anterior

La capa web anterior:

```text
Controller MVC
Thymeleaf
HttpSession
SesionActiva
AutenticacionInterceptor
VistaGlobalAdvice
Templates
```

será reemplazada por:

```text
REST Controller
Request DTO
Response DTO
Mapper
Spring Security
JWT
ApiPrincipal
ProblemDetail
```

---

## 5. Arquitectura propuesta

```text
com.facimus.procesos
|
+-- common/
|   |
|   +-- api/
|       +-- ApiExceptionHandler
|       +-- PageResponse
|
+-- security/
|   +-- ApiPrincipal
|   +-- JwtService
|   +-- JwtAuthenticationFilter
|   +-- SecurityConfig
|
+-- gestion/
|   +-- model/
|   +-- repository/
|   +-- service/
|   +-- controller/
|       +-- rest/
|           +-- dto/
|           |   +-- request/
|           |   +-- response/
|           |
|           +-- mapper/
|
+-- modelado/
    +-- model/
    +-- repository/
    +-- service/
    +-- controller/
        +-- rest/
            +-- dto/
            |   +-- request/
            |   +-- response/
            |
            +-- mapper/
```

---

## 6. API REST versionada

Todos los endpoints públicos deben ubicarse bajo:

```text
/api/v1/**
```

Ejemplos:

```text
/api/v1/auth
/api/v1/usuarios
/api/v1/procesos
/api/v1/roles
/api/v1/pools
/api/v1/lanes
/api/v1/actividades
/api/v1/gateways
/api/v1/arcos
/api/v1/mensajes
/api/v1/correlaciones
```

No deben permanecer rutas funcionales bajo:

```text
/api/**
```

sin versionado cuando se complete la migración.

---

## 7. Seguridad final

La API será completamente stateless.

Se debe configurar:

```java
SessionCreationPolicy.STATELESS
```

No se utilizará:

```text
HttpSession
```

para almacenar:

```text
empresaId
usuarioId
rol
nombreUsuario
nombreEmpresa
```

La identidad se obtendrá del Bearer JWT.

Flujo:

```text
Request
   |
Authorization: Bearer <JWT>
   |
JwtAuthenticationFilter
   |
Spring Security
   |
ApiPrincipal
   |
usuarioId
empresaId
rol
   |
REST Controller
   |
Service
```

---

## 8. Login REST

Endpoint:

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "email": "usuario@empresa.com",
  "password": "password"
}
```

Respuesta:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "usuario": {
    "id": 1,
    "nombre": "Usuario",
    "email": "usuario@empresa.com",
    "rol": "ADMINISTRADOR"
  }
}
```

---

## 9. JWT

El JWT debe contener únicamente claims necesarios:

```text
usuarioId
empresaId
rol
```

Los secretos JWT deberán manejarse mediante variables de entorno.

Nunca deben almacenarse directamente en Git.

---

## 10. Multitenancy

El cliente nunca selecciona el tenant.

No debe aceptarse como fuente confiable:

```json
{
  "empresaId": 10
}
```

El flujo correcto será:

```text
JWT
 |
ApiPrincipal
 |
empresaId
 |
Service
 |
RepositorioTenant
 |
Database
```

Todos los recursos identificados por ID deben comprobar pertenencia al tenant autenticado.

---

## 11. Prevención de IDOR

Se deben proteger recursos como:

```text
/api/v1/usuarios/{id}
/api/v1/procesos/{id}
/api/v1/roles/{id}
/api/v1/pools/{id}
/api/v1/lanes/{id}
/api/v1/actividades/{id}
/api/v1/gateways/{id}
/api/v1/arcos/{id}
/api/v1/mensajes/{id}
/api/v1/correlaciones/{id}
```

Un usuario de Empresa A nunca debe poder acceder a información perteneciente a Empresa B modificando un identificador.

---

## 12. DTOs

Las entidades JPA no deben ser expuestas directamente.

Flujo obligatorio:

```text
JSON
 |
Request DTO
 |
REST Controller
 |
Service
 |
Entity
 |
Mapper
 |
Response DTO
 |
JSON
```

---

## 13. Manejo de errores

La API deberá utilizar:

```text
ProblemDetail
```

compatible con RFC 9457.

Ejemplo:

```json
{
  "type": "about:blank",
  "title": "Recurso no encontrado",
  "status": 404,
  "detail": "No existe el proceso solicitado",
  "instance": "/api/v1/procesos/99"
}
```

Mapeo esperado:

| Situación | HTTP |
|---|---|
| Request inválido | `400` |
| Sin autenticación | `401` |
| Sin permisos | `403` |
| Recurso inexistente | `404` |
| Regla de negocio | `409` |

---

## 14. Paginación

No se debe exponer directamente:

```java
Page<T>
```

como contrato público.

Se utilizará un DTO propio:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 50,
  "totalPages": 5
}
```

Por ejemplo:

```java
PageResponse<ProcesoResponse>
```

---

## 15. Semántica RESTful

### Creación

```http
POST /api/v1/procesos
```

Respuesta:

```http
201 Created
Location: /api/v1/procesos/{id}
```

### Consulta

```http
GET /api/v1/procesos/{id}
```

### Actualización completa

```http
PUT /api/v1/procesos/{id}
```

### Actualización parcial

```http
PATCH /api/v1/procesos/{id}
```

### Eliminación

```http
DELETE /api/v1/procesos/{id}
```

Respuesta:

```http
204 No Content
```

---

## 16. Publicación de procesos

Actualmente existe una operación similar a:

```text
POST /api/procesos/{id}/publicar
```

Debe revisarse para evitar una ruta basada directamente en un verbo.

La opción preferida es:

```http
PATCH /api/v1/procesos/{id}
```

con:

```json
{
  "estado": "PUBLICADO"
}
```

si las reglas del dominio permiten modelarlo como transición de estado.

---

## 17. Eliminación de Thymeleaf y arquitectura MVC

Una vez existan los endpoints REST equivalentes y hayan sido verificados, se deberá retirar completamente la arquitectura anterior.

### Eliminar templates

Eliminar:

```text
src/main/resources/templates/
```

incluyendo cualquier vista relacionada con:

```text
empresas
procesos
roles
sesion
usuarios
fragments
error
```

### Eliminar dependencia Thymeleaf

Retirar del `pom.xml` cualquier dependencia como:

```text
spring-boot-starter-thymeleaf
```

si todavía se encuentra presente.

### Eliminar controllers MVC

Eliminar cualquier controller cuya función sea renderizar vistas.

### Eliminar infraestructura de sesión web

Retirar cuando ya no tenga consumidores:

```text
SesionActiva
AutenticacionInterceptor
VistaGlobalAdvice
WebConfig relacionado con navegación MVC
```

### Eliminar DTOs exclusivos de formularios HTML

Revisar DTOs como:

```text
LoginForm
ProcesoForm
RegistroEmpresaForm
RolProcesoForm
UsuarioForm
```

y eliminarlos si solo eran utilizados por Thymeleaf.

---

## 18. Reglas de arquitectura

ArchUnit deberá reflejar la arquitectura final.

Permitido:

```text
REST Controller -> Service
Service -> Repository
```

Prohibido:

```text
REST Controller -> Repository
```

También debe validarse:

- entidades multiempresa;
- acceso tenant-aware;
- dependencias entre `gestion` y `modelado`;
- security desacoplado de repositories;
- controllers REST sin lógica de persistencia.

---

## 19. Distribución de tareas pendientes

La migración backend restante se divide entre 5 personas.

No se incluye:

- frontend;
- Angular;
- CI/CD.

Esos componentes están fuera de esta distribución.

---

## Persona 1 — Seguridad REST, JWT y API Stateless

### Objetivo

Eliminar completamente la autenticación basada en sesión e implementar seguridad REST.

### Tareas

- implementar `SecurityFilterChain`;
- configurar `SessionCreationPolicy.STATELESS`;
- implementar `JwtService`;
- implementar `JwtAuthenticationFilter`;
- implementar `ApiPrincipal`;
- migrar login a JWT;
- eliminar dependencia de `HttpSession`;
- eliminar uso de `SesionActiva` dentro de REST;
- retirar `AutenticacionInterceptor` cuando quede obsoleto;
- diferenciar correctamente `401` y `403`;
- mantener BCrypt;
- manejar secret JWT mediante variables de entorno;
- revisar permisos por rol.

### Resultado esperado

```text
Request
 ↓
JWT
 ↓
Spring Security
 ↓
ApiPrincipal
 ↓
REST Controller
```

---

## Persona 2 — Normalización RESTful y contrato API

### Objetivo

Dejar toda la API bajo un estándar REST coherente.

### Tareas

- migrar `/api/**` a `/api/v1/**`;
- revisar naming de recursos;
- eliminar endpoints verbales innecesarios;
- implementar `ProblemDetail`;
- retirar `ErrorResponse` cuando deje de ser necesario;
- crear `PageResponse<T>`;
- eliminar exposición pública de `Page<T>`;
- agregar `Location` en creaciones;
- revisar `201`, `204`, `400`, `404`, `409`;
- revisar consistencia de Request/Response DTOs;
- auditar rutas de Gestión y Modelado;
- documentar contrato final con OpenAPI.

### Resultado esperado

Una API estable, versionada y completamente RESTful.

---

## Persona 3 — Multitenancy, autorización e IDOR

### Objetivo

Garantizar aislamiento entre empresas y autorización centralizada.

### Tareas

- obtener `empresaId` desde `ApiPrincipal`;
- obtener `usuarioId` desde `ApiPrincipal`;
- obtener rol desde `ApiPrincipal`;
- eliminar dependencias de tenant basadas en sesión;
- verificar todos los accesos por ID;
- prevenir IDOR;
- revisar repositories tenant-aware;
- asegurar `findBy...AndEmpresaId`;
- centralizar autorización por roles;
- revisar permisos de:
  - administrador;
  - editor;
  - lector;
- comprobar relaciones entre recursos del mismo tenant;
- validar que ningún request pueda seleccionar manualmente empresa.

### Resultado esperado

```text
JWT
 ↓
ApiPrincipal
 ↓
empresaId
 ↓
Service
 ↓
RepositorioTenant
```

---

## Persona 4 — REST del módulo Gestión

### Objetivo

Cerrar completamente la API de Gestión.

### Recursos

```text
Empresa
Usuario
Proceso
RolProceso
HistorialCambio
```

### Tareas

#### Usuarios

- revisar CRUD;
- desactivación;
- DTOs;
- permisos;
- respuestas HTTP.

#### Procesos

- revisar CRUD;
- filtros;
- paginación;
- historial;
- publicación;
- cambio de estado;
- soft delete.

#### Roles

- revisar CRUD;
- eliminación lógica;
- uso dentro de procesos.

#### Empresa

- revisar endpoints necesarios;
- eliminar operaciones relacionadas únicamente con páginas HTML.

#### Limpieza

- eliminar controllers MVC del módulo;
- eliminar formularios Thymeleaf que queden obsoletos;
- asegurar:

```text
Controller -> Service -> Repository
```

### Resultado esperado

Todo `gestion` expuesto únicamente mediante `/api/v1/**`.

---

## Persona 5 — REST del módulo Modelado BPMN

### Objetivo

Cerrar y normalizar la API de modelado.

### Recursos

```text
Pool
Lane
Actividad
Gateway
Arco
Mensaje
Correlacion
```

### Tareas

#### Pool

- revisar CRUD;
- relación con Proceso;
- tenant.

#### Lane

- revisar CRUD;
- relación con Pool;
- relación con RolProceso.

#### Actividad

- revisar CRUD;
- posición;
- relación con Lane.

#### Gateway

- revisar CRUD;
- tipo;
- posición.

#### Arco

- revisar origen;
- destino;
- condición;
- etiqueta;
- consistencia de tenant y proceso.

#### Mensaje

- revisar relaciones;
- origen y destino;
- proceso relacionado.

#### Correlación

- revisar relación con Mensaje;
- cardinalidad;
- contrato REST.

#### Arquitectura

- revisar nesting;
- reducir URLs excesivamente profundas;
- mantener Services existentes;
- eliminar cualquier dependencia de presentación anterior.

### Resultado esperado

Todo el módulo BPMN accesible únicamente mediante REST.

---

## 20. Dependencias entre tareas

```text
Persona 1
Seguridad + JWT
     |
     v
Persona 3
Multitenancy + autorización
```

En paralelo:

```text
Persona 2
Contrato REST transversal

Persona 4
Gestión

Persona 5
Modelado BPMN
```

La coordinación será:

```text
Persona 1 -> identidad
Persona 2 -> estándar HTTP/REST
Persona 3 -> seguridad de datos
Persona 4 -> dominio Gestión
Persona 5 -> dominio BPMN
```

---

## 21. Orden recomendado

### Bloque 1

Persona 1:

- Security;
- JWT;
- stateless;
- ApiPrincipal.

Persona 2:

- `/api/v1`;
- ProblemDetail;
- PageResponse;
- contrato HTTP.

### Bloque 2

Persona 3:

- tenant desde principal;
- autorización;
- IDOR.

### Bloque 3

Persona 4:

- cierre Gestión.

Persona 5:

- cierre Modelado.

### Bloque 4

Limpieza final:

- eliminar Thymeleaf;
- eliminar templates;
- eliminar MVC;
- eliminar sesión;
- eliminar clases obsoletas;
- actualizar documentación.

---

## 22. Definition of Done

La migración REST se considera terminada cuando:

- no existen templates Thymeleaf;
- no existen controllers MVC;
- no existe autenticación por `HttpSession`;
- `/api/v1/**` es la única API pública;
- Spring Security utiliza JWT;
- la API es stateless;
- existe `ApiPrincipal`;
- el tenant se deriva del principal;
- no se serializan Entities directamente;
- se utiliza `ProblemDetail`;
- existe `PageResponse`;
- los POST devuelven `201 + Location` cuando corresponde;
- DELETE devuelve `204`;
- todos los recursos respetan aislamiento multiempresa;
- ArchUnit refleja la arquitectura REST final;
- OpenAPI documenta el contrato final.

---

## 23. Arquitectura final esperada

```text
                    BETA-BACK
                        |
                    /api/v1/**
                        |
                 Spring Security
                        |
                   Bearer JWT
                        |
                  ApiPrincipal
                        |
              REST Controllers
                        |
              Request/Response DTO
                        |
                     Mapper
                        |
                    Services
                        |
                  Repositories
                        |
                     JPA
                        |
              PostgreSQL / H2
```

No existirán en la arquitectura final:

```text
Thymeleaf
MVC Views
Templates
HttpSession
SesionActiva
Controllers de presentación
```

---

## 24. Decisión final

`Beta-back` será un backend exclusivamente RESTful.

La migración conservará:

```text
Model
Repository
Service
```

y sustituirá completamente la capa web anterior por:

```text
REST Controller
DTO
Mapper
Spring Security
JWT
ApiPrincipal
ProblemDetail
```

Toda comunicación externa se realizará mediante HTTP + JSON a través de:

```text
/api/v1/**
```
=======
# Beta-back

Backend del **Sistema de Gestion de Procesos Multiempresa**, desarrollado
como parte del curso de Desarrollo Web. Se trata de una aplicacion web
construida con **Spring Boot 4.1, Thymeleaf y JPA** que implementa un modelo
de dominio completo (13 entidades, 4 enums) con logica de negocio en capas
y vistas server-side.

El proyecto sigue la arquitectura consolidada definida por el equipo (rama
`feature/consolidado-arquitectura` de la wiki del proyecto) y cubre las 28
historias de usuario del enunciado.

## Requisitos previos

- JDK 21 (Temurin recomendado).
- Maven no requiere instalacion: el proyecto incluye su propio wrapper
  (`mvnw` / `mvnw.cmd`).
- Docker (opcional, necesario unicamente para el despliegue en contenedores).

## Ejecucion local

```bash
./mvnw spring-boot:run
```

En Windows (cmd/PowerShell):

```bash
mvnw.cmd spring-boot:run
```

La aplicacion queda disponible en `http://localhost:8080` y redirige
automaticamente a `/login`.

**Usuario de demostracion** (creado automaticamente en el primer arranque
por `DatosDemoInitializer`):

| Campo | Valor |
|---|---|
| Correo | `admin@demo.com` |
| Contrasena | `admin123` |

Los datos se almacenan en una base H2 embebida en archivo
(`./data/procesos.mv.db`, excluida de git). Para reiniciar el estado de la
base de datos, se debe detener la aplicacion y eliminar la carpeta `data/`.

## Ejecucion con Docker

La imagen se construye con un Dockerfile multi-stage que compila el
proyecto, ejecuta los tests y genera un JAR ligero sobre JRE 21.

```bash
docker build -t facimus/procesos-back .
```

Para ejecutar el contenedor conectado a una base de datos PostgreSQL
externa se debe activar el perfil `prod` mediante variables de entorno:

```bash
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=<ip-base-datos> \
  -e DB_PORT=5432 \
  -e DB_NAME=procesos \
  -e DB_USER=procesos \
  -e DB_PASSWORD=<contrasena> \
  facimus/procesos-back
```

Sin el perfil `prod`, la aplicacion utiliza H2 embebida por defecto.

## Arquitectura

El proyecto adopta un empaquetado modular por dominio organizado en dos
bloques, tal como lo establece el enunciado del curso:

```
com.facimus.procesos
├── common/       EntidadEmpresa, excepciones, RepositorioTenant<T>
├── config/       Sesion, interceptor de autenticacion, datos de demo
├── gestion/      Empresa, Usuario, Proceso, HistorialCambio, RolProceso
│   ├── model/  repository/  service/  controller/
└── modelado/     Pool, Lane, NodoFlujo (Actividad/Gateway), Arco, Mensaje, Correlacion
    ├── model/  repository/  service/
```

Las decisiones de arquitectura que rigen el proyecto se encuentran
documentadas en detalle en [`docs/guia-tecnica.md`](docs/guia-tecnica.md).
A continuacion se resumen las principales:

- **Multi-tenencia**: toda entidad que pertenece a una empresa extiende
  `EntidadEmpresa` (columna `empresa_id`, `updatable = false`). Los
  repositorios exponen metodos `*AndEmpresaId` y acotan cada consulta por
  el `empresaId` almacenado en la sesion.
- **Autenticacion (Entrega 1)**: se implementa mediante `HttpSession`
  (`empresaId`, `usuarioId`, `rolAcceso`). Spring Security se incorporara
  en la Entrega final; actualmente solo se utiliza `spring-security-crypto`
  para el cifrado de contrasenas con BCrypt.
- **Validacion de negocio**: se realiza en la capa de servicio antes de
  persistir. Las violaciones lanzan `ReglaNegocioException`, capturada por
  un `@ControllerAdvice` global que la traduce en un mensaje visible para el
  usuario.
- **Herencia JPA**: `NodoFlujo` utiliza la estrategia `SINGLE_TABLE` con
  columna discriminadora `tipo_nodo`. Sus unicos subtipos son `Actividad` y
  `Gateway`.
- **Eliminacion logica**: `Proceso` y `RolProceso` disponen de un campo
  `activo`; no se ejecutan operaciones `DELETE` fisicas sobre estas
  entidades con el fin de preservar la trazabilidad.

## Alcance de la Entrega 1

La primera entrega comprende 22 rutas Thymeleaf correspondientes al bloque
de **gestion**: sesion, registro de empresa, administracion de usuarios,
gestion de procesos y roles de proceso.

| Modulo | Historias de usuario | Rutas |
|---|---|---|
| Empresas y usuarios | HU-01, HU-02, HU-03 | `/empresas/registro`, `/usuarios/**`, `/login` |
| Procesos | HU-04 a HU-07 | `/procesos/**` |
| Roles de proceso | HU-17 a HU-20 | `/roles/**` |

Las historias HU-08 a HU-16 y HU-21 a HU-28 (modelado del diagrama) cuentan
con su logica de dominio y servicios ya implementados en `modelado/service/`,
preparados para ser expuestos mediante API REST en la Entrega 2.

## Pruebas

El proyecto cuenta con una suite de 30 tests organizados en tres categorias:

```bash
# Ejecutar todos los tests
./mvnw test

# Solo tests de arquitectura (ArchUnit)
./mvnw test -Dtest="MultitenenciaTest,EmpaquetadoTest,HerenciaJpaTest,RepositorioTenantTest"

# Solo tests unitarios de servicios
./mvnw test -Dtest="EmpresaServiceTest,UsuarioServiceTest,ProcesoServiceTest,RolProcesoServiceTest"
```

| Categoria | Tests | Que valida |
|---|---|---|
| Contexto de Spring | 1 | La aplicacion arranca con las 13 entidades y 11 repositorios |
| Arquitectura (ArchUnit) | 12 | Empaquetado modular, multi-tenencia, herencia JPA, enums STRING, separacion controller-service-repository |
| Unitarios de servicios | 17 | Logica de negocio de EmpresaService, UsuarioService, ProcesoService y RolProcesoService |

Los tests de arquitectura garantizan que las decisiones del consolidado se
cumplan en cada push. Si un colaborador introduce una violacion (por ejemplo,
un enum mapeado como `ORDINAL` o un repositorio que no extiende
`RepositorioTenant`), el pipeline de CI rechaza el cambio automaticamente.

## Integracion continua

El repositorio dispone de un pipeline de GitHub Actions
(`.github/workflows/ci.yml`) que se ejecuta en cada push y pull request:

| Job | Descripcion |
|---|---|
| **Build & Test** | Compila y ejecuta los 30 tests en Ubuntu y Windows con JDK 21. Genera reportes de tests y cobertura JaCoCo. |
| **Architecture Guard** | Ejecuta exclusivamente los 12 tests de ArchUnit y genera un reporte separado de reglas de arquitectura. |
| **Docker Build** | Construye la imagen Docker y verifica que el contenedor arranca correctamente. |

## Documentacion adicional

- [`docs/guia-tecnica.md`](docs/guia-tecnica.md) — Guia tecnica completa
  del proyecto: decisiones de arquitectura, inventario de tests, pipeline
  CI/CD, despliegue en Docker, topologia de VMs y guia para contribuir.
>>>>>>> 1c29e0a2dd30b4714cfbf5be2fc11bd0649a074f
