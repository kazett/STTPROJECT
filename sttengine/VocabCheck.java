package id.stt.engine.voice;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Cek kata phrase ada di kosakata model Vosk (graph/words.txt pada model small). */
public final class VocabCheck {

    private final Set<String> words = new HashSet<>();
    private final boolean available;

    public VocabCheck(File modelDir) {
        File file = new File(modelDir, "graph/words.txt");
        boolean ok = false;
        if (file.isFile()) {
            try {
                for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                    int space = line.indexOf(' ');
                    words.add((space < 0 ? line : line.substring(0, space)).toLowerCase(Locale.ROOT));
                }
                ok = true;
            } catch (IOException ignored) {
                // dianggap tidak tersedia
            }
        }
        this.available = ok;
    }

    public boolean available() {
        return available;
    }

    /** Kata dalam phrase yang tidak ada di kosakata model. */
    public List<String> missing(String phrase) {
        List<String> missing = new ArrayList<>();
        if (!available) {
            return missing;
        }
        for (String word : phrase.split("\\s+")) {
            if (!words.contains(word)) {
                missing.add(word);
            }
        }
        return missing;
    }
}
