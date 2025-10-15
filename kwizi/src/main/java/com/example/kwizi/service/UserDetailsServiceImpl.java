package com.example.kwizi.service;

import com.example.kwizi.model.User;
import com.example.kwizi.repository.AuthenticationRepository;
import com.example.kwizi.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final AuthenticationRepository authenticationRepository;


    @Autowired
    public UserDetailsServiceImpl(AuthenticationRepository authenticationRepository) {
        this.authenticationRepository = authenticationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username пуст или равен null");
        }

        logger.debug("Загрузка пользователя "+username);

        User user = authenticationRepository.findByUsername(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format("Пользователь '%s' не найден", username)
                ));

        logger.debug("Успешная загрузка пользователя " + username);

        return new UserDetailsImpl(user);
    }
}