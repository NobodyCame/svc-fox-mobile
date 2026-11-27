package dev.yukiinotenshi.foxoperator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.zip.ZipInputStream;

final class OperatorRuntime {
    private static final String RELEASE = "20261110";

    private OperatorRuntime() {}

    static synchronized String executable(Path cache, String configured) throws IOException {
        if (configured != null && !configured.isBlank() && !configured.equalsIgnoreCase("auto")) return configured;
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        boolean windows = os.startsWith("windows");
        boolean x64 = arch.equals("amd64") || arch.equals("x86_64");
        String platform;
        if (windows && x64) platform = "windows-x64";
        else if (os.startsWith("linux") && x64) platform = "linux-x64";
        else if (os.startsWith("linux") && (arch.equals("aarch64") || arch.equals("arm64"))) platform = "linux-arm64";
        else throw new IOException("no embedded Fox Mobile runtime for " + os + "/" + arch + "; configure Python manually");

        String id = platform + "-" + RELEASE;
        String relativeExecutable = windows ? "python.exe" : "bin/python3.11";
        Path installed = cache.resolve(id);
        Path executable = installed.resolve(relativeExecutable);
        if (Files.isRegularFile(executable)) return executable.toAbsolutePath().toString();

        Files.createDirectories(cache);
        try (var resource = OperatorRuntime.class.getResourceAsStream("/fox-operator/runtime/" + id + ".zip")) {
            if (resource == null) throw new IOException("missing bundled runtime " + id);
            Path staging = Files.createTempDirectory(cache, ".operator-");
            try (var zip = new ZipInputStream(resource)) {
                for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    Path target = staging.resolve(entry.getName()).normalize();
                    if (!target.startsWith(staging)) throw new IOException("bad runtime archive path");
                    if (entry.isDirectory()) Files.createDirectories(target);
                    else {
                        Files.createDirectories(target.getParent());
                        Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            Path stagedExecutable = staging.resolve(relativeExecutable);
            if (!Files.isRegularFile(stagedExecutable) || (!windows && !stagedExecutable.toFile().setExecutable(true, true))) {
                throw new IOException("cannot prepare bundled executable " + id);
            }
            try {
                Files.move(staging, installed);
            } catch (java.nio.file.FileAlreadyExistsException e) {
                if (!Files.isRegularFile(executable)) throw e;
            }
        }
        return executable.toAbsolutePath().toString();
    }
}
