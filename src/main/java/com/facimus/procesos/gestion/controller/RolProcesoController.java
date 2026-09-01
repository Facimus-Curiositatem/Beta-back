package com.facimus.procesos.gestion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.controller.dto.RolProcesoForm;
import com.facimus.procesos.gestion.model.RolProceso;
import com.facimus.procesos.gestion.service.RolProcesoService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-17 a HU-20: roles de proceso (solo administrador crea/edita/elimina). */
@Controller
@RequiredArgsConstructor
public class RolProcesoController {

    private final RolProcesoService rolProcesoService;

    @GetMapping("/roles")
    public String lista(HttpSession session, Model model) {
        Long empresaId = SesionActiva.empresaId(session);
        model.addAttribute("roles", rolProcesoService.listarConUso(empresaId));
        return "roles/lista";
    }

    @GetMapping("/roles/nuevo")
    public String formularioNuevo(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!SesionActiva.esAdministrador(session)) {
            redirectAttributes.addFlashAttribute("error", "Solo un administrador puede crear roles de proceso.");
            return "redirect:/roles";
        }
        model.addAttribute("rolProcesoForm", new RolProcesoForm());
        model.addAttribute("esNuevo", true);
        return "roles/formulario";
    }

    @PostMapping("/roles")
    public String crear(@Validated @ModelAttribute("rolProcesoForm") RolProcesoForm form, BindingResult bindingResult,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!SesionActiva.esAdministrador(session)) {
            redirectAttributes.addFlashAttribute("error", "Solo un administrador puede crear roles de proceso.");
            return "redirect:/roles";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("esNuevo", true);
            return "roles/formulario";
        }
        try {
            Long empresaId = SesionActiva.empresaId(session);
            rolProcesoService.crear(empresaId, form.getNombre(), form.getDescripcion());
            return "redirect:/roles";
        } catch (ReglaNegocioException ex) {
            bindingResult.rejectValue("nombre", "regla.negocio", ex.getMessage());
            model.addAttribute("esNuevo", true);
            return "roles/formulario";
        }
    }

    @GetMapping("/roles/{id}/editar")
    public String formularioEditar(@PathVariable Long id, HttpSession session, Model model,
            RedirectAttributes redirectAttributes) {
        if (!SesionActiva.esAdministrador(session)) {
            redirectAttributes.addFlashAttribute("error", "Solo un administrador puede editar roles de proceso.");
            return "redirect:/roles";
        }
        Long empresaId = SesionActiva.empresaId(session);
        RolProceso rol = rolProcesoService.obtener(empresaId, id);

        RolProcesoForm form = new RolProcesoForm();
        form.setNombre(rol.getNombre());
        form.setDescripcion(rol.getDescripcion());

        model.addAttribute("rolProcesoForm", form);
        model.addAttribute("rolId", id);
        model.addAttribute("esNuevo", false);
        return "roles/formulario";
    }

    @PostMapping("/roles/{id}")
    public String editar(@PathVariable Long id, @Validated @ModelAttribute("rolProcesoForm") RolProcesoForm form,
            BindingResult bindingResult, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!SesionActiva.esAdministrador(session)) {
            redirectAttributes.addFlashAttribute("error", "Solo un administrador puede editar roles de proceso.");
            return "redirect:/roles";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("rolId", id);
            model.addAttribute("esNuevo", false);
            return "roles/formulario";
        }
        try {
            Long empresaId = SesionActiva.empresaId(session);
            rolProcesoService.editar(empresaId, id, form.getNombre(), form.getDescripcion());
            return "redirect:/roles";
        } catch (ReglaNegocioException ex) {
            bindingResult.rejectValue("nombre", "regla.negocio", ex.getMessage());
            model.addAttribute("rolId", id);
            model.addAttribute("esNuevo", false);
            return "roles/formulario";
        }
    }

    @PostMapping("/roles/{id}/eliminar")
    public String eliminar(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!SesionActiva.esAdministrador(session)) {
            redirectAttributes.addFlashAttribute("error", "Solo un administrador puede eliminar roles de proceso.");
            return "redirect:/roles";
        }
        Long empresaId = SesionActiva.empresaId(session);
        rolProcesoService.eliminar(empresaId, id);
        return "redirect:/roles";
    }
}
