package com.appsdeveloperblog.userservice.exception;

public class UsersServiceException extends RuntimeException {

  public UsersServiceException(String message) {
    super(message);
  }
}  //Make exception more descriptive
// Cleaner to include the HTTP status inside the exception itself