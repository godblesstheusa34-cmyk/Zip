#version 300 es
precision highp float;
layout(location=0) in vec2 aPosition;
layout(location=1) in vec2 aUv;
uniform mat4 uMvp;
uniform vec2 uCrest;
uniform vec2 uSigma;
uniform float uAmplitude;
out vec2 vUv;
out vec3 vNormal;
void main() {
    vec2 d = aPosition - uCrest;
    float envelope = exp(-dot(d / uSigma, d / uSigma) * 0.5);
    float phase = d.x * 6.2831853 / (uSigma.x * 1.7);
    float z = uAmplitude * envelope * cos(phase);
    float dzdx = uAmplitude * envelope * (-d.x/(uSigma.x*uSigma.x)*cos(phase) - 6.2831853/(uSigma.x*1.7)*sin(phase));
    float dzdy = -d.y/(uSigma.y*uSigma.y) * z;
    vNormal = normalize(vec3(-dzdx, -dzdy, 1.0));
    vUv = aUv;
    gl_Position = uMvp * vec4(aPosition, z, 1.0);
}
