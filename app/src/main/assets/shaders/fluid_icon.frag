#version 300 es
precision mediump float;
uniform sampler2D uTexture;
in vec2 vUv;
in vec3 vNormal;
out vec4 fragColor;
void main() {
    vec4 texel = texture(uTexture, vUv);
    vec3 light = normalize(vec3(-0.35, -0.65, 0.90));
    vec3 halfVector = normalize(light + vec3(0.0, 0.0, 1.0));
    float diffuse = max(dot(vNormal, light), 0.0);
    float specular = pow(max(dot(vNormal, halfVector), 0.0), 32.0);
    fragColor = vec4(texel.rgb * (0.72 + 0.28 * diffuse) + vec3(specular * 0.18), texel.a);
}
