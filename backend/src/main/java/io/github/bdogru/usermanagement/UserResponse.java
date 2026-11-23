package io.github.bdogru.usermanagement;

public record UserResponse(Long id, String name, String email, long version) {

	static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getVersion());
	}

}
