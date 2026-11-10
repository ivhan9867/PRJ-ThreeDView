package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite - Modern OpenGL 3.3 Core & Java OOP.
 * Mengintegrasikan seluruh 5 Indikator Visual Realism:
 * 1. Shading Models: Blinn-Phong vs Phong Reflection Model (Key 'B')
 * 2. Flat Shading (Facet Normal) vs Smooth Shading (Vertex Normal Averaging) (Key 'F')
 * 3. Hidden Surface Removal: Z-Buffer Depth Test (Key 'Z') & Back-Face Culling (Key 'C')
 * 4. Texture Mapping: UV Mapping (Key 'T') & Filtering Nearest vs Bilinear Mipmapped (Key 'M')
 * 5. Real-Time Shadows: Two-Pass Shadow Mapping dengan Depth FBO & PCF Filter (Key 'X')
 *
 * Navigasi & Interaksi:
 * - Left Click + Drag : Orbit kamera mengelilingi model
 * - Scroll Mouse      : Zoom in / zoom out
 * - Key 1 - 4         : Ganti Model 3D (1: Sphere, 2: House 3D, 3: Torus, 4: Cube)
 * - Key L             : Animasi rotasi sumber cahaya (orbiting dynamic light)
 * - Key R             : Reset kamera & parameter ke default
 * - Up / Down Arrow   : Sesuaikan FOV kamera
 * - ESC               : Keluar aplikasi
 */
public class ThreeDViewing {

    private long window;
    private int windowWidth = 1280;
    private int windowHeight = 720;

    // Kamera & Proyeksi
    private Camera3D camera;
    private PerspectiveProjection perspective;

    // Shaders & FBO
    private Shader phongShader;
    private Shader shadowShader;
    private ShadowMapFBO shadowFBO;

    // Tekstur
    private Texture2D brickTexture;
    private Texture2D crateTexture;

    // Model Geometri (Smooth vs Flat variants)
    private Mesh3D sphereSmooth;
    private Mesh3D sphereFlat;
    private Mesh3D houseSmooth;
    private Mesh3D houseFlat;
    private Mesh3D torusSmooth;
    private Mesh3D torusFlat;
    private Mesh3D cubeSmooth;
    private Mesh3D cubeFlat;
    private Mesh3D groundPlane;

    // Status Interaktif (5 Indikator CPMK)
    private boolean useBlinnPhong = true;       // Indikator 1: Phong vs Blinn-Phong
    private boolean smoothShading = true;       // Indikator 2: Smooth vs Flat Shading
    private boolean depthTest = true;           // Indikator 3: Z-Buffer Depth Test
    private boolean backFaceCull = true;        // Indikator 3: Back-Face Culling
    private boolean useTexture = true;          // Indikator 4: Texture Mapping ON/OFF
    private boolean filterBilinear = true;      // Indikator 4: Filtering Nearest vs Bilinear
    private boolean useShadows = true;          // Indikator 5: Shadow Mapping 2-Pass

    private int activeModelIndex = 0;           // 0: Sphere, 1: House 3D, 2: Torus, 3: Cube
    private boolean animateLight = true;        // Animasi rotasi sumber cahaya
    private float lightAngle = 0.8f;

    // Kontrol Mouse
    private boolean dragging = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    // Pengukuran FPS
    private int frameCount = 0;
    private double lastFpsTime = 0;
    private int currentFps = 60;

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW!");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(windowWidth, windowHeight, "3D Visual Realism Suite - Loading...", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Gagal membuat window GLFW!");
        }

        setupCallbacks();

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable V-Sync
        GL.createCapabilities();

        // Inisialisasi Kamera & Proyeksi
        camera = new Camera3D(new Vector3f(0, 3, 7), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        perspective = new PerspectiveProjection(45.0f, (float) windowWidth / windowHeight, 0.1f, 100.0f);

        // Inisialisasi Shaders
        phongShader = new Shader("/shaders/phong.vert", "/shaders/phong.frag");
        shadowShader = new Shader("/shaders/shadowDepth.vert", "/shaders/shadowDepth.frag");

        // Inisialisasi Shadow Map FBO (2048 x 2048 depth resolution)
        shadowFBO = new ShadowMapFBO(2048, 2048);

        // Inisialisasi Tekstur
        brickTexture = new Texture2D("/textures/stone_brick.png");
        crateTexture = new Texture2D("/textures/wood_crate.png");

        // Inisialisasi Model Geometri
        sphereSmooth = Mesh3D.createSphere(1.4f, 32, 32, true);
        sphereFlat   = Mesh3D.createSphere(1.4f, 18, 18, false);

        houseSmooth  = Mesh3D.createHouse(1.0f, true);
        houseFlat    = Mesh3D.createHouse(1.0f, false);

        torusSmooth  = Mesh3D.createTorus(1.2f, 0.45f, 32, 24, true);
        torusFlat    = Mesh3D.createTorus(1.2f, 0.45f, 18, 12, false);

        cubeSmooth   = Mesh3D.createCube(1.8f, true);
        cubeFlat     = Mesh3D.createCube(1.8f, false);

        // Ground Plane di ketinggian y = -1.6 untuk menampung proyeksi bayangan
        groundPlane  = Mesh3D.createGroundPlane(16.0f, -1.6f, 4.0f);

        // Inisialisasi HSR
        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(backFaceCull);

        updateWindowTitle();
        glfwShowWindow(window);
        lastFpsTime = glfwGetTime();

        printConsoleInstructions();
    }

    private void setupCallbacks() {
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            if (w > 0 && h > 0) {
                windowWidth = w;
                windowHeight = h;
                perspective.setAspectRatio((float) w / h);
            }
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS && action != GLFW_REPEAT) return;

            if (action == GLFW_PRESS) {
                switch (key) {
                    case GLFW_KEY_ESCAPE:
                        glfwSetWindowShouldClose(win, true);
                        break;

                    // Indikator 1: Shading Model Phong vs Blinn-Phong
                    case GLFW_KEY_B:
                        useBlinnPhong = !useBlinnPhong;
                        System.out.println("[Indikator 1] Shading Model: " + (useBlinnPhong ? "Blinn-Phong" : "Phong"));
                        break;

                    // Indikator 2: Flat vs Smooth Shading
                    case GLFW_KEY_F:
                        smoothShading = !smoothShading;
                        System.out.println("[Indikator 2] Normal Interpolation: " + (smoothShading ? "Smooth (Per-Vertex Averaging)" : "Flat (Per-Face Normal)"));
                        break;

                    // Indikator 3: Hidden Surface Removal - Z-Buffer
                    case GLFW_KEY_Z:
                        depthTest = !depthTest;
                        HSRManager.setupDepthTest(depthTest);
                        System.out.println("[Indikator 3] Z-Buffer Depth Test: " + (depthTest ? "ENABLED" : "DISABLED"));
                        break;

                    // Indikator 3: Hidden Surface Removal - Back-Face Culling
                    case GLFW_KEY_C:
                        backFaceCull = !backFaceCull;
                        HSRManager.setupBackFaceCulling(backFaceCull);
                        System.out.println("[Indikator 3] Back-Face Culling: " + (backFaceCull ? "ENABLED" : "DISABLED"));
                        break;

                    // Indikator 4: Texture Mapping ON / OFF
                    case GLFW_KEY_T:
                        useTexture = !useTexture;
                        System.out.println("[Indikator 4] Texture Mapping: " + (useTexture ? "ENABLED" : "DISABLED"));
                        break;

                    // Indikator 4: Texture Filtering Nearest vs Bilinear Mipmapped
                    case GLFW_KEY_M:
                        filterBilinear = !filterBilinear;
                        brickTexture.setFilterMode(filterBilinear);
                        crateTexture.setFilterMode(filterBilinear);
                        System.out.println("[Indikator 4] Texture Filter: " + (filterBilinear ? "Bilinear Mipmapped" : "Nearest Neighbor"));
                        break;

                    // Indikator 5: Real-Time Shadow Mapping ON / OFF
                    case GLFW_KEY_X:
                        useShadows = !useShadows;
                        System.out.println("[Indikator 5] Shadow Mapping: " + (useShadows ? "ENABLED" : "DISABLED"));
                        break;

                    // Switch 3D Models
                    case GLFW_KEY_1:
                        activeModelIndex = 0;
                        System.out.println("[Model 3D] Sphere Aktif");
                        break;
                    case GLFW_KEY_2:
                        activeModelIndex = 1;
                        System.out.println("[Model 3D] Rumah 3D (House 3D) Aktif");
                        break;
                    case GLFW_KEY_3:
                        activeModelIndex = 2;
                        System.out.println("[Model 3D] Torus Aktif");
                        break;
                    case GLFW_KEY_4:
                        activeModelIndex = 3;
                        System.out.println("[Model 3D] Cube Aktif");
                        break;

                    // Toggle Animasi Cahaya
                    case GLFW_KEY_L:
                        animateLight = !animateLight;
                        System.out.println("[Lighting] Animasi Rotasi Sumber Cahaya: " + (animateLight ? "ON" : "OFF"));
                        break;

                    // Reset Kamera & Pengaturan
                    case GLFW_KEY_R:
                        camera.reset();
                        perspective.setFovDegree(45.0f);
                        useBlinnPhong = true;
                        smoothShading = true;
                        depthTest = true;
                        backFaceCull = true;
                        useTexture = true;
                        filterBilinear = true;
                        useShadows = true;
                        HSRManager.setupDepthTest(true);
                        HSRManager.setupBackFaceCulling(true);
                        brickTexture.setFilterMode(true);
                        crateTexture.setFilterMode(true);
                        System.out.println("[System] Pengaturan dan Posisi Kamera Direset.");
                        break;
                }
            }

            // Pengaturan FOV Kamera (Arrow Up / Down)
            if (key == GLFW_KEY_UP) {
                perspective.setFovDegree(perspective.getFovDegree() - 1.5f);
            } else if (key == GLFW_KEY_DOWN) {
                perspective.setFovDegree(perspective.getFovDegree() + 1.5f);
            }

            updateWindowTitle();
        });

        // Kontrol Mouse Drag untuk Orbit Kamera
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
            }
        });

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (dragging) {
                float dx = (float) (x - lastMouseX);
                float dy = (float) (y - lastMouseY);
                camera.orbit(-dx * 0.35f, dy * 0.35f);
            }
            lastMouseX = x;
            lastMouseY = y;
        });

        // Scroll Mouse untuk Zoom
        glfwSetScrollCallback(window, (win, dx, dy) -> {
            camera.zoom((float) (-dy * 0.5f));
            updateWindowTitle();
        });
    }

    private void updateWindowTitle() {
        String[] modelNames = {"Sphere", "House 3D", "Torus", "Cube"};
        String title = String.format("3D Visual Realism Suite | [B] Shd:%s | [F] Nrm:%s | [Z] Dep:%s | [C] Cul:%s | [T/M] Tex:%s(%s) | [X] Shw:%s | [1-4] Mdl:%s | FPS:%d",
                useBlinnPhong ? "Blinn-Phong" : "Phong",
                smoothShading ? "Smooth" : "Flat",
                depthTest ? "ON" : "OFF",
                backFaceCull ? "ON" : "OFF",
                useTexture ? "ON" : "OFF",
                filterBilinear ? "BilinMip" : "Near",
                useShadows ? "ON" : "OFF",
                modelNames[activeModelIndex],
                currentFps);
        glfwSetWindowTitle(window, title);
    }

    private void printConsoleInstructions() {
        System.out.println("=================================================================");
        System.out.println("           3D VISUAL REALISM SUITE (MINGGU 11)");
        System.out.println("=================================================================");
        System.out.println("PANDUAN KONTROL KEYBOARD & MOUSE:");
        System.out.println("  [B]     : Toggle Shading Model (Phong <-> Blinn-Phong) [Indikator 1]");
        System.out.println("  [F]     : Toggle Interpolasi Permukaan (Flat <-> Smooth) [Indikator 2]");
        System.out.println("  [Z]     : Toggle HSR Z-Buffer Depth Test [Indikator 3]");
        System.out.println("  [C]     : Toggle HSR Back-Face Culling [Indikator 3]");
        System.out.println("  [T]     : Toggle Texture Mapping ON/OFF [Indikator 4]");
        System.out.println("  [M]     : Toggle Texture Filtering (Nearest <-> Bilinear Mipmap) [Indikator 4]");
        System.out.println("  [X]     : Toggle Two-Pass Real-Time Shadow Mapping [Indikator 5]");
        System.out.println("  [1-4]   : Ganti Objek 3D (1: Sphere, 2: Rumah 3D, 3: Torus, 4: Kubus)");
        System.out.println("  [L]     : Toggle Animasi Rotasi Sumber Cahaya");
        System.out.println("  [R]     : Reset Pengaturan Kamera & Efek ke Default");
        System.out.println("  [Drag]  : Klik Kiri + Geser Mouse untuk Orbiting Kamera");
        System.out.println("  [Scroll]: Zoom In / Zoom Out Kamera");
        System.out.println("  [Panah] : Naik / Turun untuk menyesuaikan FOV Kamera");
        System.out.println("  [ESC]   : Keluar Program");
        System.out.println("=================================================================");
    }

    private void loop() {
        double lastFrameTime = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
            double currentFrameTime = glfwGetTime();
            float deltaTime = (float) (currentFrameTime - lastFrameTime);
            lastFrameTime = currentFrameTime;

            // Hitung FPS
            frameCount++;
            if (currentFrameTime - lastFpsTime >= 1.0) {
                currentFps = frameCount;
                frameCount = 0;
                lastFpsTime = currentFrameTime;
                updateWindowTitle();
            }

            // Update Rotasi Sumber Cahaya
            if (animateLight) {
                lightAngle += deltaTime * 0.75f;
            }
            Vector3f lightPos = new Vector3f(
                    (float) Math.sin(lightAngle) * 6.5f,
                    7.5f,
                    (float) Math.cos(lightAngle) * 6.5f
            );

            render(lightPos);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void render(Vector3f lightPos) {
        int[] fbW = new int[1];
        int[] fbH = new int[1];
        glfwGetFramebufferSize(window, fbW, fbH);
        if (fbW[0] == 0 || fbH[0] == 0) return;

        // Pilih Mesh aktif sesuai mode Flat vs Smooth
        Mesh3D activeMesh = getActiveMesh();
        Texture2D activeTex = (activeModelIndex == 3 || activeModelIndex == 1) ? crateTexture : brickTexture;

        // Transformasi Model Objek 3D
        Matrix4f objectModel = new Matrix4f().identity();
        if (activeModelIndex == 1) { // House 3D
            objectModel.translate(0.0f, -0.6f, 0.0f);
        } else {
            objectModel.translate(0.0f, 0.2f, 0.0f);
        }

        // Transformasi Bidang Lantai (Ground Plane)
        Matrix4f groundModel = new Matrix4f().identity();

        // Matriks Transformasi Light Space (Directional Light Orthographic Projection)
        Matrix4f lightProjection = new Matrix4f().ortho(-9.0f, 9.0f, -9.0f, 9.0f, 1.0f, 25.0f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f));
        Matrix4f lightSpaceMatrix = new Matrix4f(lightProjection).mul(lightView);

        // =====================================================================
        // PASS 1: SHADOW MAPPING DEPTH PASS (Sudut Pandang Sumber Cahaya)
        // =====================================================================
        if (useShadows) {
            shadowFBO.bindForWriting();
            glEnable(GL_DEPTH_TEST);

            // Front-face culling pada depth pass untuk mencegah self-shadow acne & peter panning
            glEnable(GL_CULL_FACE);
            glCullFace(GL_FRONT);

            shadowShader.bind();
            shadowShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);

            // Gambar objek ke dalam depth map
            shadowShader.setMat4("model", objectModel);
            activeMesh.render();

            // Kembalikan culling ke back-face
            glCullFace(GL_BACK);
            shadowFBO.unbind(fbW[0], fbH[0]);
        }

        // =====================================================================
        // PASS 2: MAIN SCENE RENDER PASS (Sudut Pandang Kamera Mata Pengamat)
        // =====================================================================
        glViewport(0, 0, fbW[0], fbH[0]);
        glClearColor(0.10f, 0.11f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Terapkan Pengaturan HSR (Indikator 3)
        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(backFaceCull);

        phongShader.bind();
        phongShader.setMat4("projection", perspective.calculateProjectionMatrix());
        phongShader.setMat4("view", camera.calculateLookAtMatrix());
        phongShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        phongShader.setVec3("viewPos", camera.getEye());
        phongShader.setVec3("lightPos", lightPos);
        phongShader.setVec3("lightColor", new Vector3f(1.0f, 0.98f, 0.92f));

        phongShader.setBool("useBlinnPhong", useBlinnPhong);
        phongShader.setBool("useTexture", useTexture);
        phongShader.setBool("useShadows", useShadows);

        // Bind Shadow Depth Texture ke Texture Unit 1
        shadowFBO.bindDepthTexture(1);
        phongShader.setInt("shadowMap", 1);

        // 1. Render Bidang Lantai (Ground Plane) Penampung Bayangan
        activeTex.bind(0);
        phongShader.setInt("textureSampler", 0);
        phongShader.setMat4("model", groundModel);
        phongShader.setVec3("objectColor", new Vector3f(0.85f, 0.88f, 0.92f));
        groundPlane.render();

        // 2. Render Objek 3D Utama
        phongShader.setMat4("model", objectModel);
        Vector3f objColor = (activeModelIndex == 0) ? new Vector3f(0.95f, 0.55f, 0.20f) :
                            (activeModelIndex == 1) ? new Vector3f(0.90f, 0.80f, 0.70f) :
                            (activeModelIndex == 2) ? new Vector3f(0.25f, 0.80f, 0.95f) :
                                                     new Vector3f(0.85f, 0.60f, 0.30f);
        phongShader.setVec3("objectColor", objColor);
        activeMesh.render();

        phongShader.unbind();
    }

    private Mesh3D getActiveMesh() {
        switch (activeModelIndex) {
            case 0: return smoothShading ? sphereSmooth : sphereFlat;
            case 1: return smoothShading ? houseSmooth : houseFlat;
            case 2: return smoothShading ? torusSmooth : torusFlat;
            case 3: return smoothShading ? cubeSmooth : cubeFlat;
            default: return sphereSmooth;
        }
    }

    private void cleanup() {
        sphereSmooth.cleanup();
        sphereFlat.cleanup();
        houseSmooth.cleanup();
        houseFlat.cleanup();
        torusSmooth.cleanup();
        torusFlat.cleanup();
        cubeSmooth.cleanup();
        cubeFlat.cleanup();
        groundPlane.cleanup();

        brickTexture.cleanup();
        crateTexture.cleanup();

        phongShader.cleanup();
        shadowShader.cleanup();
        shadowFBO.cleanup();

        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}
