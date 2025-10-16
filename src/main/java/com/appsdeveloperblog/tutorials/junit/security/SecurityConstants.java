package com.appsdeveloperblog.tutorials.junit.security;


public class SecurityConstants {
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    // ✅ 512‑bit (64+ byte) secret for HS512 algorithm:
    public static final String TOKEN_SECRET =
            "4f3c2f5d7a6b8c9d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0";
}
