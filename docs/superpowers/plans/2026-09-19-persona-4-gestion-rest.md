# Persona 4: REST del módulo Gestión Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar en `feature/persona-4-gestion-rest` una API REST stateless y multiempresa para Empresa, Usuario, Proceso, RolProceso e HistorialCambio, integrada con los aportes previos de Personas 1–3.

**Architecture:** La rama parte de `feature/migracion-rest`, restaura el contrato corregido de Persona 2 e integra la autorización/anti-IDOR de Persona 3 sin modificar sus ramas. Los controllers usan `@AuthenticationPrincipal ApiPrincipal`, delegan reglas a services tenant-aware y producen DTOs, `PageResponse` y `ProblemDetail` bajo `/api/v1/**`.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Web MVC, Spring Security/JWT, Spring Data JPA, H2/PostgreSQL, Jakarta Validation, JUnit 5, Mockito, MockMvc, ArchUnit, JaCoCo y Maven Wrapper.

**Spec:** `docs/superpowers/specs/2026-09-19-persona-4-gestion-rest-design.md`

## Global Constraints

- La rama de entrega es `feature/persona-4-gestion-rest`, creada desde `feature/migracion-rest`.
- No modificar ni hacer push a `main`, `feature/migracion-rest`, `P2_Correction` o `persona-3-multitenancy-idor`.
- Toda API de Gestión vive bajo `/api/v1/**` y usa HTTP + JSON; no se introduce Thymeleaf, MVC server-side ni `HttpSession`.
- `empresaId` y `usuarioId` salen exclusivamente de `ApiPrincipal`; ningún request permite elegir tenant.
- `Proceso` y `RolProceso` usan eliminación lógica; `HistorialCambio` solo se inserta y consulta.
- Un ID de otro tenant responde como `404 Not Found`.
- Las mutaciones respetan la matriz `ADMINISTRADOR` / `EDITOR` / `SOLO_LECTURA` definida en la especificación.
- Los errores usan `application/problem+json`; las listas paginadas usan `PageResponse`.
- No cambiar comportamiento funcional de Modelado salvo resolver la integración fiel de Personas 2 y 3.
- No fusionar la rama ni abrir PR sin aprobación explícita; sí subir la rama terminada a `origin`.

## Review Focus

- Un `id` válido de otra empresa debe devolver `404`, nunca datos ni `403`; se cubre en Task 6 con dos tenants reales.
- `pagina=-1`, un enum desconocido o JSON malformado deben devolver `400 ProblemDetail`, nunca `500`; se cubre en Task 2.
- `PATCH /usuarios/{id}` con cuerpo vacío debe devolver `400` y no guardar; se cubre en Task 3.
- Un proceso inactivo no debe reaparecer por listado ni detalle, y su historial no debe exponerlo; se cubre en Task 4.
- Editar un rol debe responder su conteo real de uso y eliminar uno utilizado debe conservarlo activo con `409`; se cubre en Task 5.

---

### Task 1: Workspace aislado e integración de Personas 2 y 3

**Files:**
- Modify: `.gitignore`
- Merge/restore: `src/main/**`, `src/test/**`, `postman/**`, `jmeter/**` según los commits ya publicados por Personas 2 y 3
- Preserve: `docs/superpowers/specs/2026-09-19-persona-4-gestion-rest-design.md`

**Interfaces:**
- Consumes: `origin/feature/migracion-rest`, `origin/P2_Correction`, `origin/persona-3-multitenancy-idor`
- Produces: workspace enlazado sobre `feature/persona-4-gestion-rest`, `PageResponse<T>`, `ApiExceptionHandler`, `ApiPrincipal` en controllers y pruebas anti-IDOR integradas

- [ ] **Step 1: Crear el workspace aislado**

Seguir `superpowers:using-git-worktrees`. Añadir `.worktrees/` a `.gitignore` si aún no está ignorado, hacer commit `chore: ignorar worktrees locales`, volver el checkout principal a `feature/migracion-rest` y crear `.worktrees/persona-4-gestion-rest` sobre la rama de entrega.

- [ ] **Step 2: Detectar Java 21 y establecer la línea base**

Usar el runtime Java devuelto por `load_workspace_dependencies` si `JAVA_HOME` no está configurado. Ejecutar:

```powershell
.\mvnw.cmd test
```

Expected: si la rama base falla antes de integrar, guardar la salida exacta como línea base; no atribuir esos fallos a Persona 4.

- [ ] **Step 3: Restaurar el contrato revertido de Persona 2**

Aplicar el contenido revertido sin crear cambios en la rama origen:

```powershell
git revert --no-commit b916959
git commit -m "chore: restaurar contrato REST de persona 2"
git merge --no-ff origin/P2_Correction -m "chore: integrar correcciones REST de persona 2"
```

Expected: existen `common/api/PageResponse.java` y `common/api/ApiExceptionHandler.java`; las rutas aportadas por Persona 2 usan `/api/v1`.

- [ ] **Step 4: Integrar multitenancy y autorización de Persona 3**

```powershell
git merge --no-ff origin/persona-3-multitenancy-idor -m "chore: integrar multitenancy de persona 3"
```

Resolver cruces en controllers con esta combinación: ruta y semántica HTTP de Persona 2 + `@AuthenticationPrincipal ApiPrincipal` y reglas de seguridad de Persona 3. No elegir versiones completas con `--ours` o `--theirs` cuando eso descarte una de las dos intenciones.

- [ ] **Step 5: Verificar la integración antes del trabajo de Persona 4**

```powershell
.\mvnw.cmd test
git status --short
```

Expected: compilación limpia; cualquier prueba aún roja queda identificada por clase y causa antes de añadir funcionalidades de Persona 4; no hay archivos de conflicto ni cambios sin seguimiento accidentales.

---

### Task 2: Contrato transversal de Gestión y API stateless

**Files:**
- Modify: `src/main/java/com/facimus/procesos/security/SecurityConfig.java`
- Modify: `src/main/java/com/facimus/procesos/common/api/ApiExceptionHandler.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/controller/EmpresaController.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/controller/UsuarioController.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/controller/ProcesoController.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/controller/RolProcesoController.java`
- Create: `src/test/java/com/facimus/procesos/security/ApiPrincipalRequestPostProcessor.java`
- Modify: `src/test/java/com/facimus/procesos/arquitectura/AislamientoTenantTest.java`
- Modify: `src/test/java/com/facimus/procesos/security/AutorizacionPorRolTest.java`
- Modify: controller tests under `src/test/java/com/facimus/procesos/gestion/controller/`

**Interfaces:**
- Consumes: `ApiPrincipal(Long usuarioId, Long empresaId, RolAcceso rol, String email)`, `PageResponse.from(Page<T>)`
- Produces: rutas exclusivamente `/api/v1/**`, matriz de permisos centralizada, errores RFC 9457 mediante `ProblemDetail` y `ApiPrincipalRequestPostProcessor.principal(RolAcceso)` para pruebas MVC

- [ ] **Step 1: Escribir pruebas arquitectónicas y HTTP que fallen**

Extender `AislamientoTenantTest` para fijar que producción no depende de sesión y que controllers de Gestión dependen de `ApiPrincipal`:

```java
@Test
void gestion_no_depende_de_HttpSession() {
    noClasses().that().resideInAPackage("..gestion.controller..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("jakarta.servlet.http.HttpSession")
            .check(clases);
}
```

En `ProcesoControllerTest`, añadir casos para `pagina=-1`, `estado=DESCONOCIDO` y JSON malformado, verificando `400`, `application/problem+json`, `$.status == 400` y un `$.title` no vacío. Actualizar la matriz de `AutorizacionPorRolTest` para usar solo rutas `/api/v1`.

Crear un helper de pruebas que no simule sesiones:

```java
public final class ApiPrincipalRequestPostProcessor {
    private ApiPrincipalRequestPostProcessor() {}

    public static RequestPostProcessor principal(RolAcceso rol) {
        ApiPrincipal principal = new ApiPrincipal(1L, 1L, rol, "test@acme.com");
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }
}
```

- [ ] **Step 2: Ejecutar las pruebas para comprobar el fallo**

```powershell
.\mvnw.cmd -Dtest="AislamientoTenantTest,AutorizacionPorRolTest,EmpresaControllerTest,UsuarioControllerTest,ProcesoControllerTest,RolProcesoControllerTest" test
```

Expected: FAIL por rutas antiguas, sesión o manejo incompleto de parámetros inválidos.

- [ ] **Step 3: Unificar rutas, principal y seguridad**

Todos los métodos protegidos reciben:

```java
@AuthenticationPrincipal ApiPrincipal principal
```

y pasan `principal.empresaId()` / `principal.usuarioId()` al servicio. `SecurityConfig` conserva únicamente `/api/v1/...`, permite `POST /api/v1/empresas` y `POST /api/v1/auth/login`, y expresa la matriz:

```java
.requestMatchers("/api/v1/usuarios/**").hasAuthority(ADMINISTRADOR)
.requestMatchers(HttpMethod.GET, "/api/v1/**").authenticated()
.requestMatchers("/api/v1/roles/**").hasAuthority(ADMINISTRADOR)
.requestMatchers(HttpMethod.DELETE, "/api/v1/procesos/**").hasAuthority(ADMINISTRADOR)
.requestMatchers("/api/v1/**").hasAnyAuthority(ADMINISTRADOR, EDITOR)
```

Eliminar el puente de `SesionActiva` de producción si Persona 3 no lo eliminó durante el merge.

- [ ] **Step 4: Completar `ProblemDetail` para entradas inválidas**

En `ApiExceptionHandler`, sobrescribir los manejadores de Spring MVC necesarios para devolver `ProblemDetail` con `status`, `title`, `detail` e `instance` en validación, JSON ilegible y conversión de parámetros. Mantener el catch-all con log interno y detalle público genérico.

- [ ] **Step 5: Ejecutar y dejar verde el contrato transversal**

```powershell
.\mvnw.cmd -Dtest="AislamientoTenantTest,AutorizacionPorRolTest,EmpresaControllerTest,UsuarioControllerTest,ProcesoControllerTest,RolProcesoControllerTest" test
```

Expected: PASS; no controller de Gestión usa `HttpSession`; `400/401/403` tienen forma `ProblemDetail`.

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/com/facimus/procesos/security/SecurityConfig.java src/main/java/com/facimus/procesos/common/api/ApiExceptionHandler.java src/main/java/com/facimus/procesos/gestion/controller src/test/java/com/facimus/procesos/arquitectura/AislamientoTenantTest.java src/test/java/com/facimus/procesos/security/ApiPrincipalRequestPostProcessor.java src/test/java/com/facimus/procesos/security/AutorizacionPorRolTest.java src/test/java/com/facimus/procesos/gestion/controller
git commit -m "refactor: alinear gestión con JWT y contrato api v1"
```

---

### Task 3: Empresa y ciclo de vida de usuarios

**Files:**
- Create: `src/main/java/com/facimus/procesos/gestion/controller/dto/ActualizarUsuarioRequest.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/controller/EmpresaController.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/controller/UsuarioController.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/service/UsuarioService.java`
- Modify: `src/test/java/com/facimus/procesos/gestion/controller/EmpresaControllerTest.java`
- Modify: `src/test/java/com/facimus/procesos/gestion/controller/UsuarioControllerTest.java`
- Modify: `src/test/java/com/facimus/procesos/gestion/service/UsuarioServiceTest.java`

**Interfaces:**
- Consumes: `EmpresaService.registrar(...)`, `UsuarioService.obtener(Long empresaId, Long usuarioId)`
- Produces: `UsuarioService.actualizar(Long empresaId, Long usuarioId, RolAcceso rolAcceso, Boolean activo)`, `PATCH /api/v1/usuarios/{id}`

- [ ] **Step 1: Escribir pruebas fallidas de empresa y usuario**

Añadir a los tests REST:

```java
mockMvc.perform(post("/api/v1/empresas").contentType(APPLICATION_JSON).content(registroValido))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/empresas/1"));

mockMvc.perform(patch("/api/v1/usuarios/5").with(principal(RolAcceso.ADMINISTRADOR))
        .contentType(APPLICATION_JSON)
        .content("{\"rolAcceso\":\"ADMINISTRADOR\",\"activo\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rolAcceso").value("ADMINISTRADOR"))
        .andExpect(jsonPath("$.activo").value(false));

mockMvc.perform(patch("/api/v1/usuarios/5").with(principal(RolAcceso.ADMINISTRADOR))
        .contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
```

En `UsuarioServiceTest`, fijar actualización solo de rol, solo de estado, ambas, tenant incorrecto y que `desactivar` persiste `activo=false`.

- [ ] **Step 2: Ejecutar los tests y verificar el fallo**

```powershell
.\mvnw.cmd -Dtest="EmpresaControllerTest,UsuarioControllerTest,EmpresaServiceTest,UsuarioServiceTest" test
```

Expected: FAIL porque `ActualizarUsuarioRequest` y `UsuarioService.actualizar` no existen o PATCH no acepta estado.

- [ ] **Step 3: Implementar el DTO y servicio mínimos**

Crear:

```java
public record ActualizarUsuarioRequest(RolAcceso rolAcceso, Boolean activo) {
    @AssertTrue(message = "Debe enviar al menos rolAcceso o activo.")
    public boolean isActualizacionPresente() {
        return rolAcceso != null || activo != null;
    }
}
```

Implementar:

```java
@Transactional
public Usuario actualizar(Long empresaId, Long usuarioId, RolAcceso rolAcceso, Boolean activo) {
    Usuario usuario = obtener(empresaId, usuarioId);
    if (rolAcceso != null) usuario.setRolAcceso(rolAcceso);
    if (activo != null) usuario.setActivo(activo);
    return usuarioRepository.save(usuario);
}
```

`DELETE` llama a `desactivar`; `PATCH` llama a `actualizar`. `POST` de empresa y usuario construye `Location` a partir del ID persistido.

- [ ] **Step 4: Ejecutar los tests de usuario y empresa**

```powershell
.\mvnw.cmd -Dtest="EmpresaControllerTest,UsuarioControllerTest,EmpresaServiceTest,UsuarioServiceTest" test
```

Expected: PASS, incluida la validación del PATCH vacío y el aislamiento por empresa.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/facimus/procesos/gestion/controller/dto/ActualizarUsuarioRequest.java src/main/java/com/facimus/procesos/gestion/controller/EmpresaController.java src/main/java/com/facimus/procesos/gestion/controller/UsuarioController.java src/main/java/com/facimus/procesos/gestion/service/UsuarioService.java src/test/java/com/facimus/procesos/gestion/controller/EmpresaControllerTest.java src/test/java/com/facimus/procesos/gestion/controller/UsuarioControllerTest.java src/test/java/com/facimus/procesos/gestion/service/UsuarioServiceTest.java
git commit -m "feat: completar api de empresas y usuarios"
```

---

### Task 4: Procesos, cambios de estado e historial

**Files:**
- Modify: `src/main/java/com/facimus/procesos/gestion/controller/dto/EditarProcesoRequest.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/controller/dto/CambiarEstadoProcesoRequest.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/controller/ProcesoController.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/repository/ProcesoRepository.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/service/ProcesoService.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/service/HistorialCambioService.java`
- Modify: `src/test/java/com/facimus/procesos/gestion/controller/ProcesoControllerTest.java`
- Modify: `src/test/java/com/facimus/procesos/gestion/service/ProcesoServiceTest.java`
- Create: `src/test/java/com/facimus/procesos/gestion/service/HistorialCambioServiceTest.java`

**Interfaces:**
- Consumes: `PageResponse.from(Page<ProcesoResponse>)`, `ApiPrincipal.usuarioId()` y repositorios tenant-aware
- Produces: `ProcesoService.editarDatos(...)`, `ProcesoService.cambiarEstado(...)`, `GET /api/v1/procesos/{id}/historial`

- [ ] **Step 1: Escribir pruebas fallidas del contrato de procesos**

Cubrir en `ProcesoControllerTest`:

```java
mockMvc.perform(get("/api/v1/procesos?nombre=ven&estado=BORRADOR&categoria=Operativo&pagina=2")
        .with(principal(RolAcceso.SOLO_LECTURA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.size").value(10));

mockMvc.perform(patch("/api/v1/procesos/7").with(principal(RolAcceso.EDITOR))
        .contentType(APPLICATION_JSON).content("{\"estado\":\"PUBLICADO\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("PUBLICADO"));

mockMvc.perform(get("/api/v1/procesos/7/historial").with(principal(RolAcceso.SOLO_LECTURA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].descripcionCambio").value("Proceso publicado."));
```

En `ProcesoServiceTest`, añadir casos para: detalle activo, detalle inactivo como `404`, edición que no cambia estado, publicación que registra autor, intento `PUBLICADO -> BORRADOR` como `ReglaNegocioException`, baja lógica e historial. En `HistorialCambioServiceTest`, verificar empresa/proceso/autor/fecha/descripcion al registrar y delegación de consulta con ambos IDs.

- [ ] **Step 2: Ejecutar tests focalizados y comprobar el fallo**

```powershell
.\mvnw.cmd -Dtest="ProcesoControllerTest,ProcesoServiceTest,HistorialCambioServiceTest" test
```

Expected: FAIL por endpoint de historial ausente, firmas nuevas ausentes o acceso a proceso inactivo.

- [ ] **Step 3: Separar edición de datos y transición de estado**

`EditarProcesoRequest` contiene únicamente `nombre`, `descripcion` y `categoria`. El servicio expone:

```java
Proceso editarDatos(Long empresaId, Long procesoId, Long usuarioId,
        String nombre, String descripcion, String categoria);

Proceso cambiarEstado(Long empresaId, Long procesoId, Long usuarioId,
        EstadoProceso nuevoEstado);
```

`cambiarEstado` permite `BORRADOR -> PUBLICADO`, mantiene idempotente la petición al estado actual sin duplicar historial y rechaza `PUBLICADO -> BORRADOR` con `ReglaNegocioException`.

- [ ] **Step 4: Excluir procesos inactivos y exponer historial**

Añadir al repositorio:

```java
Optional<Proceso> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);
```

`ProcesoService.obtener` usa esa consulta. El endpoint `/historial` primero llama a `procesoService.obtener(empresaId, id)` para validar tenant y actividad, luego mapea `historialCambioService.listarPorProceso(empresaId, id)` a `HistorialCambioResponse`.

- [ ] **Step 5: Mantener paginación, filtros y auditoría**

Validar `@RequestParam(defaultValue = "0") @Min(0) int pagina`; construir `PageRequest.of(pagina, 10, Sort.by("fechaModificacion").descending())`; mapear mediante `PageResponse.from`. Registrar exactamente un historial en creación, edición real, publicación y baja lógica.

- [ ] **Step 6: Ejecutar tests focalizados**

```powershell
.\mvnw.cmd -Dtest="ProcesoControllerTest,ProcesoServiceTest,HistorialCambioServiceTest" test
```

Expected: PASS; filtros se delegan, página tiene metadatos estables, inactivos devuelven `404` e historial queda ordenado por el repositorio.

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/com/facimus/procesos/gestion/controller/dto/EditarProcesoRequest.java src/main/java/com/facimus/procesos/gestion/controller/dto/CambiarEstadoProcesoRequest.java src/main/java/com/facimus/procesos/gestion/controller/ProcesoController.java src/main/java/com/facimus/procesos/gestion/repository/ProcesoRepository.java src/main/java/com/facimus/procesos/gestion/service/ProcesoService.java src/main/java/com/facimus/procesos/gestion/service/HistorialCambioService.java src/test/java/com/facimus/procesos/gestion/controller/ProcesoControllerTest.java src/test/java/com/facimus/procesos/gestion/service/ProcesoServiceTest.java src/test/java/com/facimus/procesos/gestion/service/HistorialCambioServiceTest.java
git commit -m "feat: completar procesos e historial de cambios"
```

---

### Task 5: Roles de proceso y uso real

**Files:**
- Modify: `src/main/java/com/facimus/procesos/gestion/controller/RolProcesoController.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/repository/RolProcesoRepository.java`
- Modify: `src/main/java/com/facimus/procesos/gestion/service/RolProcesoService.java`
- Modify: `src/test/java/com/facimus/procesos/gestion/controller/RolProcesoControllerTest.java`
- Modify: `src/test/java/com/facimus/procesos/gestion/service/RolProcesoServiceTest.java`

**Interfaces:**
- Consumes: `LaneRepository.findAllByRolProcesoIdAndEmpresaId(Long rolId, Long empresaId)`
- Produces: `RolProcesoService.contarUsos(Long empresaId, Long rolId)`, respuestas `RolProcesoVistaResponse` coherentes después de crear, consultar y editar

- [ ] **Step 1: Escribir pruebas fallidas de roles**

Añadir casos que verifiquen:

```java
mockMvc.perform(post("/api/v1/roles").with(principal(RolAcceso.ADMINISTRADOR))
        .contentType(APPLICATION_JSON).content(rolValido))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/roles/5"));

mockMvc.perform(put("/api/v1/roles/5").with(principal(RolAcceso.ADMINISTRADOR))
        .contentType(APPLICATION_JSON).content(rolEditado))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.procesosQueLoUsan").value(2))
        .andExpect(jsonPath("$.enUso").value(true));
```

En servicio: obtener rol inactivo devuelve `RecursoNoEncontradoException`; eliminar rol usado lanza conflicto y no guarda; eliminar rol libre guarda `activo=false`; conteo usa tenant e ID.

- [ ] **Step 2: Ejecutar tests y comprobar el fallo**

```powershell
.\mvnw.cmd -Dtest="RolProcesoControllerTest,RolProcesoServiceTest" test
```

Expected: FAIL si editar devuelve uso cero, obtener acepta inactivos o `contarUsos` no está integrado.

- [ ] **Step 3: Implementar consulta activa, conteo y soft delete**

Añadir:

```java
Optional<RolProceso> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);

public long contarUsos(Long empresaId, Long rolId) {
    obtener(empresaId, rolId);
    return laneRepository.findAllByRolProcesoIdAndEmpresaId(rolId, empresaId).size();
}
```

Crear y editar responden el uso real; obtener no recorre el listado completo, sino que obtiene el rol tenant-aware y calcula su uso. Eliminar mantiene la verificación de lanes y cambia `activo=false` sin `delete` físico.

- [ ] **Step 4: Ejecutar tests de roles**

```powershell
.\mvnw.cmd -Dtest="RolProcesoControllerTest,RolProcesoServiceTest" test
```

Expected: PASS, incluidos `Location`, uso real, `404` tenant-aware y `409` cuando está en uso.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/facimus/procesos/gestion/controller/RolProcesoController.java src/main/java/com/facimus/procesos/gestion/repository/RolProcesoRepository.java src/main/java/com/facimus/procesos/gestion/service/RolProcesoService.java src/test/java/com/facimus/procesos/gestion/controller/RolProcesoControllerTest.java src/test/java/com/facimus/procesos/gestion/service/RolProcesoServiceTest.java
git commit -m "feat: completar api de roles de proceso"
```

---

### Task 6: Integración multiempresa y regresión completa

**Files:**
- Modify: `src/test/java/com/facimus/procesos/security/AislamientoEmpresasIntegracionTest.java`
- Modify: `src/test/java/com/facimus/procesos/security/AutorizacionPorRolTest.java`
- Modify: `postman/Facimus-Procesos-API.postman_collection.json` only if its Gestión requests do not match the final contract
- Modify: `README.md` only where its REST endpoint inventory is stale

**Interfaces:**
- Consumes: API REST terminada, JWT real, H2 y datos de dos empresas
- Produces: evidencia end-to-end de permisos, anti-IDOR, soft delete, trazabilidad y contrato `/api/v1`

- [ ] **Step 1: Extender la integración con dos tenants**

Crear datos de Empresa A y Empresa B, autenticar usuarios reales y fijar estos escenarios en `AislamientoEmpresasIntegracionTest`:

```text
GET    /api/v1/usuarios/{usuarioB}             con token A -> 404
GET    /api/v1/procesos/{procesoB}             con token A -> 404
PATCH  /api/v1/procesos/{procesoB}             con token A -> 404
GET    /api/v1/procesos/{procesoB}/historial   con token A -> 404
GET    /api/v1/roles/{rolB}                    con token A -> 404
DELETE /api/v1/roles/{rolB}                    con token A -> 404
```

Añadir un flujo del tenant correcto: crear proceso, editarlo, publicarlo, consultar cuatro eventos de historial y eliminarlo; después, listado y detalle no lo exponen.

- [ ] **Step 2: Ejecutar integración y observar cualquier fallo**

```powershell
.\mvnw.cmd -Dtest="AislamientoEmpresasIntegracionTest,AutorizacionPorRolTest,SeguridadIntegracionTest" test
```

Expected: PASS. Si falla, corregir únicamente la capa responsable y repetir primero la prueba específica.

- [ ] **Step 3: Actualizar ejemplos consumibles**

Alinear las solicitudes de Gestión en Postman y el inventario REST del README con `/api/v1`, `PATCH` de usuario/proceso, endpoint de historial y formato de paginación. No alterar ejemplos funcionales de Modelado.

- [ ] **Step 4: Ejecutar verificación completa**

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -Dtest="*ArquitecturaTest,*TenantTest" test
git diff --check origin/feature/migracion-rest...HEAD
git status --short
```

Expected: Maven `BUILD SUCCESS`, todas las pruebas en verde, JaCoCo por encima del 50% configurado, ArchUnit en verde, diff sin errores de whitespace y árbol limpio.

- [ ] **Step 5: Revisar el diff completo**

Usar `superpowers:requesting-code-review` y revisar específicamente: filtrado por tenant, rutas heredadas `/api/`, dependencias de `HttpSession`, borrados físicos, exposición de entidades JPA, errores que devuelvan `500` para entradas del cliente y cambios accidentales en Modelado.

- [ ] **Step 6: Aplicar observaciones y volver a verificar**

Por cada hallazgo aceptado, escribir o reforzar primero la prueba que lo reproduce, aplicar el cambio mínimo y repetir `mvnw.cmd clean verify`.

- [ ] **Step 7: Commit final de QA/documentación**

```powershell
git add src/main src/test README.md postman/Facimus-Procesos-API.postman_collection.json
git commit -m "test: validar gestión rest multiempresa"
```

Omitir el commit si no quedan cambios después de los commits funcionales.

- [ ] **Step 8: Verificación previa a publicación**

Usar `superpowers:verification-before-completion`; guardar el resumen real de pruebas, cobertura y cualquier bloqueo. Confirmar:

```powershell
git branch --show-current
git log --oneline origin/feature/migracion-rest..HEAD
git status --short
```

Expected: rama `feature/persona-4-gestion-rest`, commits convencionales esperados y árbol limpio.

- [ ] **Step 9: Publicar solo la rama de Persona 4**

```powershell
git push -u origin feature/persona-4-gestion-rest
```

Expected: push aceptado; no crear ni fusionar PR. Entregar el enlace de la rama y un reporte honesto de cambios, pruebas, cobertura y bloqueos.
