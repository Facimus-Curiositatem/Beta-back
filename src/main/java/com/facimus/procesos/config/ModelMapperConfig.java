package com.facimus.procesos.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.AbstractConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.facimus.procesos.gestion.controller.dto.EmpresaResponse;
import com.facimus.procesos.gestion.controller.dto.HistorialCambioResponse;
import com.facimus.procesos.gestion.controller.dto.InvitacionUsuarioResponse;
import com.facimus.procesos.gestion.controller.dto.PermisoEstructuraResponse;
import com.facimus.procesos.gestion.controller.dto.ProcesoCompartidoResponse;
import com.facimus.procesos.gestion.controller.dto.ProcesoResponse;
import com.facimus.procesos.gestion.controller.dto.UsuarioResponse;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.HistorialCambio;
import com.facimus.procesos.gestion.model.InvitacionUsuario;
import com.facimus.procesos.gestion.model.PermisoEstructura;
import com.facimus.procesos.gestion.model.ProcesoCompartido;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.modelado.controller.dto.ActividadResponse;
import com.facimus.procesos.modelado.controller.dto.ArcoResponse;
import com.facimus.procesos.modelado.controller.dto.CorrelacionResponse;
import com.facimus.procesos.modelado.controller.dto.EventoMensajeResponse;
import com.facimus.procesos.modelado.controller.dto.GatewayResponse;
import com.facimus.procesos.modelado.controller.dto.LaneResponse;
import com.facimus.procesos.modelado.controller.dto.MensajeResponse;
import com.facimus.procesos.modelado.controller.dto.PoolResponse;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.Arco;
import com.facimus.procesos.modelado.model.Correlacion;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Gateway;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoActividad;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.addConverter(new AbstractConverter<Empresa, EmpresaResponse>() {
            @Override
            protected EmpresaResponse convert(Empresa e) {
                return new EmpresaResponse(e.getId(), e.getNombre(), e.getNit(),
                        e.getCorreoContacto(), e.getFechaRegistro());
            }
        });

        mapper.addConverter(new AbstractConverter<Usuario, UsuarioResponse>() {
            @Override
            protected UsuarioResponse convert(Usuario u) {
                return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(),
                        u.getRolAcceso(), u.isActivo());
            }
        });

        mapper.addConverter(new AbstractConverter<Proceso, ProcesoResponse>() {
            @Override
            protected ProcesoResponse convert(Proceso p) {
                return new ProcesoResponse(p.getId(), p.getNombre(), p.getDescripcion(),
                        p.getCategoria(), p.getEstado(), p.isActivo(),
                        p.getFechaCreacion(), p.getFechaModificacion());
            }
        });

        mapper.addConverter(new AbstractConverter<InvitacionUsuario, InvitacionUsuarioResponse>() {
            @Override
            protected InvitacionUsuarioResponse convert(InvitacionUsuario i) {
                return new InvitacionUsuarioResponse(i.getId(), i.getEmail(),
                        i.getRolAcceso(), i.getToken(), i.getFechaExpiracion());
            }
        });

        mapper.addConverter(new AbstractConverter<PermisoEstructura, PermisoEstructuraResponse>() {
            @Override
            protected PermisoEstructuraResponse convert(PermisoEstructura p) {
                return new PermisoEstructuraResponse(p.getRolAcceso(), p.isCrearPool(),
                        p.isEditarPool(), p.isEliminarPool(), p.isCrearLane(),
                        p.isEditarLane(), p.isEliminarLane());
            }
        });

        mapper.addConverter(new AbstractConverter<HistorialCambio, HistorialCambioResponse>() {
            @Override
            protected HistorialCambioResponse convert(HistorialCambio h) {
                return new HistorialCambioResponse(h.getId(), h.getFechaCambio(),
                        h.getDescripcionCambio(), h.getAutor().getNombre());
            }
        });

        mapper.addConverter(new AbstractConverter<ProcesoCompartido, ProcesoCompartidoResponse>() {
            @Override
            protected ProcesoCompartidoResponse convert(ProcesoCompartido c) {
                return new ProcesoCompartidoResponse(c.getId(), c.getProceso().getId(),
                        c.getProceso().getNombre(), c.getEmpresa().getId(),
                        c.getEmpresa().getNombre(), c.getEmpresaInvitada().getId(),
                        c.getEmpresaInvitada().getNombre(), c.isSoloLectura());
            }
        });

        mapper.addConverter(new AbstractConverter<Pool, PoolResponse>() {
            @Override
            protected PoolResponse convert(Pool p) {
                return new PoolResponse(p.getId(), p.getNombre(), p.getTipoParticipante(),
                        p.isCajaNegra(), p.getOrden(), p.getProceso().getId());
            }
        });

        mapper.addConverter(new AbstractConverter<Lane, LaneResponse>() {
            @Override
            protected LaneResponse convert(Lane l) {
                return new LaneResponse(l.getId(), l.getNombre(), l.getOrden(),
                        l.getPool().getId(), l.getRolProceso().getId(),
                        l.getRolProceso().getNombre());
            }
        });

        mapper.addConverter(new AbstractConverter<Actividad, ActividadResponse>() {
            @Override
            protected ActividadResponse convert(Actividad a) {
                TipoActividad tipo = a.getTipoActividad() != null
                        ? a.getTipoActividad() : TipoActividad.TAREA;
                return new ActividadResponse(a.getId(), a.getNombre(), a.getDescripcion(),
                        a.getPosicionX(), a.getPosicionY(), a.getLane().getId(), tipo);
            }
        });

        mapper.addConverter(new AbstractConverter<Gateway, GatewayResponse>() {
            @Override
            protected GatewayResponse convert(Gateway g) {
                return new GatewayResponse(g.getId(), g.getNombre(), g.getTipoGateway(),
                        g.getPosicionX(), g.getPosicionY(), g.getLane().getId());
            }
        });

        mapper.addConverter(new AbstractConverter<EventoMensaje, EventoMensajeResponse>() {
            @Override
            protected EventoMensajeResponse convert(EventoMensaje e) {
                return new EventoMensajeResponse(e.getId(), e.getNombre(), e.getTipoEvento(),
                        e.getContenido(), e.getClaveCorrelacion(), e.getPosicionX(),
                        e.getPosicionY(), e.getLane().getId(), e.isOrigenExterno());
            }
        });

        mapper.addConverter(new AbstractConverter<Arco, ArcoResponse>() {
            @Override
            protected ArcoResponse convert(Arco a) {
                return new ArcoResponse(a.getId(), a.getEtiqueta(), a.getCondicion(),
                        a.getOrigen().getId(), a.getDestino().getId(), a.getPool().getId());
            }
        });

        mapper.addConverter(new AbstractConverter<Correlacion, CorrelacionResponse>() {
            @Override
            protected CorrelacionResponse convert(Correlacion c) {
                return new CorrelacionResponse(c.getId(), c.getCriterio(),
                        c.getMensaje().getId());
            }
        });

        mapper.addConverter(new AbstractConverter<Mensaje, MensajeResponse>() {
            @Override
            protected MensajeResponse convert(Mensaje m) {
                return new MensajeResponse(m.getId(), m.getNombre(), m.getContenido(),
                        m.getPoolOrigen().getId(), m.getPoolDestino().getId(),
                        m.getProceso().getId(),
                        m.getEventoThrow() != null ? m.getEventoThrow().getId() : null,
                        m.getEventoCatch() != null ? m.getEventoCatch().getId() : null,
                        m.getClaveCorrelacion(), m.getTipoDestinoExterno(),
                        m.getDestinoExterno(), m.getPoliticaFalloNotificacion(),
                        m.getActividadError() != null ? m.getActividadError().getId() : null,
                        m.getPoliticaSinCaso(),
                        m.getEventoCatch() == null && m.getTipoDestinoExterno() == null);
            }
        });

        return mapper;
    }
}
