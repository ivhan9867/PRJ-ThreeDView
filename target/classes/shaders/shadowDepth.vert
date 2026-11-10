#version 330 core

// ==========================================================
// FILE: src/main/resources/shaders/shadowDepth.vert
// Pass 1: Render scene from directional light viewpoint
// ==========================================================

layout (location = 0) in vec3 aPos;

uniform mat4 lightSpaceMatrix;
uniform mat4 model;

void main() {
    gl_Position = lightSpaceMatrix * model * vec4(aPos, 1.0);
}
