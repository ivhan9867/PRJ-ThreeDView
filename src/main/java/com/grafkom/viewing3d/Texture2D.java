package com.grafkom.viewing3d;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/**
 * Praktikum 3 - Java Texture Loader & UV Mapping.
 * Membaca berkas gambar PNG/JPG menggunakan STBImage dan memuatnya ke memori GPU.
 * Mendukung peralihan filter mode (GL_NEAREST vs GL_LINEAR_MIPMAP_LINEAR).
 */
public class Texture2D {

    private final int textureID;
    private boolean bilinearMipmapped = true;

    public Texture2D(String filePathOrResource) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Parameter Texture Wrap
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // Parameter Filter Default (Bilinear Mipmapped)
        setFilterMode(true);

        stbi_set_flip_vertically_on_load(true);

        boolean loaded = false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = null;

            // 1. Coba muat langsung dari file sistem
            File file = new File(filePathOrResource);
            if (file.exists()) {
                data = stbi_load(filePathOrResource, width, height, channels, 4);
            } else {
                // 2. Coba muat dari Classpath Resource
                String resPath = filePathOrResource.startsWith("/") ? filePathOrResource : "/" + filePathOrResource;
                InputStream is = getClass().getResourceAsStream(resPath);
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    ByteBuffer rawBuffer = BufferUtils.createByteBuffer(bytes.length);
                    rawBuffer.put(bytes);
                    rawBuffer.flip();
                    data = stbi_load_from_memory(rawBuffer, width, height, channels, 4);
                }
            }

            if (data != null) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
                glGenerateMipmap(GL_TEXTURE_2D);
                stbi_image_free(data);
                loaded = true;
            }
        } catch (Exception e) {
            System.err.println("Gagal membaca tekstur '" + filePathOrResource + "': " + e.getMessage());
        }

        if (!loaded) {
            System.err.println("Info: Menggunakan tekstur prosedural berpola checkerboard.");
            generateCheckerboardPattern();
        }
    }

    private void generateCheckerboardPattern() {
        int width = 256;
        int height = 256;
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean check = ((x / 32) + (y / 32)) % 2 == 0;
                byte color = (byte) (check ? 235 : 55);
                buffer.put(color);
                buffer.put(color);
                buffer.put(color);
                buffer.put((byte) 255);
            }
        }
        buffer.flip();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    /**
     * Mengatur mode interpolasi filter tekstur:
     * - Nearest Neighbor (GL_NEAREST): Pikselasi tajam
     * - Bilinear Mipmapped (GL_LINEAR_MIPMAP_LINEAR): Transisi halus tanpa aliasing
     */
    public void setFilterMode(boolean bilinearMipmapped) {
        this.bilinearMipmapped = bilinearMipmapped;
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (bilinearMipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void toggleFilterMode() {
        setFilterMode(!bilinearMipmapped);
    }

    public boolean isBilinearMipmapped() {
        return bilinearMipmapped;
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public int getTextureID() {
        return textureID;
    }

    public void cleanup() {
        glDeleteTextures(textureID);
    }
}
