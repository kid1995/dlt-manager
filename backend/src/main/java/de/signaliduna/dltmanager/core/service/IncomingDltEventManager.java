package de.signaliduna.dltmanager.core.service;

import de.signaliduna.dltmanager.adapter.db.DltEventPersistenceAdapter;
import de.signaliduna.dltmanager.core.model.DltEvent;

public class IncomingDltEventManager {
	private final DltEventPersistenceAdapter dltEventPersistenceAdapter;

	public IncomingDltEventManager(DltEventPersistenceAdapter dltEventPersistenceAdapter) {
		this.dltEventPersistenceAdapter = dltEventPersistenceAdapter;
	}

	public void onDltEvent(DltEvent data) {
		dltEventPersistenceAdapter.save(data);
	}
}
