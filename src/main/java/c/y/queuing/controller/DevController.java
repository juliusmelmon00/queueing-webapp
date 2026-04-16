package c.y.queuing.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import c.y.queuing.entity.AppUser;
import c.y.queuing.repository.AppUserRepository;

@RestController
public class DevController {

    private final AppUserRepository userRepo;
    private final PasswordEncoder encoder;

    public DevController(AppUserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    @GetMapping("/dev/reset-admin")
    public String resetAdmin() {

        userRepo.deleteAll(); // clear users

        AppUser admin = new AppUser();
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("admin123"));
        admin.setRole("ADMIN");
        admin.setIsActive(true);

        userRepo.save(admin);

        return "Admin reset successful!";
    }
}