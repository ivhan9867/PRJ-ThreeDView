package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.*;

/**
 * Praktikum 4 - Shadow Mapping Depth FBO Manager.
 * Merancang Framebuffer Object (FBO) untuk menampung Shadow Map 2D Depth Texture (Pass 1).
 */
public class ShadowMapFBO {

    private final int fboID;
    private final int depthTextureID;
    private final int shadowWidth;
    private final int shadowHeight;

    public ShadowMapFBO() {
        this(2048, 2048);
    }

    public ShadowMapFBO(int width, int height) {
        this.shadowWidth = width;
        this.shadowHeight = height;

        fboID = glGenFramebuffers();
        depthTextureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTextureID);

        // Buat tekstur depth buffer 24-bit float/fixed untuk akurasi tinggi
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, shadowWidth, shadowHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (java.nio.ByteBuffer) null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);

        // Nilai warna batas luar 1.0 (kedalaman maksimum) agar objek di luar frustum cahaya tidak dibayangi
        float[] borderColor = {1.0f, 1.0f, 1.0f, 1.0f};
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureID, 0);

        // Tidak memerlukan color buffer untuk depth-only pass
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Shadow FBO Inisialisasi Gagal! Kode Status: " + status);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Mengaktifkan Depth FBO untuk penulisan kedalaman dari sudut pandang cahaya (Pass 1).
     */
    public void bindForWriting() {
        glViewport(0, 0, shadowWidth, shadowHeight);
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Melepas binding FBO kembali ke Default Screen Framebuffer (Pass 2).
     */
    public void unbind(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
    }

    public int getDepthTextureID() {
        return depthTextureID;
    }

    public void bindDepthTexture(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
    }

    public int getShadowWidth() {
        return shadowWidth;
    }

    public int getShadowHeight() {
        return shadowHeight;
    }

    public void cleanup() {
        glDeleteFramebuffers(fboID);
        glDeleteTextures(depthTextureID);
    }
}
