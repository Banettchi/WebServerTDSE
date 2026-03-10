package co.edu.escuelaing.reflexionlab.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps an HTTP GET request to a handler method.
 * The value specifies the URI path that the method will handle.
 * Example: @GetMapping("/hello")
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GetMapping {
    String value() default "/";
}
