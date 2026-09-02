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
import com.facimus.procesos.gestion.controller.dto.RegistroEmpresaForm;
import com.facimus.procesos.gestion.service.EmpresaService;

import lombok.RequiredArgsConstructor;

/** HU-01: registro de una nueva empresa y su administrador inicial. */
@Controller
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    @GetMapping("/empresas/registro")
    public String formulario(Model model) {
        if (!model.containsAttribute("registroEmpresaForm")) {
            model.addAttribute("registroEmpresaForm", new RegistroEmpresaForm());
        }
        return "empresas/registro";
    }

    @PostMapping("/empresas/registro")
    public String registrar(@Validated @ModelAttribute("registroEmpresaForm") RegistroEmpresaForm form,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "empresas/registro";
        }
        try {
            empresaService.registrar(form.getNombreEmpresa(), form.getNit(), form.getCorreoContacto(),
                    form.getNombreAdmin(), form.getEmailAdmin(), form.getPasswordAdmin());
            redirectAttributes.addFlashAttribute("mensaje", "Empresa registrada. Ya puedes iniciar sesion.");
            return "redirect:/login";
        } catch (ReglaNegocioException ex) {
            bindingResult.rejectValue("nit", "regla.negocio", ex.getMessage());
            return "empresas/registro";
        }
    }
}
