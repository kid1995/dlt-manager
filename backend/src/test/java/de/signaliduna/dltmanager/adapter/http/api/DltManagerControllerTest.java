package de.signaliduna.dltmanager.adapter.http.api;

import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockAuthentication;
import com.c4_soft.springaddons.security.oauth2.test.webmvc.AutoConfigureAddonsWebmvcResourceServerSecurity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.signaliduna.dltmanager.adapter.db.DltEventPersistenceAdapter;
import de.signaliduna.dltmanager.adapter.http.client.PapierantragEingangAdapter;
import de.signaliduna.dltmanager.config.WebSecurityConfig;
import de.signaliduna.dltmanager.core.model.AdminAction;
import de.signaliduna.dltmanager.core.model.AdminActionHistoryItem;
import de.signaliduna.dltmanager.core.model.DltEventAction;
import de.signaliduna.dltmanager.core.service.DltEventAdminService;
import de.signaliduna.dltmanager.test.SharedTestData;
import de.signaliduna.elpa.jwtadapter.config.JwtAdapterConfig;
import de.signaliduna.elpa.jwtadapter.core.JwtAdapter;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static de.signaliduna.dltmanager.test.TestUtil.assertThatJsonString;
import static de.signaliduna.dltmanager.test.TestUtil.captureSingleArg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureAddonsWebmvcResourceServerSecurity
@Import({WebSecurityConfig.class, DltEventAdminService.class, PapierantragEingangAdapter.class})
@WebMvcTest(controllers = DltManagerController.class)
class DltManagerControllerTest {

	private static final String AUTH_USER = "S12345";

	private static final String DLT_EVENT1_ID = SharedTestData.DLT_EVENT_1.dltEventId();

	@Autowired
	MockMvc mockMvc;
	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	@SuppressWarnings("unused")
	private JwtAdapter jwtAdapter;

	@MockitoBean
	@SuppressWarnings("unused")
	private JwtAdapterConfig jwtAdapterConfig;

	@MockitoBean
	DltEventPersistenceAdapter dltEventPersistenceAdapter;

	@MockitoBean
	PapierantragEingangAdapter papierantragEingangAdapter;

	@Autowired
	private WebSecurityConfig webSecurityConfig;

	@BeforeEach
	void beforeEach() {
		ReflectionTestUtils.setField(this.webSecurityConfig, "authorizedUsers", new String[]{AUTH_USER});
	}

	@Nested
	class getDltEventsOverview {

		@Test
		@WithMockAuthentication(name = AUTH_USER, authType = JwtAuthenticationToken.class)
		void shouldReturnOkeWithAuthorizedUser() throws Exception {
			final var jwt = mock(Jwt.class);
			when(jwt.getClaims()).thenReturn(Map.of(
				"uid", AUTH_USER
			));
			final var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
			when(auth.getToken()).thenReturn(jwt);
			mockMvc.perform(get("/api/events/overview")
					.contentType("application/json"))
				.andExpect(status().isOk())
				.andReturn();
		}

		@Test
		@WithMockAuthentication(name = AUTH_USER, authType = JwtAuthenticationToken.class)
		void shouldReturnForbiddenWhenTokenNotFound() throws Exception {
			mockMvc.perform(get("/api/events/overview")
					.contentType("application/json"))
				.andExpect(status().isForbidden())
				.andReturn();
		}

		@Test
		@WithMockAuthentication(name = "S00000")
		void shouldReturnForbiddenWithUnauthorizedUser() throws Exception {
			mockMvc.perform(get("/api/events/overview")
					.contentType("application/json"))
				.andExpect(status().isForbidden())
				.andReturn();
		}

		@Test
		@WithMockAuthentication(name = AUTH_USER)
		void whenEmpty() throws Exception {
			final MvcResult mvcResult = mockMvc.perform(get("/api/events/overview")
					.contentType("application/json"))
				.andExpect(status().isOk())
				.andReturn();

			assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo("""
				{"dltEventItems":[]}"""
			);
		}

		@Test
		@WithMockAuthentication(name = AUTH_USER)
		void withOneDltEvent() throws Exception {
			when(dltEventPersistenceAdapter.streamAll()).thenReturn(Stream.of(SharedTestData.DLT_EVENT_1));
			final MvcResult mvcResult = mockMvc.perform(get("/api/events/overview")
					.contentType("application/json"))
				.andExpect(status().isOk())
				.andReturn();

			assertThatJsonString(mvcResult.getResponse().getContentAsString()).isEqualTo(
				"""
					{
					  "dltEventItems": [
					    {
					      "dltEventId": "dltEvent1Id",
					      "originalEventId": "originalEvent1Id",
					      "serviceName": "partnersync",
					      "addToDltTimestamp": "2024-01-01T01:01:00",
					      "topic": "topic",
					      "partition": "partition",
					      "traceId": "traceId",
					      "payloadMediaType": "application/json",
					      "error": "error",
					      "lastAdminAction": null,
					      "availableActions": [
					        {
					          "name": "Retry",
					          "description": "Triggers a re-processing of the event that caused the DLT-event in papierantrag-eingang."
					        },
					        {
					          "name": "Delete",
					          "description": "Deletes an DltEvent from the the dlt-manager database."
					        }
					      ]
					    }
					  ]
					}
					"""
			);
		}
	}

	@Nested
	class getDltEventOverviewItemByDltEventId {
		@Test
		@WithMockAuthentication(name = AUTH_USER)
		void shouldWork() throws Exception {
			when(dltEventPersistenceAdapter.findDltEventById(DLT_EVENT1_ID)).thenReturn(Optional.of(SharedTestData.DLT_EVENT_1));
			final MvcResult mvcResult = mockMvc.perform(get("/api/events/overview/" + DLT_EVENT1_ID)
					.contentType("application/json"))
				.andExpect(status().isOk())
				.andReturn();

			assertThatJsonString(mvcResult.getResponse().getContentAsString()).isEqualTo("""
					{
						"dltEventId": "dltEvent1Id",
						"originalEventId": "originalEvent1Id",
						"serviceName": "partnersync",
						"addToDltTimestamp": "2024-01-01T01:01:00",
						"topic": "topic",
						"partition": "partition",
						"traceId": "traceId",
						"payloadMediaType": "application/json",
						"error": "error",
						"lastAdminAction": null,
						"availableActions": [
							{
								"name": "Retry",
								"description": "Triggers a re-processing of the event that caused the DLT-event in papierantrag-eingang."
							},
							{
								"name": "Delete",
								"description": "Deletes an DltEvent from the the dlt-manager database."
							}
						]
					}
				"""
			);
		}
	}

	@Nested
	class getDltEventDetails {

		@Test
		@WithMockAuthentication(name = AUTH_USER)
		void shouldReturnDetailsForKnownDltEventId() throws Exception {
			when(dltEventPersistenceAdapter.findDltEventById(DLT_EVENT1_ID)).thenReturn(Optional.of(SharedTestData.DLT_EVENT_1));
			final MvcResult mvcResult = mockMvc.perform(get("/api/events/details/" + DLT_EVENT1_ID)
					.contentType("application/json"))
				.andExpect(status().isOk())
				.andReturn();

			assertThatJsonString(mvcResult.getResponse().getContentAsString()).isEqualTo("""
				{
				  "dltEventId": "dltEvent1Id",
				  "originalEventId": "originalEvent1Id",
				  "serviceName": "partnersync",
				  "addToDltTimestamp": "2024-01-01T01:01:00",
				  "topic": "topic",
				  "partition": "partition",
				  "traceId": "traceId",
				  "payload": "{\\"processId\\":\\"processId\\",\\"elpaId\\":null,\\"resourceId\\":null,\\"antrag\\":{\\"allgemeineDaten\\":{\\"beratungsprotokollVollstaendig\\":null,\\"eingangsdatum\\":null,\\"antragstellungsdatum\\":null,\\"gesellschaft\\":null,\\"sipId\\":null,\\"unterschriftVn\\":null,\\"neese\\":null},\\"antragsteller\\":{\\"personendaten\\":{\\"type\\":\\"NatuerlichePersonendaten\\",\\"anrede\\":\\"Frau\\",\\"nachname\\":\\"Mustermann\\",\\"name1\\":null,\\"vorname\\":\\"Sabine\\",\\"namenszusatz\\":null,\\"adelstitel\\":null,\\"akademischerTitel\\":\\"Dr.\\",\\"familienstand\\":\\"MARRIED\\",\\"geburtsdatum\\":[2000,1,1],\\"staatsangehoerigkeit\\":\\"DE\\",\\"beruflicheTaetigkeit\\":\\"beruflicheTaetigkeit\\",\\"berufsstellung\\":\\"berufsstellung\\",\\"branche\\":null},\\"partnerIdentifikationsdaten\\":{\\"panr\\":\\"panr\\",\\"vnr\\":\\"vnr\\",\\"paid\\":\\"paid\\"},\\"adresse\\":{\\"strasse\\":\\"Neue Rabenstr.\\",\\"plz\\":\\"20354\\",\\"hausnummer\\":\\"15\\",\\"ort\\":\\"Hamburg\\",\\"adressInfo\\":null,\\"land\\":\\"DE\\"},\\"kontaktdaten\\":{\\"telefon\\":null,\\"fax\\":null,\\"email\\":null,\\"mobil\\":null},\\"einwilligungenUwg\\":{\\"telefonKontakt\\":null,\\"smsKontakt\\":null,\\"faxKontakt\\":null,\\"emailKontakt\\":null}},\\"versicherungsdaten\\":null,\\"vermittlerdaten\\":{\\"name\\":null,\\"gd\\":null,\\"btr\\":null,\\"adpAnteilList\\":null,\\"externeOrdnungsbegriffe\\":null},\\"zahlungsdaten\\":{\\"zahlungsart\\":null,\\"zahlungsweise\\":null}},\\"metadaten\\":{\\"rohdatenUuid\\":\\"myRohdatenUuid\\",\\"antragDokumentId\\":null,\\"gdAntragsId\\":null,\\"prozessVersion\\":null,\\"antragsStatus\\":null,\\"vertriebsKanal\\":null}}",
				  "payloadMediaType": "application/json",
				  "error": "error",
				  "stackTrace": "stacktrace",
				  "lastAdminAction": null,
				  "availableActions": [
				    {
				      "name": "Retry",
				      "description": "Triggers a re-processing of the event that caused the DLT-event in papierantrag-eingang."
				    },
				    {
				      "name": "Delete",
				      "description": "Deletes an DltEvent from the the dlt-manager database."
				    }
				  ]
				}"""
			);
		}

		@Test
		@WithMockAuthentication(name = AUTH_USER)
		void shouldReturnNotFoundForUnknownDltEventId() throws Exception {
			mockMvc.perform(get("/api/events/details/" + DLT_EVENT1_ID)
					.contentType("application/json"))
				.andExpect(status().isNotFound())
				.andReturn();
		}
	}

	@Nested
	class triggerReprocessing {
		@Test
		@WithMockAuthentication(name = AUTH_USER)
		void withUnknownDltEventId() throws Exception {
			when(dltEventPersistenceAdapter.findDltEventById(DLT_EVENT1_ID)).thenReturn(Optional.empty());
			mockMvc.perform(post("/api/events/re-processing/" + DLT_EVENT1_ID)
					.contentType("application/json"))
				.andExpect(status().isNotFound())
				.andReturn();
		}

		@Test
		@WithMockAuthentication(name = AUTH_USER)
		void withKnownDltEventId() throws Exception {
			// given
			when(dltEventPersistenceAdapter.findDltEventById(DLT_EVENT1_ID)).thenReturn(Optional.of(SharedTestData.DLT_EVENT_1));
			doNothing().when(papierantragEingangAdapter).resendPapierantrag("myRohdatenUuid");

			// when
			mockMvc.perform(post("/api/events/re-processing/" + DLT_EVENT1_ID)
					.contentType("application/json"))
				.andExpect(status().isOk())
				.andReturn();

			// then
			verify(papierantragEingangAdapter).resendPapierantrag("myRohdatenUuid");
			final var capturedAdminActionHistoryItem = captureSingleArg(AdminActionHistoryItem.class, c ->
				verify(dltEventPersistenceAdapter).updateLastAdminActionForDltEvent(eq(DLT_EVENT1_ID), c.capture())).getValue();
			assertThat(capturedAdminActionHistoryItem).usingRecursiveComparison().ignoringFieldsOfTypes(LocalDateTime.class).isEqualTo(
				AdminActionHistoryItem.builder()
					.actionName(AdminAction.RESEND_TO_PAPIERANTRAG_EINGANG.name())
					.userName(AUTH_USER)
					.status(DltEventAction.Status.TRIGGERED.name())
					.timestamp(SharedTestData.DLT_EVENT_1.addToDltTimestamp())
					.build()
			);
		}

		@Test
		@WithMockAuthentication(name = AUTH_USER)
		void withKnownDltEventIdWhenResendPapierantragFails() throws Exception {
			// given
			when(dltEventPersistenceAdapter.findDltEventById(DLT_EVENT1_ID)).thenReturn(Optional.of(SharedTestData.DLT_EVENT_1));
			doThrow(new FeignException.ServiceUnavailable("Mock exception", createMockRequest(Request.HttpMethod.POST), new byte[]{}, Map.of()))
				.when(papierantragEingangAdapter).resendPapierantrag("myRohdatenUuid");

			// when
			mockMvc.perform(post("/api/events/re-processing/" + DLT_EVENT1_ID)
					.contentType("application/json"))
				.andExpect(status().isOk())
				.andReturn();

			// then
			verify(papierantragEingangAdapter).resendPapierantrag("myRohdatenUuid");
			final var capturedAdminActionHistoryItem = captureSingleArg(AdminActionHistoryItem.class, c ->
				verify(dltEventPersistenceAdapter).updateLastAdminActionForDltEvent(eq(DLT_EVENT1_ID), c.capture())).getValue();
			assertThat(capturedAdminActionHistoryItem).usingRecursiveComparison().ignoringFieldsOfTypes(LocalDateTime.class).isEqualTo(
				AdminActionHistoryItem.builder()
					.actionName(AdminAction.RESEND_TO_PAPIERANTRAG_EINGANG.name())
					.userName(AUTH_USER)
					.status(DltEventAction.Status.FAILED.name())
					.statusError("Mock exception")
					.timestamp(SharedTestData.DLT_EVENT_1.addToDltTimestamp())
					.build()
			);
		}

		@Test
		@WithMockAuthentication(name = AUTH_USER)
		void withKnownDltEventIdButMissingRohdatenUuid() throws Exception {
			final var event = SharedTestData.DLT_EVENT_1.toBuilder().payload(SharedTestData.DLT_EVENT_1.payload().replace("rohdatenUuid", "gardatenUuid")).build();
			when(dltEventPersistenceAdapter.findDltEventById(DLT_EVENT1_ID)).thenReturn(Optional.of(event));
			mockMvc.perform(post("/api/events/re-processing/" + DLT_EVENT1_ID)
					.contentType("application/json"))
				.andExpect(status().isInternalServerError())
				.andReturn();

			verify(papierantragEingangAdapter, never()).resendPapierantrag(any());
		}
	}

	@Nested
	class deleteDltEvent {
		@Test
		@WithMockAuthentication(name = AUTH_USER)
		void withUnknownDltEventId() throws Exception {
			when(dltEventPersistenceAdapter.deleteByDltEventId(DLT_EVENT1_ID)).thenReturn(false);
			mockMvc.perform(delete("/api/events/" + DLT_EVENT1_ID)
					.contentType("application/json"))
				.andExpect(status().isNotFound())
				.andReturn();
		}

		@Test
		@WithMockAuthentication(name = AUTH_USER)
		void withKnownDltEventId() throws Exception {
			when(dltEventPersistenceAdapter.deleteByDltEventId(DLT_EVENT1_ID)).thenReturn(true);
			mockMvc.perform(delete("/api/events/" + DLT_EVENT1_ID)
					.contentType("application/json"))
				.andExpect(status().isNoContent())
				.andReturn();
		}
	}

	private Request createMockRequest(Request.HttpMethod httpMethod) {
		var url = "http://example.com/test/mock";
		return feign.Request.create(httpMethod, url, Map.of(), feign.Request.Body.create(new byte[]{}), null);
	}

	String asJsonString(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
