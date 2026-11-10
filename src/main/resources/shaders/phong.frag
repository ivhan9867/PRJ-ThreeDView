#version 330 core

// ==========================================================
// FILE: src/main/resources/shaders/phong.frag (Fragment Shader)
// Praktikum 1, 4 & 5: Shading Models, Textures & Shadow Mapping
// ==========================================================

out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;

uniform sampler2D textureSampler;
uniform sampler2D shadowMap;

uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadows;

/**
 * Kalkulasi bayangan berbasis Shadow Map (Two-Pass Shadow Mapping).
 * Dilengkapi dengan Dynamic Slope Bias (penanganan Shadow Acne & Peter Panning)
 * serta Percentage-Closer Filtering (PCF 3x3 Kernel) untuk soft shadow.
 */
float calculateShadow(vec4 fragPosLightSpace, vec3 normal, vec3 lightDir) {
    // 1. Perspective divide (Clip Space -> NDC [-1, 1])
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;

    // 2. Transformasi NDC [-1, 1] ke rentang tekstur depth [0, 1]
    projCoords = projCoords * 0.5 + 0.5;

    // 3. Jika fragment berada di luar jangkauan frustum cahaya (far plane), jangan dibayangi
    if (projCoords.z > 1.0) {
        return 0.0;
    }

    // 4. Dynamic Slope-Scaled Depth Bias untuk mengatasi Shadow Acne
    float bias = max(0.005 * (1.0 - dot(normal, lightDir)), 0.0008);

    // 5. PCF (Percentage-Closer Filtering) 3x3 untuk menghasilkan bayangan lembut (soft edge)
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += (projCoords.z - bias > pcfDepth) ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;

    return shadow;
}

void main() {
    // -------------------------------------------------------------
    // 1. Ambient Lighting (Cahaya Latar Konstan)
    // -------------------------------------------------------------
    float ambientStrength = 0.22;
    vec3 ambient = ambientStrength * lightColor;

    // -------------------------------------------------------------
    // 2. Diffuse Lighting (Lambertian Reflectance)
    // -------------------------------------------------------------
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // -------------------------------------------------------------
    // 3. Specular Lighting (Phong vs Blinn-Phong Reflection Model)
    // -------------------------------------------------------------
    float specularStrength = 0.85;
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;

    if (useBlinnPhong) {
        // Blinn-Phong: Halfway Vector H = (L + V) / ||L + V||
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0); // Blinn-Phong Highlight
    } else {
        // Standard Phong: Reflection Vector R = 2(N.L)N - L
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Phong Highlight
    }
    vec3 specular = specularStrength * spec * lightColor;

    // -------------------------------------------------------------
    // 4. Shadow Evaluation (Two-Pass Real-Time Shadow Mapping)
    // -------------------------------------------------------------
    float shadow = useShadows ? calculateShadow(FragPosLightSpace, norm, lightDir) : 0.0;

    // -------------------------------------------------------------
    // 5. Texture & Color Composition
    // -------------------------------------------------------------
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;

    // Bayangan hanya mereduksi komponen diffuse dan specular, ambient tetap menyinari
    vec3 lighting = ambient + (1.0 - shadow) * (diffuse + specular);
    vec3 result = lighting * baseColor;

    FragColor = vec4(result, 1.0);
}
