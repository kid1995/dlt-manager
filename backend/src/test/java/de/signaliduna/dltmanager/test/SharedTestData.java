package de.signaliduna.dltmanager.test;

import de.signaliduna.dltmanager.core.model.DltEvent;
import de.signaliduna.elpa.sharedlib.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static de.signaliduna.dltmanager.test.TestUtil.asJsonString;

public class SharedTestData {

	public static final Vorgang VORGANG = Vorgang.builder()
		.processId("processId")
		.antrag(Antrag.builder()
			.antragsteller(Antragsteller.builder()
				.personendaten(
					NatuerlichePersonendaten.builder()
						.anrede("Frau")
						.vorname("Sabine")
						.nachname("Mustermann")
						.akademischerTitel("Dr.")
						.familienstand("MARRIED")
						.geburtsdatum(LocalDate.of(2000, 1, 1))
						.staatsangehoerigkeit("DE")
						.beruflicheTaetigkeit("beruflicheTaetigkeit")
						.berufsstellung("berufsstellung")
						.branche(null)
						.build()
				)
				.adresse(
					Adresse.builder()
						.ort("Hamburg")
						.strasse("Neue Rabenstr.")
						.hausnummer("15")
						.plz("20354")
						.land("DE")
						.build()
				)
				.kontaktdaten(Kontaktdaten.builder().build())
				.einwilligungenUwg(EinwilligungenUwg.builder().build())
				.kontaktdaten(Kontaktdaten.builder().build())
				.einwilligungenUwg(EinwilligungenUwg.builder().build())
				.partnerIdentifikationsdaten(PartnerIdentifikationsdaten.builder()
					.paid("paid").vnr("vnr").panr("panr").build())
				.build())
			.allgemeineDaten(AllgemeineDaten.builder().build())
			.vermittlerdaten(Vermittlerdaten.builder().build())
			.zahlungsdaten(Zahlungsdaten.builder().build())
			.build())
		.metadaten(Metadaten.builder().rohdatenUuid("myRohdatenUuid").build())
		.build();


	public static final DltEvent DLT_EVENT_1 = DltEvent.builder()
		.dltEventId("dltEvent1Id")
		.originalEventId("originalEvent1Id")
		.serviceName("partnersync")
		.addToDltTimestamp(LocalDateTime.of(2024, 1, 1, 1, 1))
		.topic("topic")
		.partition("partition")
		.stackTrace("stacktrace")
		.error("error")
		.traceId("traceId")
		.payload(asJsonString(SharedTestData.VORGANG))
		.payloadMediaType("application/json")
		.build();

	private SharedTestData() {
	}
}
