package io.github.bdogru.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// No email: it cannot be changed, and a request that sends one is rejected as an unknown property.
public record UpdateUserRequest(
		@NotBlank @Size(max = 255) String name,
		@NotNull Long version) {
}
