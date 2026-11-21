package com.cmc.hackathon4.domain.user.repository;

import java.util.Optional;

import com.cmc.hackathon4.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByDeviceId(String deviceId);

	boolean existsByDeviceId(String deviceId);

}
