package com.jalaldeveloper.accountingsystem;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleBoundaryArchitectureTest {

    @Test
    void crossModuleDataaccessImportsShouldNotExist() throws IOException {
        Path backendRoot = Path.of(System.getProperty("user.dir")).getParent().getParent();
        List<String> violations = new ArrayList<>();
        String[] contexts = {"accounting", "purchase", "sales", "pos", "inventory", "contacts"};

        try (Stream<Path> files = Files.walk(backendRoot)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("-service"))
                    .filter(path -> path.toString().contains("src/main/java"))
                    .filter(path -> !path.toString().contains("/accounting-service/accounting-container/"))
                    .forEach(path -> {
                        String source = read(path);
                        if (source.isBlank()) {
                            return;
                        }
                        String fileContext = contextFor(path.toString(), contexts);
                        for (String context : contexts) {
                            if (context.equals(fileContext)) {
                                continue;
                            }
                            String marker = "import com.jalaldeveloper.accountingsystem." + context + ".dataaccess.";
                            if (source.contains(marker)) {
                                violations.add(path + " -> " + marker);
                            }
                        }
                    });
        }

        assertTrue(violations.isEmpty(), "Cross-module dataaccess imports found:\n" + String.join("\n", violations));
    }

    private static String contextFor(String path, String[] contexts) {
        for (String context : contexts) {
            if (path.contains("/" + context + "-service/")) {
                return context;
            }
        }
        return "";
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "";
        }
    }
}
