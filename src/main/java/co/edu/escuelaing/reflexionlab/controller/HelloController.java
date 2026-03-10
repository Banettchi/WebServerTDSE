package co.edu.escuelaing.reflexionlab.controller;

import co.edu.escuelaing.reflexionlab.annotation.GetMapping;
import co.edu.escuelaing.reflexionlab.annotation.RestController;

/**
 * Simple REST controller that returns a greeting at the root path.
 * Demonstrates the @RestController and @GetMapping annotations.
 * Note: The "/" path is intentionally NOT mapped here so that the static
 * index.html is served from resources/static/index.html.
 */
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Greetings from MicroSpring Boot!";
    }
}
