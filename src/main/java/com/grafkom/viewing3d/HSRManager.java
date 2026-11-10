package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/**
 * Praktikum 2 - Hidden Surface Removal (HSR) Setup.
 * Mengonfigurasi Z-Buffer Depth Test dan Back-Face Culling pada LWJGL 3.
 */
public class HSRManager {

    private static boolean depthTestEnabled = true;
    private static boolean backFaceCullingEnabled = true;

    /**
     * Konfigurasi Z-Buffer Depth Test.
     * Mengeliminasi fragment yang tertutup oleh geometri yang lebih dekat ke kamera.
     */
    public static void setupDepthTest(boolean enable) {
        depthTestEnabled = enable;
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS); // Pixel lebih dekat lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    /**
     * Konfigurasi Back-Face Culling.
     * Mengabaikan poligon yang menghadap ke belakang (arah berlawanan arah pandang).
     */
    public static void setupBackFaceCulling(boolean enable) {
        backFaceCullingEnabled = enable;
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);  // Buang polygon belakang
            glFrontFace(GL_CCW);  // Counter-Clockwise adalah tampak depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }

    public static boolean isDepthTestEnabled() {
        return depthTestEnabled;
    }

    public static boolean isBackFaceCullingEnabled() {
        return backFaceCullingEnabled;
    }

    public static void toggleDepthTest() {
        setupDepthTest(!depthTestEnabled);
    }

    public static void toggleBackFaceCulling() {
        setupBackFaceCulling(!backFaceCullingEnabled);
    }
}
