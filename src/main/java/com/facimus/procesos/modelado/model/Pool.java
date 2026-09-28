package com.facimus.procesos.modelado.model;

import java.util.LinkedHashSet;
import java.util.Set;

import com.facimus.procesos.common.EntidadEmpresa;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.model.RolAcceso;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pools")
public class Pool extends EntidadEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_participante", nullable = false)
    private TipoParticipante tipoParticipante;

    @Column(name = "caja_negra", nullable = false)
    private boolean cajaNegra = false;

    @Column(nullable = false)
    private int orden;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pool_roles_edicion", joinColumns = @JoinColumn(name = "pool_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol_acceso", nullable = false)
    private Set<RolAcceso> rolesEdicion = new LinkedHashSet<>(Set.of(
            RolAcceso.ADMINISTRADOR, RolAcceso.EDITOR));

    @ManyToOne(optional = false)
    @JoinColumn(name = "proceso_id", nullable = false)
    private Proceso proceso;
}
