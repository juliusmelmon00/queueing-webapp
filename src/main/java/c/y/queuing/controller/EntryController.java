package c.y.queuing.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EntryController {

    public static final String QR_ALLOWED = "QR_ALLOWED";

    @GetMapping("/entry")
    public String entry(HttpSession session) {
        // Mark this browser session as allowed (came from QR)
        session.setAttribute(QR_ALLOWED, true);
        return "redirect:/client";
    }

    @GetMapping("/entry-denied")
    public String denied() {
        return "entry-denied";
    }
}
