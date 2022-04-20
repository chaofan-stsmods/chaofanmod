package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

public class SpriteBatchExtension {
    public static void drawProgress(SpriteBatch sb, TextureRegion region, float x, float y, float width, float height, float startDegree, float endDegree) {
        if (!sb.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        }

        startDegree = clipDegree(startDegree + 45) - 45;
        endDegree = clipDegree(endDegree - startDegree) + startDegree;

        if (startDegree == endDegree) {
            sb.draw(region, x, y, width, height);
            return;
        }

        float currentDegree = startDegree;
        float nextDegree = -45;
        boolean rightTop = false;
        do {
            nextDegree += 180;
            rightTop = !rightTop;
            if (currentDegree >= nextDegree) {
                continue;
            }
            if (rightTop) {
                drawProgressRightTop(sb, region, x, y, width, height, currentDegree, Math.min(nextDegree, endDegree));
            } else {
                drawProgressLeftBottom(sb, region, x, y, width, height, currentDegree, Math.min(nextDegree, endDegree));
            }
            currentDegree = nextDegree;
        } while (nextDegree < endDegree);
    }

    private static void drawProgressRightTop(SpriteBatch sb, TextureRegion region, float x, float y, float width, float height, float startDegree, float endDegree) {
        startDegree = clipDegree(startDegree + 45) - 45;
        endDegree = clipDegree(endDegree + 45) - 45;

        float[] vertices = sb.vertices;
        Texture texture = region.texture;
        if (texture != sb.lastTexture) {
            sb.switchTexture(texture);
        } else if (sb.idx == vertices.length) {
            sb.flush();
        }

        float cx = x + width / 2;
        float cy = y + height / 2;
        float cu = (region.u + region.u2) / 2;
        float cv = (region.v + region.v2) / 2;

        float x1;
        float y1;
        float u1;
        float v1;
        float x3;
        float y3;
        float u3;
        float v3;

        if (endDegree > 45) {
            y1 = y + height;
            x1 = cx + (y1 - cy) / MathUtils.cosDeg(90 - endDegree) * MathUtils.sinDeg(90 - endDegree);
            v1 = region.v2;
            u1 = cu + (v1 - cv) / MathUtils.cosDeg(90 - endDegree) * MathUtils.sinDeg(90 - endDegree);
        } else {
            x1 = x + width;
            y1 = cy + (x1 - cx) / MathUtils.cosDeg(endDegree) * MathUtils.sinDeg(endDegree);
            u1 = region.u2;
            v1 = cv + (u1 - cu) / MathUtils.cosDeg(endDegree) * MathUtils.sinDeg(endDegree);
        }

        if (startDegree > 45) {
            y3 = y + height;
            x3 = cx + (y3 - cy) / MathUtils.cosDeg(90 - startDegree) * MathUtils.sinDeg(90 - startDegree);
            v3 = region.v2;
            u3 = cu + (v3 - cv) / MathUtils.cosDeg(90 - startDegree) * MathUtils.sinDeg(90 - startDegree);
        } else {
            x3 = x + width;
            y3 = cy + (x3 - cx) / MathUtils.cosDeg(startDegree) * MathUtils.sinDeg(startDegree);
            u3 = region.u2;
            v3 = cv + (u3 - cu) / MathUtils.cosDeg(startDegree) * MathUtils.sinDeg(startDegree);
        }

        float x2;
        float y2;
        float u2;
        float v2;

        if ((startDegree > 45) == (endDegree > 45)) {
            x2 = x3;
            y2 = y3;
            u2 = u3;
            v2 = v3;
        } else {
            x2 = x + width;
            y2 = y + height;
            u2 = region.u2;
            v2 = region.v2;
        }

        float color = sb.color;
        int idx = sb.idx;
        vertices[idx] = cx;
        vertices[idx + 1] = cy;
        vertices[idx + 2] = color;
        vertices[idx + 3] = cu;
        vertices[idx + 4] = cv;
        vertices[idx + 5] = x1;
        vertices[idx + 6] = y1;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u1;
        vertices[idx + 9] = v1;
        vertices[idx + 10] = x2;
        vertices[idx + 11] = y2;
        vertices[idx + 12] = color;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;
        vertices[idx + 15] = x3;
        vertices[idx + 16] = y3;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u3;
        vertices[idx + 19] = v3;
        sb.idx = idx + 20;
    }

    private static void drawProgressLeftBottom(SpriteBatch sb, TextureRegion region, float x, float y, float width, float height, float startDegree, float endDegree) {
        startDegree = clipDegree(startDegree + 45) - 45;
        endDegree = clipDegree(endDegree);

        float[] vertices = sb.vertices;
        Texture texture = region.texture;
        if (texture != sb.lastTexture) {
            sb.switchTexture(texture);
        } else if (sb.idx == vertices.length) {
            sb.flush();
        }

        float cx = x + width / 2;
        float cy = y + height / 2;
        float cu = (region.u + region.u2) / 2;
        float cv = (region.v + region.v2) / 2;

        float x1;
        float y1;
        float u1;
        float v1;
        float x3;
        float y3;
        float u3;
        float v3;

        if (endDegree > 225) {
            y1 = y;
            x1 = cx + (y1 - cy) / MathUtils.cosDeg(270 - endDegree) * MathUtils.sinDeg(270 - endDegree);
            v1 = region.v;
            u1 = cu + (v1 - cv) / MathUtils.cosDeg(270 - endDegree) * MathUtils.sinDeg(270 - endDegree);
        } else {
            x1 = x;
            y1 = cy + (x1 - cx) / MathUtils.cosDeg(endDegree - 180) * MathUtils.sinDeg(endDegree - 180);
            u1 = region.u;
            v1 = cv + (u1 - cu) / MathUtils.cosDeg(endDegree - 180) * MathUtils.sinDeg(endDegree - 180);
        }

        if (startDegree > 225) {
            y3 = y;
            x3 = cx + (y3 - cy) / MathUtils.cosDeg(270 - startDegree) * MathUtils.sinDeg(270 - startDegree);
            v3 = region.v;
            u3 = cu + (v3 - cv) / MathUtils.cosDeg(270 - startDegree) * MathUtils.sinDeg(270 - startDegree);
        } else {
            x3 = x;
            y3 = cy + (x3 - cx) / MathUtils.cosDeg(startDegree - 180) * MathUtils.sinDeg(startDegree - 180);
            u3 = region.u;
            v3 = cv + (u3 - cu) / MathUtils.cosDeg(startDegree - 180) * MathUtils.sinDeg(startDegree - 180);
        }

        float x0;
        float y0;
        float u0;
        float v0;

        if ((startDegree > 225) == (endDegree > 225)) {
            x0 = x3;
            y0 = y3;
            u0 = u3;
            v0 = v3;
        } else {
            x0 = x;
            y0 = y;
            u0 = region.u;
            v0 = region.v;
        }

        float color = sb.color;
        int idx = sb.idx;
        vertices[idx] = x0;
        vertices[idx + 1] = y0;
        vertices[idx + 2] = color;
        vertices[idx + 3] = u0;
        vertices[idx + 4] = v0;
        vertices[idx + 5] = x1;
        vertices[idx + 6] = y1;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u1;
        vertices[idx + 9] = v1;
        vertices[idx + 10] = cx;
        vertices[idx + 11] = cy;
        vertices[idx + 12] = color;
        vertices[idx + 13] = cu;
        vertices[idx + 14] = cv;
        vertices[idx + 15] = x3;
        vertices[idx + 16] = y3;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u3;
        vertices[idx + 19] = v3;
        sb.idx = idx + 20;
    }

    private static float clipDegree(float degree) {
        if (degree < 0) {
            degree += 360 * (1 + Math.floor(-degree / 360));
        } else if (degree >= 360) {
            degree = degree % 360;
        }
        return degree;
    }
}
