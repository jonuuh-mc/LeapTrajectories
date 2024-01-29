package net.jonuuh.ltj;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;

public class Render
{
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Color lineColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    private final Color boxColorHit = new Color(1.0F, 0.0F, 0.0F);
    private final Color boxColorMiss = new Color(0.0F, 1.0F, 0.0F);

    @SubscribeEvent
    public void render(RenderWorldLastEvent event)
    {
        EntityPlayerSP player = mc.thePlayer;
        Item heldItem = player.getHeldItem() != null ? player.getHeldItem().getItem() : null;

        if (!isThrowable(heldItem) || mc.gameSettings.thirdPersonView != 0)
        {
            return;
        }

        RenderManager renderManager = mc.getRenderManager();
        float[] renderPos = getRenderPositions(renderManager);

        boolean holdingBow = heldItem instanceof ItemBow;
        boolean hit = false;

        Vec3 playerVector = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        float playerYaw = (float) Math.toRadians(player.rotationYaw);
        float playerPitch = (float) Math.toRadians(player.rotationPitch);
        float motionFactor = holdingBow ? 1.0F : 0.4F;
        double gravity = holdingBow ? 0.05D : heldItem instanceof ItemPotion ? 0.4D : heldItem instanceof ItemFishingRod ? 0.15D : 0.03D;

        // Init position
        Vec3 pos = new Vec3(
                renderPos[0] - Math.cos(playerYaw) * 0.16F,
                renderPos[1] + player.getEyeHeight() - 0.07F,
                renderPos[2] - Math.sin(playerYaw) * 0.16F);

        // Init motion
        Vec3Extra motion = new Vec3Extra(
                -Math.sin(playerYaw) * Math.cos(playerPitch) * motionFactor,
                -Math.sin(playerPitch) * motionFactor,
                Math.cos(playerYaw) * Math.cos(playerPitch) * motionFactor);

        motion = motion.divide(Math.sqrt(motion.dotProduct(motion)));
        motion = holdingBow ? motion.multiply(getBowPower(player)) : motion.multiply(1.5F); // Velocity

        // GL setup
        GL11.glPushMatrix();
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(lineColor.r, lineColor.g, lineColor.b, lineColor.a);
        GL11.glLineWidth(2F);

        // Draw trajectory
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < 1000; i++)
        {
            GL11.glVertex3d(pos.xCoord - renderPos[0], pos.yCoord - renderPos[1], pos.zCoord - renderPos[2]);

            pos = pos.addVector(motion.xCoord * 0.1, motion.yCoord * 0.1, motion.zCoord * 0.1);

            motion = motion.multiply(0.999D);
            motion = new Vec3Extra(motion.xCoord, motion.yCoord - (gravity * 0.1), motion.zCoord);

            AxisAlignedBB collisionBox = new AxisAlignedBB(
                    pos.xCoord - 0.25d, pos.yCoord - 0.25d, pos.zCoord - 0.25d,
                    pos.xCoord + 0.25d, pos.yCoord + 0.25d, pos.zCoord + 0.25d);

            if (mc.theWorld.rayTraceBlocks(playerVector, pos) != null)
            {
                break;
            }
            else if (!mc.theWorld.checkNoEntityCollision(collisionBox, mc.thePlayer))
            {
                hit = true;
                break;
            }
        }
        GL11.glEnd();

        // Draw trajectory end box
        drawEndBox(renderPos, pos, hit);

        // GL reset
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glPopMatrix();
    }

    private boolean isThrowable(Item item)
    {
        return item instanceof ItemBow || item instanceof ItemSnowball || item instanceof ItemEgg
                || item instanceof ItemEnderPearl || item instanceof ItemPotion || item instanceof ItemFishingRod;
    }

    private float getBowPower(EntityPlayerSP player)
    {
        float bowPower = (72000 - player.getItemInUseCount()) / 20.0F;
        bowPower = (bowPower * bowPower + bowPower * 2.0F);

        // clamp power if full charge or no charge
        return (bowPower > 3.0F || bowPower <= 0.3F) ? 3.0F : bowPower;
    }

    private float[] getRenderPositions(RenderManager renderManager)
    {
        float[] positions = new float[3];
        try
        {
            Field renderPosX = RenderManager.class.getDeclaredField("renderPosX");
            Field renderPosY = RenderManager.class.getDeclaredField("renderPosY");
            Field renderPosZ = RenderManager.class.getDeclaredField("renderPosZ");

            renderPosX.setAccessible(true);
            renderPosY.setAccessible(true);
            renderPosZ.setAccessible(true);

            positions[0] = ((Double) renderPosX.get(renderManager)).floatValue();
            positions[1] = ((Double) renderPosY.get(renderManager)).floatValue();
            positions[2] = ((Double) renderPosZ.get(renderManager)).floatValue();
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            e.printStackTrace();
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_RED + "[LTJ] Failed to access render positions"));
        }
        return positions;
    }

    private void drawEndBox(float[] renderPos, Vec3 pos, boolean hit)
    {
        AxisAlignedBB box = new AxisAlignedBB(0, 0, 0, 0.25d, 0.25d, 0.25d);
        double renderX = (pos.xCoord - renderPos[0]) - (box.maxX / 2.0F);
        double renderY = (pos.yCoord - renderPos[1]) - (box.maxY / 2.0F);
        double renderZ = (pos.zCoord - renderPos[2]) - (box.maxZ / 2.0F);
        Color boxColor = hit ? boxColorHit : boxColorMiss;

        GL11.glPushMatrix();
        GL11.glTranslated(renderX, renderY, renderZ);
        GL11.glColor4f(boxColor.r, boxColor.g, boxColor.b, 0.25F);
        drawSolidBox(box);
        GL11.glColor4f(boxColor.r, boxColor.g, boxColor.b, 0.75F);
        drawOutlinedBox(box);
        GL11.glPopMatrix();
    }

    private void drawSolidBox(AxisAlignedBB bb)
    {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);

        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);

        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);

        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);

        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);

        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glEnd();
    }

    private void drawOutlinedBox(AxisAlignedBB bb)
    {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);

        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);

        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);

        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);

        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);

        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);

        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);

        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);

        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);

        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);

        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);

        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glEnd();
    }

    private static class Color
    {
        float r;
        float g;
        float b;
        float a;

        private Color(float r, float g, float b, float a)
        {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        private Color(float r, float g, float b)
        {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}
