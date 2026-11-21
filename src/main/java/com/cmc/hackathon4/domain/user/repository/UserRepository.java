package com.cmc.hackathon4.domain.user.repository;

import java.util.Optional;

import com.cmc.hackathon4.domain.user.entity.DeviceIdHasher;
import com.cmc.hackathon4.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByDeviceIdHash(String deviceIdHash);

	boolean existsByDeviceIdHash(String deviceIdHash);

	/** @param deviceId 앱이 보낸 기기 UUID 원본. DB 에는 해시만 있으므로 해시해서 찾는다. */
	default Optional<User> findByDeviceId(String deviceId) {
		return findByDeviceIdHash(DeviceIdHasher.hash(deviceId));
	}

	/** @param deviceId 앱이 보낸 기기 UUID 원본 */
	default boolean existsByDeviceId(String deviceId) {
		return existsByDeviceIdHash(DeviceIdHasher.hash(deviceId));
	}

}
