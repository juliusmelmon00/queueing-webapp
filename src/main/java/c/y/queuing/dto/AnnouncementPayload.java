package c.y.queuing.dto;

public record AnnouncementPayload(
	Long id,
	String queueNo,
	String department,
	String status
){}
