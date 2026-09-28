package com.facimus.procesos.modelado.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.modelado.model.Correlacion;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.repository.CorrelacionRepository;
import com.facimus.procesos.modelado.repository.MensajeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CorrelacionService {

    private final CorrelacionRepository correlacionRepository;
    private final MensajeRepository mensajeRepository;
    private final AuditoriaModeladoService auditoriaModeladoService;

    @Transactional
    public Correlacion definir(Long empresaId, Long mensajeId, String criterio) {
        Mensaje mensaje = mensajeRepository.findByIdAndEmpresaId(mensajeId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado."));

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
        mensajeRepository.save(mensaje);
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
        mensajeRepository.findByIdAndEmpresaId(mensajeId, empresaId).ifPresent(mensaje -> {
            mensaje.setClaveCorrelacion(null);
            mensajeRepository.save(mensaje);
            auditoriaModeladoService.registrar(mensaje.getProceso(),
                    "Correlacion eliminada del mensaje " + mensaje.getNombre() + ".");
        });
    }

    private void validarAmbiguedad(Long empresaId, Mensaje mensaje, String criterio) {
        boolean ambiguo = mensajeRepository.findAllByProcesoIdAndEmpresaId(mensaje.getProceso().getId(), empresaId)
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
