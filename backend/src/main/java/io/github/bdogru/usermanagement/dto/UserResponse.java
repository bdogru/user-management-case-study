package io.github.bdogru.usermanagement.dto;

import io.github.bdogru.usermanagement.entity.User;

public record UserResponse(Long id, String name, String email, long version) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getVersion());
	}

}
