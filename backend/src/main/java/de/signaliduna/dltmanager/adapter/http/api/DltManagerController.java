package de.signaliduna.dltmanager.adapter.http.api;

import de.signaliduna.dltmanager.adapter.http.api.mapper.ApiModelMapper;
import de.signaliduna.dltmanager.adapter.http.api.model.DltEventFullItemDto;
import de.signaliduna.dltmanager.adapter.http.api.model.DltEventOverviewItemDto;
import de.signaliduna.dltmanager.adapter.http.api.model.GetDltEventsResponse;
import de.signaliduna.dltmanager.core.service.DltEventAdminService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api")
@PreAuthorize("isAuthenticated() and isAuthorizedUser()")
@RestController
public class DltManagerController {

	private final DltEventAdminService adminService;

	public DltManagerController(DltEventAdminService adminService) {
		this.adminService = adminService;
	}

	@GetMapping("/events/overview")
	@Operation(summary = "Provides an overview of all persisted DltEvents.")
	public ResponseEntity<GetDltEventsResponse> getDltEventsOverview() {
		List<DltEventOverviewItemDto> eventItems = adminService.getDltEvents().stream().map(ApiModelMapper::toDltEventOverviewItemDto).toList();
		return ResponseEntity.ok(new GetDltEventsResponse(eventItems));
	}

	@GetMapping("/events/overview/{dltEventId}")
	@Operation(summary = "Provides overview data for the DltEvent with the given dltEventId.")
	public ResponseEntity<DltEventOverviewItemDto> getDltEventOverviewItemByDltEventId(@PathVariable("dltEventId") String dltEventId) {
		return adminService.getDltEventByDltEventId(dltEventId).map(ApiModelMapper::toDltEventOverviewItemDto).map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/events/details/{dltEventId}")
	@Operation(summary = "Provides details for the given dltEventId.")
	public ResponseEntity<DltEventFullItemDto> getDltEventDetails(@PathVariable("dltEventId") String dltEventId) {
		return adminService.getDltEventByDltEventId(dltEventId).map(ApiModelMapper::toDltEventFullItemDto).map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/events/re-processing/{dltEventId}")
	@Operation(summary = "Triggers a re-processing of the original event that caused the specified DltEvent.")
	public ResponseEntity<Void> triggerReprocessing(@PathVariable("dltEventId") String dltEventId, Authentication authentication) {
		if (adminService.resendPapierantrag(dltEventId, authentication.getName())) {
			return ResponseEntity.ok().build();
		}
		return ResponseEntity.notFound().build();
	}

	@DeleteMapping("/events/{dltEventId}")
	@Operation(summary = "Deletes the DltEvent with the given dltEventId.")
	public ResponseEntity<Void> deleteDltEvent(@PathVariable("dltEventId") String dltEventId, Authentication authentication) {
		if (adminService.deleteDltEvent(dltEventId, authentication.getName())) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}
}
