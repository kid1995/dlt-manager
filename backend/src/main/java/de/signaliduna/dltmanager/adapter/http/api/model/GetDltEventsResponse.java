package de.signaliduna.dltmanager.adapter.http.api.model;

import java.util.List;

public record GetDltEventsResponse(List<DltEventOverviewItemDto> dltEventItems) {
}
