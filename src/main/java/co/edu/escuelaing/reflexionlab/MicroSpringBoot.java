package co.edu.escuelaing.reflexionlab;

import co.edu.escuelaing.reflexionlab.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * MicroSpringBoot - Minimal IoC Web Framework Entry Point.
 *
 * Usage modes:
 * 1. Classpath scan (recommended): java -cp target/classes
 * co.edu.escuelaing.reflexionlab.MicroSpringBoot
 * -> Scans target/classes for all @RestController classes automatically.
 *
 * 2. Explicit class (like test frameworks): java -cp target/classes
 * co.edu.escuelaing.reflexionlab.MicroSpringBoot
 * co.edu.escuelaing.reflexionlab.controller.HelloController
 * -> Loads only the specified class as a controller.
 */
public class MicroSpringBoot {

    public static void main(String[] args) throws IOException {
        HttpServer server = new HttpServer();

        if (args.length > 0) {
            // Mode 1: Load a specific class passed as command-line argument
            System.out.println("[MicroSpringBoot] Loading controller from argument: " + args[0]);
            try {
                Class<?> controllerClass = Class.forName(args[0]);
                if (controllerClass.isAnnotationPresent(RestController.class)) {
                    server.registerController(controllerClass);
                } else {
                    System.err.println("[MicroSpringBoot] Warning: " + args[0]
                            + " is not annotated with @RestController. Registering anyway.");
                    server.registerController(controllerClass);
                }
            } catch (ClassNotFoundException e) {
                System.err.println("[MicroSpringBoot] ERROR: Class not found: " + args[0]);
                System.exit(1);
            }
        } else {
            // Mode 2: Auto-scan classpath for all @RestController classes
            System.out.println("[MicroSpringBoot] Scanning classpath for @RestController components...");
            List<Class<?>> controllers = ClassScanner.findRestControllers();

            if (controllers.isEmpty()) {
                System.err.println("[MicroSpringBoot] No @RestController classes found in classpath.");
            } else {
                System.out.println("[MicroSpringBoot] Found " + controllers.size() + " controller(s).");
                for (Class<?> controller : controllers) {
                    server.registerController(controller);
                }
            }
        }

        // Start the server
        server.start();
    }
}
