package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/**
 * GLSL Shader Program Loader & Uniform Manager (OpenGL 3.3 Core).
 */
public class Shader {

    private final int programID;
    private final Map<String, Integer> uniformLocationCache = new HashMap<>();

    public Shader(String vertResourcePath, String fragResourcePath) {
        String vertSource = loadResource(vertResourcePath);
        String fragSource = loadResource(fragResourcePath);

        int vertShader = compileShader(GL_VERTEX_SHADER, vertSource, vertResourcePath);
        int fragShader = compileShader(GL_FRAGMENT_SHADER, fragSource, fragResourcePath);

        programID = glCreateProgram();
        glAttachShader(programID, vertShader);
        glAttachShader(programID, fragShader);
        glLinkProgram(programID);

        if (glGetProgrami(programID, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programID);
            throw new RuntimeException("Shader Linker Gagal:\n" + log);
        }

        glDetachShader(programID, vertShader);
        glDetachShader(programID, fragShader);
        glDeleteShader(vertShader);
        glDeleteShader(fragShader);
    }

    private int compileShader(int type, String source, String name) {
        int shaderID = glCreateShader(type);
        glShaderSource(shaderID, source);
        glCompileShader(shaderID);

        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shaderID);
            String typeStr = (type == GL_VERTEX_SHADER) ? "Vertex" : "Fragment";
            throw new RuntimeException("Kompilasi " + typeStr + " Shader Gagal (" + name + "):\n" + log);
        }
        return shaderID;
    }

    private String loadResource(String path) {
        String resPath = path.startsWith("/") ? path : "/" + path;
        try (InputStream is = getClass().getResourceAsStream(resPath)) {
            if (is == null) {
                throw new IllegalArgumentException("Berkas resource shader tidak ditemukan: " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal memuat shader resource: " + path, e);
        }
    }

    public void bind() {
        glUseProgram(programID);
    }

    public void unbind() {
        glUseProgram(0);
    }

    private int getUniformLocation(String name) {
        if (uniformLocationCache.containsKey(name)) {
            return uniformLocationCache.get(name);
        }
        int loc = glGetUniformLocation(programID, name);
        uniformLocationCache.put(name, loc);
        return loc;
    }

    public void setMat4(String name, Matrix4f mat) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer fb = stack.mallocFloat(16);
                mat.get(fb);
                glUniformMatrix4fv(loc, false, fb);
            }
        }
    }

    public void setVec3(String name, Vector3f vec) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            glUniform3f(loc, vec.x, vec.y, vec.z);
        }
    }

    public void setVec3(String name, float x, float y, float z) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            glUniform3f(loc, x, y, z);
        }
    }

    public void setFloat(String name, float value) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            glUniform1f(loc, value);
        }
    }

    public void setInt(String name, int value) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            glUniform1i(loc, value);
        }
    }

    public void setBool(String name, boolean value) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            glUniform1i(loc, value ? 1 : 0);
        }
    }

    public void cleanup() {
        glDeleteProgram(programID);
    }
}
