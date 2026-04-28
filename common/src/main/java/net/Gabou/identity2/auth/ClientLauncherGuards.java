package net.Gabou.identity2.auth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import dev.architectury.platform.Platform;

public final class ClientLauncherGuards {
    private static final String SUSPICIOUS_FILE_NAME = "TLauncherAdditional.json";
    private static volatile String detectedReason;

    private ClientLauncherGuards() {
    }

    public static void enforce() {
        if (Platform.isDevelopmentEnvironment()) {
            detectedReason = null;
            return;
        }
        detectedReason = detectSuspiciousLauncher();
    }

    public static String detectSuspiciousLauncher() {
        String suspiciousFile = findSuspiciousFile();
        return suspiciousFile == null ? null : "file:" + suspiciousFile;
    }

    public static String getDetectedReason() {
        return detectedReason;
    }

    private static String findSuspiciousFile() {
        Path cwd = Paths.get(System.getProperty("user.dir", "."));
        Path candidate = cwd.resolve(SUSPICIOUS_FILE_NAME);
        if (Files.exists(candidate)) {
            return candidate.toAbsolutePath().toString();
        }
        return null;
    }
}
