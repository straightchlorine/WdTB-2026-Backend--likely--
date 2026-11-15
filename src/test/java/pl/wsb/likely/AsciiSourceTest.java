package pl.wsb.likely;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.US_ASCII;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Keeps our code plain ASCII, so it reads the same in every editor, terminal and diff. */
class AsciiSourceTest {

    // Not checked: course-owned files (rules tests, the start page, docs) and data.sql, whose
    // comments are in Polish on purpose.
    private static final List<String> ROOTS =
            List.of(
                    "pom.xml",
                    "src/main/java",
                    "src/main/resources/application.properties",
                    "src/test/java/pl/wsb/likely");

    @Test
    @DisplayName("Code contains only ASCII characters")
    void codeIsAscii() throws IOException {
        Path base = Path.of(System.getProperty("project.dir", "."));
        List<String> offenders = new ArrayList<>();
        for (String root : ROOTS) {
            try (Stream<Path> files = Files.walk(base.resolve(root))) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    List<String> lines = Files.readAllLines(file);
                    for (int i = 0; i < lines.size(); i++) {
                        if (!US_ASCII.newEncoder().canEncode(lines.get(i))) {
                            offenders.add(base.relativize(file) + ":" + (i + 1));
                        }
                    }
                }
            }
        }
        assertThat(offenders).as("lines with non-ASCII characters").isEmpty();
    }
}
