package com.example.jolp.dto;

import com.example.jolp.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

/*
Spring Security에서 사용자 인증 정보를 관리하는 역할
UserDetails 인터페이스를 구현하여
Spring Security가 사용자 계정의 정보(예: 사용자 이름, 비밀번호, 권한 등)를 처리할 수 있도록 합니다.
 */

public class CustomUserDetails implements UserDetails {
    private final UserEntity userEntity;
    //스프링 시큐리티에서 사용자 인증정보를 제공하기 위해 유저엔티티 데이터 활용 - final로 선언
    public CustomUserDetails(UserEntity userEntity) {
        this.userEntity = userEntity;
    //객체를 생성할 때 유저엔티티 객체를 받아와 초기화. 얘로 조회한 정보를 전달
    }

    //사용자 권한을 반환하는 메서드. 스프링 시큐리티가 권한을 확인해 사용자의 접근이 가능한지 확인
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();
        collection.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return userEntity.getRole();
                //db에서 권한 정보 가져오고, 아래에서 컬렉션으로 반환하여 스프링 시큐리티가 확인할 수 있게 함
            }
        });
        return collection;
    }

    @Override
    public String getPassword() {
        return userEntity.getPassword();
    }

    @Override
    public String getUsername() {
        return userEntity.getUsername();
    }

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
//        System.out.println("isCredentialsNonExpired() 호출됨" + userEntity.getPasswordExpirationDate().isAfter(LocalDateTime.now()));
//        System.out.println(userEntity.getPasswordExpirationDate()+"/"+LocalDateTime.now());
        return userEntity.getPasswordExpirationDate().isAfter(LocalDateTime.now());
    }


    @Override
    public boolean isEnabled() {
        return true;
    }
}