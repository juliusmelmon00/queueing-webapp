package c.y.queuing.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import c.y.queuing.entity.Announcement;
import c.y.queuing.repository.AnnouncementRepository;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementController(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @GetMapping("/next")
    public Map<String, Object> next(@RequestParam(name = "after", defaultValue = "0") Long afterId) {

        Optional<Announcement> next = announcementRepository.findTopByIdGreaterThanOrderByIdAsc(afterId);

        //if (next.isEmpty()) return Map.of("found", false);

        Announcement a = next.get();
        return new java.util.HashMap<>() {{
            put("found", true);
            put("id", a.getId());
            put("queueNo", a.getQueueNo() == null ? "" : a.getQueueNo());
            put("department", a.getDepartment() == null ? "" : a.getDepartment());
            put("status", a.getStatus() == null ? "" : a.getStatus());
        }};
    
    }
}
