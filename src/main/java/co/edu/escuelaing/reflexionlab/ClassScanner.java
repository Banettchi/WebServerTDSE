package co.edu.escuelaing.reflexionlab;

import co.edu.escuelaing.reflexionlab.annotation.RestController;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans all entries on the current classpath to find classes
 * annotated with @RestController and returns them for registration.
 * Works in both standalone and Maven test execution modes.
 */
public class ClassScanner {

    /**
     * Scans every directory on the classpath for @RestController annotated classes.
     * 
     * @return List of discovered controller classes
     */
    public static List<Class<?>> findRestControllers() {
        List<Class<?>> controllers = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        try {
            // Enumerate ALL classpath root URLs (handles test-classes + classes)
            Enumeration<URL> roots = ClassScanner.class.getClassLoader().getResources("");
            while (roots.hasMoreElements()) {
                URL url = roots.nextElement();
                if (!"file".equals(url.getProtocol()))
                    continue;
                try {
                    File root = new File(url.toURI());
                    if (root.exists() && root.isDirectory()) {
                        String rootPath = root.getAbsolutePath();
                        if (visited.add(rootPath)) {
                            System.out.println("[ClassScanner] Scanning: " + rootPath);
                            scanDirectory(root, root, controllers);
                        }
                    }
                } catch (URISyntaxException e) {
                    // skip malformed URLs
                }
            }
        } catch (Exception e) {
            System.err.println("[ClassScanner] Error scanning classpath: " + e.getMessage());
        }

        return controllers;
    }

    /**
     * Recursively scans a directory for .class files and loads them.
     */
    private static void scanDirectory(File root, File current, List<Class<?>> controllers) {
        File[] files = current.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(root, file, controllers);
            } else if (file.getName().endsWith(".class")) {
                String className = getClassName(root, file);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(RestController.class)) {
                        // Avoid duplicates when multiple classpath roots contain same class
                        if (controllers.stream().noneMatch(c -> c.equals(clazz))) {
                            System.out.println("[ClassScanner] Found @RestController: " + className);
                            controllers.add(clazz);
                        }
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError e) {
                    // Skip classes that can't be loaded (inner classes, test infra, etc.)
                }
            }
        }
    }

    /**
     * Converts a .class file path to a fully qualified class name.
     */
    private static String getClassName(File root, File classFile) {
        String rootPath = root.getAbsolutePath();
        String filePath = classFile.getAbsolutePath();
        String relativePath = filePath.substring(rootPath.length() + 1);
        // Convert path separators to dots and remove .class extension
        return relativePath.replace(File.separatorChar, '.').replace('/', '.').replaceAll("\\.class$", "");
    }
}
