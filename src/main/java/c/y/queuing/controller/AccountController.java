package c.y.queuing.controller;

import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import c.y.queuing.entity.AppUser;
import c.y.queuing.repository.AppUserRepository;

@Controller
public class AccountController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/account/password")
    public String changePasswordForm(Model model, Principal principal, Authentication authentication) {
        AppUser user = appUserRepository.findByUsername(principal.getName()).orElseThrow();

        String backUrl = "/lobby";
        String backLabel = "Back";

        if (authentication != null) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            if (isAdmin) {
                backUrl = "/admin/config";
                backLabel = "Back to Admin Config";
            }
        }

        model.addAttribute("appUser", user);
        model.addAttribute("backUrl", backUrl);
        model.addAttribute("backLabel", backLabel);

        return "account-password";
    }

    @PostMapping("/account/password")
    public String changePassword(
            Principal principal,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword) {

        AppUser user = appUserRepository.findByUsername(principal.getName()).orElseThrow();

        String cleanCurrent = currentPassword == null ? "" : currentPassword.trim();
        String cleanNew = newPassword == null ? "" : newPassword.trim();
        String cleanConfirm = confirmPassword == null ? "" : confirmPassword.trim();

        if (!passwordEncoder.matches(cleanCurrent, user.getPassword())) {
            return "redirect:/account/password?currentInvalid";
        }

        if (cleanNew.isEmpty() || cleanNew.length() < 4) {
            return "redirect:/account/password?newInvalid";
        }

        if (!cleanNew.equals(cleanConfirm)) {
            return "redirect:/account/password?mismatch";
        }

        if (passwordEncoder.matches(cleanNew, user.getPassword())) {
            return "redirect:/account/password?samePassword";
        }

        user.setPassword(passwordEncoder.encode(cleanNew));
        appUserRepository.save(user);

        return "redirect:/account/password?saved";
    }
}