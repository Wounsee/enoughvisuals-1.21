package fun.enoughvisuals.systems.hud;

import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class Grid {

                                              
                    
                                              

    private static final float GRID_SIZE       = 6f;
    private static final float SNAP_THRESHOLD  = 6f;
    private static final float EDGE_MARGIN     = 4f;
    private static final float ELEMENT_GAP     = 3f;

             
    private static final int GUIDE_COLOR_CENTER   = 0x6C63FF;
    private static final int GUIDE_COLOR_EDGE     = 0xFF6B6B;
    private static final int GUIDE_COLOR_ELEMENT  = 0x4DFFB3;
    private static final int GUIDE_COLOR_DIST     = 0xFFAA44;

    private static float gridAlpha = 0;

                                      
    private static final List<GuideLine> activeGuides = new ArrayList<>();

    private static class GuideLine {
        final float x1, y1, x2, y2;
        final int color;
        final float alpha;

        GuideLine(float x1, float y1, float x2, float y2, int color, float alpha) {
            this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2;
            this.color = color; this.alpha = alpha;
        }
    }

                     
    private static class SnapResult {
        float value;
        boolean snapped;
        int guideColor;

        SnapResult(float value, boolean snapped, int guideColor) {
            this.value = value;
            this.snapped = snapped;
            this.guideColor = guideColor;
        }
    }

                                              
                            
                                              

    public static void snap(HudElement element) {
        float screenW = HudElement.hudScreenWidth();
        float screenH = HudElement.hudScreenHeight();

        float x = element.getX();
        float y = element.getY();
        float w = element.getWidth();
        float h = element.getHeight();

        List<HudElement> others = DragAndDrop.getInstance().getActiveElements();

                                    
        x = snapAxis(x, w, screenW, true, element, others);

                                  
        y = snapAxis(y, h, screenH, false, element, others);

                                           
        x = Math.round(x / GRID_SIZE) * GRID_SIZE;
        y = Math.round(y / GRID_SIZE) * GRID_SIZE;

                                    
        x = clamp(x, 0, screenW - w);
        y = clamp(y, 0, screenH - h);

        element.setX(x);
        element.setY(y);
    }

       
                                    
                                  
                      
                                                         
                                        
       
    private static float snapAxis(float pos, float size, float screenSize,
                                  boolean horizontal, HudElement self,
                                  List<HudElement> others) {
        float best = pos;
        float bestDist = SNAP_THRESHOLD + 1;

        float left = pos;
        float center = pos + size / 2f;
        float right = pos + size;

                                  
        float d = Math.abs(left - EDGE_MARGIN);
        if (d < bestDist) { bestDist = d; best = EDGE_MARGIN; }

                                 
        d = Math.abs(right - (screenSize - EDGE_MARGIN));
        if (d < bestDist) { bestDist = d; best = screenSize - EDGE_MARGIN - size; }

                          
        d = Math.abs(center - screenSize / 2f);
        if (d < bestDist) { bestDist = d; best = screenSize / 2f - size / 2f; }

                                     
        float third1 = screenSize / 3f;
        float third2 = screenSize * 2f / 3f;
        d = Math.abs(center - third1);
        if (d < bestDist) { bestDist = d; best = third1 - size / 2f; }
        d = Math.abs(center - third2);
        if (d < bestDist) { bestDist = d; best = third2 - size / 2f; }

                                
        for (HudElement other : others) {
            if (other == self) continue;

            float oPos, oSize;
            if (horizontal) {
                oPos = other.getX(); oSize = other.getWidth();
            } else {
                oPos = other.getY(); oSize = other.getHeight();
            }

            float oLeft = oPos;
            float oCenter = oPos + oSize / 2f;
            float oRight = oPos + oSize;

                             
            d = Math.abs(left - oLeft);
            if (d < bestDist) { bestDist = d; best = oLeft; }

                               
            d = Math.abs(right - oRight);
            if (d < bestDist) { bestDist = d; best = oRight - size; }

                             
            d = Math.abs(center - oCenter);
            if (d < bestDist) { bestDist = d; best = oCenter - size / 2f; }

                                         
            d = Math.abs(left - oRight);
            if (d < SNAP_THRESHOLD) {
                float gap = ELEMENT_GAP;
                float dd = Math.abs(left - (oRight + gap));
                if (dd < bestDist) { bestDist = dd; best = oRight + gap; }
            }

                                         
            d = Math.abs(right - oLeft);
            if (d < SNAP_THRESHOLD) {
                float gap = ELEMENT_GAP;
                float dd = Math.abs(right - (oLeft - gap));
                if (dd < bestDist) { bestDist = dd; best = oLeft - gap - size; }
            }

                                                                
            d = Math.abs(left - oRight);
            if (d < bestDist) { bestDist = d; best = oRight; }

                                        
            d = Math.abs(right - oLeft);
            if (d < bestDist) { bestDist = d; best = oLeft - size; }
        }

        return bestDist <= SNAP_THRESHOLD ? best : pos;
    }

                                              
                                 
                                              

       
                                                              
                                              
       
    public static float[] liveSnap(HudElement element) {
        return liveSnapAt(element, element.getX(), element.getY());
    }

    public static float[] liveSnapAt(HudElement element, float x, float y) {
        float screenW = HudElement.hudScreenWidth();
        float screenH = HudElement.hudScreenHeight();

        float w = element.getWidth();
        float h = element.getHeight();

        List<HudElement> others = DragAndDrop.getInstance().getActiveElements();

        activeGuides.clear();

                       
        float[] xResult = liveSnapAxis(x, y, w, h, screenW, screenH, true, element, others);
        float snappedX = xResult[0];

                       
        float[] yResult = liveSnapAxis(y, snappedX, h, w, screenH, screenW, false, element, others);
        float snappedY = yResult[0];

        snappedX = clamp(snappedX, 0, screenW - w);
        snappedY = clamp(snappedY, 0, screenH - h);

        return new float[]{ snappedX, snappedY };
    }

       
                                                               
       
    private static float[] liveSnapAxis(float pos, float crossPos, float size, float crossSize,
                                        float screenSize, float crossScreenSize,
                                        boolean horizontal, HudElement self,
                                        List<HudElement> others) {
        float best = pos;
        float bestDist = SNAP_THRESHOLD + 1;
        int bestColor = GUIDE_COLOR_CENTER;
                                               
        float bestGuidePos = -1;

        float left = pos;
        float center = pos + size / 2f;
        float right = pos + size;

                                                    
        float grid = Math.round(pos / GRID_SIZE) * GRID_SIZE;
        float d = Math.abs(pos - grid);
        if (d < 2.2f && d < bestDist) {
            bestDist = d;
            best = grid;
            bestGuidePos = grid;
            bestColor = GUIDE_COLOR_CENTER;
        }

                                     
        d = Math.abs(left - EDGE_MARGIN);
        if (d < bestDist) {
            bestDist = d; best = EDGE_MARGIN;
            bestGuidePos = EDGE_MARGIN; bestColor = GUIDE_COLOR_EDGE;
        }

                                    
        d = Math.abs(right - (screenSize - EDGE_MARGIN));
        if (d < bestDist) {
            bestDist = d; best = screenSize - EDGE_MARGIN - size;
            bestGuidePos = screenSize - EDGE_MARGIN; bestColor = GUIDE_COLOR_EDGE;
        }

                               
        d = Math.abs(center - screenSize / 2f);
        if (d < bestDist) {
            bestDist = d; best = screenSize / 2f - size / 2f;
            bestGuidePos = screenSize / 2f; bestColor = GUIDE_COLOR_CENTER;
        }

                               
        float third1 = screenSize / 3f;
        float third2 = screenSize * 2f / 3f;
        d = Math.abs(center - third1);
        if (d < bestDist) {
            bestDist = d; best = third1 - size / 2f;
            bestGuidePos = third1; bestColor = GUIDE_COLOR_CENTER;
        }
        d = Math.abs(center - third2);
        if (d < bestDist) {
            bestDist = d; best = third2 - size / 2f;
            bestGuidePos = third2; bestColor = GUIDE_COLOR_CENTER;
        }

                                     
        for (HudElement other : others) {
            if (other == self) continue;

            float oPos = horizontal ? other.getX() : other.getY();
            float oSize = horizontal ? other.getWidth() : other.getHeight();
            float oCrossPos = horizontal ? other.getY() : other.getX();
            float oCrossSize = horizontal ? other.getHeight() : other.getWidth();

            float oLeft = oPos;
            float oCenter = oPos + oSize / 2f;
            float oRight = oPos + oSize;

                             
            d = Math.abs(left - oLeft);
            if (d < bestDist) {
                bestDist = d; best = oLeft;
                bestGuidePos = oLeft; bestColor = GUIDE_COLOR_ELEMENT;
            }

                               
            d = Math.abs(right - oRight);
            if (d < bestDist) {
                bestDist = d; best = oRight - size;
                bestGuidePos = oRight; bestColor = GUIDE_COLOR_ELEMENT;
            }

                             
            d = Math.abs(center - oCenter);
            if (d < bestDist) {
                bestDist = d; best = oCenter - size / 2f;
                bestGuidePos = oCenter; bestColor = GUIDE_COLOR_ELEMENT;
            }

                             
            d = Math.abs(left - (oRight + ELEMENT_GAP));
            if (d < bestDist) {
                bestDist = d; best = oRight + ELEMENT_GAP;
                bestGuidePos = -1; bestColor = GUIDE_COLOR_ELEMENT;
            }

            d = Math.abs(right - (oLeft - ELEMENT_GAP));
            if (d < bestDist) {
                bestDist = d; best = oLeft - ELEMENT_GAP - size;
                bestGuidePos = -1; bestColor = GUIDE_COLOR_ELEMENT;
            }
        }

                              
        if (bestDist <= SNAP_THRESHOLD && bestGuidePos >= 0) {
            float intensity = 1f - (bestDist / SNAP_THRESHOLD);
            if (horizontal) {
                activeGuides.add(new GuideLine(
                        bestGuidePos, 0, bestGuidePos, crossScreenSize,
                        bestColor, intensity * 0.5f));
            } else {
                activeGuides.add(new GuideLine(
                        0, bestGuidePos, crossScreenSize, bestGuidePos,
                        bestColor, intensity * 0.5f));
            }
        }

                                                            
        if (bestDist <= SNAP_THRESHOLD) {
            addDistanceGuides(self, best, size, crossPos, crossSize,
                    horizontal, others, screenSize, crossScreenSize);
        }

        if (bestDist <= SNAP_THRESHOLD) {
            return new float[]{ best };
        }
        return new float[]{ pos };
    }

       
                                                 
                                                           
                              
       
    private static void addDistanceGuides(HudElement self, float pos, float size,
                                          float crossPos, float crossSize,
                                          boolean horizontal,
                                          List<HudElement> others,
                                          float screenSize, float crossScreenSize) {
        float left = pos;
        float right = pos + size;

        for (int i = 0; i < others.size(); i++) {
            HudElement a = others.get(i);
            if (a == self) continue;

            float aPos = horizontal ? a.getX() : a.getY();
            float aSize = horizontal ? a.getWidth() : a.getHeight();
            float aRight = aPos + aSize;

            for (int j = i + 1; j < others.size(); j++) {
                HudElement b = others.get(j);
                if (b == self) continue;

                float bPos = horizontal ? b.getX() : b.getY();
                float bSize = horizontal ? b.getWidth() : b.getHeight();
                float bRight = bPos + bSize;

                                         
                float distAB = Math.min(
                        Math.abs(aPos - bRight),
                        Math.abs(bPos - aRight)
                );

                                          
                float distSelfA = Math.min(
                        Math.abs(left - aRight),
                        Math.abs(aPos - right)
                );

                                          
                float distSelfB = Math.min(
                        Math.abs(left - bRight),
                        Math.abs(bPos - right)
                );

                float tolerance = 2f;

                if (Math.abs(distSelfA - distAB) < tolerance
                        || Math.abs(distSelfB - distAB) < tolerance) {
                                                
                    float midCross = crossPos + crossSize / 2f;

                    if (horizontal) {
                                                            
                        if (Math.abs(distSelfA - distAB) < tolerance) {
                            float gapStart, gapEnd;
                            if (left > aRight) {
                                gapStart = aRight; gapEnd = left;
                            } else {
                                gapStart = right; gapEnd = aPos;
                            }
                            if (gapEnd > gapStart) {
                                activeGuides.add(new GuideLine(
                                        gapStart, midCross, gapEnd, midCross,
                                        GUIDE_COLOR_DIST, 0.35f));
                            }
                        }
                    } else {
                        if (Math.abs(distSelfA - distAB) < tolerance) {
                            float gapStart, gapEnd;
                            if (left > aRight) {
                                gapStart = aRight; gapEnd = left;
                            } else {
                                gapStart = right; gapEnd = aPos;
                            }
                            if (gapEnd > gapStart) {
                                float midX = crossPos + crossSize / 2f;
                                activeGuides.add(new GuideLine(
                                        midX, gapStart, midX, gapEnd,
                                        GUIDE_COLOR_DIST, 0.35f));
                            }
                        }
                    }
                }
            }
        }
    }

                                              
              
                                              

    public static void render(DrawContext ctx, float delta, boolean dragging) {
        float target = dragging ? 1f : 0f;
        gridAlpha = RenderUtil.animate(gridAlpha, target, 0.20f, RenderUtil.getFrameDelta());
        if (gridAlpha < 0.02f) { gridAlpha = 0; return; }

        float screenW = HudElement.hudScreenWidth();
        float screenH = HudElement.hudScreenHeight();

        float baseA = gridAlpha;

        float hudScale = HudElement.hudScale();
        ctx.getMatrices().push();
        ctx.getMatrices().scale(hudScale, hudScale, 1f);

                                                    
        float centerA = baseA * 0.08f;
        RenderUtil.drawRect(ctx, screenW / 2f - 0.25f, 0, 0.5f, screenH,
                RenderUtil.withAlpha(GUIDE_COLOR_CENTER, centerA));
        RenderUtil.drawRect(ctx, 0, screenH / 2f - 0.25f, screenW, 0.5f,
                RenderUtil.withAlpha(GUIDE_COLOR_CENTER, centerA));

                                                               
        float thirdA = baseA * 0.04f;
        RenderUtil.drawRect(ctx, screenW / 3f - 0.25f, 0, 0.5f, screenH,
                RenderUtil.withAlpha(GUIDE_COLOR_CENTER, thirdA));
        RenderUtil.drawRect(ctx, screenW * 2f / 3f - 0.25f, 0, 0.5f, screenH,
                RenderUtil.withAlpha(GUIDE_COLOR_CENTER, thirdA));
        RenderUtil.drawRect(ctx, 0, screenH / 3f - 0.25f, screenW, 0.5f,
                RenderUtil.withAlpha(GUIDE_COLOR_CENTER, thirdA));
        RenderUtil.drawRect(ctx, 0, screenH * 2f / 3f - 0.25f, screenW, 0.5f,
                RenderUtil.withAlpha(GUIDE_COLOR_CENTER, thirdA));

                                     
        float edgeA = baseA * 0.03f;
        RenderUtil.drawRect(ctx, EDGE_MARGIN - 0.25f, 0, 0.5f, screenH,
                RenderUtil.withAlpha(GUIDE_COLOR_EDGE, edgeA));
        RenderUtil.drawRect(ctx, screenW - EDGE_MARGIN - 0.25f, 0, 0.5f, screenH,
                RenderUtil.withAlpha(GUIDE_COLOR_EDGE, edgeA));
        RenderUtil.drawRect(ctx, 0, EDGE_MARGIN - 0.25f, screenW, 0.5f,
                RenderUtil.withAlpha(GUIDE_COLOR_EDGE, edgeA));
        RenderUtil.drawRect(ctx, 0, screenH - EDGE_MARGIN - 0.25f, screenW, 0.5f,
                RenderUtil.withAlpha(GUIDE_COLOR_EDGE, edgeA));

                                    
        HudElement drag = DragAndDrop.getInstance().getDragging();
        if (drag != null) {
                                
            float[] snapped = liveSnap(drag);

            for (GuideLine guide : activeGuides) {
                float ga = baseA * guide.alpha;
                if (ga < 0.01f) continue;

                float dx = guide.x2 - guide.x1;
                float dy = guide.y2 - guide.y1;

                if (Math.abs(dx) < 0.01f) {
                                         
                    float lineX = guide.x1;
                    float lineY = Math.min(guide.y1, guide.y2);
                    float lineH = Math.abs(dy);
                    RenderUtil.drawRect(ctx, lineX - 0.25f, lineY, 0.5f, lineH,
                            RenderUtil.withAlpha(guide.color, ga));
                } else if (Math.abs(dy) < 0.01f) {
                                           
                    float lineX = Math.min(guide.x1, guide.x2);
                    float lineY = guide.y1;
                    float lineW = Math.abs(dx);
                    RenderUtil.drawRect(ctx, lineX, lineY - 0.25f, lineW, 0.5f,
                            RenderUtil.withAlpha(guide.color, ga));
                }
            }

                                                              
            renderAlignmentLines(ctx, drag, baseA);
        }
        ctx.getMatrices().pop();
    }

       
                                                                
       
    private static void renderAlignmentLines(DrawContext ctx, HudElement drag, float baseA) {
        float dx = drag.getX(), dy = drag.getY();
        float dw = drag.getWidth(), dh = drag.getHeight();
        float dcx = dx + dw / 2f, dcy = dy + dh / 2f;

        float screenW = HudElement.hudScreenWidth();
        float screenH = HudElement.hudScreenHeight();

        for (HudElement other : DragAndDrop.getInstance().getActiveElements()) {
            if (other == drag) continue;

            float ox = other.getX(), oy = other.getY();
            float ow = other.getWidth(), oh = other.getHeight();
            float ocx = ox + ow / 2f, ocy = oy + oh / 2f;

            float threshold = SNAP_THRESHOLD * 1.5f;

                                                             
            float[][] vAligns = {
                    {dx, ox}, {dx + dw, ox + ow}, {dcx, ocx},
                    {dx, ox + ow}, {dx + dw, ox}
            };
            for (float[] align : vAligns) {
                if (Math.abs(align[0] - align[1]) < threshold) {
                    float intensity = 1f - Math.abs(align[0] - align[1]) / threshold;
                    float lineX = align[1];
                    float minY = Math.min(dy, oy);
                    float maxY = Math.max(dy + dh, oy + oh);
                    RenderUtil.drawRect(ctx, lineX - 0.25f, minY, 0.5f, maxY - minY,
                            RenderUtil.withAlpha(GUIDE_COLOR_ELEMENT, baseA * 0.2f * intensity));
                }
            }

                                                               
            float[][] hAligns = {
                    {dy, oy}, {dy + dh, oy + oh}, {dcy, ocy},
                    {dy, oy + oh}, {dy + dh, oy}
            };
            for (float[] align : hAligns) {
                if (Math.abs(align[0] - align[1]) < threshold) {
                    float intensity = 1f - Math.abs(align[0] - align[1]) / threshold;
                    float lineY = align[1];
                    float minX = Math.min(dx, ox);
                    float maxX = Math.max(dx + dw, ox + ow);
                    RenderUtil.drawRect(ctx, minX, lineY - 0.25f, maxX - minX, 0.5f,
                            RenderUtil.withAlpha(GUIDE_COLOR_ELEMENT, baseA * 0.2f * intensity));
                }
            }
        }
    }

                                              
                        
                                              

       
                                                                           
                                         
       
    public static void renderDistanceOverlay(DrawContext ctx, HudElement element, float alpha) {
        if (element == null || alpha < 0.02f) return;

        float screenW = HudElement.hudScreenWidth();
        float screenH = HudElement.hudScreenHeight();

        float x = element.getX(), y = element.getY();
        float w = element.getWidth(), h = element.getHeight();

        float a = alpha * 0.25f;

                     
        if (x > 1) {
            float midY = y + h / 2f;
            RenderUtil.drawRect(ctx, 0, midY - 0.25f, x, 0.5f,
                    RenderUtil.withAlpha(GUIDE_COLOR_DIST, a));
        }

                      
        float rightDist = screenW - (x + w);
        if (rightDist > 1) {
            float midY = y + h / 2f;
            RenderUtil.drawRect(ctx, x + w, midY - 0.25f, rightDist, 0.5f,
                    RenderUtil.withAlpha(GUIDE_COLOR_DIST, a));
        }

                       
        if (y > 1) {
            float midX = x + w / 2f;
            RenderUtil.drawRect(ctx, midX - 0.25f, 0, 0.5f, y,
                    RenderUtil.withAlpha(GUIDE_COLOR_DIST, a));
        }

                      
        float bottomDist = screenH - (y + h);
        if (bottomDist > 1) {
            float midX = x + w / 2f;
            RenderUtil.drawRect(ctx, midX - 0.25f, y + h, 0.5f, bottomDist,
                    RenderUtil.withAlpha(GUIDE_COLOR_DIST, a));
        }
    }

                                              
            
                                              

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
