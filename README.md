# Facimus Procesos — Backend
 
Backend REST para la gestion de procesos BPMN en entornos multiempresa.
 
## Stack tecnologico
 
- Java 21
- Spring Boot 4.1
- Spring Security + JWT (stateless)
- Spring Data JPA / Hibernate
- H2 (desarrollo) / PostgreSQL (produccion)
- JaCoCo (cobertura) + ArchUnit (arquitectura)
- Docker (multi-stage build)
- GitHub Actions (CI/CD)
## Prerrequisitos
 
- JDK 21+
- Maven 3.9+ (o usar el wrapper incluido `./mvnw`)
## Ejecucion local
 
```bash
./mvnw spring-boot:run
```
 
La aplicacion inicia en `http://localhost:8080` con H2. En el perfil por defecto se crean dos empresas demo, cada una con su administrador:
 
| Empresa             | Usuario          | Contrasena |
|---------------------|------------------|------------|
| Empresa Demo S.A.S. | `admin@demo.com` | `admin123` |
| Empresa Beta S.A.S. | `admin@beta.com` | `admin123` |
 
Estos datos no se cargan en el perfil `prod`.
 
## Variables de entorno
 
| Variable                | Descripcion                           | Default                    |
|-------------------------|---------------------------------------|----------------------------|
| `JWT_SECRET`            | Clave para firmar tokens JWT          | Generada al iniciar (obligatoria en `prod`) |
| `JWT_EXPIRATION_MINUTES`| Duracion del access token en minutos  | `15`                       |
| `JWT_REFRESH_DAYS`      | Duracion del refresh token en dias    | `7`                        |
| `CORS_ALLOWED_ORIGINS`  | Origenes permitidos (separados por ,) | `http://localhost:4200`    |
| `DB_HOST`               | Host de PostgreSQL (perfil prod)      | `localhost`                |
| `DB_PORT`               | Puerto de PostgreSQL (perfil prod)    | `5432`                     |
| `DB_NAME`               | Nombre de la base (perfil prod)       | `procesos`                 |
| `DB_USER`               | Usuario de la base (perfil prod)      | `procesos`                 |
| `DB_PASSWORD`           | Contrasena de la base (perfil prod)   | —                          |
 
## Endpoints principales
 
| Recurso              | Base path                                                        |
|----------------------|------------------------------------------------------------------|
| Auth                 | `POST /api/v1/auth/login`, `/refresh`, `/logout`                 |
| Empresas             | `POST /api/v1/empresas`                                          |
| Procesos             | `/api/v1/procesos` (incluye `/{id}/historial` y `/{id}/diagrama`) |
| Procesos compartidos | `/api/v1/procesos/{id}/compartidos`, `/api/v1/procesos-compartidos` |
| Usuarios             | `/api/v1/usuarios`                                               |
| Invitaciones         | `/api/v1/usuarios/invitaciones` (`POST /{token}/aceptar`)        |
| Roles                | `/api/v1/roles`                                                  |
| Permisos estructura  | `/api/v1/permisos-estructura`                                    |
| Pools                | `/api/v1/procesos/{id}/pools`                                    |
| Lanes                | `/api/v1/pools/{id}/lanes` (`PATCH .../lanes/orden`)             |
| Actividades          | `/api/v1/lanes/{id}/actividades`                                 |
| Gateways             | `/api/v1/lanes/{id}/gateways`                                    |
| Eventos de mensaje   | `/api/v1/lanes/{id}/eventos-mensaje`                             |
| Arcos                | `POST /api/v1/arcos`, `GET /api/v1/pools/{id}/arcos`             |
| Mensajes             | `/api/v1/procesos/{id}/mensajes`                                 |
| Correlaciones        | `/api/v1/mensajes/{id}/correlacion`                              |
 
Documentacion interactiva disponible en `/swagger-ui/index.html` (solo fuera del perfil `prod`).
 
### Roles de acceso
 
| Rol            | Permisos                                                                 |
|----------------|--------------------------------------------------------------------------|
| `ADMINISTRADOR` | Todo, incluido gestionar usuarios, roles, permisos, compartir y eliminar |
| `EDITOR`        | Crear y editar procesos y elementos del modelo                           |
| `SOLO_LECTURA`  | Solo consultar                                                           |
 
Cada empresa solo ve sus propios datos: el `empresaId` se toma del token y un recurso de otra empresa responde 404.
 
## Tests
 
```bash
./mvnw verify
```
 
Ejecuta tests unitarios, de integracion y reglas de arquitectura (ArchUnit). JaCoCo exige un minimo del 50% de cobertura de linea.
 
Pruebas de API y de carga:
 
- `postman/`: colecciones `Facimus-HU-Flow` (flujo de las 28 HU) y `Facimus-Procesos-API` (catalogo de endpoints). Se ejecutan con Newman.
- `jmeter/`: planes de carga de login, CRUD de procesos, CRUD de roles y modelado BPMN.
## Docker
 
La imagen arranca con el perfil `prod`, asi que necesita PostgreSQL y `JWT_SECRET`:
 
```bash
docker build -t facimus-procesos .
docker run -p 8080:8080 \
  -e JWT_SECRET=mi_clave_secreta \
  -e DB_HOST=host.docker.internal -e DB_NAME=procesos \
  -e DB_USER=procesos -e DB_PASSWORD=mi_contrasena \
  facimus-procesos
```
 
En produccion se despliega con `deploy/compose.yml` en la VM beta-back (limite de 1 GB).
 
## CI/CD
 
El workflow `.github/workflows/ci.yml` corre en cada pull request y push a `main`:
 
1. Build, pruebas JUnit y cobertura JaCoCo.
2. Reglas de arquitectura (ArchUnit).
3. Analisis en SonarCloud.
4. Imagen Docker publicada en GHCR.
5. Colecciones Postman y planes JMeter contra un backend temporal en la VM (scripts en `ci/`).
6. Despliegue en la VM beta-back (solo `main`, con aprobacion manual).
## Estructura del proyecto
 
```
src/main/java/com/facimus/procesos/
  common/       Kernel compartido (EntidadEmpresa, excepciones, paginacion)
  config/       Inicializadores, OpenAPI
  security/     JWT, filtros, CORS
  gestion/      Modulo de gestion (empresas, usuarios, procesos, roles, historial)
  modelado/     Modulo de modelado BPMN (pools, lanes, nodos, arcos, mensajes)
postman/        Colecciones de pruebas de API
jmeter/         Planes de pruebas de carga
ci/             Scripts del pipeline
deploy/         compose.yml de produccion
sql/            Scripts SQL de cambios al esquema
```
 
