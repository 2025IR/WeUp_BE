package com.example.weup.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    private final RedisTemplate<Object, Object> redisTemplate;

    public RedisService(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void testRedisConnection() {

        redisTemplate.opsForValue().set("testKey", "Hello Redis !!");

        String value = (String) redisTemplate.opsForValue().get("testKey");
        System.out.println("Redis Test !!! : " + value);
    }
}
