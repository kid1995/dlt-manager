package de.signaliduna.dltmanager.adapter.http.api;

import de.signaliduna.dltmanager.adapter.app.exception.SiErrorMessage;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@ControllerAdvice
public class SecurityControllerAdvice {
	private static final Logger log = LoggerFactory.getLogger(SecurityControllerAdvice.class);

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<SiErrorMessage> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
    final var userPrincipal = (request == null)? null : request.getUserPrincipal();
		if (userPrincipal != null
        && userPrincipal.getName() != null) {
      log.warn("Access denied for user: '{}'.", userPrincipal.getName(), ex);
    } else {
      log.warn("Access denied for '{}'.", request);
    }
    final SiErrorMessage siErrorMessage = new SiErrorMessage(ex.getMessage(), "Access denied for user.");
    return new ResponseEntity<>(siErrorMessage, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<SiErrorMessage> handleRuntimeException(RuntimeException ex) {
    log.error("RuntimeException occurred during processing.", ex);
    final SiErrorMessage siErrorMessage = new SiErrorMessage(ex.getMessage(), "General runtime exception.");
    return new ResponseEntity<>(siErrorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<SiErrorMessage> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
    log.error("HttpMessageNotReadableException while processing request.", ex);
    final SiErrorMessage siErrorMessage = new SiErrorMessage(ex.getMessage(), "Malformed request payload.");
    return new ResponseEntity<>(siErrorMessage, HttpStatus.BAD_REQUEST);
  }

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<SiErrorMessage> handleConstraintViolationException(ConstraintViolationException ex) {
		var sb = new StringBuilder();
		sb.append(ex.getMessage());
		String violations = ex.getConstraintViolations().stream()
			.map(cv -> cv == null ? "null" : cv.getPropertyPath() + ": " + cv.getMessage())
			.collect(Collectors.joining(", "));
		sb.append(violations);
		var msg = sb.toString().trim();
		log.error("ConstraintViolationException while processing request.\n{}", msg, ex);
		var siErrorMessage = new SiErrorMessage(msg, "Constraint violation.");
		return new ResponseEntity<>(siErrorMessage, HttpStatus.BAD_REQUEST);
	}
}
