package c.y.queuing.dto;

import java.util.List;

public record LobbyPayload(List<DepartmentView> departments) {

    public record DepartmentView(
            String code,
            String name,
            TicketView nowServing,
            List<TicketView> waiting
    ) {}

    public record TicketView(String queueNo, String status) {}
}