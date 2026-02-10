package de.signaliduna.dltmanager.config;

import com.c4_soft.springaddons.security.oidc.spring.SpringAddonsMethodSecurityExpressionHandler;
import com.c4_soft.springaddons.security.oidc.spring.SpringAddonsMethodSecurityExpressionRoot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

	@Value("${authorization.users}")
	String[] authorizedUsers;

	@Bean
	MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
		return new SpringAddonsMethodSecurityExpressionHandler(() -> new CustomMethodSecurityExpressionRoot(Arrays.asList(authorizedUsers)));
	}

	private static final class CustomMethodSecurityExpressionRoot extends SpringAddonsMethodSecurityExpressionRoot {
		private static final String JWT_USER_ID_KEY = "uid";
		private final List<String> authorizedUsers;

		public CustomMethodSecurityExpressionRoot(List<String> authorizedUsers) {
			this.authorizedUsers = authorizedUsers;
		}

		public boolean isAuthorizedUser() {
			Authentication authentication = getAuthentication();
			if (authentication instanceof JwtAuthenticationToken jwtAuth) {
				if (jwtAuth.getToken() == null) {
					return false;
				}
				String uid = jwtAuth.getToken().getClaims().get(JWT_USER_ID_KEY).toString();
				return this.authorizedUsers.contains(uid);
			} else {
				return this.authorizedUsers.contains(authentication.getName());
			}
		}
	}
}
