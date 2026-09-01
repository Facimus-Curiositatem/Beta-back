package com.facimus.procesos.gestion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.controller.dto.LoginForm;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.service.UsuarioService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-03: inicio y cierre de sesion. */
@Controller
@RequiredArgsConstructor
public class SesionController {

    private final UsuarioService usuarioService;

    @GetMapping("/login")
    public String formulario(Model model) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        return "sesion/login";
    }

    @PostMapping("/login")
    public String autenticar(@Validated @ModelAttribute("loginForm") LoginForm form, BindingResult bindingResult,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "sesion/login";
        }
        try {
            Usuario usuario = usuarioService.autenticar(form.getEmail(), form.getPassword());
            session.setAttribute(SesionActiva.EMPRESA_ID, usuario.getEmpresa().getId());
            session.setAttribute(SesionActiva.USUARIO_ID, usuario.getId());
            session.setAttribute(SesionActiva.ROL_ACCESO, usuario.getRolAcceso());
            session.setAttribute(SesionActiva.NOMBRE_USUARIO, usuario.getNombre());
            session.setAttribute(SesionActiva.NOMBRE_EMPRESA, usuario.getEmpresa().getNombre());
            return "redirect:/procesos";
        } catch (ReglaNegocioException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
