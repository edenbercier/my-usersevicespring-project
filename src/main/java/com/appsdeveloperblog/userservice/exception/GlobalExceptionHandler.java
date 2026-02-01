package com.appsdeveloperblog.userservice.exception;

import org.springframework.security.access.AccessDeniedException;

import javax.naming.AuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


  // Handle @Valid validation errors
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult().getAllErrors().forEach(error -> {
      String field = ((FieldError) error).getField();
      String message = error.getDefaultMessage();
      errors.put(field, message);
    });

    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
  }

  //  Handle custom business exceptions
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
    return new ResponseEntity<>(
        Map.of("error", "Access Denied"),
        HttpStatus.FORBIDDEN
    );
  }
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<?> handleAuthentication(AuthenticationException ex) {
    return new ResponseEntity<>(
        Map.of("error", ex.getMessage()),
        HttpStatus.UNAUTHORIZED
    );
  }


  // Fallback handler (catches ANY unhandled error)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleGeneral(Exception ex) {
    return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
  }
  @ExceptionHandler(JwtAuthenticationException.class)
  public ResponseEntity<?> handleJwt(JwtAuthenticationException ex) {
    return new ResponseEntity<>(
        Map.of("error", ex.getMessage()),
        HttpStatus.UNAUTHORIZED
    );
  }
}
