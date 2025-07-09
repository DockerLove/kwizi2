package com.example.kwizi.security;

import com.example.kwizi.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserDetailsImpl implements UserDetails {

    private final User user;

    public UserDetailsImpl(User user) {
        this.user = user;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //  В данном примере у нас нет ролей, поэтому возвращаем пустую коллекцию.
        //  В реальном приложении здесь нужно будет получить роли пользователя из базы данных
        //  и преобразовать их в GrantedAuthority.
        return List.of();

        //Пример с ролями
        //List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        //return authorities;

    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Или user.isAccountNonExpired(), если у вас есть такое поле в User
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Или user.isAccountNonLocked()
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Или user.isCredentialsNonExpired()
    }

    // Дополнительные геттеры
    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }
}