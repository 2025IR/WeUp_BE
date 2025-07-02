package com.example.weup.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter  //TODO. Setter 없어도 될 것 같은데
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {
    private String bucket;
    private String accessKey;
    private String secretKey;
    private String region;
}

