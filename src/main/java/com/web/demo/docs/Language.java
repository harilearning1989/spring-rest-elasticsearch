package com.web.demo.docs;

public record Language(
        String iso639_1,
        String iso639_2,
        String name,
        String nativeName
) {}
