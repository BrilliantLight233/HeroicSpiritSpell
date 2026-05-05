package com.lin.heroic_spirit_spell.entity.spells;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 神圣斩击光刃渲染器
 * 使用 billboard 技术渲染光刃
 */
public class HolySlashRenderer extends EntityRenderer<HolySlashProjectile> {
    
    // 使用铁魔法的纹理，或自定义纹理
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.parse("heroic_spirit_spell:textures/entity/holy_slash_large.png");
    
    public HolySlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F; // 光刃没有阴影
    }
    
    @Override
    public void render(HolySlashProjectile entity, float entityYaw, float partialTicks, 
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        // 跟随实体俯仰角（实体俯仰来自施法者目视角）
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch+90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F)); 
        
        // 缩放：根据半径调整
        float radius = entity.getRadius();
        float scale = radius * 2.0F;
        poseStack.scale(scale, scale, scale);
        
        // 渲染固定世界朝向平面
        
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        
        // 渲染两个三角形组成的面
        float minU = 0.0F;
        float maxU = 1.0F;
        float minV = 0.0F;
        float maxV = 1.0F;
        int light = 15728880; // 全亮
        
        // 左下角
        vertex(vertexConsumer, matrix4f, matrix3f, light, -0.5F, -0.5F, minU, maxV);
        // 右下角
        vertex(vertexConsumer, matrix4f, matrix3f, light, 0.5F, -0.5F, maxU, maxV);
        // 右上角
        vertex(vertexConsumer, matrix4f, matrix3f, light, 0.5F, 0.5F, maxU, minV);
        // 左上角
        vertex(vertexConsumer, matrix4f, matrix3f, light, -0.5F, 0.5F, minU, minV);
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    private static void vertex(VertexConsumer vertexConsumer, Matrix4f matrix, Matrix3f normal, 
                               int light, float x, float y, float u, float v) {
        vertexConsumer.addVertex(matrix, x, y, 0.0F)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0.0F, 1.0F, 0.0F);
    }
    
    @Override
    public ResourceLocation getTextureLocation(HolySlashProjectile entity) {
        return TEXTURE;
    }
}
