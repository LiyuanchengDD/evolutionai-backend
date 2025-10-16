package com.example.grpcdemo.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class UserJwtAuthenticationConverter implements Converter<Jwt, UserAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public UserAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);
        List<GrantedAuthority> merged = new ArrayList<>(authorities);

        String role = jwt.getClaimAsString("role");
        if (StringUtils.hasText(role)) {
            merged.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
        }

        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
        boolean dev = issuer.startsWith("app://dev");
        AuthenticatedUser principal = new AuthenticatedUser(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                dev
        );
        return new UserAuthenticationToken(principal, jwt, merged);
    }
}

