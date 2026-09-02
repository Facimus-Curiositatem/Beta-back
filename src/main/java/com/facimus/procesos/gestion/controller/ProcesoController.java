package com.facimus.procesos.gestion.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.controller.dto.ProcesoForm;
import com.facimus.procesos.gestion.model.EstadoProceso;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.service.HistorialCambioService;
import com.facimus.procesos.gestion.service.ProcesoService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-04 a HU-07: creacion, edicion, eliminacion logica y consulta de procesos. */
@Controller
@RequiredArgsConstructor
public class ProcesoController {

    private static final int TAMANO_PAGINA = 10;

    private final ProcesoService procesoService;
    private final HistorialCambioService historialCambioService;

    @GetMapping("/procesos")
    public String lista(@RequestParam(required = false) String nombre,
            @RequestParam(required = false) EstadoProceso estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "0") int pagina,
            HttpSession session, Model model) {
        Long empresaId = SesionActiva.empresaId(session);
        Page<Proceso> procesos = procesoService.buscar(empresaId, nombre, estado, categoria,
                PageRequest.of(pagina, TAMANO_PAGINA, Sort.by("fechaModificacion").descending()));

        model.addAttribute("procesos", procesos);
        model.addAttribute("nombre", nombre);
        model.addAttribute("estado", estado);
        model.addAttribute("categoria", categoria);
        model.addAttribute("estados", EstadoProceso.values());
        return "procesos/lista";
    }

    @GetMapping("/procesos/nuevo")
    public String formularioNuevo(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!SesionActiva.puedeEditar(session)) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para crear procesos.");
            return "redirect:/procesos";
        }
        model.addAttribute("procesoForm", new ProcesoForm());
        model.addAttribute("esNuevo", true);
        return "procesos/formulario";
    }

    @PostMapping("/procesos")
    public String crear(@Validated @ModelAttribute("procesoForm") ProcesoForm form, BindingResult bindingResult,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!SesionActiva.puedeEditar(session)) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para crear procesos.");
            return "redirect:/procesos";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("esNuevo", true);
            return "procesos/formulario";
        }
        try {
            Long empresaId = SesionActiva.empresaId(session);
            Long usuarioId = SesionActiva.usuarioId(session);
            Proceso proceso = procesoService.crear(empresaId, usuarioId, form.getNombre(), form.getDescripcion(),
                    form.getCategoria());
            return "redirect:/procesos/" + proceso.getId();
        } catch (ReglaNegocioException ex) {
            bindingResult.rejectValue("nombre", "regla.negocio", ex.getMessage());
            model.addAttribute("esNuevo", true);
            return "procesos/formulario";
        }
    }

    @GetMapping("/procesos/{id}")
    public String detalle(@PathVariable Long id, HttpSession session, Model model) {
        Long empresaId = SesionActiva.empresaId(session);
        Proceso proceso = procesoService.obtener(empresaId, id);
        model.addAttribute("proceso", proceso);
        model.addAttribute("historial", historialCambioService.listarPorProceso(empresaId, id));
        return "procesos/detalle";
    }

    @GetMapping("/procesos/{id}/editar")
    public String formularioEditar(@PathVariable Long id, HttpSession session, Model model,
            RedirectAttributes redirectAttributes) {
        if (!SesionActiva.puedeEditar(session)) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para editar procesos.");
            return "redirect:/procesos/" + id;
        }
        Long empresaId = SesionActiva.empresaId(session);
        Proceso proceso = procesoService.obtener(empresaId, id);

        ProcesoForm form = new ProcesoForm();
        form.setNombre(proceso.getNombre());
        form.setDescripcion(proceso.getDescripcion());
        form.setCategoria(proceso.getCategoria());
        form.setEstado(proceso.getEstado());

        model.addAttribute("procesoForm", form);
        model.addAttribute("procesoId", id);
        model.addAttribute("esNuevo", false);
        return "procesos/formulario";
    }

    @PostMapping("/procesos/{id}")
    public String editar(@PathVariable Long id, @Validated @ModelAttribute("procesoForm") ProcesoForm form,
            BindingResult bindingResult, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!SesionActiva.puedeEditar(session)) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para editar procesos.");
            return "redirect:/procesos/" + id;
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("procesoId", id);
            model.addAttribute("esNuevo", false);
            return "procesos/formulario";
        }
        try {
            Long empresaId = SesionActiva.empresaId(session);
            Long usuarioId = SesionActiva.usuarioId(session);
            procesoService.editar(empresaId, id, usuarioId, form.getNombre(), form.getDescripcion(),
                    form.getCategoria(), form.getEstado());
            return "redirect:/procesos/" + id;
        } catch (ReglaNegocioException ex) {
            bindingResult.rejectValue("nombre", "regla.negocio", ex.getMessage());
            model.addAttribute("procesoId", id);
            model.addAttribute("esNuevo", false);
            return "procesos/formulario";
        }
    }

    @PostMapping("/procesos/{id}/publicar")
    public String publicar(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!SesionActiva.puedeEditar(session)) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para publicar procesos.");
            return "redirect:/procesos/" + id;
        }
        Long empresaId = SesionActiva.empresaId(session);
        Long usuarioId = SesionActiva.usuarioId(session);
        procesoService.publicar(empresaId, id, usuarioId);
        return "redirect:/procesos/" + id;
    }

    @PostMapping("/procesos/{id}/eliminar")
    public String eliminar(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!SesionActiva.esAdministrador(session)) {
            redirectAttributes.addFlashAttribute("error", "Solo un administrador puede eliminar procesos.");
            return "redirect:/procesos/" + id;
        }
        Long empresaId = SesionActiva.empresaId(session);
        Long usuarioId = SesionActiva.usuarioId(session);
        procesoService.eliminarLogico(empresaId, id, usuarioId);
        return "redirect:/procesos";
    }
}
