precision highp float;

uniform vec3                iResolution;
uniform float               iGlobalTime;
uniform sampler2D           iChannel0;
varying vec2                texCoord;

//uniform vec2 scale;
//uniform float alpha;
//uniform float radius2;
//uniform float factor;

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    float m_pi_2 = 1.570963;
    float scale = 1.0;
    float pi = 3.14159265;
    float alpha = scale * 2.0 + 0.75;
    float bound2 = 0.25 * scale;
    float bound = sqrt(bound2);
    float radius = 1.15 * bound;
    float radius2 = radius * radius;
    float max_radian = 0.5 * pi - atan(alpha / bound * sqrt(radius2 - bound2));
    float factor = bound / max_radian;

    vec2 coord = texCoord - vec2(0.5, 0.5);
    float dist = length(coord * scale);
    float radian = m_pi_2 - atan(alpha * sqrt(radius2 - dist * dist), dist);
    float scalar = radian * factor / dist;
    vec2 new_coord = coord * scalar + vec2(0.5, 0.5);
    fragColor = texture2D(iChannel0, new_coord);

}

void main() {
    mainImage(gl_FragColor, texCoord);
}