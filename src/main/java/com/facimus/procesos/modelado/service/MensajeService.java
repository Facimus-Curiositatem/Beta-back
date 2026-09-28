package com.facimus.procesos.modelado.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.repository.ProcesoRepository;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.Correlacion;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.model.PoliticaFalloNotificacion;
import com.facimus.procesos.modelado.model.PoliticaMensajeSinCaso;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoDestinoExterno;
import com.facimus.procesos.modelado.model.TipoEventoMensaje;
import com.facimus.procesos.modelado.model.TipoParticipante;
import com.facimus.procesos.modelado.repository.CorrelacionRepository;
import com.facimus.procesos.modelado.repository.MensajeRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;
import com.facimus.procesos.modelado.repository.PoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final PoolRepository poolRepository;
    private final ProcesoRepository procesoRepository;
    private final CorrelacionRepository correlacionRepository;
    private final NodoFlujoRepository nodoFlujoRepository;
    private final AuditoriaModeladoService auditoriaModeladoService;

    @Transactional
    public Mensaje crear(Long empresaId, Long procesoId, String nombre, String contenido, Long poolOrigenId,
            Long poolDestinoId) {
        return crearInterno(empresaId, procesoId, nombre, contenido, poolOrigenId, poolDestinoId,
                null, null, "idProceso", null, null, null, null,
                PoliticaMensajeSinCaso.DESCARTAR, false);
    }

    @Transactional
    public Mensaje crear(Long empresaId, Long procesoId, String nombre, String contenido, Long poolOrigenId,
            Long poolDestinoId, Long eventoThrowId, Long eventoCatchId, String claveCorrelacion,
            TipoDestinoExterno tipoDestinoExterno, String destinoExterno,
            PoliticaFalloNotificacion politicaFalloNotificacion, Long actividadManejoErrorId,
            PoliticaMensajeSinCaso politicaSinCaso) {
        return crearInterno(empresaId, procesoId, nombre, contenido, poolOrigenId, poolDestinoId,
                eventoThrowId, eventoCatchId, claveCorrelacion, tipoDestinoExterno, destinoExterno,
                politicaFalloNotificacion, actividadManejoErrorId, politicaSinCaso, true);
    }

    private Mensaje crearInterno(Long empresaId, Long procesoId, String nombre, String contenido, Long poolOrigenId,
            Long poolDestinoId, Long eventoThrowId, Long eventoCatchId, String claveCorrelacion,
            TipoDestinoExterno tipoDestinoExterno, String destinoExterno,
            PoliticaFalloNotificacion politicaFalloNotificacion, Long actividadManejoErrorId,
            PoliticaMensajeSinCaso politicaSinCaso, boolean exigirThrow) {
        if (poolOrigenId.equals(poolDestinoId)) {
            throw new ReglaNegocioException("Un mensaje debe conectar dos pools diferentes.");
        }
        Proceso proceso = procesoRepository.findByIdAndEmpresaId(procesoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado."));
        Pool poolOrigen = poolRepository.findByIdAndEmpresaId(poolOrigenId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool de origen no encontrado."));
        Pool poolDestino = poolRepository.findByIdAndEmpresaId(poolDestinoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool de destino no encontrado."));
        if (!poolOrigen.getProceso().getId().equals(procesoId) || !poolDestino.getProceso().getId().equals(procesoId)) {
            throw new ReglaNegocioException("Los pools del mensaje deben pertenecer al proceso indicado.");
        }

        EventoMensaje eventoThrow = resolverEvento(empresaId, eventoThrowId, "Message Throw");
        EventoMensaje eventoCatch = resolverEvento(empresaId, eventoCatchId, "Message Catch");
        if (exigirThrow && eventoThrow == null) {
            throw new ReglaNegocioException("El mensaje debe estar asociado a un evento Message Throw.");
        }

        String correlacion = StringUtils.hasText(claveCorrelacion) ? claveCorrelacion : "idProceso";
        if (eventoThrow != null) {
            validarThrow(eventoThrow, poolOrigen, nombre, correlacion);
        }
        if (eventoCatch != null) {
            validarCatch(eventoCatch, poolDestino, nombre, correlacion);
        }

        Actividad actividadError = resolverActividadError(empresaId, procesoId, actividadManejoErrorId);
        validarDestinoExterno(poolDestino, eventoCatch, tipoDestinoExterno, destinoExterno,
                politicaFalloNotificacion, actividadError);
        validarAmbiguedad(empresaId, procesoId, nombre, correlacion, null);

        Mensaje mensaje = new Mensaje();
        mensaje.setEmpresa(proceso.getEmpresa());
        mensaje.setProceso(proceso);
        mensaje.setNombre(nombre);
        mensaje.setContenido(contenido);
        mensaje.setPoolOrigen(poolOrigen);
        mensaje.setPoolDestino(poolDestino);
        mensaje.setEventoThrow(eventoThrow);
        mensaje.setEventoCatch(eventoCatch);
        mensaje.setClaveCorrelacion(correlacion);
        mensaje.setTipoDestinoExterno(tipoDestinoExterno);
        mensaje.setDestinoExterno(destinoExterno);
        mensaje.setPoliticaFalloNotificacion(politicaFalloNotificacion);
        mensaje.setActividadManejoError(actividadError);
        mensaje.setPoliticaSinCaso(politicaSinCaso != null ? politicaSinCaso : PoliticaMensajeSinCaso.DESCARTAR);
        mensaje = mensajeRepository.save(mensaje);

        Correlacion entidadCorrelacion = new Correlacion();
        entidadCorrelacion.setEmpresa(proceso.getEmpresa());
        entidadCorrelacion.setMensaje(mensaje);
        entidadCorrelacion.setCriterio(correlacion);
        correlacionRepository.save(entidadCorrelacion);

        auditoriaModeladoService.registrar(proceso, "Mensaje creado: " + mensaje.getNombre() + ".");
        return mensaje;
    }

    @Transactional
    public Mensaje editar(Long empresaId, Long mensajeId, String nombre, String contenido) {
        Mensaje mensaje = obtener(empresaId, mensajeId);
        return editar(empresaId, mensajeId, nombre, contenido,
                mensaje.getEventoCatch() != null ? mensaje.getEventoCatch().getId() : null,
                mensaje.getClaveCorrelacion(), mensaje.getTipoDestinoExterno(), mensaje.getDestinoExterno(),
                mensaje.getPoliticaFalloNotificacion(),
                mensaje.getActividadManejoError() != null ? mensaje.getActividadManejoError().getId() : null,
                mensaje.getPoliticaSinCaso());
    }

    @Transactional
    public Mensaje editar(Long empresaId, Long mensajeId, String nombre, String contenido, Long eventoCatchId,
            String claveCorrelacion, TipoDestinoExterno tipoDestinoExterno, String destinoExterno,
            PoliticaFalloNotificacion politicaFalloNotificacion, Long actividadManejoErrorId,
            PoliticaMensajeSinCaso politicaSinCaso) {
        Mensaje mensaje = obtener(empresaId, mensajeId);
        EventoMensaje eventoCatch = resolverEvento(empresaId, eventoCatchId, "Message Catch");
        String correlacion = StringUtils.hasText(claveCorrelacion) ? claveCorrelacion : mensaje.getClaveCorrelacion();
        if (mensaje.getEventoThrow() != null) {
            validarThrow(mensaje.getEventoThrow(), mensaje.getPoolOrigen(), nombre, correlacion);
        }
        if (eventoCatch != null) {
            validarCatch(eventoCatch, mensaje.getPoolDestino(), nombre, correlacion);
        }
        Actividad actividadError = resolverActividadError(empresaId, mensaje.getProceso().getId(), actividadManejoErrorId);
        validarDestinoExterno(mensaje.getPoolDestino(), eventoCatch, tipoDestinoExterno, destinoExterno,
                politicaFalloNotificacion, actividadError);
        validarAmbiguedad(empresaId, mensaje.getProceso().getId(), nombre, correlacion, mensajeId);

        mensaje.setNombre(nombre);
        mensaje.setContenido(contenido);
        mensaje.setEventoCatch(eventoCatch);
        mensaje.setClaveCorrelacion(correlacion);
        mensaje.setTipoDestinoExterno(tipoDestinoExterno);
        mensaje.setDestinoExterno(destinoExterno);
        mensaje.setPoliticaFalloNotificacion(politicaFalloNotificacion);
        mensaje.setActividadManejoError(actividadError);
        mensaje.setPoliticaSinCaso(politicaSinCaso != null ? politicaSinCaso : PoliticaMensajeSinCaso.DESCARTAR);
        mensaje = mensajeRepository.save(mensaje);

        Mensaje mensajeActual = mensaje;
        Correlacion correlacionEntidad = correlacionRepository.findByMensajeIdAndEmpresaId(mensajeId, empresaId)
                .orElseGet(() -> {
                    Correlacion nueva = new Correlacion();
                    nueva.setEmpresa(mensajeActual.getEmpresa());
                    nueva.setMensaje(mensajeActual);
                    return nueva;
                });
        correlacionEntidad.setCriterio(correlacion);
        correlacionRepository.save(correlacionEntidad);
        auditoriaModeladoService.registrar(mensaje.getProceso(), "Mensaje editado: " + mensaje.getNombre() + ".");
        return mensaje;
    }

    @Transactional
    public void eliminar(Long empresaId, Long mensajeId) {
        Mensaje mensaje = obtener(empresaId, mensajeId);
        var proceso = mensaje.getProceso();
        String nombre = mensaje.getNombre();
        Optional<Correlacion> correlacion = correlacionRepository.findByMensajeIdAndEmpresaId(mensajeId, empresaId);
        correlacion.ifPresent(correlacionRepository::delete);
        mensajeRepository.delete(mensaje);
        auditoriaModeladoService.registrar(proceso, "Mensaje eliminado: " + nombre + ".");
    }

    public List<Mensaje> listarPorProceso(Long empresaId, Long procesoId) {
        if (!procesoRepository.existsByIdAndEmpresaId(procesoId, empresaId)) {
            throw new RecursoNoEncontradoException("Proceso no encontrado.");
        }
        return mensajeRepository.findAllByProcesoIdAndEmpresaId(procesoId, empresaId);
    }

    public Mensaje obtener(Long empresaId, Long mensajeId) {
        return mensajeRepository.findByIdAndEmpresaId(mensajeId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado."));
    }

    private EventoMensaje resolverEvento(Long empresaId, Long eventoId, String etiqueta) {
        if (eventoId == null) {
            return null;
        }
        return nodoFlujoRepository.findByIdAndEmpresaId(eventoId, empresaId)
                .filter(EventoMensaje.class::isInstance)
                .map(EventoMensaje.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException(etiqueta + " no encontrado."));
    }

    private Actividad resolverActividadError(Long empresaId, Long procesoId, Long actividadId) {
        if (actividadId == null) {
            return null;
        }
        Actividad actividad = nodoFlujoRepository.findByIdAndEmpresaId(actividadId, empresaId)
                .filter(Actividad.class::isInstance)
                .map(Actividad.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad de manejo de error no encontrada."));
        if (!actividad.getLane().getPool().getProceso().getId().equals(procesoId)) {
            throw new ReglaNegocioException("La actividad de manejo de error debe pertenecer al mismo proceso.");
        }
        return actividad;
    }

    private void validarThrow(EventoMensaje evento, Pool poolOrigen, String nombre, String claveCorrelacion) {
        if (evento.getTipoEvento() != TipoEventoMensaje.THROW) {
            throw new ReglaNegocioException("El evento de origen debe ser de tipo THROW.");
        }
        if (!evento.getLane().getPool().getId().equals(poolOrigen.getId())) {
            throw new ReglaNegocioException("El Message Throw debe pertenecer al pool de origen.");
        }
        validarNombreYCorrelacion(evento, nombre, claveCorrelacion);
    }

    private void validarCatch(EventoMensaje evento, Pool poolDestino, String nombre, String claveCorrelacion) {
        if (evento.getTipoEvento() == TipoEventoMensaje.THROW) {
            throw new ReglaNegocioException("El evento receptor debe ser un Message Catch.");
        }
        if (!evento.getLane().getPool().getId().equals(poolDestino.getId())) {
            throw new ReglaNegocioException("El Message Catch debe pertenecer al pool de destino.");
        }
        validarNombreYCorrelacion(evento, nombre, claveCorrelacion);
    }

    private void validarNombreYCorrelacion(EventoMensaje evento, String nombre, String claveCorrelacion) {
        if (!evento.getNombre().equalsIgnoreCase(nombre)) {
            throw new ReglaNegocioException("El nombre del evento de mensaje debe coincidir con el mensaje.");
        }
        if (StringUtils.hasText(claveCorrelacion)
                && !claveCorrelacion.equalsIgnoreCase(evento.getClaveCorrelacion())) {
            throw new ReglaNegocioException("La clave de correlacion debe coincidir entre Throw, Catch y mensaje.");
        }
    }

    private void validarDestinoExterno(Pool poolDestino, EventoMensaje eventoCatch,
            TipoDestinoExterno tipoDestinoExterno, String destinoExterno,
            PoliticaFalloNotificacion politicaFalloNotificacion, Actividad actividadError) {
        if (tipoDestinoExterno == null) {
            return;
        }
        if (poolDestino.getTipoParticipante() != TipoParticipante.SISTEMA_EXTERNO || !poolDestino.isCajaNegra()) {
            throw new ReglaNegocioException(
                    "Un destino externo debe representarse con un pool SISTEMA_EXTERNO de caja negra.");
        }
        if (!StringUtils.hasText(destinoExterno)) {
            throw new ReglaNegocioException("Debe documentarse el destino externo del mensaje.");
        }
        if (eventoCatch != null) {
            throw new ReglaNegocioException("Un mensaje a un sistema externo no requiere Message Catch interno.");
        }
        if (politicaFalloNotificacion == null) {
            throw new ReglaNegocioException("Debe indicarse que ocurre en el modelo si la notificacion falla.");
        }
        if (politicaFalloNotificacion == PoliticaFalloNotificacion.DERIVAR_ACTIVIDAD && actividadError == null) {
            throw new ReglaNegocioException("Debe indicar la actividad de manejo de error.");
        }
    }

    private void validarAmbiguedad(Long empresaId, Long procesoId, String nombre, String clave, Long mensajeActualId) {
        boolean ambiguo = mensajeRepository.findAllByProcesoIdAndEmpresaId(procesoId, empresaId).stream()
                .filter(m -> mensajeActualId == null || !m.getId().equals(mensajeActualId))
                .anyMatch(m -> m.getNombre().equalsIgnoreCase(nombre)
                        && clave.equalsIgnoreCase(m.getClaveCorrelacion() != null ? m.getClaveCorrelacion() : ""));
        if (ambiguo) {
            throw new ReglaNegocioException(
                    "Ya existe otro mensaje con el mismo nombre y clave de correlacion en este proceso.");
        }
    }
}
