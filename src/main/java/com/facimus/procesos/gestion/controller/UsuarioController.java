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
import com.facimus.procesos.gestion.controller.dto.UsuarioForm;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.service.UsuarioService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-02: administracion de colaboradores de la empresa (solo administrador). */
@Controller
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/usuarios")
    public String lista(HttpSession session, Model model) {
        if (!esAdministrador(session)) {
            return "redirect:/procesos";
        }
        Long empresaId = SesionActiva.empresaId(session);
        model.addAttribute("usuarios", usuarioService.listarPorEmpresa(empresaId));
        return "usuarios/lista";
    }

    @GetMapping("/usuarios/nuevo")
    public String formularioNuevo(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!esAdministrador(session)) {
            redirectAttributes.addFlashAttribute("error", "Solo un administrador puede crear usuarios.");
            return "redirect:/procesos";
        }
        model.addAttribute("usuarioForm", new UsuarioForm());
        model.addAttribute("esNuevo", true);
        return "usuarios/formulario";
    }

    @PostMapping("/usuarios")
    public String crear(@Validated @ModelAttribute("usuarioForm") UsuarioForm form, BindingResult bindingResult,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!esAdministrador(session)) {
            redirectAttributes.addFlashAttribute("error", "Solo un administrador puede crear usuarios.");
            return "redirect:/procesos";
        }
        if (bindingResult.hasErrors() || form.getPassword() == null || form.getPassword().isBlank()) {
            if (form.getPassword() == null || form.getPassword().isBlank()) {
                bindingResult.rejectValue("password", "obligatoria", "La contrasena es obligatoria.");
            }
            model.addAttribute("esNuevo", true);
            return "usuarios/formulario";
        }
        try {
            Long empresaId = SesionActiva.empresaId(session);
            usuarioService.crearColaborador(empresaId, form.getNombre(), form.getEmail(), form.getPassword(),
                    form.getRolAcceso());
            return "redirect:/usuarios";
        } catch (ReglaNegocioException ex) {
            bindingResult.rejectValue("email", "regla.negocio", ex.getMessage());
            model.addAttribute("esNuevo", true);
            return "usuarios/formulario";
        }
    }

    @GetMapping("/usuarios/{id}/editar")
    public String formularioEditar(@PathVariable Long id, HttpSession session, Model model,
            RedirectAttributes redirectAttributes) {
        if (!esAdministrador(session)) {
            redirectAttributes.addFlashAttribute("error", "Solo un administrador puede editar usuarios.");
            return "redirect:/procesos";
        }
        Long empresaId = SesionActiva.empresaId(session);
        Usuario usuario = usuarioService.obtener(empresaId, id);

        UsuarioForm form = new UsuarioForm();
        form.setNombre(usuario.getNombre());
        form.setEmail(usuario.getEmail());
        form.setRolAcceso(usuario.getRolAcceso());

        model.addAttribute("usuarioForm", form);
        model.addAttribute("usuarioId", id);
        model.addAttribute("esNuevo", false);
        return "usuarios/formulario";
    }

    @PostMapping("/usuarios/{id}")
    public String editarRol(@PathVariable Long id, @ModelAttribute("usuarioForm") UsuarioForm form,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!esAdministrador(session)) {
            redirectAttributes.addFlashAttribute("error", "Solo un administrador puede editar usuarios.");
            return "redirect:/procesos";
        }
        Long empresaId = SesionActiva.empresaId(session);
        usuarioService.cambiarRolAcceso(empresaId, id, form.getRolAcceso());
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/desactivar")
    public String desactivar(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!esAdministrador(session)) {
            redirectAttributes.addFlashAttribute("error", "Solo un administrador puede desactivar usuarios.");
            return "redirect:/procesos";
        }
        Long empresaId = SesionActiva.empresaId(session);
        usuarioService.desactivar(empresaId, id);
        return "redirect:/usuarios";
    }

    private boolean esAdministrador(HttpSession session) {
        return SesionActiva.esAdministrador(session);
    }
}
