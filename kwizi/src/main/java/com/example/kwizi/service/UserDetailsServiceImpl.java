package com.example.kwizi.service;

import com.example.kwizi.model.User;
import com.example.kwizi.repository.AuthenticationRepository;
import com.example.kwizi.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AuthenticationRepository authenticationRepository;
    private final Environment environment;

    @Autowired
    public UserDetailsServiceImpl(AuthenticationRepository authenticationRepository, Environment environment) {
        this.authenticationRepository = authenticationRepository;
        this.environment = environment;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = authenticationRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с username: " + username + " не найден!"));

        return new UserDetailsImpl(user);
    }
}