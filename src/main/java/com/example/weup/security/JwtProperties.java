package com.example.weup.security;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    private long accessTokenExpiration;

    private long refreshTokenExpiration;

}


//@Getter
//@Setter
//@Component
//@ConfigurationProperties(prefix = "jwt")
//public class JwtProperties {
//    private String secret;
//    private AccessToken accessToken;
//    private RefreshToken refreshToken;
//
//    @Getter
//    @Setter
//    public static class AccessToken {
//        private long expiration;
//    }
//
//    @Getter
//    @Setter
//    public static class RefreshToken {
//        private long expiration;
//    }
//}