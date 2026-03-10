package co.edu.escuelaing.reflexionlab;

import co.edu.escuelaing.reflexionlab.annotation.GetMapping;
import co.edu.escuelaing.reflexionlab.annotation.RequestParam;
import co.edu.escuelaing.reflexionlab.annotation.RestController;
import co.edu.escuelaing.reflexionlab.controller.GreetingController;
import co.edu.escuelaing.reflexionlab.controller.HelloController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the MicroSpringBoot IoC Framework.
 * Verifies annotation detection, route registration, and @RequestParam
 * extraction.
 */
public class MicroSpringBootTest {

    // ===== @RestController annotation tests =====

    @Test
    @DisplayName("HelloController should be annotated with @RestController")
    void testHelloControllerHasRestControllerAnnotation() {
        assertTrue(
                HelloController.class.isAnnotationPresent(RestController.class),
                "HelloController must have @RestController annotation");
    }

    @Test
    @DisplayName("GreetingController should be annotated with @RestController")
    void testGreetingControllerHasRestControllerAnnotation() {
        assertTrue(
                GreetingController.class.isAnnotationPresent(RestController.class),
                "GreetingController must have @RestController annotation");
    }

    // ===== @GetMapping annotation tests =====

    @Test
    @DisplayName("HelloController.hello() should have @GetMapping(\"/hello\")")
    void testHelloControllerGetMappingOnHello() throws NoSuchMethodException {
        Method method = HelloController.class.getMethod("hello");
        assertTrue(method.isAnnotationPresent(GetMapping.class));
        assertEquals("/hello", method.getAnnotation(GetMapping.class).value());
    }

    @Test
    @DisplayName("GreetingController.greeting() should have @GetMapping(\"/greeting\")")
    void testGreetingControllerGetMapping() throws NoSuchMethodException {
        Method method = GreetingController.class.getMethod("greeting", String.class);
        assertTrue(method.isAnnotationPresent(GetMapping.class));
        assertEquals("/greeting", method.getAnnotation(GetMapping.class).value());
    }

    // ===== @RequestParam annotation tests =====

    @Test
    @DisplayName("GreetingController.greeting() param should have @RequestParam with defaultValue=\"World\"")
    void testGreetingControllerRequestParam() throws NoSuchMethodException {
        Method method = GreetingController.class.getMethod("greeting", String.class);
        Parameter[] params = method.getParameters();
        assertEquals(1, params.length);
        assertTrue(params[0].isAnnotationPresent(RequestParam.class));
        RequestParam rp = params[0].getAnnotation(RequestParam.class);
        assertEquals("name", rp.value());
        assertEquals("World", rp.defaultValue());
    }

    // ===== Controller logic tests =====

    @Test
    @DisplayName("GreetingController.greeting() with name returns 'Hola <name>'")
    void testGreetingWithName() {
        GreetingController controller = new GreetingController();
        String result = controller.greeting("Diego");
        assertEquals("Hola Diego", result);
    }

    @Test
    @DisplayName("GreetingController.greeting() with default value returns 'Hola World'")
    void testGreetingWithDefaultValue() {
        GreetingController controller = new GreetingController();
        String result = controller.greeting("World"); // Using default
        assertEquals("Hola World", result);
    }

    @Test
    @DisplayName("HelloController.hello() returns greeting string")
    void testHelloControllerReturnsGreeting() {
        HelloController controller = new HelloController();
        String result = controller.hello();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("MicroSpring Boot"));
    }

    // ===== ClassScanner test =====

    @Test
    @DisplayName("ClassScanner should find at least the two example controllers")
    void testClassScannerFindsControllers() {
        List<Class<?>> controllers = ClassScanner.findRestControllers();
        assertNotNull(controllers);
        assertFalse(controllers.isEmpty(), "ClassScanner should find at least one @RestController");

        boolean foundHello = controllers.stream()
                .anyMatch(c -> c.equals(HelloController.class));
        boolean foundGreeting = controllers.stream()
                .anyMatch(c -> c.equals(GreetingController.class));

        assertTrue(foundHello, "Should find HelloController");
        assertTrue(foundGreeting, "Should find GreetingController");
    }

    // ===== HttpServer route registration test =====

    @Test
    @DisplayName("HttpServer should register routes from controller without errors")
    void testHttpServerRegisterController() {
        HttpServer server = new HttpServer();
        // Should not throw
        assertDoesNotThrow(() -> server.registerController(HelloController.class));
        assertDoesNotThrow(() -> server.registerController(GreetingController.class));
    }
}
