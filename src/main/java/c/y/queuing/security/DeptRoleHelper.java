package c.y.queuing.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import c.y.queuing.entity.AppUser;
import c.y.queuing.repository.AppUserRepository;

@Component
public class DeptRoleHelper {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AppUserRepository appUserRepository;

    public DeptRoleHelper(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public boolean isAdmin(Authentication auth) {
        if (auth == null) {
            return false;
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> (ROLE_PREFIX + "ADMIN").equals(a));
    }

    /**
     * Resolve the user's real department.
     *
     * New design:
     * - ADMIN stays special
     * - OPERATOR / STAFF users get department from AppUser.department
     *
     * Backward compatibility:
     * - if department is null, fall back to old role-based department names
     */
    public String resolveDept(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }

        AppUser user = appUserRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return null;
        }

        // New correct source of truth
        if (user.getDepartment() != null
                && user.getDepartment().getCode() != null
                && !user.getDepartment().getCode().isBlank()) {
            return user.getDepartment().getCode().toUpperCase();
        }

        // Backward-compatible fallback for old accounts still using role-as-department
        String role = user.getRole();
        if (role != null && !role.isBlank()) {
            String normalized = role.toUpperCase();

            // Ignore generic roles
            if (!normalized.equals("ADMIN")
                    && !normalized.equals("OPERATOR")
                    && !normalized.equals("STAFF")) {
                return normalized;
            }
        }

        return null;
    }

    public boolean hasDeptRole(Authentication auth) {
        return resolveDept(auth) != null;
    }
}