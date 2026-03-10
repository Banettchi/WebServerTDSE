package co.edu.escuelaing.reflexionlab.controller;

import co.edu.escuelaing.reflexionlab.annotation.GetMapping;
import co.edu.escuelaing.reflexionlab.annotation.RequestParam;
import co.edu.escuelaing.reflexionlab.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

/**
 * REST controller that demonstrates @RequestParam support.
 * Uses an AtomicLong counter to track requests.
 *
 * Example usage:
 * GET /greeting -> "Hola World"
 * GET /greeting?name=Ana -> "Hola Ana"
 */
@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hola " + name;
    }

    @GetMapping("/greet")
    public String greetFormatted(@RequestParam(value = "name", defaultValue = "World") String name) {
        long count = counter.incrementAndGet();
        return String.format(template, name) + " (request #" + count + ")";
    }
}
