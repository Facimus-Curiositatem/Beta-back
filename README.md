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
