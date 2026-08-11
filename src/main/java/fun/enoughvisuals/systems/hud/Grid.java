package fun.enoughvisuals.systems.hud;

import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Magnetic HUD guides ported from expensive-latest's SnapManager. */
public final class Grid {
    private static final float SNAP_THRESHOLD = 5.5f;
    private static final float GUIDE_WIDTH = 1f;
    private static final int GUIDE_COLOR = 0xFFFFFFFF;

    private static final List<Guide> guides = new ArrayList<>();
    private static float guideAlpha;
    private static boolean snapping;
    private static boolean snappedX;
    private static boolean snappedY;

    private Grid() {
    }

    public static float[] liveSnapAt(HudElement element, float x, float y) {
        guides.clear();
        float screenW = HudElement.hudScreenWidth();
        float screenH = HudElement.hudScreenHeight();
        float width = element.getWidth();
        float height = element.getHeight();

        float visualY = y - HudBottomInset.offsetFor(y, height, screenH);
        AxisSnap sx = element.isHorizontalDragLocked()
                ? AxisSnap.none(x)
                : snapAxis(element, x, visualY, width, height, screenW, screenH, true);
        AxisSnap visualSy = snapAxis(element, visualY, sx.value, height, width,
                screenH, screenW, false);
        float layoutY = visualSy.snapped
                ? HudBottomInset.layoutYForVisual(visualSy.value, height, screenH)
                : y;
        snappedX = sx.snapped;
        snappedY = visualSy.snapped;
        snapping = sx.snapped || visualSy.snapped;
        return new float[]{
                clamp(sx.value, 0f, Math.max(0f, screenW - width)),
                clamp(layoutY, 0f, Math.max(0f, screenH - height))
        };
    }

    private static AxisSnap snapAxis(HudElement self, float position, float crossPosition,
                                     float size, float crossSize, float screenSize,
                                     float crossScreenSize, boolean horizontal) {
        float start = position;
        float end = position + size;
        float center = position + size * 0.5f;
        float screenCenter = screenSize * 0.5f;

        if (Math.abs(center - screenCenter) < SNAP_THRESHOLD) {
            addGuide(screenCenter, horizontal, 0f, crossScreenSize);
            return new AxisSnap(screenCenter - size * 0.5f, true);
        }

        for (HudElement other : DragAndDrop.getInstance().getActiveElements()) {
            if (other == self || other.isRemoving()) continue;
            float otherPos = horizontal ? other.getX() : other.getVisualY();
            float otherSize = horizontal ? other.getWidth() : other.getHeight();
            float otherCross = horizontal ? other.getVisualY() : other.getX();
            float otherCrossSize = horizontal ? other.getHeight() : other.getWidth();
            float otherEnd = otherPos + otherSize;
            float otherCenter = otherPos + otherSize * 0.5f;
            float guideStart = Math.min(crossPosition, otherCross) - 10f;
            float guideEnd = Math.max(crossPosition + crossSize,
                    otherCross + otherCrossSize) + 10f;

            if (Math.abs(start - otherPos) < SNAP_THRESHOLD) {
                addGuide(otherPos, horizontal, guideStart, guideEnd);
                return new AxisSnap(otherPos, true);
            }
            if (Math.abs(end - otherEnd) < SNAP_THRESHOLD) {
                addGuide(otherEnd, horizontal, guideStart, guideEnd);
                return new AxisSnap(otherEnd - size, true);
            }
            if (Math.abs(center - otherCenter) < SNAP_THRESHOLD) {
                addGuide(otherCenter, horizontal, guideStart, guideEnd);
                return new AxisSnap(otherCenter - size * 0.5f, true);
            }
            if (Math.abs(start - otherEnd) < SNAP_THRESHOLD) {
                addGuide(otherEnd, horizontal, guideStart, guideEnd);
                return new AxisSnap(otherEnd, true);
            }
            if (Math.abs(end - otherPos) < SNAP_THRESHOLD) {
                addGuide(otherPos, horizontal, guideStart, guideEnd);
                return new AxisSnap(otherPos - size, true);
            }
        }
        return AxisSnap.none(position);
    }

    private static void addGuide(float position, boolean vertical, float start, float end) {
        guides.add(new Guide(position, vertical, start, end));
    }

    public static void render(DrawContext ctx, float delta, boolean dragging) {
        guideAlpha = RenderUtil.animate(guideAlpha,
                dragging && snapping && !guides.isEmpty() ? 1f : 0f,
                0.14f, RenderUtil.getFrameDelta());
        if (guideAlpha <= 0.01f) {
            if (!dragging) guides.clear();
            return;
        }

        float hudScale = HudElement.hudScale();
        ctx.getMatrices().push();
        ctx.getMatrices().scale(hudScale, hudScale, 1f);
        int color = RenderUtil.withAlpha(GUIDE_COLOR,
                RenderUtil.easeOutQuad(guideAlpha) * 0.82f);
        for (Guide guide : guides) {
            if (guide.vertical) {
                RenderUtil.drawRect(ctx, guide.position - GUIDE_WIDTH * 0.5f,
                        guide.start, GUIDE_WIDTH, guide.end - guide.start, color);
            } else {
                RenderUtil.drawRect(ctx, guide.start,
                        guide.position - GUIDE_WIDTH * 0.5f,
                        guide.end - guide.start, GUIDE_WIDTH, color);
            }
        }
        ctx.getMatrices().pop();
    }

    public static void clear() {
        snapping = false;
        snappedX = false;
        snappedY = false;
    }

    public static boolean isSnappedX() {
        return snappedX;
    }

    public static boolean isSnappedY() {
        return snappedY;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record AxisSnap(float value, boolean snapped) {
        private static AxisSnap none(float value) {
            return new AxisSnap(value, false);
        }
    }

    private record Guide(float position, boolean vertical, float start, float end) {
    }
}
