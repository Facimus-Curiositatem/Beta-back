package com.facimus.procesos.gestion.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.model.ProcesoCompartido;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.repository.EmpresaRepository;
import com.facimus.procesos.gestion.repository.ProcesoCompartidoRepository;
import com.facimus.procesos.gestion.repository.ProcesoRepository;
import com.facimus.procesos.gestion.repository.UsuarioRepository;
import com.facimus.procesos.gestion.service.dto.ProcesoCompartidoDetalle;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.Arco;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Gateway;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.repository.ArcoRepository;
import com.facimus.procesos.modelado.repository.LaneRepository;
import com.facimus.procesos.modelado.repository.MensajeRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;
import com.facimus.procesos.modelado.repository.PoolRepository;

import lombok.RequiredArgsConstructor;

/** HU-23: comparticion de procesos entre empresas en modo solo lectura. */
@Service
@RequiredArgsConstructor
public class ProcesoCompartidoService {

    private final ProcesoCompartidoRepository procesoCompartidoRepository;
    private final ProcesoRepository procesoRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PoolRepository poolRepository;
    private final LaneRepository laneRepository;
    private final NodoFlujoRepository nodoFlujoRepository;
    private final ArcoRepository arcoRepository;
    private final MensajeRepository mensajeRepository;
    private final HistorialCambioService historialCambioService;

    @Transactional
    public ProcesoCompartido compartir(Long empresaId, Long usuarioId, Long procesoId, Long empresaInvitadaId) {
        Proceso proceso = procesoRepository.findByIdAndEmpresaIdAndActivoTrue(procesoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado."));
        if (empresaId.equals(empresaInvitadaId)) {
            throw new ReglaNegocioException("No es necesario compartir un proceso con la empresa propietaria.");
        }
        Empresa invitada = empresaRepository.findById(empresaInvitadaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa invitada no encontrada."));
        Usuario autor = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        ProcesoCompartido compartido = procesoCompartidoRepository
                .findByProcesoIdAndEmpresaIdAndEmpresaInvitadaId(procesoId, empresaId, empresaInvitadaId)
                .orElseGet(ProcesoCompartido::new);
        compartido.setEmpresa(proceso.getEmpresa());
        compartido.setProceso(proceso);
        compartido.setEmpresaInvitada(invitada);
        compartido.setSoloLectura(true);
        compartido.setActivo(true);
        compartido = procesoCompartidoRepository.save(compartido);

        historialCambioService.registrar(proceso, autor,
                "Proceso compartido en modo solo lectura con " + invitada.getNombre() + ".");
        return compartido;
    }

    @Transactional
    public void dejarDeCompartir(Long empresaId, Long usuarioId, Long procesoId, Long empresaInvitadaId) {
        Proceso proceso = procesoRepository.findByIdAndEmpresaIdAndActivoTrue(procesoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado."));
        Usuario autor = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
        ProcesoCompartido compartido = procesoCompartidoRepository
                .findByProcesoIdAndEmpresaIdAndEmpresaInvitadaIdAndActivoTrue(
                        procesoId, empresaId, empresaInvitadaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La empresa indicada no tiene acceso compartido."));
        compartido.setActivo(false);
        procesoCompartidoRepository.save(compartido);
        historialCambioService.registrar(proceso, autor,
                "Se retiro el acceso compartido a la empresa " + compartido.getEmpresaInvitada().getNombre() + ".");
    }

    public List<ProcesoCompartido> listarCompartidosPorPropietario(Long empresaId, Long procesoId) {
        if (!procesoRepository.existsByIdAndEmpresaId(procesoId, empresaId)) {
            throw new RecursoNoEncontradoException("Proceso no encontrado.");
        }
        return procesoCompartidoRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(procesoId, empresaId);
    }

    public List<ProcesoCompartido> listarRecibidos(Long empresaInvitadaId) {
        return procesoCompartidoRepository.findAllByEmpresaInvitadaIdAndActivoTrue(empresaInvitadaId);
    }

    public ProcesoCompartidoDetalle obtenerCompartido(Long empresaInvitadaId, Long procesoId) {
        ProcesoCompartido compartido = procesoCompartidoRepository
                .findByProcesoIdAndEmpresaInvitadaIdAndActivoTrue(procesoId, empresaInvitadaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso compartido no encontrado."));
        Long empresaPropietariaId = compartido.getEmpresa().getId();

        List<Pool> pools = poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(procesoId, empresaPropietariaId);
        List<Lane> lanes = laneRepository.findAllByPool_ProcesoIdAndEmpresaId(procesoId, empresaPropietariaId);
        var nodos = nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(procesoId, empresaPropietariaId).stream()
                .filter(nodo -> nodo.isActivo()).toList();
        List<Actividad> actividades = nodos.stream()
                .filter(Actividad.class::isInstance).map(Actividad.class::cast).toList();
        List<Gateway> gateways = nodos.stream()
                .filter(Gateway.class::isInstance).map(Gateway.class::cast).toList();
        List<EventoMensaje> eventos = nodos.stream()
                .filter(EventoMensaje.class::isInstance).map(EventoMensaje.class::cast).toList();
        List<Arco> arcos = arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(procesoId, empresaPropietariaId).stream()
                .filter(Arco::isActivo).toList();
        List<Mensaje> mensajes = mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(
                procesoId, empresaPropietariaId);

        return new ProcesoCompartidoDetalle(compartido.getProceso(), compartido, pools, lanes,
                actividades, gateways, eventos, arcos, mensajes);
    }
}
