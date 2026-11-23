package io.github.bdogru.usermanagement.service;

import java.util.List;
import java.util.Locale;

import io.github.bdogru.usermanagement.dto.CreateUserRequest;
import io.github.bdogru.usermanagement.dto.UpdateUserRequest;
import io.github.bdogru.usermanagement.dto.UserResponse;
import io.github.bdogru.usermanagement.entity.User;
import io.github.bdogru.usermanagement.exception.UserNotFoundException;
import io.github.bdogru.usermanagement.repository.UserRepository;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository repository;

	UserService(UserRepository repository) {
		this.repository = repository;
	}

	public List<UserResponse> findAll() {
		return repository.findAll().stream().map(UserResponse::from).toList();
	}

	public UserResponse findById(long id) {
		return UserResponse.from(get(id));
	}

	// A duplicate email fails on the unique constraint and surfaces as DataIntegrityViolationException.
	public UserResponse create(CreateUserRequest request) {
		User user = new User(request.name(), request.email().toLowerCase(Locale.ROOT));
		return UserResponse.from(repository.saveAndFlush(user));
	}

	@Transactional
	public UserResponse update(long id, UpdateUserRequest request) {
		User user = get(id);
		if (user.getVersion() != request.version()) {
			throw new ObjectOptimisticLockingFailureException(User.class, id);
		}
		user.setName(request.name());
		// Flushing here bumps the version, so the response carries the new one.
		return UserResponse.from(repository.saveAndFlush(user));
	}

	@Transactional
	public void delete(long id) {
		repository.delete(get(id));
	}

	private User get(long id) {
		return repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
	}

}
