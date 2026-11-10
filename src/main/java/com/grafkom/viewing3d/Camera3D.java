package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Kamera 3D Orbital Terenkapsulasi.
 * Menghitung matriks View (LookAt) secara analitis dari vektor basis (u, v, n).
 * Navigasi orbital (turntable) berpusat mengelilingi target pandang.
 */
public class Camera3D {

    private static final float MIN_RADIUS = 1.5f;
    private static final float MAX_RADIUS = 50.0f;
    private static final float MAX_ELEVATION = 89.0f; // Hindari gimbal lock dengan sumbu Up

    private final Vector3f eye;
    private final Vector3f target;
    private final Vector3f up;

    private float radius = 7.0f;
    private float azimuthDeg = 35.0f;
    private float elevationDeg = 25.0f;

    public Camera3D(Vector3f eye, Vector3f target, Vector3f up) {
        this.eye = new Vector3f(eye);
        this.target = new Vector3f(target);
        this.up = new Vector3f(up);
        updateOrbitalPosition(this.radius, this.azimuthDeg, this.elevationDeg);
    }

    /**
     * Menghitung View Matrix (World Space -> View Space) secara manual.
     */
    public Matrix4f calculateLookAtMatrix() {
        Vector3f n = new Vector3f();
        eye.sub(target, n).normalize();          // View Plane Normal

        Vector3f u = new Vector3f();
        up.cross(n, u).normalize();              // Right Vector

        Vector3f v = new Vector3f();
        n.cross(u, v);                           // True Up Vector

        Matrix4f viewMatrix = new Matrix4f();
        viewMatrix.m00(u.x).m10(u.y).m20(u.z).m30(-u.dot(eye));
        viewMatrix.m01(v.x).m11(v.y).m21(v.z).m31(-v.dot(eye));
        viewMatrix.m02(n.x).m12(n.y).m22(n.z).m32(-n.dot(eye));
        viewMatrix.m03(0.0f).m13(0.0f).m23(0.0f).m33(1.0f);
        return viewMatrix;
    }

    /**
     * Konversi koordinat sferis orbital ke posisi eye kamera relatif terhadap target.
     */
    public void updateOrbitalPosition(float radius, float azimuthDeg, float elevationDeg) {
        this.radius = clamp(radius, MIN_RADIUS, MAX_RADIUS);
        this.azimuthDeg = azimuthDeg % 360.0f;
        this.elevationDeg = clamp(elevationDeg, -MAX_ELEVATION, MAX_ELEVATION);

        float azRad = (float) Math.toRadians(this.azimuthDeg);
        float elRad = (float) Math.toRadians(this.elevationDeg);

        eye.x = target.x + this.radius * (float) (Math.cos(elRad) * Math.sin(azRad));
        eye.y = target.y + this.radius * (float) Math.sin(elRad);
        eye.z = target.z + this.radius * (float) (Math.cos(elRad) * Math.cos(azRad));
    }

    public void orbit(float deltaAzimuthDeg, float deltaElevationDeg) {
        updateOrbitalPosition(radius, azimuthDeg + deltaAzimuthDeg, elevationDeg + deltaElevationDeg);
    }

    public void zoom(float deltaRadius) {
        updateOrbitalPosition(radius + deltaRadius, azimuthDeg, elevationDeg);
    }

    public void reset() {
        updateOrbitalPosition(7.0f, 35.0f, 25.0f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public Vector3f getEye() {
        return new Vector3f(eye);
    }

    public Vector3f getTarget() {
        return new Vector3f(target);
    }

    public Vector3f getUp() {
        return new Vector3f(up);
    }

    public float getRadius() {
        return radius;
    }

    public float getAzimuthDeg() {
        return azimuthDeg;
    }

    public float getElevationDeg() {
        return elevationDeg;
    }
}
