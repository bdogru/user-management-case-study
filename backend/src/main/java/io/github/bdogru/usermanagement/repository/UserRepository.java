package io.github.bdogru.usermanagement.repository;

import io.github.bdogru.usermanagement.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
