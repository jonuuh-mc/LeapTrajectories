package net.jonuuh.ltj.event.render;

import net.jonuuh.ltj.Config;
import net.jonuuh.ltj.util.Color;
import net.jonuuh.ltj.util.Util;
import net.jonuuh.ltj.util.Vec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class Renderer
{
    private final Minecraft mc;
    private final Color lineColor = Config.getLineColor();
    private final float playerH = 1.8F;
    private final float playerW = 0.6F;

    private List<Vec> lastPositions;
    private Vec lastFinalPos;
    private int lastCollisionY;

    public Renderer(Minecraft mc)
    {
        this.mc = mc;
    }

    // TODO: rendergameoverlay fall dmg from leap (and dmg against entities in range?)

    // should be called 100 times a second (5x tick speed, every 10ms)
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        EntityPlayerSP player = mc.thePlayer;

//        if (!Util.isSpider(player))
//        {
//            return;
//        }

        if (Config.isLeaping())
        {
            render(lastPositions, getRenderPosition(player, event.partialTicks), lastFinalPos, lastCollisionY);
            return;
        }

        if (!Util.canLeap(player))
        {
            return;
        }

        // TODO: sprinting & !ground can be true without jumping (run off something) (use onLivingJump instead?)
        List<Vec> positions = new ArrayList<>();
        Vec renderPos = getRenderPosition(player, event.partialTicks);
        Vec leapMotion = Util.getLeapMotion(player);
        Vec initSimPlayerPos = new Vec(
                renderPos.x - (float) (Math.cos(Math.toRadians(player.rotationYaw)) * 0.16F),
                renderPos.y + playerH / 2.0F,
                renderPos.z - (float) (Math.sin(Math.toRadians(player.rotationYaw)) * 0.16F));
        Vec initSimPlayerMotion = new Vec(
                leapMotion.x /*+ (!player.isSprinting() && player.onGround ? (float) player.motionX : (float) (player.motionX + (Math.sin(player.rotationYaw * 0.017453292F) * 0.2F)))*/,
                leapMotion.y,
                leapMotion.z/* + (!player.isSprinting() && player.onGround ? (float) player.motionZ : (float) (player.motionZ - (Math.cos(player.rotationYaw * 0.017453292F) * 0.2F)))*/);

        SimulatedPlayer simPlayer = new SimulatedPlayer(mc, player, initSimPlayerPos, initSimPlayerMotion);
        int collisionY = 0;

        // Calculate trajectory (player's future positions)
        while (simPlayer.getPosVec().y > 0.0F)
        {
            Vec simPlayerPos = simPlayer.getPosVec();
            positions.add(simPlayerPos);

            AxisAlignedBB collisionBox = new AxisAlignedBB(
                    simPlayerPos.x - (playerW / 2), simPlayerPos.y - (playerH / 2), simPlayerPos.z - (playerW / 2),
                    simPlayerPos.x + (playerW / 2), simPlayerPos.y + (playerH / 2), simPlayerPos.z + (playerW / 2));

            simPlayer.move();

            BlockPos collisionBlock = Util.isBlockCollision(mc.theWorld, collisionBox);
            if (collisionBlock != null && !mc.thePlayer.getEntityBoundingBox().intersectsWith(collisionBox))
            {
                collisionY = collisionBlock.getY();
                break;
            }
        }
//        positions.add(simPlayer.getPosVec());

        lastPositions = positions;
        lastFinalPos = simPlayer.getPosVec();
        lastCollisionY = collisionY;

        render(positions, renderPos, simPlayer.getPosVec(), collisionY);
    }

    private void render(List<Vec> positions, Vec renderPos, Vec finalPos, int collisionY)
    {
        // GL setup
        GL11.glPushMatrix();
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(lineColor.r, lineColor.g, lineColor.b, lineColor.a);
        GL11.glLineWidth(3F);

//        List<Vec> removePositions = new ArrayList<>();
//        for (int i = 0; i < positions.size() - 1; i++)
//        {
//            Vec oneThird = positions.get(i).multiply(1F / 3F).addVec(positions.get(i + 1).multiply(1 - (1F / 3F)));
//            Vec twoThird = positions.get(i).multiply(2F / 3F).addVec(positions.get(i + 1).multiply(1 - (2F / 3F)));
//
//            GL11.glColor4f(1.0F, 0.0F, 0.0F, 1.0F);
//
//            AxisAlignedBB oneThirdBox = new AxisAlignedBB(
//                    oneThird.x - (playerW / 2), oneThird.y - (playerH / 2), oneThird.z - (playerW / 2),
//                    oneThird.x + (playerW / 2), oneThird.y + (playerH / 2), oneThird.z + (playerW / 2));
//
//            AxisAlignedBB twoThirdBox = new AxisAlignedBB(
//                    twoThird.x - (playerW / 2), twoThird.y - (playerH / 2), twoThird.z - (playerW / 2),
//                    twoThird.x + (playerW / 2), twoThird.y + (playerH / 2), twoThird.z + (playerW / 2));
//
////            drawOutlinedBox(oneThirdBox);
////            drawOutlinedBox(twoThirdBox);
//
//            if (isBlockCollision(oneThirdBox) == null /*&& !mc.thePlayer.getEntityBoundingBox().intersectsWith(oneThirdBox)*/
//                    /*|| isBlockCollision(twoThirdBox) != null*/ /*&& !mc.thePlayer.getEntityBoundingBox().intersectsWith(twoThirdBox)*/)
//            {
////                System.out.println("collision at: " + oneThirdBox + " " + twoThirdBox);
////                for (int j = i; j < positions.size() - 1; j++)
////                {
////                    removePositions.add(positions.get(j));
////                }
////                break;
//                drawOutlinedBox(oneThirdBox);
////                drawOutlinedBox(twoThirdBox);
//            }
//        }

//        positions.removeAll(removePositions);

//        Vec lastPos = positions.remove(positions.size() - 1);

        // Draw trajectory line
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (Vec pos : positions)
        {
            GL11.glVertex3d(pos.x - renderPos.x, pos.y - renderPos.y, pos.z - renderPos.z);
        }
        GL11.glEnd();

//        // Debug
//        for (Vec pos : positions)
//        {
//            GL11.glColor4f(0.0F, 0.0F, 1.0F, 0.25F);
//            drawOutlinedBox(new AxisAlignedBB(
//                    pos.x - (playerW / 2), pos.y - (playerH / 2), pos.z - (playerW / 2),
//                    pos.x + (playerW / 2), pos.y + (playerH / 2), pos.z + (playerW / 2)));
//        }

        // Draw trajectory end box
        if (positions.size() > 1)
        {
            drawEndBox(renderPos, finalPos, collisionY);
        }

        // GL reset
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private Vec getRenderPosition(EntityPlayerSP player, float partialTicks)
    {
        return new Vec(
                (float) (player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks),
                (float) (player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks),
                (float) (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks));
    }

    private void drawEndBox(Vec renderPos, Vec finalPos, int collisionY)
    {
        AxisAlignedBB box = new AxisAlignedBB(0, 0, 0, playerW, playerH, playerW);
        EntityPlayer closestPlayer = mc.theWorld.getClosestPlayer(finalPos.x, finalPos.y, finalPos.z, 4);
        Color boxColor = closestPlayer != null && !(closestPlayer instanceof EntityPlayerSP) ? Config.getBoxColorHit() : Config.getBoxColorMiss();

        GL11.glPushMatrix();
        GL11.glTranslated((finalPos.x - renderPos.x) - (box.maxX / 2.0F), collisionY + 1 - renderPos.y, (finalPos.z - renderPos.z) - (box.maxZ / 2.0F));
        GL11.glColor4f(boxColor.r, boxColor.g, boxColor.b, 0.25F);
        drawSolidBox(box);
        GL11.glColor4f(boxColor.r, boxColor.g, boxColor.b, 0.50F);
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
        //////////
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);

        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);

        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);

        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        //////////
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
}
