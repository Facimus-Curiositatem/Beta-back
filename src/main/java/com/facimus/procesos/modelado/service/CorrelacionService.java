package com.facimus.procesos.modelado.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.modelado.model.Correlacion;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.repository.CorrelacionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CorrelacionService {

    private final CorrelacionRepository correlacionRepository;
    private final MensajeService mensajeService;
    private final AuditoriaModeladoService auditoriaModeladoService;

    @Transactional
    public Correlacion definir(Long empresaId, Long mensajeId, String criterio) {
        Mensaje mensaje = mensajeService.obtener(empresaId, mensajeId);

        validarAmbiguedad(empresaId, mensaje, criterio);

        EventoMensaje eventoThrow = mensaje.getEventoThrow();
        EventoMensaje eventoCatch = mensaje.getEventoCatch();
        if (eventoThrow != null && !criterio.equalsIgnoreCase(eventoThrow.getClaveCorrelacion())) {
            throw new ReglaNegocioException("La correlacion debe coincidir con la definida en el Message Throw.");
        }
        if (eventoCatch != null && !criterio.equalsIgnoreCase(eventoCatch.getClaveCorrelacion())) {
            throw new ReglaNegocioException("La correlacion debe coincidir con la definida en el Message Catch.");
        }

        Correlacion correlacion = correlacionRepository.findByMensajeIdAndEmpresaId(mensajeId, empresaId)
                .orElseGet(() -> {
                    Correlacion nueva = new Correlacion();
                    nueva.setEmpresa(mensaje.getEmpresa());
                    nueva.setMensaje(mensaje);
                    return nueva;
                });
        correlacion.setCriterio(criterio);
        mensaje.setClaveCorrelacion(criterio);
        mensajeService.guardar(mensaje);
        correlacion = correlacionRepository.save(correlacion);
        auditoriaModeladoService.registrar(mensaje.getProceso(),
                "Correlacion actualizada para el mensaje " + mensaje.getNombre() + ".");
        return correlacion;
    }

    public Correlacion obtener(Long empresaId, Long mensajeId) {
        return correlacionRepository.findByMensajeIdAndEmpresaId(mensajeId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Este mensaje no tiene correlacion definida."));
    }

    @Transactional
    public void eliminar(Long empresaId, Long mensajeId) {
        Correlacion correlacion = correlacionRepository.findByMensajeIdAndEmpresaId(mensajeId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Este mensaje no tiene correlacion definida."));
        correlacionRepository.delete(correlacion);
        mensajeService.buscar(empresaId, mensajeId).ifPresent(mensaje -> {
            mensaje.setClaveCorrelacion(null);
            mensajeService.guardar(mensaje);
            auditoriaModeladoService.registrar(mensaje.getProceso(),
                    "Correlacion eliminada del mensaje " + mensaje.getNombre() + ".");
        });
    }

    @Transactional
    public Correlacion guardar(Correlacion correlacion) {
        return correlacionRepository.save(correlacion);
    }

    public Optional<Correlacion> buscarPorMensaje(Long empresaId, Long mensajeId) {
        return correlacionRepository.findByMensajeIdAndEmpresaId(mensajeId, empresaId);
    }

    @Transactional
    public void eliminarPorMensaje(Long empresaId, Long mensajeId) {
        correlacionRepository.findByMensajeIdAndEmpresaId(mensajeId, empresaId)
                .ifPresent(correlacionRepository::delete);
    }

    private void validarAmbiguedad(Long empresaId, Mensaje mensaje, String criterio) {
        boolean ambiguo = mensajeService.listarTodosPorProceso(empresaId, mensaje.getProceso().getId())
                .stream()
                .filter(otro -> !otro.getId().equals(mensaje.getId()))
                .anyMatch(otro -> otro.getNombre().equalsIgnoreCase(mensaje.getNombre())
                        && criterio.equalsIgnoreCase(otro.getClaveCorrelacion() != null
                                ? otro.getClaveCorrelacion() : ""));
        if (ambiguo) {
            throw new ReglaNegocioException(
                    "La combinacion nombre y clave de correlacion es ambigua dentro del proceso.");
        }
    }
}
