package com.facimus.procesos.common.api;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException ex, WebRequest req) {
        return construir(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), req);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail manejarReglaNegocio(ReglaNegocioException ex, WebRequest req) {
        return construir(HttpStatus.CONFLICT, "Regla de negocio violada", ex.getMessage(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail manejarNoAutenticado(AuthenticationException ex, WebRequest req) {
        return construir(HttpStatus.UNAUTHORIZED, "No autenticado", "Credenciales inválidas o token ausente", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail manejarSinPermiso(AccessDeniedException ex, WebRequest req) {
        return construir(HttpStatus.FORBIDDEN, "Sin permisos", "No tienes permisos para esta operación", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(MethodArgumentNotValidException ex, WebRequest req) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return construir(HttpStatus.BAD_REQUEST, "Validación fallida", detalle, req);
    }
    
    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarErrorInesperado(Exception ex, WebRequest req) {
        log.error("Error inesperado en {}", req.getDescription(false), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Intenta nuevamente más tarde.", req);
    }
    
    private ProblemDetail construir(HttpStatus status, String titulo, String detalle, WebRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setTitle(titulo);
        pd.setInstance(java.net.URI.create(req.getDescription(false).replace("uri=", "")));
        return pd;
    }
}