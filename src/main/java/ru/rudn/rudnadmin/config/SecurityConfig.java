package ru.rudn.rudnadmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.keycloak.client-id}")
    private String keycloakClientId;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "KEYCLOAK_MANAGER")
                        .requestMatchers("/api/**").hasAnyRole("ADMIN", "MANAGER")
                        .anyRequest().denyAll()
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public Converter<Jwt, JwtAuthenticationToken> jwtAuthenticationConverter() {
        final var scopeConverter = new JwtGrantedAuthoritiesConverter();
        return jwt -> {
            final var scopeAuthorities = scopeConverter.convert(jwt);
            final var roleAuthorities = extractRoleAuthorities(jwt);

            final var mergedAuthorities = Stream.concat(
                            scopeAuthorities == null ? Stream.empty() : scopeAuthorities.stream(),
                            roleAuthorities.stream()
                    )
                    .collect(Collectors.toSet());

            return new JwtAuthenticationToken(jwt, mergedAuthorities, jwt.getSubject());
        };
    }

    private Set<GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
        final var result = new HashSet<GrantedAuthority>();

        final var resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            final Object clientAccess = resourceAccess.get(keycloakClientId);
            if (clientAccess instanceof Map<?, ?> clientClaims) {
                final Object clientRoles = clientClaims.get("roles");
                if (clientRoles instanceof Collection<?> rolesCollection) {
                    for (Object roleObj : rolesCollection) {
                        if (roleObj instanceof String role && !role.isBlank()) {
                            result.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
                        }
                    }
                }
            }
        }

        return result;
    }
}
