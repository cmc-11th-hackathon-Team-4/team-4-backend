package com.cmc.hackathon4.domain.user.entity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 기기 UUID(deviceId)를 DB 에 평문으로 두지 않기 위한 단방향 해시. 결과는 SHA-256 의 소문자 hex 64자.
 * 해시값으로 사용자를 바로 조회해야 해서 같은 입력에 항상 같은 값이 나오는 SHA-256 을 쓴다 (docs/adr/0009).
 */
public final class DeviceIdHasher {

	private DeviceIdHasher() {
	}

	public static String hash(String deviceId) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(deviceId.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", e);
		}
	}

}
