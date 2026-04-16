package c.y.queuing.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import c.y.queuing.entity.AppUser;
import c.y.queuing.repository.AppUserRepository;

@Configuration
public class UsersConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner initUsers(
            AppUserRepository userRepo,
            PasswordEncoder encoder
    ) {
        return args -> {
            if (userRepo.findByUsername("admin").isPresent()) {
                return;
            }

            AppUser user = new AppUser();
            user.setUsername("admin");
            user.setPassword(encoder.encode("admin123"));
            user.setRole("ADMIN");
            user.setIsActive(true);

            userRepo.save(user);
        };
    }
}