#version 330 core

// ==========================================================
// FILE: src/main/resources/shaders/phong.vert (Vertex Shader)
// Praktikum 1: Shading Models & Shader Implementation
// ==========================================================

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoords;
out vec4 FragPosLightSpace;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;

void main() {
    // 1. Transformasi posisi vertex ke World Space
    FragPos = vec3(model * vec4(aPos, 1.0));

    // 2. Normal Matrix (invers transpos model) untuk menjaga ortogonalitas normal saat non-uniform scale
    Normal = mat3(transpose(inverse(model))) * aNormal;

    // 3. Passthrough koordinat tekstur UV
    TexCoords = aTexCoords;

    // 4. Transformasi posisi fragment ke Light Space untuk Shadow Mapping
    FragPosLightSpace = lightSpaceMatrix * vec4(FragPos, 1.0);

    // 5. Posisi akhir vertex pada Clip Space
    gl_Position = projection * view * vec4(FragPos, 1.0);
}
