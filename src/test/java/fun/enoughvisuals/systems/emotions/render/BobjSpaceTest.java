package fun.enoughvisuals.systems.emotions.render;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BobjSpaceTest {
    @Test
    void deltaConversionMatchesTheVertexPathAroundTheMinecraftFloor() {
        Vector4f restBobj = new Vector4f(0.375f, 1.375f, 0.125f, 1.0f);
        Matrix4f bobjDelta = new Matrix4f()
                .translate(0.0f, 0.75f, 0.0f)
                .rotateZ((float) (Math.PI * 0.5))
                .translate(0.0f, -0.75f, 0.0f);

        Vector4f animatedBobj = bobjDelta.transform(new Vector4f(restBobj));
        Vector4f expectedMinecraft = mapPoint(animatedBobj);
        Vector4f actualMinecraft = BobjSpace.deltaToMinecraft(bobjDelta)
                .transform(mapPoint(restBobj));

        assertEquals(expectedMinecraft.x, actualMinecraft.x, 1.0e-5f);
        assertEquals(expectedMinecraft.y, actualMinecraft.y, 1.0e-5f);
        assertEquals(expectedMinecraft.z, actualMinecraft.z, 1.0e-5f);
        assertEquals(1.0f, actualMinecraft.w, 1.0e-5f);
    }

    private static Vector4f mapPoint(Vector4f point) {
        return new Vector4f(
                point.x,
                BobjSpace.MINECRAFT_MODEL_FLOOR_Y - point.y,
                -point.z,
                point.w);
    }
}
