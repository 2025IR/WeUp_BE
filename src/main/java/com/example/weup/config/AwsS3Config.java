//package com.example.weup.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class AwsS3Config {
//
//    @Value("${cloud.aws.region.static}")
//    private String region;
//
//    @Bean
//    public S3Client s3Client() {
//        return S3Client.builder()
//                .region(Region.of(region))
//                // 자격 증명은 DefaultCredentialsProvider를 사용할 수도 있고,
//                // 아래와 같이 명시적으로 설정할 수도 있습니다.
//                // .credentialsProvider(ProfileCredentialsProvider.create("default"))
//                .build();
//    }
//}