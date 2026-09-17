#version 300 es
precision highp float;
layout(location=0) in vec2 aPosition;
uniform mat4 uMvp;
uniform vec2 uOffset;
uniform float uScale;
void main() {
    gl_Position = uMvp * vec4(aPosition * uScale + uOffset, 0.0, 1.0);
}
