package com.example.academic_service.config;

import com.example.academic_service.entity.Submodule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    Submodule submodule();
    String action(); // "CREATE" | "READ" | "UPDATE" | "DELETE"
}
