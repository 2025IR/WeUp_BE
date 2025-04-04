package com.example.weup.dto.security;

import com.example.weup.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {
    private final User user;
    //스프링 시큐리티에서 사용자 인증정보를 제공하기 위해 유저엔티티 데이터 활용
    public CustomUserDetails(User user) {
        this.user = user;
    //객체를 생성할 때 유저엔티티 객체를 받아와 초기화 및 얘로 조회한 정보를 전달
    }

    //사용자 권한을 반환하는 메서드. 스프링 시큐리티가 권한을 확인해 사용자의 접근이 가능한지 확인
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();
        collection.add(new GrantedAuthority() {
            public String getAuthority() {
                return user.getRole();
                //db에서 권한 정보 가져오고, 아래에서 컬렉션으로 반환하여 스프링 시큐리티가 확인할 수 있게 함
            }
        });
        return collection;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // 변동 가능성 있음
    @Override
    public String getUsername() {
        return user.getName();
    }

    public String getEmail() {
        return user.getEmail();
    }

//    public String getProfileImage() {
//        return userEntity.getProfileImage();
//    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
        //계정 만료 여부.
        //return userEntity.getExpirationDate().isAfter(LocalDateTime.now());
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return user.getPasswordExpirationDate().isAfter(LocalDateTime.now());
    }


    @Override
    public boolean isEnabled() {
        return true;
    }
} 