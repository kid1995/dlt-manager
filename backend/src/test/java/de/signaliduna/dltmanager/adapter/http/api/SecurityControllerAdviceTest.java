package de.signaliduna.dltmanager.adapter.http.api;

import de.signaliduna.dltmanager.adapter.app.exception.SiErrorMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.WebRequest;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityControllerAdviceTest {
    private static final String RUNTIME_EXCEPTION_MESSAGE = "Runtime exception message";
    private static final String ACESS_DENIED_EXCEPTION_MESSAGE = "Access denied exception message";

    @InjectMocks
    SecurityControllerAdvice classUnderTest;

    @Mock
    RuntimeException runtimeExceptionMock;
    @Mock
    AccessDeniedException accessDeniedExceptionMock;
    @Mock
    HttpMessageNotReadableException httpMessageNotReadableExceptionMock;
    @Mock
    WebRequest webRequestMock;
    @Mock
    Principal principalMock;

    @Test
    void handleAccessDeniedException() {
        when(accessDeniedExceptionMock.getMessage()).thenReturn(ACESS_DENIED_EXCEPTION_MESSAGE);
        when(webRequestMock.getUserPrincipal()).thenReturn(principalMock);
        when(principalMock.getName()).thenReturn("S12345");

        ResponseEntity<SiErrorMessage> siErrorMessageResponseEntity = classUnderTest.handleAccessDeniedException(accessDeniedExceptionMock, webRequestMock);

        assertThat(siErrorMessageResponseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(siErrorMessageResponseEntity.getBody()).isEqualTo(new SiErrorMessage(ACESS_DENIED_EXCEPTION_MESSAGE, "Access denied for user."));
    }

    @Test
    void handleAccessDeniedExceptionMissingPrincipal() {
        when(accessDeniedExceptionMock.getMessage()).thenReturn(ACESS_DENIED_EXCEPTION_MESSAGE);
        when(webRequestMock.getUserPrincipal()).thenReturn(principalMock);
        when(principalMock.getName()).thenReturn(null);

        ResponseEntity<SiErrorMessage> siErrorMessageResponseEntity = classUnderTest.handleAccessDeniedException(accessDeniedExceptionMock, webRequestMock);

        assertThat(siErrorMessageResponseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(siErrorMessageResponseEntity.getBody()).isEqualTo(new SiErrorMessage(ACESS_DENIED_EXCEPTION_MESSAGE, "Access denied for user."));
    }

    @Test
    void handleAccessDeniedExceptionMissingPrincipalName() {
        when(accessDeniedExceptionMock.getMessage()).thenReturn(ACESS_DENIED_EXCEPTION_MESSAGE);

        ResponseEntity<SiErrorMessage> siErrorMessageResponseEntity = classUnderTest.handleAccessDeniedException(accessDeniedExceptionMock, webRequestMock);

        assertThat(siErrorMessageResponseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(siErrorMessageResponseEntity.getBody()).isEqualTo(new SiErrorMessage(ACESS_DENIED_EXCEPTION_MESSAGE, "Access denied for user."));
    }

    @Test
    void handleAccessDeniedExceptionMissingRequest() {
        when(accessDeniedExceptionMock.getMessage()).thenReturn(ACESS_DENIED_EXCEPTION_MESSAGE);

        ResponseEntity<SiErrorMessage> siErrorMessageResponseEntity = classUnderTest.handleAccessDeniedException(accessDeniedExceptionMock, null);

        assertThat(siErrorMessageResponseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(siErrorMessageResponseEntity.getBody()).isEqualTo(new SiErrorMessage(ACESS_DENIED_EXCEPTION_MESSAGE, "Access denied for user."));
    }

    @Test
    void handleRuntimeException() {
        when(runtimeExceptionMock.getMessage()).thenReturn(RUNTIME_EXCEPTION_MESSAGE);

        ResponseEntity<SiErrorMessage> siErrorMessageResponseEntity = classUnderTest.handleRuntimeException(runtimeExceptionMock);

        assertThat(siErrorMessageResponseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(siErrorMessageResponseEntity.getBody()).isEqualTo(new SiErrorMessage(RUNTIME_EXCEPTION_MESSAGE, "General runtime exception."));
    }

    @Test
    void handleHttpMessageNotReadableException() {
        final var message = "http message not readable";
        when(httpMessageNotReadableExceptionMock.getMessage()).thenReturn(message);

        ResponseEntity<SiErrorMessage> siErrorMessageResponseEntity = classUnderTest.handleHttpMessageNotReadableException(httpMessageNotReadableExceptionMock);

        assertThat(siErrorMessageResponseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(siErrorMessageResponseEntity.getBody()).isEqualTo(new SiErrorMessage(message, "Malformed request payload."));
    }

    @Test
    void shouldHandleConstrainViolationException_violationsEmpty() {
        var ex = new ConstraintViolationException("Test exception", Set.of());
        ResponseEntity<SiErrorMessage> siErrorMessageResponseEntity = classUnderTest.handleConstraintViolationException(ex);

        assertThat(siErrorMessageResponseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(siErrorMessageResponseEntity.getBody()).isEqualTo(new SiErrorMessage("Test exception", "Constraint violation."));
    }

    @Test
    void shouldHandleConstrainViolationException_violationsNonEmpty() {
        @SuppressWarnings("unchecked") final ConstraintViolation<String> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("violation-message");
        final var path = mock(Path.class);
        when(path.toString()).thenReturn("<value of path.toString()>");
        when(violation.getPropertyPath()).thenReturn(path);
        var ex = new ConstraintViolationException("Test exception", Set.of(violation));

        ResponseEntity<SiErrorMessage> siErrorMessageResponseEntity = classUnderTest.handleConstraintViolationException(ex);

        assertThat(siErrorMessageResponseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(siErrorMessageResponseEntity.getBody()).isEqualTo(new SiErrorMessage("Test exception<value of path.toString()>: violation-message", "Constraint violation."));
    }

    @Test
    void shouldHandleConstrainViolationException_violationSetContainsNull() {
        final Set<ConstraintViolation<?>> constraintViolations = new LinkedHashSet<>();
        constraintViolations.add(null);
        var ex = new ConstraintViolationException("Test exception", constraintViolations);

        ResponseEntity<SiErrorMessage> siErrorMessageResponseEntity = classUnderTest.handleConstraintViolationException(ex);

        assertThat(siErrorMessageResponseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(siErrorMessageResponseEntity.getBody()).isEqualTo(new SiErrorMessage("Test exceptionnull", "Constraint violation."));
    }
}
