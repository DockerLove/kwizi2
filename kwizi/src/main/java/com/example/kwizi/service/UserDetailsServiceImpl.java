package com.example.kwizi.service;

import com.example.kwizi.model.User;
import com.example.kwizi.repository.AuthenticationRepository;
import com.example.kwizi.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AuthenticationRepository authenticationRepository;

    @Autowired
    public UserDetailsServiceImpl(AuthenticationRepository authenticationRepository) {
        this.authenticationRepository = authenticationRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = authenticationRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с username: " + username + " не найден!"));

        return new UserDetailsImpl(user);
    }
}