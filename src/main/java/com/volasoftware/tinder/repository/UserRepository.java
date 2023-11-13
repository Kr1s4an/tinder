package com.volasoftware.tinder.repository;

import com.volasoftware.tinder.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface UserRepository extends JpaRepository<User, java.lang.Long> {
    Optional<User> findOneByEmail(String email);
}
