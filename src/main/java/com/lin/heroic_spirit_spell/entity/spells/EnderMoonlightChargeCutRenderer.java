package com.lin.heroic_spirit_spell.entity.spells;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * V-cut: end portal core + lightning rim. Rim slightly offset in Z for depth.
 */
public class EnderMoonlightChargeCutRenderer extends EntityRenderer<EnderMoonlightChargeCutEntity> {

    /** Small Z bias so rim draws over portal; avoid large values (visible seam between layers). */
    private static final float RIM_Z_OFFSET_BLOCKS = 0.25f;

    private static final float PHASE_A_END = 0.12f;
    private static final float PHASE_B_END = 0.35f;
    private static final float PHASE_C_END = 0.70f;
    /**
     * Lightning mesh is slightly narrower than the end-portal core so the rim stays inside the portal silhouette
     * (lightning RenderType reads wider than its quads).
     */
    private static final float RIM_MESH_WIDTH_SCALE = 0.25f;
    private static final float CAP_REGION_RATIO = 0.20f;
    private static final float CAP_TAPER_MIN = 0.70f;
    private static final float CAP_TAPER_MAX = 0.85f;
    private static final float CORE_MAX_ALPHA = 0.30f;
    private static final float RIM_PULSE_FREQUENCY = 0.72f;
    private static final float CAP_FLICKER_FREQUENCY = 2.20f;
    private static final int RIM_RED = 214;
    private static final int RIM_GREEN = 236;
    private static final int RIM_BLUE = 255;

    public EnderMoonlightChargeCutRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull EnderMoonlightChargeCutEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        if (entity.isProcessingStarted()) {
            float progress = Mth.clamp(
                    entity.getProcessedDistanceForRender(partialTicks),
                    0.0f,
                    entity.getDistanceBlocks()
            );
            RiftVisualState state = calculateVisualState(entity, partialTicks);
            int coreAlpha = Mth.clamp((int) (state.coreAlpha() * 255.0f), 0, 255);
            int rimAlpha = Mth.clamp((int) (state.rimAlpha() * 255.0f), 0, 255);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
            poseStack.translate(0.0, 0.0, progress);

            if (coreAlpha > 0) {
                VertexConsumer core = buffer.getBuffer(RenderType.endPortal());
                drawVCutLayer(
                        poseStack,
                        core,
                        state.nearHalfWidth(),
                        state.farHalfWidth(),
                        state.notchDepth(),
                        EnderMoonlightChargeCutEntity.AREA_HEIGHT_BLOCKS,
                        255,
                        255,
                        255,
                        coreAlpha,
                        0.0f
                );
            }

            if (rimAlpha > 0) {
                poseStack.pushPose();
                poseStack.translate(0.0, 0.0, RIM_Z_OFFSET_BLOCKS);
                VertexConsumer rim = buffer.getBuffer(RenderType.lightning());
                float rimNearHalfWidth = state.nearHalfWidth() * RIM_MESH_WIDTH_SCALE;
                float rimFarHalfWidth = state.farHalfWidth() * RIM_MESH_WIDTH_SCALE;
                drawVCutLayer(
                        poseStack,
                        rim,
                        rimNearHalfWidth,
                        rimFarHalfWidth,
                        calculateNotchDepth(rimFarHalfWidth),
                        EnderMoonlightChargeCutEntity.AREA_HEIGHT_BLOCKS,
                        RIM_RED,
                        RIM_GREEN,
                        RIM_BLUE,
                        rimAlpha,
                        state.capFlicker()
                );
                poseStack.popPose();
            }

            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EnderMoonlightChargeCutEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static RiftVisualState calculateVisualState(EnderMoonlightChargeCutEntity entity, float partialTicks) {
        float activeTicks = Math.max(
                0.0f,
                entity.tickCount - EnderMoonlightChargeCutEntity.PROCESS_START_DELAY_TICKS + partialTicks
        );
        float totalTicks = Math.max(1.0f, (float) EnderMoonlightChargeCutEntity.PROCESS_DURATION_TICKS);
        float t = Mth.clamp(activeTicks / totalTicks, 0.0f, 1.0f);

        float open = 1.0f;
        if (t < PHASE_A_END) {
            open = Mth.lerp(t / PHASE_A_END, 0.03f, 0.16f);
        } else if (t < PHASE_B_END) {
            float openT = smoothStep((t - PHASE_A_END) / (PHASE_B_END - PHASE_A_END));
            open = Mth.lerp(openT, 0.16f, 1.0f);
        }

        float stitch = t <= PHASE_C_END ? 0.0f : smoothStep((t - PHASE_C_END) / (1.0f - PHASE_C_END));
        float farClose = smoothStep(Mth.clamp(stitch * 1.35f, 0.0f, 1.0f));
        float nearClose = smoothStep(Mth.clamp((stitch - 0.35f) / 0.65f, 0.0f, 1.0f));

        float nearHalfWidth = EnderMoonlightChargeCutEntity.VISUAL_NEAR_HALF_WIDTH_BLOCKS * open * Math.max(0.0f, 1.0f - nearClose);
        float farHalfWidth = EnderMoonlightChargeCutEntity.VISUAL_FAR_HALF_WIDTH_BLOCKS * open * Math.max(0.0f, 1.0f - farClose);
        nearHalfWidth = Math.max(nearHalfWidth, 0.002f);
        farHalfWidth = Math.max(farHalfWidth, 0.002f);

        float rimBaseAlpha = 0.0f;
        if (t < PHASE_A_END) {
            rimBaseAlpha = Mth.lerp(t / PHASE_A_END, 0.42f, 0.72f);
        } else if (t < PHASE_B_END) {
            rimBaseAlpha = Mth.lerp((t - PHASE_A_END) / (PHASE_B_END - PHASE_A_END), 0.72f, 0.98f);
        } else if (t < PHASE_C_END) {
            rimBaseAlpha = 0.90f;
        } else {
            rimBaseAlpha = Mth.lerp((t - PHASE_C_END) / (1.0f - PHASE_C_END), 0.90f, 0.28f);
        }
        float rimPulse = 0.82f + 0.18f * Mth.sin((entity.tickCount + partialTicks) * RIM_PULSE_FREQUENCY * (float) (Math.PI * 2.0));
        float rimAlpha = Mth.clamp(rimBaseAlpha * rimPulse, 0.0f, 1.0f);

        float coreAlpha = 0.0f;
        if (t >= PHASE_A_END) {
            if (t < PHASE_B_END) {
                float delayedB = Mth.clamp((t - (PHASE_A_END + 0.08f)) / ((PHASE_B_END - PHASE_A_END) - 0.08f), 0.0f, 1.0f);
                coreAlpha = CORE_MAX_ALPHA * smoothStep(delayedB);
            } else if (t < PHASE_C_END) {
                coreAlpha = CORE_MAX_ALPHA;
            } else {
                coreAlpha = CORE_MAX_ALPHA * (1.0f - smoothStep((t - PHASE_C_END) / (1.0f - PHASE_C_END)));
            }
        }

        float capSin = 0.72f + 0.28f * Mth.sin((entity.tickCount + partialTicks) * CAP_FLICKER_FREQUENCY * (float) (Math.PI * 2.0));
        float capNoise = 0.60f + 0.40f * hash01(entity.getId() * 37.0f + entity.tickCount * 1.31f);
        float capFlicker = Mth.clamp(capSin * 0.65f + capNoise * 0.35f, 0.0f, 1.0f);
        if (t < PHASE_B_END) {
            capFlicker = Mth.lerp(Mth.clamp(t / PHASE_B_END, 0.0f, 1.0f), 0.45f, capFlicker);
        }

        return new RiftVisualState(
                nearHalfWidth,
                farHalfWidth,
                calculateNotchDepth(farHalfWidth),
                open,
                coreAlpha,
                rimAlpha,
                capFlicker
        );
    }

    private static float smoothStep(float value) {
        float t = Mth.clamp(value, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float hash01(float seed) {
        return Mth.frac(Mth.sin(seed * 12.9898f) * 43758.547f);
    }

    private static void drawVCutLayer(PoseStack poseStack, VertexConsumer vc,
                                      float nearHalfWidth, float farHalfWidth, float notchDepth, float height,
                                      int red, int green, int blue, int alpha, float capFlicker) {
        if (nearHalfWidth <= 0.0f || farHalfWidth <= 0.0f || height <= 0.0f || alpha <= 0) {
            return;
        }
        Matrix4f matrix = poseStack.last().pose();
        float capHeight = height * CAP_REGION_RATIO;
        float topStart = height - capHeight;
        float capTaper = Mth.lerp(capFlicker, CAP_TAPER_MIN, CAP_TAPER_MAX);
        int capAlpha = Mth.clamp((int) (alpha * Mth.lerp(capFlicker, 0.92f, 1.22f)), 0, 255);

        drawVCutSegment(matrix, vc, nearHalfWidth, farHalfWidth, notchDepth, 0.0f, capHeight, capTaper, 1.0f,
                red, green, blue, alpha);
        drawVCutSegment(matrix, vc, nearHalfWidth, farHalfWidth, notchDepth, capHeight, topStart, 1.0f, 1.0f,
                red, green, blue, alpha);
        drawVCutSegment(matrix, vc, nearHalfWidth, farHalfWidth, notchDepth, topStart, height, 1.0f, capTaper,
                red, green, blue, capAlpha);
        drawVCutSegment(matrix, vc, nearHalfWidth, farHalfWidth, notchDepth, 0.0f, capHeight, capTaper, 1.0f,
                red, green, blue, capAlpha);
    }

    private static void drawVCutSegment(Matrix4f matrix, VertexConsumer vc,
                                        float nearHalfWidth, float farHalfWidth, float notchDepth,
                                        float yStart, float yEnd, float startScale, float endScale,
                                        int red, int green, int blue, int alpha) {
        if (alpha <= 0 || yEnd <= yStart) {
            return;
        }

        float leftTopNear = -nearHalfWidth * endScale;
        float leftBottomNear = -nearHalfWidth * startScale;
        float leftBottomFar = -farHalfWidth * startScale;
        float leftTopFar = -farHalfWidth * endScale;
        addQuadDoubleSided(matrix, vc,
                leftTopNear, yEnd, 0.0f,
                leftBottomNear, yStart, 0.0f,
                leftBottomFar, yStart, -notchDepth,
                leftTopFar, yEnd, -notchDepth,
                red, green, blue, alpha);

        float rightTopNear = nearHalfWidth * endScale;
        float rightTopFar = farHalfWidth * endScale;
        float rightBottomFar = farHalfWidth * startScale;
        float rightBottomNear = nearHalfWidth * startScale;
        addQuadDoubleSided(matrix, vc,
                rightTopNear, yEnd, 0.0f,
                rightTopFar, yEnd, -notchDepth,
                rightBottomFar, yStart, -notchDepth,
                rightBottomNear, yStart, 0.0f,
                red, green, blue, alpha);
    }

    private static float calculateNotchDepth(float farHalfWidth) {
        float halfAngleRad = (EnderMoonlightChargeCutEntity.V_NOTCH_ANGLE_DEGREES * 0.5f) * Mth.DEG_TO_RAD;
        float tanHalf = (float) Math.tan(halfAngleRad);
        if (tanHalf <= 1.0e-4f) {
            return EnderMoonlightChargeCutEntity.MIN_NOTCH_DEPTH;
        }
        return Mth.clamp(
                farHalfWidth / tanHalf,
                EnderMoonlightChargeCutEntity.MIN_NOTCH_DEPTH,
                EnderMoonlightChargeCutEntity.MAX_NOTCH_DEPTH
        );
    }

    /**
     * Two passes with opposite winding so both sides survive back-face culling (e.g. viewing head-on along cast axis).
     * Uses plane normal from cross product so slanted V faces light correctly.
     */
    private static void addQuadDoubleSided(Matrix4f matrix, VertexConsumer vc,
                                           float x0, float y0, float z0,
                                           float x1, float y1, float z1,
                                           float x2, float y2, float z2,
                                           float x3, float y3, float z3,
                                           int red, int green, int blue, int alpha) {
        Vec3 e1 = new Vec3(x1 - x0, y1 - y0, z1 - z0);
        Vec3 e2 = new Vec3(x2 - x0, y2 - y0, z2 - z0);
        Vec3 cross = e1.cross(e2);
        double len = cross.length();
        if (len < 1e-7) {
            addQuadWithNormal(matrix, vc, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, red, green, blue, alpha, 0f, 0f, 1f);
            addQuadWithNormal(matrix, vc, x0, y0, z0, x3, y3, z3, x2, y2, z2, x1, y1, z1, red, green, blue, alpha, 0f, 0f, -1f);
            return;
        }
        float nx = (float) (cross.x / len);
        float ny = (float) (cross.y / len);
        float nz = (float) (cross.z / len);
        addQuadWithNormal(matrix, vc, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, red, green, blue, alpha, nx, ny, nz);
        addQuadWithNormal(matrix, vc, x0, y0, z0, x3, y3, z3, x2, y2, z2, x1, y1, z1, red, green, blue, alpha, -nx, -ny, -nz);
    }

    private static void addQuadWithNormal(Matrix4f matrix, VertexConsumer vc,
                                          float x0, float y0, float z0,
                                          float x1, float y1, float z1,
                                          float x2, float y2, float z2,
                                          float x3, float y3, float z3,
                                          int red, int green, int blue, int alpha,
                                          float nx, float ny, float nz) {
        int light = LightTexture.FULL_BRIGHT;
        vc.addVertex(matrix, x0, y0, z0).setColor(red, green, blue, alpha).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(matrix, x3, y3, z3).setColor(red, green, blue, alpha).setLight(light).setNormal(nx, ny, nz);
    }

    private record RiftVisualState(float nearHalfWidth, float farHalfWidth, float notchDepth, float open,
                                   float coreAlpha, float rimAlpha, float capFlicker) {}
}
