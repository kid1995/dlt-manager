package de.signaliduna.dltmanager.adapter.http.client;


import org.springframework.stereotype.Component;

@Component
public class PapierantragEingangAdapter {

	private final PapierantragEingangClient papierantragEingangClient;

	public PapierantragEingangAdapter(PapierantragEingangClient papierantragEingangClient) {
		this.papierantragEingangClient = papierantragEingangClient;
	}

	public void resendPapierantrag(String papierantragUuid) {
		papierantragEingangClient.resendPapierantrag(papierantragUuid);
	}
}
