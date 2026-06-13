package io.github.user.evedium.gpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GPUScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("GPUScanner");

    private static final Pattern[] PASCAL_PATTERNS = {
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GTX\\s+10(\\d{2})(?:\\s+Ti|\\s+SUPER)?"),
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GTX\\s+1050\\s+Ti"),
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GTX\\s+1060"),
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GTX\\s+1070"),
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GTX\\s+1080"),
        Pattern.compile("(?:NVIDIA\\s+)?TITAN\\s+X\\s*\\(Pascal\\)"),
        Pattern.compile("(?:NVIDIA\\s+)?TITAN\\s+Xp"),
        Pattern.compile("Quadro\\s+P\\d{4}")
    };

    private static final Pattern[] TURING_PATTERNS = {
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+RTX\\s+20(\\d{2})(?:\\s+Ti|\\s+SUPER)?"),
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GTX\\s+16(\\d{2})(?:\\s+Ti|\\s+SUPER)?"),
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GTX\\s+1650\\s+Ti"),
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GTX\\s+1660\\s+(?:Ti|SUPER)?"),
        Pattern.compile("Quadro\\s+RTX\\s+\\d{4}"),
        Pattern.compile("Quadro\\s+T\\d{4}")
    };

    private static final Pattern[] AMPERE_PATTERNS = {
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+RTX\\s+30(\\d{2})(?:\\s+Ti)?")
    };

    private static final Pattern[] ADA_PATTERNS = {
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+RTX\\s+40(\\d{2})(?:\\s+Ti|\\s+SUPER)?")
    };

    private static final Pattern[] BLACKWELL_PATTERNS = {
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+RTX\\s+50(\\d{2})(?:\\s+Ti)?")
    };

    private static final Pattern[] MAXWELL_PATTERNS = {
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GTX\\s+(?:750|750\\s+Ti|960|970|980|980\\s+Ti|Titan\\s+X)"),
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GTX\\s+9(\\d{2})(?:\\s+Ti)?")
    };

    private static final Pattern[] KEPLER_PATTERNS = {
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GTX\\s+(?:650|650\\s+Ti|660|660\\s+Ti|670|680|690|760|770|780|780\\s+Ti|Titan|Titan\\s+Black|Titan\\s+Z)"),
        Pattern.compile("(?:NVIDIA\\s+)?GeForce\\s+GT\\s+6(?:40|50|60|70|80)")
    };

    public static GPUArchitecture detectArchitecture(String renderer, String vendor, String version) {
        if (renderer == null || renderer.isEmpty()) {
            LOGGER.warn("Cannot detect GPU: renderer string is null/empty");
            return GPUArchitecture.UNKNOWN;
        }

        if (vendor != null && !vendor.toLowerCase().contains("nvidia") &&
            !renderer.toLowerCase().contains("nvidia")) {
            if (vendor.toLowerCase().contains("amd") ||
                vendor.toLowerCase().contains("advanced micro devices") ||
                renderer.toLowerCase().contains("amd") ||
                renderer.toLowerCase().contains("radeon") ||
                vendor.toLowerCase().contains("intel") ||
                renderer.toLowerCase().contains("intel") ||
                vendor.toLowerCase().contains("apple") ||
                renderer.toLowerCase().contains("apple")) {
                LOGGER.info("Non-NVIDIA GPU detected: {}", vendor);
                return GPUArchitecture.UNSUPPORTED;
            }
            LOGGER.warn("Unknown GPU vendor: {}. Assuming NVIDIA.", vendor);
        }

        for (var pattern : BLACKWELL_PATTERNS) {
            if (pattern.matcher(renderer).find()) {
                return GPUArchitecture.BLACKWELL;
            }
        }

        for (var pattern : ADA_PATTERNS) {
            if (pattern.matcher(renderer).find()) {
                return GPUArchitecture.ADA_LOVELACE;
            }
        }

        for (var pattern : AMPERE_PATTERNS) {
            if (pattern.matcher(renderer).find()) {
                return GPUArchitecture.AMPERE;
            }
        }

        for (var pattern : TURING_PATTERNS) {
            if (pattern.matcher(renderer).find()) {
                return GPUArchitecture.TURING;
            }
        }

        for (var pattern : PASCAL_PATTERNS) {
            if (pattern.matcher(renderer).find()) {
                return GPUArchitecture.PASCAL;
            }
        }

        for (var pattern : MAXWELL_PATTERNS) {
            if (pattern.matcher(renderer).find()) {
                return GPUArchitecture.MAXWELL;
            }
        }

        for (var pattern : KEPLER_PATTERNS) {
            if (pattern.matcher(renderer).find()) {
                return GPUArchitecture.KEPLER;
            }
        }

        if (renderer.toLowerCase().contains("nvidia") &&
            !renderer.toLowerCase().contains("geforce") &&
            !renderer.toLowerCase().contains("quadro") &&
            !renderer.toLowerCase().contains("tesla")) {
            return GPUArchitecture.UNSUPPORTED;
        }

        LOGGER.warn("Could not identify GPU architecture from: {}. Defaulting to PASCAL (safe fallback).", renderer);
        return GPUArchitecture.PASCAL;
    }
}
