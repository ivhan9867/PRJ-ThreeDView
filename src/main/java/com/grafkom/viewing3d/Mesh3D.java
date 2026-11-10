package com.grafkom.viewing3d;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Representasi Geometri 3D Berbasis Modern OpenGL (VAO & VBO).
 * Mengimplementasikan:
 * - Vektor Normal Vertex (Smooth Shading dengan Vertex Normal Averaging)
 * - Vektor Normal Facet (Flat Shading per-face normal)
 * - Koordinat Tekstur UV
 * - Model Geometri: UV Sphere, Rumah 3D (House 3D), Torus, Cube, dan Ground Plane.
 */
public class Mesh3D {

    private final int vaoID;
    private final int vboID;
    private final int vertexCount;

    public Mesh3D(float[] vertexData) {
        this.vertexCount = vertexData.length / 8; // pos(3) + normal(3) + uv(2) = 8 floats per vertex

        vaoID = glGenVertexArrays();
        vboID = glGenBuffers();

        glBindVertexArray(vaoID);

        FloatBuffer buffer = BufferUtils.createByteBuffer(vertexData.length * Float.BYTES).asFloatBuffer();
        buffer.put(vertexData);
        buffer.flip();

        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;

        // Attrib 0: Position (vec3)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        // Attrib 1: Normal (vec3)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Attrib 2: TexCoords (vec2)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render() {
        glBindVertexArray(vaoID);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vboID);
        glDeleteVertexArrays(vaoID);
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    /**
     * Membuat Mesh Bola (UV Sphere).
     * @param radius jari-jari bola
     * @param rings jumlah cincin horizontal (latitude)
     * @param sectors jumlah sektor vertikal (longitude)
     * @param smooth true = Smooth Shading (per-vertex normal), false = Flat Shading (per-face normal)
     */
    public static Mesh3D createSphere(float radius, int rings, int sectors, boolean smooth) {
        List<Float> vertices = new ArrayList<>();

        for (int r = 0; r < rings; ++r) {
            float phi0 = (float) Math.PI * (float) r / rings;
            float phi1 = (float) Math.PI * (float) (r + 1) / rings;

            float y0 = radius * (float) Math.cos(phi0);
            float y1 = radius * (float) Math.cos(phi1);
            float r0 = radius * (float) Math.sin(phi0);
            float r1 = radius * (float) Math.sin(phi1);

            for (int s = 0; s < sectors; ++s) {
                float theta0 = 2.0f * (float) Math.PI * (float) s / sectors;
                float theta1 = 2.0f * (float) Math.PI * (float) (s + 1) / sectors;

                Vector3f p00 = new Vector3f(r0 * (float) Math.sin(theta0), y0, r0 * (float) Math.cos(theta0));
                Vector3f p10 = new Vector3f(r1 * (float) Math.sin(theta0), y1, r1 * (float) Math.cos(theta0));
                Vector3f p01 = new Vector3f(r0 * (float) Math.sin(theta1), y0, r0 * (float) Math.cos(theta1));
                Vector3f p11 = new Vector3f(r1 * (float) Math.sin(theta1), y1, r1 * (float) Math.cos(theta1));

                Vector2f uv00 = new Vector2f((float) s / sectors, (float) r / rings);
                Vector2f uv10 = new Vector2f((float) s / sectors, (float) (r + 1) / rings);
                Vector2f uv01 = new Vector2f((float) (s + 1) / sectors, (float) r / rings);
                Vector2f uv11 = new Vector2f((float) (s + 1) / sectors, (float) (r + 1) / rings);

                // Triangle 1: (p00, p10, p01)
                addTriangle(vertices, p00, p10, p01, uv00, uv10, uv01, smooth);
                // Triangle 2: (p01, p10, p11)
                addTriangle(vertices, p01, p10, p11, uv01, uv10, uv11, smooth);
            }
        }

        return toMesh(vertices);
    }

    /**
     * Membuat Mesh Torus (Donat 3D) untuk mengamati lengkungan pencahayaan.
     */
    public static Mesh3D createTorus(float rMajor, float rMinor, int rings, int sides, boolean smooth) {
        List<Float> vertices = new ArrayList<>();

        for (int r = 0; r < rings; ++r) {
            float u0 = 2.0f * (float) Math.PI * (float) r / rings;
            float u1 = 2.0f * (float) Math.PI * (float) (r + 1) / rings;

            for (int s = 0; s < sides; ++s) {
                float v0 = 2.0f * (float) Math.PI * (float) s / sides;
                float v1 = 2.0f * (float) Math.PI * (float) (s + 1) / sides;

                Vector3f p00 = torusPoint(rMajor, rMinor, u0, v0);
                Vector3f p10 = torusPoint(rMajor, rMinor, u1, v0);
                Vector3f p01 = torusPoint(rMajor, rMinor, u0, v1);
                Vector3f p11 = torusPoint(rMajor, rMinor, u1, v1);

                Vector2f uv00 = new Vector2f((float) r / rings * 4.0f, (float) s / sides * 2.0f);
                Vector2f uv10 = new Vector2f((float) (r + 1) / rings * 4.0f, (float) s / sides * 2.0f);
                Vector2f uv01 = new Vector2f((float) r / rings * 4.0f, (float) (s + 1) / sides * 2.0f);
                Vector2f uv11 = new Vector2f((float) (r + 1) / rings * 4.0f, (float) (s + 1) / sides * 2.0f);

                addTriangle(vertices, p00, p10, p01, uv00, uv10, uv01, smooth);
                addTriangle(vertices, p01, p10, p11, uv01, uv10, uv11, smooth);
            }
        }

        return toMesh(vertices);
    }

    private static Vector3f torusPoint(float R, float r, float u, float v) {
        float x = (R + r * (float) Math.cos(v)) * (float) Math.cos(u);
        float y = r * (float) Math.sin(v);
        float z = (R + r * (float) Math.cos(v)) * (float) Math.sin(u);
        return new Vector3f(x, y, z);
    }

    /**
     * Membuat Model Rumah 3D Kompleks (House 3D) dengan dinding, atap pelana segitiga, cerobong, dan pintu.
     */
    public static Mesh3D createHouse(float scale, boolean smooth) {
        List<Float> vertices = new ArrayList<>();
        float s = scale;

        // 1. Dinding Depan Utama (z = s)
        addQuad(vertices,
                new Vector3f(-s, -s,  s), new Vector3f( s, -s,  s),
                new Vector3f( s,  s,  s), new Vector3f(-s,  s,  s),
                new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1), new Vector2f(0, 1), smooth);

        // 2. Dinding Belakang (z = -s)
        addQuad(vertices,
                new Vector3f( s, -s, -s), new Vector3f(-s, -s, -s),
                new Vector3f(-s,  s, -s), new Vector3f( s,  s, -s),
                new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1), new Vector2f(0, 1), smooth);

        // 3. Dinding Kiri (x = -s)
        addQuad(vertices,
                new Vector3f(-s, -s, -s), new Vector3f(-s, -s,  s),
                new Vector3f(-s,  s,  s), new Vector3f(-s,  s, -s),
                new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1), new Vector2f(0, 1), smooth);

        // 4. Dinding Kanan (x = s)
        addQuad(vertices,
                new Vector3f( s, -s,  s), new Vector3f( s, -s, -s),
                new Vector3f( s,  s, -s), new Vector3f( s,  s,  s),
                new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1), new Vector2f(0, 1), smooth);

        // 5. Dasar Lantai Rumah (y = -s)
        addQuad(vertices,
                new Vector3f(-s, -s, -s), new Vector3f( s, -s, -s),
                new Vector3f( s, -s,  s), new Vector3f(-s, -s,  s),
                new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1), new Vector2f(0, 1), smooth);

        // 6. Atap Pelana Segitiga Depan (Gable Front)
        Vector3f roofPeakFront = new Vector3f(0, s * 1.8f, s);
        addTriangle(vertices,
                new Vector3f(-s, s, s), new Vector3f(s, s, s), roofPeakFront,
                new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(0.5f, 1), smooth);

        // 7. Atap Pelana Segitiga Belakang (Gable Back)
        Vector3f roofPeakBack = new Vector3f(0, s * 1.8f, -s);
        addTriangle(vertices,
                new Vector3f(s, s, -s), new Vector3f(-s, s, -s), roofPeakBack,
                new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(0.5f, 1), smooth);

        // 8. Lereng Atap Kiri
        addQuad(vertices,
                new Vector3f(-s, s, -s), roofPeakBack,
                roofPeakFront, new Vector3f(-s, s, s),
                new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1), new Vector2f(0, 1), smooth);

        // 9. Lereng Atap Kanan
        addQuad(vertices,
                roofPeakBack, new Vector3f(s, s, -s),
                new Vector3f(s, s, s), roofPeakFront,
                new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1), new Vector2f(0, 1), smooth);

        // 10. Cerobong Asap (Chimney)
        float cMinX = 0.2f * s, cMaxX = 0.5f * s;
        float cMinZ = -0.5f * s, cMaxZ = -0.2f * s;
        float cBaseY = s * 1.2f, cTopY = s * 2.2f;

        // Sisi cerobong
        addQuad(vertices, new Vector3f(cMinX, cBaseY, cMaxZ), new Vector3f(cMaxX, cBaseY, cMaxZ), new Vector3f(cMaxX, cTopY, cMaxZ), new Vector3f(cMinX, cTopY, cMaxZ), new Vector2f(0,0), new Vector2f(1,0), new Vector2f(1,1), new Vector2f(0,1), smooth);
        addQuad(vertices, new Vector3f(cMaxX, cBaseY, cMinZ), new Vector3f(cMinX, cBaseY, cMinZ), new Vector3f(cMinX, cTopY, cMinZ), new Vector3f(cMaxX, cTopY, cMinZ), new Vector2f(0,0), new Vector2f(1,0), new Vector2f(1,1), new Vector2f(0,1), smooth);
        addQuad(vertices, new Vector3f(cMinX, cBaseY, cMinZ), new Vector3f(cMinX, cBaseY, cMaxZ), new Vector3f(cMinX, cTopY, cMaxZ), new Vector3f(cMinX, cTopY, cMinZ), new Vector2f(0,0), new Vector2f(1,0), new Vector2f(1,1), new Vector2f(0,1), smooth);
        addQuad(vertices, new Vector3f(cMaxX, cBaseY, cMaxZ), new Vector3f(cMaxX, cBaseY, cMinZ), new Vector3f(cMaxX, cTopY, cMinZ), new Vector3f(cMaxX, cTopY, cMaxZ), new Vector2f(0,0), new Vector2f(1,0), new Vector2f(1,1), new Vector2f(0,1), smooth);
        // Tutup atas cerobong
        addQuad(vertices, new Vector3f(cMinX, cTopY, cMinZ), new Vector3f(cMaxX, cTopY, cMinZ), new Vector3f(cMaxX, cTopY, cMaxZ), new Vector3f(cMinX, cTopY, cMaxZ), new Vector2f(0,0), new Vector2f(1,0), new Vector2f(1,1), new Vector2f(0,1), smooth);

        return toMesh(vertices);
    }

    /**
     * Membuat Model Kubus Warna / Tekstur (Cube).
     */
    public static Mesh3D createCube(float size, boolean smooth) {
        List<Float> vertices = new ArrayList<>();
        float h = size / 2.0f;

        // Front (+z)
        addQuad(vertices, new Vector3f(-h, -h,  h), new Vector3f( h, -h,  h), new Vector3f( h,  h,  h), new Vector3f(-h,  h,  h),
                new Vector2f(0,0), new Vector2f(1,0), new Vector2f(1,1), new Vector2f(0,1), smooth);
        // Back (-z)
        addQuad(vertices, new Vector3f( h, -h, -h), new Vector3f(-h, -h, -h), new Vector3f(-h,  h, -h), new Vector3f( h,  h, -h),
                new Vector2f(0,0), new Vector2f(1,0), new Vector2f(1,1), new Vector2f(0,1), smooth);
        // Left (-x)
        addQuad(vertices, new Vector3f(-h, -h, -h), new Vector3f(-h, -h,  h), new Vector3f(-h,  h,  h), new Vector3f(-h,  h, -h),
                new Vector2f(0,0), new Vector2f(1,0), new Vector2f(1,1), new Vector2f(0,1), smooth);
        // Right (+x)
        addQuad(vertices, new Vector3f( h, -h,  h), new Vector3f( h, -h, -h), new Vector3f( h,  h, -h), new Vector3f( h,  h,  h),
                new Vector2f(0,0), new Vector2f(1,0), new Vector2f(1,1), new Vector2f(0,1), smooth);
        // Top (+y)
        addQuad(vertices, new Vector3f(-h,  h,  h), new Vector3f( h,  h,  h), new Vector3f( h,  h, -h), new Vector3f(-h,  h, -h),
                new Vector2f(0,0), new Vector2f(1,0), new Vector2f(1,1), new Vector2f(0,1), smooth);
        // Bottom (-y)
        addQuad(vertices, new Vector3f(-h, -h, -h), new Vector3f( h, -h, -h), new Vector3f( h, -h,  h), new Vector3f(-h, -h,  h),
                new Vector2f(0,0), new Vector2f(1,0), new Vector2f(1,1), new Vector2f(0,1), smooth);

        return toMesh(vertices);
    }

    /**
     * Membuat Bidang Lantai (Ground Plane) untuk menampung proyeksi bayangan nyata (Shadow Mapping).
     */
    public static Mesh3D createGroundPlane(float size, float yLevel, float uvRepeat) {
        List<Float> vertices = new ArrayList<>();
        float h = size / 2.0f;

        Vector3f p0 = new Vector3f(-h, yLevel,  h);
        Vector3f p1 = new Vector3f( h, yLevel,  h);
        Vector3f p2 = new Vector3f( h, yLevel, -h);
        Vector3f p3 = new Vector3f(-h, yLevel, -h);

        Vector2f uv0 = new Vector2f(0, 0);
        Vector2f uv1 = new Vector2f(uvRepeat, 0);
        Vector2f uv2 = new Vector2f(uvRepeat, uvRepeat);
        Vector2f uv3 = new Vector2f(0, uvRepeat);

        addQuad(vertices, p0, p1, p2, p3, uv0, uv1, uv2, uv3, true);

        return toMesh(vertices);
    }

    // =========================================================================
    // GEOMETRY HELPERS
    // =========================================================================

    private static void addQuad(List<Float> vertices,
                                Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3,
                                Vector2f uv0, Vector2f uv1, Vector2f uv2, Vector2f uv3,
                                boolean smooth) {
        addTriangle(vertices, p0, p1, p2, uv0, uv1, uv2, smooth);
        addTriangle(vertices, p0, p2, p3, uv0, uv2, uv3, smooth);
    }

    private static void addTriangle(List<Float> vertices,
                                    Vector3f p0, Vector3f p1, Vector3f p2,
                                    Vector2f uv0, Vector2f uv1, Vector2f uv2,
                                    boolean smooth) {
        // Hitung normal face analitis: (p1 - p0) x (p2 - p0)
        Vector3f edge1 = new Vector3f(p1).sub(p0);
        Vector3f edge2 = new Vector3f(p2).sub(p0);
        Vector3f faceNormal = new Vector3f(edge1).cross(edge2).normalize();

        Vector3f n0, n1, n2;
        if (smooth) {
            // Pada kurva sferis/halus, normal analitis titik adalah radial atau averaging
            n0 = (p0.length() > 0.0001f) ? new Vector3f(p0).normalize() : faceNormal;
            n1 = (p1.length() > 0.0001f) ? new Vector3f(p1).normalize() : faceNormal;
            n2 = (p2.length() > 0.0001f) ? new Vector3f(p2).normalize() : faceNormal;
        } else {
            // Flat shading: seluruh 3 titik memiliki normal konstan (facet normal)
            n0 = faceNormal;
            n1 = faceNormal;
            n2 = faceNormal;
        }

        pushVertex(vertices, p0, n0, uv0);
        pushVertex(vertices, p1, n1, uv1);
        pushVertex(vertices, p2, n2, uv2);
    }

    private static void pushVertex(List<Float> list, Vector3f p, Vector3f n, Vector2f uv) {
        list.add(p.x);
        list.add(p.y);
        list.add(p.z);

        list.add(n.x);
        list.add(n.y);
        list.add(n.z);

        list.add(uv.x);
        list.add(uv.y);
    }

    private static Mesh3D toMesh(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return new Mesh3D(arr);
    }
}
