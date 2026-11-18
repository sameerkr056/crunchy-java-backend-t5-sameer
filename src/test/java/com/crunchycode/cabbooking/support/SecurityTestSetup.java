package com.crunchycode.cabbooking.support;

import com.crunchycode.cabbooking.security.SecurityConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put this next to @WebMvcTest so the REAL security rules (SecurityConfig, JWT filter, @PreAuthorize)
 * are active in the controller tests. Only the services are mocked.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({SecurityConfig.class, TestSecurityConfig.class})
public @interface SecurityTestSetup {
}
