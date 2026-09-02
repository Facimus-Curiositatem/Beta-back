# Beta-back


BETA — Arquitectura RESTful, Angular y plan de trabajo del equipo

Proyecto: Beta — Sistema de Gestión de Procesos Multiempresa
Backend: Spring Boot 4.1 / Java 21
Frontend: Angular
Calidad: JUnit + ArchUnit + JaCoCo + SonarQube
Repositorio backend: Facimus-Curiositatem/Beta-back
Rama base actual: entrega-1-backend
Equipo: 6 integrantes
Fecha: 2 de septiembre de 2026

⸻

1. Propósito

Este documento consolida las decisiones técnicas aprobadas para evolucionar Beta desde una aplicación basada en Spring MVC + Thymeleaf hacia una arquitectura con:

* frontend SPA en Angular;
* API RESTful versionada;
* seguridad stateless;
* aislamiento multiempresa;
* DTOs explícitos;
* manejo estándar de errores;
* CI/CD;
* Docker;
* JaCoCo;
* SonarQube;
* Quality Gate;
* distribución clara de responsabilidades entre 6 integrantes.

La migración será incremental. Las vistas Thymeleaf existentes podrán convivir temporalmente con la nueva API hasta que Angular cubra los mismos flujos funcionales.

⸻

2. Estado actual del backend

Actualmente Beta-back cuenta con:

* Java 21.
* Spring Boot 4.1.
* Spring MVC.
* Thymeleaf.
* Spring Data JPA.
* Hibernate.
* H2 para desarrollo.
* PostgreSQL para producción.
* Maven.
* Docker.
* GitHub Actions.
* JaCoCo.
* ArchUnit.
* JUnit.
* BCrypt.
* Arquitectura modular dividida en:
    * gestion;
    * modelado.
* Multitenencia mediante:
    * EntidadEmpresa;
    * RepositorioTenant<T>.
* Autenticación web actual basada en HttpSession.
* Services ya implementados para:
    * empresas;
    * usuarios;
    * procesos;
    * historial;
    * roles;
    * pools;
    * lanes;
    * actividades;
    * gateways;
    * arcos;
    * mensajes;
    * correlaciones.

⸻

3. Arquitectura objetivo

La arquitectura objetivo será:

                    BETA-FRONT
                      ANGULAR
                         |
                         | HTTPS + JSON
                         v
                    /api/v1/**
                         |
                  Spring Security
                         |
                    Bearer JWT
                         |
                  REST Controllers
                         |
                 Request / Response
                       DTOs
                         |
                      Mappers
                         |
                 Services existentes
                         |
                  Repositories JPA
                         |
               PostgreSQL / H2

Durante la transición:

Browser
   |
Controllers MVC / Thymeleaf
   |
Services existentes
   |
Repositories

y:

Angular / Postman
      |
   /api/v1/**
      |
REST Controllers
      |
mismos Services

convivirán temporalmente.

⸻

4. Decisiones arquitectónicas consolidadas

Área	Decisión
Frontend	Angular será el framework oficial de Beta-front.
API	Se construirá una API RESTful bajo /api/v1/**.
Migración	Se utilizará una migración incremental tipo Strangler.
Compatibilidad	Thymeleaf permanecerá temporalmente funcionando.
Estado API	La API REST será stateless.
Seguridad	Spring Security + Bearer JWT.
Multitenencia	El tenant se deriva del usuario autenticado.
DTOs	No se serializan entidades JPA directamente.
Errores	ProblemDetail compatible con RFC 9457.
Paginación	DTO estable, no Page<T> como contrato público.
Cobertura	JaCoCo permanece como herramienta de cobertura.
Calidad	SonarQube centraliza análisis y Quality Gate.
Arquitectura	ArchUnit permanece activo.
CI/CD	GitHub Actions.
Contenedores	Docker.
Versionado	API inicial /api/v1.

⸻

5. Estrategia de migración

No se reescribirá el backend desde cero.

Se conservarán:

Model
Repository
Service

y se agregará:

REST Controller
DTO
Mapper
Security

La evolución será:

Spring MVC + Thymeleaf
          |
          | coexistencia
          v
REST API /api/v1
          |
          v
Angular SPA

La lógica de negocio debe seguir viviendo en los Service.

Está prohibido duplicar reglas de negocio entre:

Thymeleaf Controller

y:

REST Controller

⸻

6. Estructura propuesta

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
|   +-- SecurityConfig
|   +-- JwtAuthenticationFilter
|
+-- gestion/
|   +-- model/
|   +-- repository/
|   +-- service/
|   +-- controller/
|       +-- controllers Thymeleaf existentes
|       |
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

No se hará una reorganización masiva de paquetes solo por estética.

⸻

7. Principios RESTful

La API debe ser RESTful, no simplemente controllers que devuelven JSON.

7.1 URLs

Usar sustantivos y recursos.

Correcto:

/api/v1/procesos
/api/v1/usuarios
/api/v1/pools

Evitar:

/api/v1/crearProceso
/api/v1/eliminarUsuario
/api/v1/publicarProceso

cuando la operación pueda representarse mediante el estado del recurso.

⸻

8. Semántica HTTP

GET

Debe ser safe.

GET /api/v1/procesos

No modifica estado.

⸻

POST

Creación de recursos.

POST /api/v1/procesos

Respuesta:

201 Created
Location: /api/v1/procesos/123

⸻

PUT

Reemplazo completo e idempotente.

PUT /api/v1/procesos/123

⸻

PATCH

Actualización parcial.

PATCH /api/v1/procesos/123

Ejemplo:

{
  "estado": "PUBLICADO"
}

⸻

DELETE

Debe ser idempotente desde la perspectiva HTTP.

DELETE /api/v1/procesos/123

Respuesta:

204 No Content

Aunque internamente Proceso y RolProceso utilicen soft delete.

⸻

9. Códigos HTTP

Código	Uso
200 OK	Consulta o actualización exitosa
201 Created	Recurso creado
204 No Content	Eliminación exitosa
400 Bad Request	Request inválido
401 Unauthorized	Token inexistente, inválido o expirado
403 Forbidden	Usuario sin permisos
404 Not Found	Recurso inexistente
409 Conflict	Regla de negocio o conflicto

Nunca devolver:

200 OK

para representar errores.

⸻

10. DTOs

Está prohibido devolver entidades JPA directamente.

Evitar:

return procesoService.obtener(...);

cuando el resultado sea una entidad que Jackson serialice directamente.

El flujo debe ser:

JSON
 |
Request DTO
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

Ejemplo:

public record ProcesoResponse(
    Long id,
    String nombre,
    String descripcion,
    String categoria,
    EstadoProceso estado
) {}

⸻

11. Validación

Validación sintáctica

En Request DTO:

@NotBlank
@Email
@Size

Validación de negocio

Debe permanecer en:

Service

Ejemplo:

nombre de proceso duplicado

no debe validarse únicamente en el Controller.

⸻

12. Manejo de errores

Se reutilizarán:

ReglaNegocioException
RecursoNoEncontradoException

pero la API REST utilizará ProblemDetail.

Ejemplo:

{
  "type": "about:blank",
  "title": "Recurso no encontrado",
  "status": 404,
  "detail": "No existe el proceso solicitado",
  "instance": "/api/v1/procesos/99"
}

⸻

13. Autenticación REST

La API será:

STATELESS

Por lo tanto /api/v1/** no utilizará HttpSession como mecanismo de identidad.

Se implementará:

Spring Security
+
Bearer JWT

⸻

14. Login REST

Endpoint:

POST /api/v1/auth/login

Request:

{
  "email": "admin@demo.com",
  "password": "admin123"
}

Respuesta aproximada:

{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "usuario": {
    "id": 1,
    "nombre": "Administrador",
    "email": "admin@demo.com",
    "rol": "ADMINISTRADOR"
  }
}

Peticiones siguientes:

Authorization: Bearer <token>

⸻

15. Claims JWT

El JWT debe contener únicamente la información necesaria:

usuarioId
empresaId
rol

No debe convertirse en un contenedor de información arbitraria del usuario.

No se implementarán refresh tokens inicialmente.

Se aplica:

YAGNI

⸻

16. Multitenencia

Esta decisión es crítica.

Actualmente existen:

EntidadEmpresa
RepositorioTenant<T>

Esto debe preservarse.

El flujo será:

Request
   |
Bearer JWT
   |
Spring Security
   |
ApiPrincipal
   |
empresaId
usuarioId
rol
   |
Service
   |
RepositorioTenant
   |
Database

El frontend nunca decide el tenant.

Está prohibido confiar en:

{
  "empresaId": 5
}

como mecanismo para seleccionar empresa.

También debe evitarse usar:

/api/v1/empresas/{empresaId}/procesos

para elegir tenant.

El empresaId se obtiene del usuario autenticado.

⸻

17. Recursos REST de gestión

Autenticación

POST /api/v1/auth/login

Usuarios

GET    /api/v1/usuarios
POST   /api/v1/usuarios
GET    /api/v1/usuarios/{id}
PUT    /api/v1/usuarios/{id}
PATCH  /api/v1/usuarios/{id}
DELETE /api/v1/usuarios/{id}

Procesos

GET    /api/v1/procesos
POST   /api/v1/procesos
GET    /api/v1/procesos/{id}
PUT    /api/v1/procesos/{id}
PATCH  /api/v1/procesos/{id}
DELETE /api/v1/procesos/{id}

La publicación deberá modelarse preferiblemente como cambio de estado:

PATCH /api/v1/procesos/{id}
{
  "estado": "PUBLICADO"
}

en lugar de:

POST /procesos/{id}/publicar

salvo que el dominio justifique una publicación como recurso propio.

Roles

GET    /api/v1/roles
POST   /api/v1/roles
GET    /api/v1/roles/{id}
PUT    /api/v1/roles/{id}
DELETE /api/v1/roles/{id}

⸻

18. Recursos REST BPMN

Pools

GET    /api/v1/procesos/{procesoId}/pools
POST   /api/v1/procesos/{procesoId}/pools
GET    /api/v1/pools/{poolId}
PUT    /api/v1/pools/{poolId}
DELETE /api/v1/pools/{poolId}

Lanes

GET    /api/v1/pools/{poolId}/lanes
POST   /api/v1/pools/{poolId}/lanes
GET    /api/v1/lanes/{laneId}
PUT    /api/v1/lanes/{laneId}
DELETE /api/v1/lanes/{laneId}

Actividades

POST   /api/v1/lanes/{laneId}/actividades
GET    /api/v1/actividades/{id}
PUT    /api/v1/actividades/{id}
DELETE /api/v1/actividades/{id}

Gateways

POST   /api/v1/lanes/{laneId}/gateways
GET    /api/v1/gateways/{id}
PUT    /api/v1/gateways/{id}
DELETE /api/v1/gateways/{id}

Arcos

La estructura definitiva debe definirse según la relación real entre los nodos.

Mensajes

La ruta definitiva debe respetar la relación real con:

Proceso
Pool
Mensaje

Correlaciones

La ruta se definirá según la cardinalidad real entre:

Mensaje
Correlacion

⸻

19. Paginación

No devolver directamente:

Page<Proceso>

como contrato REST.

Crear un DTO estable:

{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 120,
  "totalPages": 6
}

⸻

20. Frontend Angular

Beta-front será una SPA en Angular.

Estructura recomendada:

src/app/
|
+-- core/
|   +-- auth/
|   +-- http/
|   +-- guards/
|   +-- interceptors/
|
+-- shared/
|   +-- components/
|   +-- models/
|
+-- features/
    +-- auth/
    +-- usuarios/
    +-- procesos/
    +-- roles/
    +-- modelado/
        +-- pools/
        +-- lanes/
        +-- actividades/
        +-- gateways/
        +-- arcos/
        +-- mensajes/
        +-- correlaciones/

⸻

21. Integración Angular

Angular deberá utilizar:

HttpClient

y un interceptor:

HTTP Interceptor
    |
Authorization: Bearer <token>

También deberá manejar:

401 -> sesión inválida
403 -> acceso denegado
404 -> recurso inexistente
409 -> conflicto de negocio

Los servicios Angular deben consumir DTOs públicos del backend.

No deben modelarse directamente sobre las entidades Hibernate.

⸻

22. CORS

No utilizar:

Access-Control-Allow-Origin: *

en producción.

Configurar algo como:

FRONTEND_ORIGIN

por variables de entorno.

En desarrollo podrá permitirse el origen local de Angular.

⸻

23. OpenAPI

Se recomienda evaluar OpenAPI/Swagger.

Objetivos:

* documentar /api/v1;
* facilitar uso de Postman;
* facilitar integración Angular;
* inspeccionar Request/Response DTOs;
* servir como contrato técnico del equipo.

Solo debe añadirse si existe compatibilidad estable con Spring Boot 4.1.

⸻

24. Calidad

La estrategia queda conformada por:

JUnit
+
ArchUnit
+
JaCoCo
+
SonarQube

Cada herramienta tiene responsabilidades distintas.

⸻

25. JaCoCo y SonarQube

SonarQube no reemplaza JaCoCo.

La arquitectura será:

Tests
  |
JaCoCo
  |
jacoco.xml
  |
SonarQube
  |
Quality Gate

JaCoCo debe generar:

target/site/jacoco/jacoco.xml

⸻

26. Quality Gate

Recomendación sobre New Code:

Métrica	Objetivo
New Bugs	0
New Vulnerabilities	0
Coverage on New Code	>= 80%
Duplicated Lines on New Code	< 3%

No se exigirá inicialmente una cobertura global artificial sobre todo el código legado.

⸻

27. ArchUnit

ArchUnit se mantiene.

Debe validar reglas como:

Controller -> Service         permitido
Controller -> Repository      prohibido
REST Controller -> Repository prohibido

También:

* entidades multiempresa;
* repositorios tenant-aware;
* estructura de paquetes;
* herencia JPA;
* enums;
* dependencias entre módulos.

⸻

28. Pipeline CI/CD objetivo

Push / Pull Request
        |
        v
Build & Test
        |
        v
Architecture Guard
        |
        v
JaCoCo XML
        |
        v
SonarQube Analysis
        |
        v
Quality Gate
        |
        v
Docker Build

⸻

29. GitHub Secrets

Configurar:

SONAR_TOKEN
SONAR_HOST_URL

Nunca almacenar tokens directamente en el repositorio.

⸻

30. Docker

Mantener el Dockerfile multi-stage existente.

Arquitectura:

Stage 1
Maven + Java 21
   |
Tests
   |
Package JAR
Stage 2
JRE 21
   |
appuser
   |
8080

No introducir secretos en Dockerfile.

⸻

31. GitFlow

Estado actual:

main
entrega-1-backend

Objetivo:

main
  |
develop
  |
  +-- feature/restful-api-v1
  +-- feature/security-jwt
  +-- feature/rest-gestion
  +-- feature/rest-modelado
  +-- feature/angular-integration
  +-- chore/quality-sonarqube

La creación de develop deberá ser acordada antes de modificar ramas remotas.

⸻

32. Conventional Commits

Ejemplos:

feat(api): expose process resource
feat(api): add stateless jwt authentication
feat(api): expose pool resources
feat(api): expose lane resources
feat(front): consume process api from angular
fix(security): enforce tenant from authenticated principal
test(api): verify process creation contract
test(security): verify tenant isolation
chore(quality): generate jacoco xml report
ci(sonar): add sonarqube analysis

⸻

33. Estrategia de implementación por fases

Fase 0 — Consolidación

* revisar rama base;
* asegurar CI verde;
* documentar arquitectura;
* definir contratos REST;
* establecer baseline de calidad.

Fase 1 — Base REST

* /api/v1;
* DTOs;
* mappers;
* ProblemDetail;
* paginación;
* convenciones comunes.

Fase 2 — Seguridad

* Spring Security;
* JWT;
* ApiPrincipal;
* CORS;
* roles;
* autorización;
* multitenancy.

Fase 3 — Gestión REST

* usuarios;
* procesos;
* roles.

Fase 4 — BPMN REST

* pools;
* lanes;
* actividades;
* gateways;
* arcos;
* mensajes;
* correlaciones.

Fase 5 — Angular

* autenticación;
* interceptor;
* guards;
* usuarios;
* procesos;
* roles;
* editor BPMN.

Fase 6 — Calidad

* cobertura;
* SonarQube;
* Quality Gate;
* integración;
* E2E;
* endurecimiento.

Fase 7 — Retiro de Thymeleaf

Solo cuando Angular cubra los flujos equivalentes y el equipo lo apruebe.

⸻

34. Distribución de responsabilidades entre 6 personas

La distribución se realiza por ownership de entregables.

⸻

Persona 1 — Arquitectura REST e integración técnica

Responsabilidades

* gobernar el contrato /api/v1;
* definir convenciones REST;
* mantener coherencia de recursos y rutas;
* definir infraestructura común de DTOs;
* definir PageResponse;
* diseñar ApiExceptionHandler;
* definir estructura de ProblemDetail;
* coordinar versionado de la API;
* garantizar que Thymeleaf y REST reutilicen Services;
* revisar dependencias entre módulos;
* coordinar integración técnica de backend;
* mantener documentación de arquitectura;
* revisar PRs arquitectónicos;
* coordinar Docker del backend cuando sea transversal.

Entregables

* estructura base REST;
* convenciones documentadas;
* infraestructura API común;
* guía de arquitectura;
* integración global.

No es responsable de

* pruebas automatizadas;
* cobertura;
* JaCoCo;
* SonarQube.

Eso corresponde a Persona 6.

⸻

Persona 2 — Seguridad y multitenancy

Responsabilidades

* Spring Security;
* JWT;
* ApiPrincipal;
* authentication filter;
* login REST;
* autorización por roles;
* extracción segura de empresaId;
* preservar RepositorioTenant;
* proteger contra acceso cross-tenant;
* configuración CORS;
* configuración de secretos;
* variables de entorno;
* compatibilidad temporal con autenticación Thymeleaf.

Entregables

* POST /api/v1/auth/login;
* Bearer JWT;
* Security Filter Chain;
* ApiPrincipal;
* configuración CORS;
* protección por roles;
* aislamiento multiempresa en seguridad.

Los defectos encontrados por las pruebas deberán ser corregidos por Persona 2, pero la autoría y mantenimiento de dichas pruebas pertenece a Persona 6.

⸻

Persona 3 — REST de Gestión

Responsabilidades

Implementar REST para:

Usuario
Proceso
RolProceso

Incluye:

* REST Controllers;
* Request DTOs;
* Response DTOs;
* mappers;
* filtros;
* paginación;
* códigos HTTP;
* Location;
* soft delete;
* publicación de procesos;
* manejo de errores del módulo;
* integración con Services existentes.

Endpoints principales

/api/v1/usuarios
/api/v1/procesos
/api/v1/roles

Restricciones

* no acceder directamente a Repositories;
* no devolver Entities JPA;
* no aceptar empresaId del frontend.

⸻

Persona 4 — REST del modelado BPMN

Responsabilidades

Implementar REST para:

Pool
Lane
Actividad
Gateway
Arco
Mensaje
Correlacion

Incluye:

* REST Controllers;
* DTOs;
* mappers;
* relaciones padre/hijo;
* recursos anidados;
* semántica REST;
* integración con Services existentes;
* soporte del futuro editor Angular.

Entregables

Endpoints funcionales BPMN bajo:

/api/v1/**

⸻

Persona 5 — Frontend Angular e integración

Responsabilidades

* arquitectura de Beta-front;
* estructura Angular;
* módulos/features;
* login;
* almacenamiento del token;
* interceptor Bearer;
* guards;
* servicios HTTP;
* manejo de errores;
* usuarios;
* procesos;
* roles;
* modelado;
* editor BPMN;
* environments;
* API base URL;
* integración progresiva con backend;
* Docker frontend/nginx cuando corresponda.

Restricciones

* no duplicar reglas de negocio del backend;
* no utilizar entidades JPA como modelos públicos;
* consumir DTOs REST.

⸻

Persona 6 — QA, Testing, Coverage y Quality Gate

Esta persona será la única responsable de todas las pruebas y de la cobertura del proyecto.

Responsabilidades exclusivas

Pruebas unitarias

* Services;
* reglas de negocio;
* casos borde;
* regresiones.

Pruebas REST

* Controllers;
* request JSON;
* response JSON;
* headers;
* status codes;
* Location;
* validaciones.

Pruebas de seguridad

* login correcto;
* login incorrecto;
* token inválido;
* token expirado;
* endpoint sin token;
* 401;
* 403;
* roles.

Pruebas de multitenancy

* aislamiento por empresa;
* IDOR;
* acceso a identificadores de otro tenant;
* manipulación de URLs;
* manipulación de JSON.

Pruebas de arquitectura

Mantener:

ArchUnit

y ampliar reglas para:

* REST controllers;
* security;
* paquetes;
* dependencias;
* acceso Controller -> Repository.

Pruebas de integración

Cuando sean necesarias:

@SpringBootTest
MockMvc
H2

Pruebas E2E

Responsabilidad sobre los flujos:

Angular
   |
REST API
   |
Backend
   |
Database

Coverage

Responsable exclusivo de:

JaCoCo

Incluyendo:

target/site/jacoco/jacoco.xml

SonarQube

Responsable de:

* configuración del scanner;
* integración Maven;
* integración CI;
* análisis;
* bugs;
* vulnerabilidades;
* smells;
* duplicaciones;
* cobertura.

Quality Gate

Responsable de validar:

New Bugs = 0
New Vulnerabilities = 0
Coverage on New Code >= 80%
Duplicated Lines on New Code < 3%

CI de calidad

Responsable de los jobs relacionados con:

* tests;
* ArchUnit;
* JaCoCo;
* SonarQube;
* Quality Gate;
* publicación de reportes.

Poder de bloqueo

Persona 6 podrá bloquear un merge si no existe evidencia suficiente de calidad.

⸻

35. Matriz resumida de ownership

Área	Responsable
Arquitectura REST	Persona 1
Infraestructura API común	Persona 1
Seguridad	Persona 2
JWT	Persona 2
Multitenancy en autenticación	Persona 2
REST Usuarios	Persona 3
REST Procesos	Persona 3
REST Roles	Persona 3
REST Pools	Persona 4
REST Lanes	Persona 4
REST Actividades	Persona 4
REST Gateways	Persona 4
REST Arcos	Persona 4
REST Mensajes	Persona 4
REST Correlaciones	Persona 4
Angular	Persona 5
Integración frontend/backend	Persona 5
Tests unitarios	Persona 6
Tests REST	Persona 6
Tests seguridad	Persona 6
Tests multitenancy	Persona 6
ArchUnit	Persona 6
Tests integración	Persona 6
E2E	Persona 6
JaCoCo	Persona 6
Coverage	Persona 6
SonarQube	Persona 6
Quality Gate	Persona 6

⸻

36. Interacción con Persona 6

Los integrantes 1 a 5:

* desarrollan funcionalidad;
* corrigen defectos encontrados;
* aportan criterios de aceptación;
* explican casos de uso.

Persona 6:

* diseña las pruebas;
* mantiene las pruebas;
* ejecuta la verificación;
* mide cobertura;
* mantiene quality gate;
* determina evidencia técnica de calidad.

⸻

37. Flujo recomendado para una feature

Ejemplo: creación de procesos.

Persona 3
   |
define contrato con Persona 1
   |
Persona 6 define prueba
   |
RED
   |
Persona 3 implementa
   |
Persona 6 verifica
   |
GREEN
   |
refactor
   |
Persona 6 valida regresión + coverage
   |
PR

Esto permite concentrar testing en una sola persona sin perder disciplina de calidad.

⸻

38. Dependencias del equipo

Persona	Depende principalmente de
Persona 1	Coordinación global
Persona 2	Persona 1
Persona 3	Personas 1 y 2
Persona 4	Personas 1 y 2
Persona 5	Personas 1, 2, 3 y 4
Persona 6	Todos los módulos, desde el inicio

Persona 6 no debe comenzar al final del proyecto.

Debe trabajar en paralelo desde la primera feature.

⸻

39. Definition of Done

Una feature solo puede considerarse terminada cuando:

* cumple el contrato REST acordado;
* utiliza DTOs;
* no expone entidades JPA;
* no accede Controller -> Repository;
* preserva multitenancy;
* utiliza códigos HTTP adecuados;
* maneja errores correctamente;
* documentación relevante está actualizada;
* Persona 6 ha verificado las pruebas aplicables;
* cobertura ha sido revisada;
* ArchUnit sigue pasando;
* Quality Gate está satisfecho cuando SonarQube esté disponible;
* build Maven es válido;
* Docker continúa construyendo cuando corresponda.

⸻

40. Uso de Codex + Superpowers

Para cambios arquitectónicos se utilizará el siguiente flujo:

using-superpowers
        |
brainstorming
        |
aprobación del equipo
        |
writing-plans
        |
using-git-worktrees
        |
test-driven-development
        |
subagent-driven-development
o executing-plans
        |
verification-before-completion
        |
requesting-code-review
        |
finishing-a-development-branch

Los diseños deberán guardarse en:

docs/superpowers/specs/

Los planes de implementación deberán guardarse en:

docs/superpowers/plans/

⸻

41. Reglas de Pull Request

* PRs pequeños y revisables.
* Conventional Commits.
* No trabajar directamente en main.
* No git push --force.
* No mezclar refactors no relacionados.
* Explicar cambios de contrato.
* Explicar variables de entorno nuevas.
* Adjuntar evidencia de calidad.
* Persona 6 valida pruebas y cobertura.
* Los owners funcionales corrigen defectos.

⸻

42. Criterios finales de aceptación

REST

Angular puede consumir:

/api/v1/**

mediante JSON.

Stateless

La API REST no depende de HttpSession.

Seguridad

JWT identifica:

usuario
empresa
rol

Multitenancy

Un usuario no puede consultar información de otra empresa manipulando:

* IDs;
* URLs;
* JSON.

DTOs

La API pública está desacoplada de JPA.

Angular

Beta-front consume la API mediante:

* servicios;
* interceptor;
* guards;
* DTOs TypeScript.

Calidad

Integrados:

JUnit
ArchUnit
JaCoCo
SonarQube
Quality Gate

Ownership de QA

Todas las responsabilidades relacionadas con:

testing
coverage
JaCoCo
SonarQube
Quality Gate
E2E
ArchUnit

pertenecen exclusivamente a Persona 6.

⸻

43. Decisión final

La arquitectura aprobada para la evolución de Beta será:

Angular
   |
RESTful API /api/v1
   |
Spring Security + JWT
   |
DTOs
   |
Services
   |
Repositories
   |
PostgreSQL / H2

con:

JUnit
+
ArchUnit
+
JaCoCo
+
SonarQube

y una migración incremental que conserva temporalmente Thymeleaf hasta que Angular cubra las funcionalidades equivalentes.

La responsabilidad total de pruebas, cobertura y Quality Gate queda centralizada en Persona 6.
