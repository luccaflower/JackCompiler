package io.github.luccaflower.jack;

import io.github.luccaflower.jack.codewriter.ClassWriter;
import io.github.luccaflower.jack.parser.JackClass;
import io.github.luccaflower.jack.parser.Parser;
import io.github.luccaflower.jack.tokenizer.IteratingTokenizer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    private static String path;

    public static void main(String[] args) {
        if (args.length == 0) {
            path = ".";
        }
        else {
            path = args[0];
        }
        var directory = new File(path);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory " + path);
        }
        List<File> files = Arrays.stream(directory.listFiles()).filter(f -> f.getName().endsWith(".jack")).toList();
        var classes = files.stream()
            .map(Main::inputStream)
            .map(BufferedInputStream::new)
            .map(Main::readAllBytes)
            .map(b -> new String(b, StandardCharsets.UTF_8))
            .map(IteratingTokenizer::new)
            .map(t -> new Parser().parse(t))
            .toList();
        classes.forEach(Main::writeClass);

    }

    private static void writeClass(JackClass c) {
        var compiled = new ClassWriter(c).write().lines().filter(l -> !l.isBlank()).collect(Collectors.joining("\n"));
        var file = new File("%s/%s.vm".formatted(path, c.name()));
        if (file.exists() && !file.delete()) {
            throw new IllegalStateException("Cannot overwrite file " + file.getName());
        }
        try (var os = new FileOutputStream(file)) {
            os.write(compiled.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] readAllBytes(BufferedInputStream is) {
        try {
            return is.readAllBytes();
        }
        catch (IOException e) {
            throw new IllegalStateException("huh.");
        }
    }

    private static FileInputStream inputStream(File f) {
        try {
            return new FileInputStream(f);
        }
        catch (FileNotFoundException e) {
            throw new IllegalStateException("That shouldn't happen");
        }
    }

}
