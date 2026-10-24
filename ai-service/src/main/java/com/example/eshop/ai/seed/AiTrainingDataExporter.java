package com.example.eshop.ai.seed;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPOutputStream;

/** Offline, bounded-memory synthetic draft export. Does not submit data to a model. */
public final class AiTrainingDataExporter {

    private AiTrainingDataExporter() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException(
                    "Usage: AiTrainingDataExporter output.jsonl.gz [even-row-count]"
            );
        }

        int count = args.length == 2
                ? Integer.parseInt(args[1])
                : AiTrainingSeedData.MAX_SIZE;

        if (count < 2
                || count > AiTrainingSeedData.MAX_SIZE
                || count % 2 != 0) {
            throw new IllegalArgumentException(
                    "Row count must be even and between 2 and 10,000,000"
            );
        }

        Path target = Path.of(args[0]).toAbsolutePath();

        if (Files.exists(target)) {
            throw new IllegalArgumentException(
                    "Output already exists: " + target
            );
        }

        Path partial = target.resolveSibling(
                target.getFileName() + ".partial"
        );
        Files.createDirectories(target.getParent());

        try (var output = Files.newOutputStream(
                partial,
                StandardOpenOption.CREATE_NEW
        );
             var gzip = new GZIPOutputStream(output, 65536) {{
                 def.setLevel(1);
             }};
             var writer = new BufferedWriter(
                     new OutputStreamWriter(
                             gzip,
                             StandardCharsets.UTF_8
                     ),
                     65536
             )) {
            for (int i = 0; i < count; i++) {
                writer.write(toJson(AiTrainingSeedData.exampleAt(i)));
                writer.newLine();

                if ((i + 1) % 100_000 == 0) {
                    System.out.println(
                            "Exported " + (i + 1) + " / " + count
                    );
                }
            }
        }

        Files.move(partial, target);
        System.out.println(
                "Completed " + count + " synthetic DRAFT rows: " + target
        );
    }

    static String toJson(AiTrainingSeedData.Example example) {
        return "{\"language\":" + quote(example.language())
                + ",\"split\":" + quote(example.split())
                + ",\"review_status\":\"DRAFT\""
                + ",\"source_verified\":false"
                + ",\"consent_confirmed\":false"
                + ",\"source\":\"synthetic:"
                + AiTrainingSeedData.GENERATOR_VERSION
                + "\",\"messages\":["
                + "{\"role\":\"user\",\"content\":"
                + quote(example.prompt()) + "},"
                + "{\"role\":\"assistant\",\"content\":"
                + quote(example.response()) + "}]}";
    }

    private static String quote(String value) {
        var out = new StringBuilder(value.length() + 2).append('"');

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }

        return out.append('"').toString();
    }
}