package com.grafkom.viewing3d;

import org.joml.Matrix4f;

/**
 * Enkapsulasi parameter proyeksi perspektif 3D.
 */
public class PerspectiveProjection {

    private float fovDegree = 45.0f;
    private float aspectRatio = 16.0f / 9.0f;
    private float zNear = 0.1f;
    private float zFar = 100.0f;

    public PerspectiveProjection() {
    }

    public PerspectiveProjection(float fovDegree, float aspectRatio, float zNear, float zFar) {
        this.fovDegree = fovDegree;
        this.aspectRatio = aspectRatio;
        this.zNear = zNear;
        this.zFar = zFar;
    }

    /**
     * Menghasilkan Matriks Proyeksi Perspektif (Eye/View Space -> Clip Space).
     */
    public Matrix4f calculateProjectionMatrix() {
        return new Matrix4f().perspective((float) Math.toRadians(fovDegree), aspectRatio, zNear, zFar);
    }

    public float getFovDegree() {
        return fovDegree;
    }

    public void setFovDegree(float fovDegree) {
        this.fovDegree = Math.max(10.0f, Math.min(130.0f, fovDegree));
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public float getZNear() {
        return zNear;
    }

    public void setZNear(float zNear) {
        this.zNear = zNear;
    }

    public float getZFar() {
        return zFar;
    }

    public void setZFar(float zFar) {
        this.zFar = zFar;
    }
}
