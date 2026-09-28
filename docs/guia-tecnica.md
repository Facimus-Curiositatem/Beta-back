# Guia tecnica del backend — Sistema de Gestion de Procesos

Documento interno del equipo. Describe el estado actual del backend, las
decisiones vigentes, la infraestructura de CI/CD, el despliegue en Docker y
las guias que todo colaborador debe seguir para contribuir sin romper las
garantias que ya estan en pie.

Ultima actualizacion: 2026-09-28 (rama `main`).

---

## 1. Estado actual del proyecto

### Stack tecnologico

| Componente | Version |
|---|---|
| Java | 21 (Temurin) |
| Spring Boot | 4.1 |
| API | REST exclusivo (JSON + JWT stateless) |
| ORM | Hibernate (via Spring Data JPA) |
| BD desarrollo | H2 embebida en archivo (`./data/`) |
| BD produccion | PostgreSQL (via perfil `prod`) |
| Seguridad | Spring Security + JWT (Bearer token) |
| Cifrado de passwords | BCrypt |
| Tests de arquitectura | ArchUnit 1.4.0 |
| Cobertura | JaCoCo 0.8.13 |
| Analisis de calidad | SonarCloud |
| CI/CD | GitHub Actions |
| Contenedorizacion | Docker (multi-stage build) |
| Documentacion API | springdoc-openapi (Swagger UI) |

### Entidades implementadas (13)

**Bloque gestion** (5): `Empresa`, `Usuario`, `Proceso`, `HistorialCambio`,
`RolProceso`.

**Bloque modelado** (8): `Pool`, `Lane`, `NodoFlujo` (abstracta), `Actividad`,
`Gateway`, `Arco`, `Mensaje`, `Correlacion`.

**Enums** (4): `RolAcceso`, `EstadoProceso`, `TipoParticipante`, `TipoGateway`.

Todas las entidades excepto `Empresa` extienden `EntidadEmpresa`
(`@MappedSuperclass`) que aporta la columna `empresa_id` con
`updatable = false`.

### Servicios implementados (12)

Bloque gestion: `EmpresaService`, `UsuarioService`, `ProcesoService`,
`HistorialCambioService`, `RolProcesoService`.

Bloque modelado: `PoolService`, `LaneService`, `ActividadService`,
`GatewayService`, `ArcoService`, `MensajeService`, `CorrelacionService`.

Todos reciben `empresaId` del controlador (extraido del `ApiPrincipal`
autenticado) y lo usan para acotar cada operacion al tenant correcto.

### Controladores REST (12)

| Modulo | Controlador | Base path |
|---|---|---|
| Auth | `AuthController` | `/api/v1/auth` |
| Empresas | `EmpresaController` | `/api/v1/empresas` |
| Usuarios | `UsuarioController` | `/api/v1/usuarios` |
| Procesos | `ProcesoController` | `/api/v1/procesos` |
| Roles | `RolProcesoController` | `/api/v1/roles` |
| Pools | `PoolController` | `/api/v1/procesos/{id}/pools`, `/api/v1/pools/{id}` |
| Lanes | `LaneController` | `/api/v1/pools/{id}/lanes`, `/api/v1/lanes/{id}` |
| Actividades | `ActividadController` | `/api/v1/lanes/{id}/actividades`, `/api/v1/actividades/{id}` |
| Gateways | `GatewayController` | `/api/v1/lanes/{id}/gateways`, `/api/v1/gateways/{id}` |
| Arcos | `ArcoController` | `/api/v1/arcos`, `/api/v1/pools/{id}/arcos` |
| Mensajes | `MensajeController` | `/api/v1/procesos/{id}/mensajes`, `/api/v1/mensajes/{id}` |
| Correlaciones | `CorrelacionController` | `/api/v1/mensajes/{id}/correlacion` |

Documentacion interactiva disponible en `/swagger-ui/index.html`.

---

## 2. Decisiones de arquitectura vigentes

Estas decisiones estan protegidas por tests de ArchUnit. Si un PR las
viola, el CI falla automaticamente. No se deben cambiar sin un ADR nuevo
discutido con el equipo.

### 2.1 Empaquetado modular por dominio

```
com.facimus.procesos
├── common/       Clases transversales (EntidadEmpresa, excepciones, RepositorioTenant)
├── config/       Configuracion de Spring (OpenAPI, datos demo)
├── security/     JWT, filtros, CORS, ApiPrincipal
├── gestion/      Entidades de gestion del sistema
│   ├── model/
│   ├── repository/
│   ├── service/
│   └── controller/
└── modelado/     Entidades del modelado BPMN
    ├── model/
    ├── repository/
    ├── service/
    └── controller/
```

**Reglas verificadas por CI:**
- Los `@RestController` viven en paquetes `controller/`.
- Los `@Service` viven en paquetes `service/`.
- Los `@Entity` viven en paquetes `model/`.
- Los controllers no acceden directamente a repositorios (deben pasar por
  servicios).
- `modelado` no depende de `gestion.controller`.

### 2.2 Multi-tenencia por columna directa

- Toda `@Entity` excepto `Empresa` extiende `EntidadEmpresa`.
- `empresa_id` es `nullable = false, updatable = false`.
- Todo repositorio excepto `EmpresaRepository` extiende `RepositorioTenant<T>`,
  que expone `findByIdAndEmpresaId`, `findAllByEmpresaId` y
  `existsByIdAndEmpresaId`.
- **Toda query nueva debe filtrar por `empresaId`. Sin excepciones.** La unica
  excepcion documentada es `UsuarioRepository.findByEmail()` para el login.

### 2.3 Herencia JPA

- `NodoFlujo` es abstracta con `@Inheritance(SINGLE_TABLE)` y
  `@DiscriminatorColumn(name = "tipo_nodo")`.
- Solo tiene dos subtipos: `Actividad` y `Gateway`.

### 2.4 Enums

Todos los campos `@Enumerated` usan `EnumType.STRING`. Nunca `ORDINAL`.
Agregar un valor al enum o reordenarlo no corrompe datos existentes.

### 2.5 Eliminacion logica

`Proceso` y `RolProceso` usan `boolean activo`. Nunca se hace `DELETE`
fisico sobre estas entidades.

### 2.6 Seguridad

- Autenticacion stateless via JWT (Bearer token).
- Tres roles de acceso: `ADMINISTRADOR`, `EDITOR`, `SOLO_LECTURA`.
- `ApiPrincipal` encapsula la identidad del usuario autenticado y el
  `empresaId` del tenant.
- Errores de autenticacion y autorizacion responden con `ProblemDetail`
  (RFC 9457).

---

## 3. Suite de tests

### 3.1 Inventario

| Tipo | Clases | Tests | Que valida |
|---|---|---|---|
| Contexto | 1 | 1 | Spring Boot arranca correctamente |
| Arquitectura (ArchUnit) | 6 | 20 | Empaquetado, multi-tenencia, herencia JPA, seguridad |
| Controllers (gestion) | 5 | 39 | Endpoints REST, validaciones, permisos |
| Controllers (modelado) | 7 | 57 | Endpoints REST de modelado BPMN |
| Servicios (gestion) | 4 | 19 | Logica de negocio de gestion |
| Servicios (modelado) | 7 | 52 | Logica de negocio de modelado |
| Seguridad e integracion | 4 | 17 | JWT, aislamiento de tenants, roles |
| **Total** | **34** | **279** | |

### 3.2 Ejecucion local

```bash
# Todos los tests
./mvnw test

# Solo tests de arquitectura
./mvnw test -Dtest="com.facimus.procesos.arquitectura.**"

# Solo tests de un servicio especifico
./mvnw test -Dtest="ArcoServiceTest"
```

El reporte de cobertura JaCoCo se genera en `target/site/jacoco/index.html`
despues de correr los tests.

### 3.3 Agregar tests nuevos

- Tests de arquitectura van en `src/test/java/com/facimus/procesos/arquitectura/`.
- Tests de controllers usan `@WebMvcTest` con mocks de servicios.
- Tests unitarios de servicio van en el paquete espejo del servicio bajo
  `src/test/`. Ejemplo: servicio en `modelado.service.ArcoService` → test en
  `modelado.service.ArcoServiceTest`.
- Usar `@ExtendWith(MockitoExtension.class)` con `@Mock` e `@InjectMocks`.
  No usar `@SpringBootTest` para tests unitarios — solo para tests de
  integracion que necesiten el contexto completo.

---

## 4. CI/CD con GitHub Actions

### 4.1 Pipeline actual (`.github/workflows/ci.yml`)

```
Push o PR a main/develop
│
├── Job 1: Build & Test (ubuntu + windows, en paralelo)
│   ├── Compilar con Maven
│   ├── Ejecutar los 279 tests
│   ├── Generar reporte de tests
│   ├── Subir reporte JaCoCo como artefacto
│   └── Empaquetar JAR
│
├── Job 2: Architecture Guard (despues de Job 1)
│   ├── Ejecutar los 20 tests de ArchUnit
│   └── Reporte separado de reglas de arquitectura
│
├── Job 3: Docker Build (despues de Jobs 1 y 2)
│   ├── Construir imagen Docker
│   └── Levantar contenedor y verificar que arranca
│
└── Job 4: SonarCloud (despues de Job 1, solo en push)
    └── Analisis de calidad y cobertura
```

### 4.2 Que bloquea un merge

Si se activa branch protection en `main`, un PR no se puede mergear si:
- Falla cualquier test unitario o de integracion.
- Se viola alguna regla de arquitectura (ArchUnit).
- La imagen Docker no se construye correctamente.
- La cobertura de codigo nuevo cae por debajo del 80% (SonarCloud quality gate).

---

## 5. Despliegue con Docker

### 5.1 Imagen Docker

El `Dockerfile` usa build multi-stage:

```
Stage 1 — build (maven:3.9-eclipse-temurin-21)
  1. Copia pom.xml y descarga dependencias (capa cacheada)
  2. Copia codigo fuente
  3. Ejecuta mvn package (compila + corre tests)
  4. Si algun test falla, el build se detiene aqui

Stage 2 — runtime (eclipse-temurin:21-jre)
  1. Copia solo el JAR del stage anterior
  2. Corre como usuario no-root (appuser, uid 1001)
  3. Expone puerto 8080
```

**Construir la imagen localmente:**

```bash
docker build -t facimus/procesos-back .
```

### 5.2 Perfiles de Spring

| Perfil | BD | DatosDemoInitializer | Uso |
|---|---|---|---|
| (default) | H2 en archivo `./data/` | Activo | Desarrollo local |
| `prod` | PostgreSQL externo | Inactivo (`@Profile("!prod")`) | Contenedores Docker / VMs |

El perfil se activa con la variable de entorno `SPRING_PROFILES_ACTIVE=prod`.

### 5.3 Variables de entorno del perfil prod

| Variable | Default | Descripcion |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | (ninguno) | Debe ser `prod` para activar PostgreSQL |
| `DB_HOST` | `localhost` | IP o hostname de PostgreSQL |
| `DB_PORT` | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | `procesos` | Nombre de la base de datos |
| `DB_USER` | `procesos` | Usuario de la base de datos |
| `DB_PASSWORD` | (vacio) | Contrasena de la base de datos |
| `JWT_SECRET` | (requerido) | Clave para firmar tokens JWT |

### 5.4 Topologia de despliegue

Tres servicios en contenedores Docker, cada uno en su propia VM.

```
VM 1 — Base de datos          VM 2 — Backend              VM 3 — Frontend
┌──────────────────┐          ┌──────────────────┐        ┌──────────────────┐
│  PostgreSQL 17   │          │  procesos-back   │        │  Angular (nginx) │
│  puerto 5432     │◄─────────│  puerto 8080     │◄───────│  puerto 80       │
│                  │   JDBC   │                  │  HTTP  │                  │
└──────────────────┘          └──────────────────┘        └──────────────────┘
```

### 5.5 Comandos de despliegue por VM

**VM 1 — Base de datos:**

```bash
docker run -d \
  --name procesos-db \
  --restart unless-stopped \
  -p 5432:5432 \
  -e POSTGRES_DB=procesos \
  -e POSTGRES_USER=procesos \
  -e POSTGRES_PASSWORD=<password-segura> \
  -v pgdata:/var/lib/postgresql/data \
  postgres:17
```

**VM 2 — Backend:**

```bash
docker run -d \
  --name procesos-back \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=<ip-vm-1> \
  -e DB_PORT=5432 \
  -e DB_NAME=procesos \
  -e DB_USER=procesos \
  -e DB_PASSWORD=<password-segura> \
  -e JWT_SECRET=<clave-segura> \
  facimus/procesos-back
```

**VM 3 — Frontend:**

```bash
docker run -d \
  --name procesos-front \
  --restart unless-stopped \
  -p 80:80 \
  facimus/procesos-front
```

### 5.6 Verificacion del despliegue

```bash
# Verificar que la BD responde
pg_isready -h <ip-vm-1> -p 5432

# Verificar que el backend responde
curl -s http://<ip-vm-2>:8080/swagger-ui/index.html | head -1

# Verificar login
curl -s -X POST http://<ip-vm-2>:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.com","password":"admin123"}'
```

---

## 6. Guia para contribuir

### 6.1 Flujo de trabajo Git

1. Crear rama desde `main`: `feature/<descripcion>`, `fix/<descripcion>`.
2. Hacer commits modulares (un archivo por commit).
3. Abrir PR contra `main`.
4. Esperar que los 4 jobs del CI pasen en verde.
5. Merge con `--no-ff` para preservar el historial de la rama.

### 6.2 Agregar una entidad nueva

1. Crear la clase en el paquete `model/` del modulo correspondiente.
2. Si pertenece a una empresa, extender `EntidadEmpresa`. Si no lo haces,
   el test `MultitenenciaTest` fallara en CI.
3. Si tiene campos enum, mapearlos con `@Enumerated(EnumType.STRING)`. Si
   usas `ORDINAL`, el test `HerenciaJpaTest` fallara.
4. Crear el repositorio extendiendo `RepositorioTenant<T>`. Si no lo haces,
   el test `RepositorioTenantTest` fallara.
5. Crear el servicio en `service/`. Si lo pones en otro paquete, el test
   `EmpaquetadoTest` fallara.
6. Si necesita controlador, crearlo en `controller/`. Que solo dependa de
   servicios, no de repositorios directamente.
7. Agregar tests unitarios para el servicio y controller nuevos.

### 6.3 Cosas que no se deben hacer

- **No crear queries sin filtro `empresaId`** en ningun repositorio (excepto
  `EmpresaRepository`). Un tenant veria datos de otro.
- **No usar `@Enumerated(EnumType.ORDINAL)`**. Reordenar el enum corrompe
  datos existentes.
- **No acceder a repositorios desde controllers**. Toda la logica pasa por
  la capa de servicio.
- **No hacer DELETE fisico** sobre `Proceso` ni `RolProceso`. Usar
  `setActivo(false)`.
- **No hacer `git push --force`** a `main`.
- **No saltarse los hooks** con `--no-verify`.
