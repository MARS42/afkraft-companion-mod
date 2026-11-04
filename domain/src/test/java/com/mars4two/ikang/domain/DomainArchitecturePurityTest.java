package com.mars4two.ikang.domain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainArchitecturePurityTest {

    private static final List<String> FORBIDDEN_PACKAGES = List.of(
            "net.minecraft.",
            "com.mojang.",
            "net.fabricmc.",
            "net.neoforged.",
            "net.minecraftforge.",
            "io.netty.",
            "org.spongepowered."
    );

    @Test
    void domainMainSourcesContainZeroMinecraftOrModloaderImports() throws IOException {
        Path mainJavaDir = Path.of("src", "main", "java");
        assertTrue(Files.isDirectory(mainJavaDir), "Expected domain/src/main/java directory to exist");

        List<Path> javaFiles;
        try (Stream<Path> stream = Files.walk(mainJavaDir)) {
            javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
        }
        assertFalse(javaFiles.isEmpty(), "Expected at least one Java source file in :domain");

        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles) {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                for (String forbidden : FORBIDDEN_PACKAGES) {
                    if (line.contains(forbidden)) {
                        violations.add(file + ":" + (i + 1) + " -> " + line.trim());
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "Forbidden Minecraft/loader references found in :domain:\n" + String.join("\n", violations));
    }
}
