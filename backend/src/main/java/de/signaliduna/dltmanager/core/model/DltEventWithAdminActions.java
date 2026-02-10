package de.signaliduna.dltmanager.core.model;

import java.util.List;

public record DltEventWithAdminActions(
	DltEvent dltEvent,
	List<DltEventAction> availableActions
) {
}
