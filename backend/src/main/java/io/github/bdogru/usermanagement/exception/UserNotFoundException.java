package io.github.bdogru.usermanagement.exception;

public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException(long id) {
		super("User " + id + " not found");
	}

}
