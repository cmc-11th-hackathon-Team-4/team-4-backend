package com.cmc.hackathon4.global.auth;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 X-Device-Id 헤더로 찾은 User 가 들어온다.
 * 헤더가 없으면 COMMON-400, 등록되지 않은 값이면 USER-404.
 * 들어오는 User 는 트랜잭션 밖에서 조회한 것이라, 값을 바꾸려면 서비스에서 id 로 다시 조회한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
