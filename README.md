# Beta-back

Backend del **Sistema de Gestion de Procesos multiempresa** — Primera entrega
(Curso Desarrollo Web): aplicacion web con **Spring Boot + Thymeleaf + JPA**.

Modelo de dominio completo (13 entidades / 4 enums), logica de negocio en
capas (servicios) y vistas server-side con Thymeleaf para **empresas,
usuarios, procesos y roles de proceso**.

Implementado siguiendo la arquitectura consolidada del curso (rama
`feature/consolidado-arquitectura` de la wiki del proyecto) y las historias
de usuario HU-01 a HU-28.

## Como ejecutar

Requiere JDK 21. No necesitas tener Maven instalado: el proyecto trae su
propio wrapper.

```bash
./mvnw spring-boot:run
```

En Windows (cmd/PowerShell):

```bash
mvnw.cmd spring-boot:run
```

La aplicacion queda disponible en `http://localhost:8080`. Redirige
automaticamente a `/login`.

**Usuario de demostracion** (creado automaticamente en el primer arranque,
ver `DatosDemoInitializer`):

- Correo: `admin@demo.com`
- Contrasena: `admin123`

Los datos se guardan en una base H2 embebida en archivo (`./data/procesos.mv.db`,
excluida de git). Para partir de cero, detén la aplicacion y borra la carpeta
`data/`.

## Arquitectura

Empaquetado modular por dominio, en dos bloques (igual que el enunciado del
curso):

```
com.facimus.procesos
├── common/       EntidadEmpresa (MappedSuperclass multiempresa), excepciones,
│                 RepositorioTenant<T> (contrato base de repositorios)
├── config/       Sesion HttpSession, interceptor de autenticacion, seed de datos
├── gestion/      Empresa, Usuario, Proceso, HistorialCambio, RolProceso
│   ├── model/  repository/  service/  controller/
└── modelado/     Pool, Lane, NodoFlujo (Actividad/Gateway), Arco, Mensaje, Correlacion
    ├── model/  repository/  service/
```

**Multitenencia**: toda entidad que pertenece a una empresa extiende
`EntidadEmpresa` (columna `empresa_id`); los repositorios exponen metodos
`*AndEmpresaId` y acotan cada consulta por el `empresaId` guardado en la
sesion — nunca se toma de la URL o del formulario.

**Autenticacion (Entrega 1)**: login propio con `HttpSession`
(`empresaId`, `usuarioId`, `rolAcceso`). Spring Security llega en la Entrega
final; por ahora solo se usa `spring-security-crypto` para cifrar
contrasenas con BCrypt.

**Validacion de negocio**: en la capa de servicio, antes de persistir. Las
violaciones lanzan `ReglaNegocioException`, que el controlador (o el
`@ControllerAdvice` global como respaldo) traduce en un mensaje de error
visible en el formulario.

**Herencia JPA**: `NodoFlujo` (Actividad/Gateway) usa `SINGLE_TABLE` con
columna discriminadora `tipo_nodo`.

**Eliminacion logica**: `Proceso` y `RolProceso` tienen campo `activo`; nunca
se hace `DELETE` fisico sobre ellos, para preservar trazabilidad
(HU-06, HU-19). Los elementos del diagrama (`Pool`, `Lane`, `Arco`, etc.) sin
uso si se eliminan fisicamente, tal como especifica el modelo de datos
consolidado.

Documentos de referencia (wiki del proyecto, rama
`feature/consolidado-arquitectura`, carpeta `ConsolidadoArquitectura/`):
`modelo-datos.md`, `servicios.md`, `controladores-entrega1.md`.

## Alcance de esta entrega

Vistas Thymeleaf (22 rutas) solo para el bloque **gestion**: sesion,
registro de empresa, usuarios, procesos y roles de proceso. El editor visual
del diagrama (pools, lanes, actividades, arcos, gateways, mensajes,
correlaciones) se construye en la Entrega 2 con la API REST + Angular; en
esta entrega esas 8 entidades y sus servicios ya existen en el dominio (para
que `Proceso` pueda crear su pool inicial y el modelo quede completo), pero
sin controladores ni vistas propias todavia.

### Historias de usuario cubiertas por las vistas de esta entrega

| Bloque | HU | Vistas |
|---|---|---|
| Empresas y usuarios | HU-01, HU-02, HU-03 | `/empresas/registro`, `/usuarios/**`, `/login` |
| Procesos | HU-04 a HU-07 | `/procesos/**` (crear, editar, publicar, eliminar logico, buscar+filtrar+paginar, detalle+historial) |
| Roles de proceso | HU-17 a HU-20 | `/roles/**` (crear, editar, eliminar validando uso, listar con indicador de uso) |

Las HU-08 a HU-16, HU-21 a HU-28 (modelado del diagrama) tienen su logica de
dominio y servicios ya implementados (`modelado/service/`), listos para
exponerse via API REST en la Entrega 2.

### Simplificaciones deliberadas de esta entrega

- **Invitacion por correo (HU-02, criterio 1)**: se reemplaza por alta
  directa con contrasena definida por el administrador; no hay envio de
  correos en la Entrega 1.
- **Login por correo (HU-03)**: el modelo de datos exige email unico *por
  empresa*, no global, pero el formulario de login solo pide correo +
  contrasena (sin pedir la empresa). Se resuelve buscando el usuario por
  correo sin acotar por tenant unicamente en esa consulta puntual — la unica
  excepcion documentada a la regla de "todo repositorio filtra por
  empresaId".
- **Historial de cambios de roles (HU-18, HU-19)**: el modelo de
  `HistorialCambio` esta atado obligatoriamente a un `Proceso`, por lo que
  los cambios sobre `RolProceso` (que es transversal a la empresa, no a un
  proceso) no generan entrada de historial en esta entrega.

## Pruebas

```bash
./mvnw test
```

Verifica que el contexto de Spring arranca correctamente con las 13
entidades y los 11 repositorios JPA.
