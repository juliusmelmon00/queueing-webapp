package c.y.queuing.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import c.y.queuing.entity.Announcement;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
	Optional<Announcement> findTopByIdGreaterThanOrderByIdAsc(Long id);

}
