package io.github.user.universalnvidium.render.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL44;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ShaderManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShaderManager");
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("#include\\s+\"([^\"]+)\"");

    private final Map<String, Integer> programs = new HashMap<>();
    private final Map<String, Integer> shaders = new HashMap<>();
    private final Set<String> resolving = new HashSet<>();

    public int getOrCreateProgram(String name, String vertexPath, String fragmentPath) {
        String key = name + "_" + vertexPath + "_" + fragmentPath;
        return programs.computeIfAbsent(key, k -> {
            try {
                return createProgram(vertexPath, fragmentPath);
            } catch (Exception e) {
                LOGGER.error("Failed to create shader program '{}': {}", name, e.getMessage());
                return 0;
            }
        });
    }

    public int getOrCreateComputeProgram(String name, String computePath) {
        return programs.computeIfAbsent(name, k -> {
            try {
                return createComputeProgram(computePath);
            } catch (Exception e) {
                LOGGER.error("Failed to create compute program '{}': {}", name, e.getMessage());
                return 0;
            }
        });
    }

    private int createProgram(String vertexPath, String fragmentPath) {
        int vertShader = compileShader(vertexPath, GL44.GL_VERTEX_SHADER);
        int fragShader = compileShader(fragmentPath, GL44.GL_FRAGMENT_SHADER);

        if (vertShader == 0 || fragShader == 0) return 0;

        int program = GL44.glCreateProgram();
        GL44.glAttachShader(program, vertShader);
        GL44.glAttachShader(program, fragShader);
        GL44.glLinkProgram(program);

        int status = GL44.glGetProgrami(program, GL44.GL_LINK_STATUS);
        if (status != GL44.GL_TRUE) {
            String log = GL44.glGetProgramInfoLog(program);
            LOGGER.error("Program linking failed ({}): {}", vertexPath, log);
            GL44.glDeleteProgram(program);
            return 0;
        }

        GL44.glDetachShader(program, vertShader);
        GL44.glDetachShader(program, fragShader);
        GL44.glDeleteShader(vertShader);
        GL44.glDeleteShader(fragShader);

        return program;
    }

    private int createComputeProgram(String computePath) {
        int compShader = compileShader(computePath, GL44.GL_COMPUTE_SHADER);
        if (compShader == 0) return 0;

        int program = GL44.glCreateProgram();
        GL44.glAttachShader(program, compShader);
        GL44.glLinkProgram(program);

        int status = GL44.glGetProgrami(program, GL44.GL_LINK_STATUS);
        if (status != GL44.GL_TRUE) {
            String log = GL44.glGetProgramInfoLog(program);
            LOGGER.error("Compute program linking failed ({}): {}", computePath, log);
            GL44.glDeleteProgram(program);
            return 0;
        }

        GL44.glDetachShader(program, compShader);
        GL44.glDeleteShader(compShader);

        return program;
    }

    private int compileShader(String path, int type) {
        String shaderKey = path + "_" + type;
        if (shaders.containsKey(shaderKey)) {
            return shaders.get(shaderKey);
        }

        String source = loadShaderSource(path, new HashSet<>());
        if (source == null) return 0;

        int shader = GL44.glCreateShader(type);
        GL44.glShaderSource(shader, source);
        GL44.glCompileShader(shader);

        int status = GL44.glGetShaderi(shader, GL44.GL_COMPILE_STATUS);
        if (status != GL44.GL_TRUE) {
            String log = GL44.glGetShaderInfoLog(shader);
            LOGGER.error("Shader compilation failed ({}): {}", path, log);
            GL44.glDeleteShader(shader);
            return 0;
        }

        shaders.put(shaderKey, shader);
        return shader;
    }

    private String loadShaderSource(String path, Set<String> includeStack) {
        if (includeStack.contains(path)) {
            LOGGER.error("Circular include detected: {}", path);
            return null;
        }
        includeStack.add(path);

        String raw = readFile(path);
        if (raw == null) return null;

        StringBuilder resolved = new StringBuilder();
        String[] lines = raw.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = INCLUDE_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                String includePath = matcher.group(1);
                String included = loadShaderSource(includePath, includeStack);
                if (included != null) {
                    resolved.append(included);
                    if (!included.endsWith("\n")) resolved.append("\n");
                }
            } else {
                resolved.append(line);
                if (i < lines.length - 1) resolved.append("\n");
            }
        }

        includeStack.remove(path);
        return resolved.toString();
    }

    private String readFile(String path) {
        try {
            String fullPath = "assets/universal_nvidium/shaders/" + path;
            InputStream stream = getClass().getClassLoader().getResourceAsStream(fullPath);
            if (stream == null) {
                LOGGER.error("Shader not found: {}", fullPath);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load shader '{}': {}", path, e.getMessage());
            return null;
        }
    }

    public int getUniformLocation(int program, String name) {
        return GL44.glGetUniformLocation(program, name);
    }

    public void setUniformMatrix4f(int program, String name, float[] matrix) {
        int loc = getUniformLocation(program, name);
        if (loc != -1) {
            GL44.glUniformMatrix4fv(loc, false, matrix);
        }
    }

    public void setUniform1i(int program, String name, int value) {
        int loc = getUniformLocation(program, name);
        if (loc != -1) {
            GL44.glUniform1i(loc, value);
        }
    }

    public void setUniform1f(int program, String name, float value) {
        int loc = getUniformLocation(program, name);
        if (loc != -1) {
            GL44.glUniform1f(loc, value);
        }
    }

    @Override
    public void close() {
        for (var entry : programs.entrySet()) {
            GL44.glDeleteProgram(entry.getValue());
        }
        programs.clear();

        for (var entry : shaders.entrySet()) {
            GL44.glDeleteShader(entry.getValue());
        }
        shaders.clear();
    }
}
