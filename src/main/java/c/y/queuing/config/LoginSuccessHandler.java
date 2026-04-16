package c.y.queuing.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import c.y.queuing.entity.AppUser;
import c.y.queuing.repository.AppUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AppUserRepository appUserRepository;

    public LoginSuccessHandler(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        String username = authentication.getName();

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + username));

        // ADMIN always goes to admin config
        if (hasRole(authentication, "ROLE_ADMIN")) {
            response.sendRedirect("/admin/config");
            return;
        }

        // Build department target first for non-admin users
        String deptTarget = null;
        if (user.getDepartment() != null && user.getDepartment().getCode() != null
                && !user.getDepartment().getCode().isBlank()) {
            deptTarget = "/dept/" + user.getDepartment().getCode().toUpperCase();
        }

        // Check saved request, but ignore generic /dept and /login flows
        SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);
        if (savedRequest != null) {
            String redirectUrl = savedRequest.getRedirectUrl();

            boolean isGenericDeptRequest =
                    redirectUrl.contains("/dept?") ||
                    redirectUrl.endsWith("/dept") ||
                    redirectUrl.contains("/login");

            if (!isGenericDeptRequest) {
                response.sendRedirect(redirectUrl);
                return;
            }
        }

        // For operator/staff users, go directly to their assigned department
        if (deptTarget != null) {
            response.sendRedirect(deptTarget);
            return;
        }

        // Fallback
        response.sendRedirect("/lobby");
    }

    private boolean hasRole(Authentication authentication, String roleName) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (roleName.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}