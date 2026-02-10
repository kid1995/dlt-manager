package de.signaliduna.dltmanager.core.model;

public record DltEventAction(
	String name,
	String description
) {

	public enum Status {
		TRIGGERED, SUCCEEDED, FAILED
	}

}
