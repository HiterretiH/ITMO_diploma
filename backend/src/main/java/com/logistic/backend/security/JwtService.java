package com.logistic.backend.security;

import com.logistic.backend.config.JwtProperties;
import com.logistic.backend.user.Role;
import com.logistic.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        String secret = jwtProperties.getSecret();
        if (secret.length() < 32) {
            secret = secret.repeat(2);
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(User user) {
        List<String> roles = user.getRoles().stream().map(Role::name).toList();
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtProperties.getExpirationMs());
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }
}
