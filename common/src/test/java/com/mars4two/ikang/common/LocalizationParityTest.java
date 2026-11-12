package com.mars4two.ikang.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mars4two.ikang.core.util.AFKraftTranslations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizationParityTest {

    private static final Path NODE_OR_MODULE_DIR = Path.of(System.getProperty("user.dir"));
    private static final Path COMMON_ROOT = resolveCommonBranchRoot(NODE_OR_MODULE_DIR);
    private static final Path LANG_DIR = COMMON_ROOT.resolve("src/main/resources/assets/afkraft_companion/lang");
    private static final Gson GSON = new Gson();

    private static Map<String, String> enUs;
    private static Map<String, String> esEs;
    private static Map<String, String> esMx;

    private static Path resolveCommonBranchRoot(Path workingDir) {
        String explicit = System.getProperty("afkraft.commonBranchRoot");
        if (explicit != null && !explicit.isBlank()) {
            return Path.of(explicit);
        }
        if (workingDir.getParent() != null
                && "versions".equals(workingDir.getParent().getFileName().toString())
                && workingDir.getParent().getParent() != null) {
            return workingDir.getParent().getParent();
        }
        return workingDir;
    }

    @BeforeAll
    static void loadLanguageFiles() throws IOException {
        Path enUsFile = LANG_DIR.resolve("en_us.json");
        Path esEsFile = LANG_DIR.resolve("es_es.json");
        Path esMxFile = LANG_DIR.resolve("es_mx.json");

        assertTrue(Files.isRegularFile(enUsFile), "Missing en_us.json at " + enUsFile);
        assertTrue(Files.isRegularFile(esEsFile), "Missing es_es.json at " + esEsFile);
        assertTrue(Files.isRegularFile(esMxFile), "Missing es_mx.json at " + esMxFile);

        java.lang.reflect.Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        enUs = GSON.fromJson(Files.readString(enUsFile), mapType);
        esEs = GSON.fromJson(Files.readString(esEsFile), mapType);
        esMx = GSON.fromJson(Files.readString(esMxFile), mapType);

        assertFalse(enUs.isEmpty(), "en_us.json must not be empty");
        assertFalse(esEs.isEmpty(), "es_es.json must not be empty");
        assertFalse(esMx.isEmpty(), "es_mx.json must not be empty");
    }

    @Test
    void englishAndSpanishHaveExactKeyParity() {
        Set<String> missingInSpanish = new HashSet<>(enUs.keySet());
        missingInSpanish.removeAll(esEs.keySet());

        Set<String> extraInSpanish = new HashSet<>(esEs.keySet());
        extraInSpanish.removeAll(enUs.keySet());

        assertTrue(missingInSpanish.isEmpty(), () -> "Keys present in en_us.json but missing in es_es.json: " + missingInSpanish);
        assertTrue(extraInSpanish.isEmpty(), () -> "Keys present in es_es.json but missing in en_us.json: " + extraInSpanish);
        assertEquals(enUs.keySet(), esEs.keySet(), "Key sets of en_us and es_es must match exactly");
    }

    @Test
    void mexicanSpanishIsIdenticalMirrorOfSpanish() {
        assertEquals(esEs, esMx, "es_mx.json must be an exact copy/mirror of es_es.json");
    }

    @Test
    void parameterPlaceholdersMatchAcrossLanguages() {
        Pattern placeholderPattern = Pattern.compile("%[0-9]*\\.?[0-9]*[sdf]");
        List<String> mismatchErrors = new ArrayList<>();

        for (String key : enUs.keySet()) {
            List<String> enPlaceholders = extractMatches(placeholderPattern, enUs.get(key));
            List<String> esPlaceholders = extractMatches(placeholderPattern, esEs.get(key));

            if (!enPlaceholders.equals(esPlaceholders)) {
                mismatchErrors.add(String.format("Key '%s' placeholder mismatch. EN: %s, ES: %s", key, enPlaceholders, esPlaceholders));
            }
        }

        assertTrue(mismatchErrors.isEmpty(), () -> "Placeholder mismatches found:\n" + String.join("\n", mismatchErrors));
    }

    @Test
    void allTranslationConstantsAreDeclaredInLanguageFiles() throws IllegalAccessException {
        List<String> missingConstants = new ArrayList<>();

        for (Field field : AFKraftTranslations.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    && Modifier.isPublic(field.getModifiers())
                    && field.getType() == String.class) {
                String key = (String) field.get(null);
                if (!enUs.containsKey(key)) {
                    missingConstants.add(field.getName() + " -> " + key);
                }
            }
        }

        assertTrue(missingConstants.isEmpty(), () -> "AFKraftTranslations constants missing in en_us.json:\n" + String.join("\n", missingConstants));
    }

    @Test
    void noUserFacingNaturalLanguageLiteralComponentsInProduction() throws IOException {
        Path mainJava = COMMON_ROOT.resolve("src/main/java");
        Path clientJava = COMMON_ROOT.resolve("src/client/java");

        List<Path> javaFiles = new ArrayList<>();
        try (Stream<Path> s = Files.walk(mainJava)) {
            javaFiles.addAll(s.filter(p -> p.toString().endsWith(".java")).toList());
        }
        try (Stream<Path> s = Files.walk(clientJava)) {
            javaFiles.addAll(s.filter(p -> p.toString().endsWith(".java")).toList());
        }

        Pattern literalPattern = Pattern.compile("Component\\.literal\\(\\s*\"([^\"]+)\"\\s*\\)");
        List<String> suspiciousLiterals = new ArrayList<>();

        // Strings with spaces or letters that look like natural language sentences/phrases
        for (Path file : javaFiles) {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) {
                    continue;
                }
                Matcher matcher = literalPattern.matcher(line);
                while (matcher.find()) {
                    String literal = matcher.group(1);
                    // Whitelist dynamic placeholders or simple non-translatable tokens like brackets or single punctuation
                    if (literal.matches("^[\\s\\[\\]{}():;,.-]*$")) {
                        continue;
                    }
                    // Any literal string with spaces or common spanish/english words is a forbidden hardcoded string
                    if (literal.contains(" ") || literal.matches(".*[a-zA-Z]{3,}.*")) {
                        suspiciousLiterals.add(COMMON_ROOT.relativize(file) + ":" + (i + 1) + " -> " + literal);
                    }
                }
            }
        }

        assertTrue(suspiciousLiterals.isEmpty(), () -> "Forbidden user-facing Component.literal found:\n" + String.join("\n", suspiciousLiterals));
    }

    private static List<String> extractMatches(Pattern pattern, String text) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }
}
