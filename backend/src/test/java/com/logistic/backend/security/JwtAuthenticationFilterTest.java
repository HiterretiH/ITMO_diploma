package com.logistic.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtService jwtService;
    @Mock DatabaseUserDetailsService userDetailsService;
    @Mock SecurityProblemResponseSupport problemResponseSupport;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter =
                new JwtAuthenticationFilter(jwtService, userDetailsService, problemResponseSupport);
    }

    @Test
    void returns503WhenUserLookupFailsWithDataAccessException() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice"))
                .thenThrow(new DataAccessResourceFailureException("pool exhausted"));

        filter.doFilterInternal(request, response, filterChain);

        verify(problemResponseSupport)
                .write(
                        eq(response),
                        eq(HttpStatus.SERVICE_UNAVAILABLE),
                        eq("Сервис временно недоступен. Повторите попытку позже."));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void continuesChainWhenBearerHeaderMissing() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(problemResponseSupport, never()).write(any(), any(), any());
    }

    @Test
    void continuesChainAfterUsernameNotFound() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer stale-token");
        when(jwtService.extractUsername("stale-token")).thenReturn("missing");
        when(userDetailsService.loadUserByUsername("missing"))
                .thenThrow(
                        new org.springframework.security.core.userdetails.UsernameNotFoundException(
                                "missing"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(problemResponseSupport, never()).write(any(), any(), any());
    }

    @Test
    void continuesChainWhenAuthenticationSucceeds() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("alice");
        UserDetails details =
                org.springframework.security.core.userdetails.User.withUsername("alice")
                        .password("x")
                        .roles("USER")
                        .build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(details);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(problemResponseSupport, never()).write(any(), any(), any());
    }
}
