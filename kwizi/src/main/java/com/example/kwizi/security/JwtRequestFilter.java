package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.repository.RevokedTokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

/*Этот фильтр перехватывает все входящие HTTP-запросы.
Он проверяет наличие заголовка Authorization в запросе.
Если заголовок присутствует и начинается с Bearer , он извлекает JWT из заголовка.
Он использует JwtUtils для проверки JWT.
Если JWT валиден, он извлекает имя пользователя из JWT и создает объект UsernamePasswordAuthenticationToken.
Он устанавливает этот объект в SecurityContextHolder, чтобы Spring Security знал, что пользователь аутентифицирован.*/
    private UserDetailsService userDetailsService;

    private JwtUtils jwtUtils;

    private final RevokedTokenRepository revokedTokenRepo; // Добавляем

    @Autowired
    public JwtRequestFilter(UserDetailsService userDetailsService,
                            JwtUtils jwtUtils,
                            RevokedTokenRepository revokedTokenRepo) {
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.revokedTokenRepo = revokedTokenRepo;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);

            try {
                // Проверяем, не отозван ли токен
                String jti = jwtUtils.extractJti(jwt);
                if (revokedTokenRepo.existsById(jti)) {
                    throw new JwtAuthenticationException("Токен был отозван");
                }

                String username = jwtUtils.getUsernameFromToken(jwt);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtUtils.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (JwtAuthenticationException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getServletPath().equals("/api/auth/login"); // Не фильтруем /api/users/authenticate
    }
}