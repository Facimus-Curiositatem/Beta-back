# Persona 4: REST del módulo Gestión — Diseño

## Objetivo

Completar el módulo Gestión de `Beta-back` como una API REST stateless bajo
`/api/v1/**`, complementando el trabajo previo de seguridad, contrato HTTP y
multitenancy sin modificar ni publicar cambios en las ramas de las demás
personas.

La entrega se realizará únicamente en la rama
`feature/persona-4-gestion-rest`, creada desde `feature/migracion-rest`.

## Alcance

La implementación comprende los recursos `Empresa`, `Usuario`, `Proceso`,
`RolProceso` e `HistorialCambio`. Reutiliza las entidades, repositorios y
servicios actuales, y añade o ajusta solamente las operaciones necesarias para
cerrar el contrato del módulo.

Quedan fuera de alcance:

- cambios funcionales en el módulo Modelado BPMN;
- administración global de empresas;
- interfaces HTML, Thymeleaf o controladores MVC;
- autenticación con sesión;
- cambios directos en `main`, `feature/migracion-rest` o las ramas de otras
  personas;
- merge a otra rama sin aprobación del responsable del repositorio.

## Estrategia de integración

La nueva rama partirá de `feature/migracion-rest`. En ella se incorporarán los
aportes vigentes de Persona 2 y Persona 3, conservando su historial cuando sea
posible y resolviendo los cruces con estas prioridades:

1. Seguridad JWT y `ApiPrincipal` como fuente única de identidad.
2. Autorización centralizada y aislamiento anti-IDOR de Persona 3.
3. Versionado, métodos HTTP, `Location`, `PageResponse` y `ProblemDetail` de
   Persona 2.
4. Comportamiento funcional y pruebas de Gestión de Persona 4.

Los conflictos fuera de Gestión se resolverán únicamente para conservar de
forma fiel los aportes previos. No se añadirá comportamiento nuevo al módulo
Modelado.

## Arquitectura y flujo de datos

Toda petición protegida seguirá este flujo:

```text
Bearer JWT
  -> JwtAuthenticationFilter
  -> ApiPrincipal
  -> SecurityConfig (autorización por rol)
  -> REST controller
  -> request DTO validado
  -> service(empresaId, usuarioId, ...)
  -> repository con filtro de tenant
  -> response DTO
```

Los controladores recibirán la identidad mediante
`@AuthenticationPrincipal ApiPrincipal`. Ningún endpoint aceptará
`empresaId` o `usuarioId` como fuente de autoridad desde el path, query string
o cuerpo JSON.

Los controladores se limitarán a validación, autorización declarativa,
orquestación y mapeo HTTP. Las reglas de unicidad, soft delete, trazabilidad y
uso de roles permanecerán en los servicios.

## Contrato REST

### Empresa

| Método | Ruta | Acceso | Resultado |
|---|---|---|---|
| `POST` | `/api/v1/empresas` | Público | Registra empresa y administrador inicial; responde `201`, `Location` y `EmpresaResponse` |

No se expone un CRUD global de empresas porque el dominio actual no contiene
ese caso de uso.

### Usuario

| Método | Ruta | Acceso | Resultado |
|---|---|---|---|
| `GET` | `/api/v1/usuarios` | Administrador | Lista usuarios activos de la empresa autenticada |
| `POST` | `/api/v1/usuarios` | Administrador | Crea colaborador; responde `201` y `Location` |
| `GET` | `/api/v1/usuarios/{id}` | Administrador | Consulta un usuario del mismo tenant |
| `PATCH` | `/api/v1/usuarios/{id}` | Administrador | Actualiza parcialmente rol y/o estado |
| `DELETE` | `/api/v1/usuarios/{id}` | Administrador | Desactiva lógicamente el usuario; responde `204` |

El DTO de actualización parcial exigirá al menos uno de `rolAcceso` o
`activo`. La desactivación por `DELETE` seguirá siendo idempotente desde el
punto de vista del estado final.

### Proceso

| Método | Ruta | Acceso | Resultado |
|---|---|---|---|
| `GET` | `/api/v1/procesos` | Autenticado | Lista procesos activos con filtros y paginación |
| `POST` | `/api/v1/procesos` | Administrador o editor | Crea proceso; responde `201` y `Location` |
| `GET` | `/api/v1/procesos/{id}` | Autenticado | Obtiene el detalle del proceso del mismo tenant |
| `PUT` | `/api/v1/procesos/{id}` | Administrador o editor | Reemplaza los datos editables del proceso |
| `PATCH` | `/api/v1/procesos/{id}` | Administrador o editor | Cambia el estado, incluida la publicación |
| `DELETE` | `/api/v1/procesos/{id}` | Administrador | Ejecuta baja lógica; responde `204` |
| `GET` | `/api/v1/procesos/{id}/historial` | Autenticado | Lista la trazabilidad descendente por fecha |

El listado admitirá `nombre`, `estado`, `categoria` y página. Responderá con
el `PageResponse` común y ordenará por fecha de modificación descendente. Los
procesos inactivos no aparecerán en listados ni serán accesibles como recursos
activos.

Cada creación, edición, transición de estado y baja lógica registrará un
`HistorialCambio` con la empresa, el proceso y el autor autenticado.

### Rol de proceso

| Método | Ruta | Acceso | Resultado |
|---|---|---|---|
| `GET` | `/api/v1/roles` | Autenticado | Lista roles activos y su uso real |
| `POST` | `/api/v1/roles` | Administrador | Crea rol; responde `201` y `Location` |
| `GET` | `/api/v1/roles/{id}` | Autenticado | Consulta rol del mismo tenant y su uso |
| `PUT` | `/api/v1/roles/{id}` | Administrador | Actualiza nombre y descripción |
| `DELETE` | `/api/v1/roles/{id}` | Administrador | Ejecuta baja lógica si el rol no está en uso |

La eliminación de un rol utilizado por lanes responderá `409 Conflict` y no
alterará el registro.

## Multitenancy y autorización

El tenant se obtendrá exclusivamente de `ApiPrincipal.empresaId()`. Toda
lectura o escritura de entidades pertenecientes a una empresa usará métodos de
repositorio o servicios que incluyan ese identificador.

Un identificador válido de otra empresa se tratará como recurso no encontrado
y responderá `404`, sin revelar que existe en otro tenant.

Permisos:

- `ADMINISTRADOR`: administración de usuarios y roles; todas las operaciones
  de procesos.
- `EDITOR`: lectura general y creación, edición o cambio de estado de procesos.
- `SOLO_LECTURA`: consultas de procesos, historial y roles.
- anónimo: únicamente registro de empresa y login.

La autorización se centralizará en Spring Security. Los controladores no
duplicarán comprobaciones manuales de rol que ya estén expresadas en
`SecurityConfig`.

## Errores y validación

Las respuestas de error usarán `application/problem+json` mediante
`ProblemDetail`:

- `400 Bad Request`: JSON, parámetros o DTOs inválidos;
- `401 Unauthorized`: token ausente, inválido o usuario inactivo;
- `403 Forbidden`: usuario autenticado sin el rol requerido;
- `404 Not Found`: recurso inexistente, inactivo o de otro tenant;
- `409 Conflict`: duplicados, transición inválida o rol en uso;
- `500 Internal Server Error`: detalle público genérico y registro interno de
  la excepción.

Los mensajes de validación no incluirán contraseñas, tokens, identificadores de
otro tenant ni detalles internos.

## Pruebas y criterios de aceptación

La implementación se hará con ciclos TDD. Como mínimo se cubrirán:

- rutas bajo `/api/v1` y ausencia de dependencias de `HttpSession` en Gestión;
- `201 Created` y `Location` en cada creación;
- estructura de `PageResponse` y combinación de filtros;
- validación de cuerpos y parámetros;
- permisos de `ADMINISTRADOR`, `EDITOR` y `SOLO_LECTURA`;
- `401` sin autenticación y `403` con rol insuficiente;
- aislamiento entre dos empresas y protección anti-IDOR;
- cambios parciales de rol/estado del usuario y desactivación lógica;
- creación, edición, transición de estado y soft delete de procesos;
- generación y consulta ordenada de `HistorialCambio`;
- conteo de uso y soft delete de roles;
- conflictos por nombres duplicados y roles en uso;
- forma `ProblemDetail` para `400`, `404` y `409`.

Antes de publicar la rama se ejecutarán:

1. pruebas focalizadas durante cada ciclo TDD;
2. suite Maven completa;
3. pruebas ArchUnit;
4. verificación del reporte JaCoCo y su umbral configurado;
5. revisión del diff completo contra `feature/migracion-rest`.

## Entrega Git

Los cambios se agruparán en commits convencionales y atómicos, incluyendo la
integración de dependencias previas, contrato de usuarios, contrato de
procesos/historial, roles y QA. Solo se publicará
`feature/persona-4-gestion-rest` en `origin`.

No se abrirá ni fusionará un pull request sin aprobación explícita.
