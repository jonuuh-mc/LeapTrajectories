package io.jonuuh.leaptrajectories.event.render;

import io.jonuuh.leaptrajectories.Config;
import io.jonuuh.leaptrajectories.util.Color;
import io.jonuuh.leaptrajectories.util.Util;
import io.jonuuh.leaptrajectories.util.Vec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Renderer
{
    private final Minecraft mc;
    private final Color lineColor = Config.getLineColor();
    private final float playerH = 1.8F;
    private final float playerW = 0.6F;

    private List<Vec> lastPositions;
    private Set<BlockPos> blocksTraveledThrough;
    private double distTraveled = 0;
    private Vec lastFinalPos;
    private MovingObjectPosition lastHit;

    public Renderer()
    {
        this.mc = Minecraft.getMinecraft();
    }

    // TODO: rendergameoverlay fall dmg from leap (and dmg against entities in range?)
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Text event)
    {
        ScaledResolution sr = new ScaledResolution(mc);

//        for (ValidTargetPlayer validTargetPlayer : ValidTargetPlayerFactory.getValidTargetPlayers(mc, config))
//        {
//            String hpStr = Utilities.roundToHalf(validTargetPlayer.getHealth() / 2.0F) + "\u2764";
//            String hpStr = Float.toString(Utilities.roundToHalf(validTargetPlayer.getHealth()));
//            float scale = config.getRenderScale();

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) (sr.getScaledWidth() / 2), (float) (sr.getScaledHeight() / 2), 0.0F);
//            GlStateManager.translate(-getFontRenderer().getStringWidth(hpStr) * scale / 2.0F, -getFontRenderer().FONT_HEIGHT * scale, 0.0F); // insanity
//            GlStateManager.scale(scale, scale, scale);

//            GlStateManager.rotate(validTargetPlayer.getAngle(), 0.0F, 0.0F, 1.0F);
//            GlStateManager.translate(0, config.getRenderYOffset() * scale, 0.0F);
//            GlStateManager.rotate(-validTargetPlayer.getAngle(), 0.0F, 0.0F, 1.0F);

//            mc.getTextureManager().bindTexture(validTargetPlayer.getSkin());
//            Gui.drawScaledCustomSizeModalRect(getFontRenderer().getStringWidth(hpStr) / 4, getFontRenderer().FONT_HEIGHT, 8, 8, 8, 8, 12, 12, 64.0F, 64.0F);
        mc.fontRendererObj.drawStringWithShadow(String.valueOf(distTraveled), 0, 0, -1);
        GlStateManager.popMatrix();
//        }
        distTraveled = 0;
    }


    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        EntityPlayerSP player = mc.thePlayer;

//        if (!Util.isSpider(player))
//        {
//            return;
//        }

//        if (!Util.canLeap(player) && !Config.isLeaping())
//        {
//            lastPositions = null;
//            return;
//        }

        if (Config.isLeaping()) /*|| (Util.canLeap(player) && player.motionX == 0 && player.motionZ == 0 && Math.abs(player.motionY - (-0.0784F)) < 1E-9)*/ // TODO: account for rotation & reset if not held
        {
//            System.out.println("rendering last " + player.motionY);
            render(lastPositions, getRenderPosition(player, event.partialTicks), lastFinalPos, lastHit);
            return;
        }

        if (!Util.canLeap(player))
        {
            return;
        }

        // TODO: sprinting & !ground can be true without jumping (run off something) (use onLivingJump instead?)

//        blocksTraveledThrough = new HashSet<>();
        List<Vec> positions = new ArrayList<>();
        Vec renderPos = getRenderPosition(player, event.partialTicks);
        SimulatedPlayer simPlayer = getSimulatedPlayer(renderPos, player);
        float collisionY = 0;

        // Calculate trajectory (player's future positions)
        MovingObjectPosition hit = calcPositions(positions, simPlayer);
//        positions.add(simPlayer.getPosVec());

        lastPositions = positions;
        lastFinalPos = simPlayer.getPosVec();
        lastHit = hit;

        render(positions, renderPos, simPlayer.getPosVec(), hit);
    }

    private SimulatedPlayer getSimulatedPlayer(Vec renderPos, EntityPlayerSP player)
    {
        Vec leapMotion = Util.getLeapMotion(player);

        Vec initSimPlayerPos = new Vec(
                renderPos.x - (float) (Math.cos(Math.toRadians(player.rotationYaw)) * 0.0F),
                renderPos.y + playerH / 2.0F,
                renderPos.z - (float) (Math.sin(Math.toRadians(player.rotationYaw)) * 0.0F));

        Vec initSimPlayerMotion = new Vec(
                leapMotion.x /*+ (!player.isSprinting() && player.onGround ? (float) player.motionX : (float) (player.motionX + (Math.sin(player.rotationYaw * 0.017453292F) * 0.2F)))*/,
                leapMotion.y,
                leapMotion.z/* + (!player.isSprinting() && player.onGround ? (float) player.motionZ : (float) (player.motionZ - (Math.cos(player.rotationYaw * 0.017453292F) * 0.2F)))*/);

        return new SimulatedPlayer(mc, player, initSimPlayerPos, initSimPlayerMotion);
    }

    private MovingObjectPosition calcPositions(List<Vec> positions, SimulatedPlayer simPlayer)
    {
        while (simPlayer.getPosVec().y > 0.0F /*simPlayer.motion.x != 0.0*/)
        {
            Vec simPlayerPos = simPlayer.getPosVec();

            if (!positions.isEmpty())
            {
                Vec lastPos = positions.get(positions.size() - 1);
                distTraveled += Math.sqrt(Math.pow((lastPos.x - simPlayerPos.x), 2) +
                        Math.pow((lastPos.y - simPlayerPos.y), 2) +
                        Math.pow((lastPos.z - simPlayerPos.z), 2));
            }

            positions.add(simPlayerPos);

            AxisAlignedBB collisionBox = new AxisAlignedBB(
                    simPlayerPos.x - (playerW / 2), simPlayerPos.y - (playerH / 2), simPlayerPos.z - (playerW / 2),
                    simPlayerPos.x + (playerW / 2), simPlayerPos.y + (playerH / 2), simPlayerPos.z + (playerW / 2));

            simPlayer.move();

            // GL setup
            GL11.glPushMatrix();
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 0.0F, 0.0F, 0.33F);
            GL11.glLineWidth(3F);

            Vec renderPos = getRenderPosition(mc.thePlayer, 0.0F);
            GL11.glTranslatef(simPlayerPos.x - renderPos.x, simPlayerPos.y - renderPos.y, simPlayerPos.z - renderPos.z);
            drawOutlinedBox(collisionBox);
            // GL reset
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();

//            BlockPos airBlock = Util.isAirBlockCollision(mc.theWorld, collisionBox);
//            if (airBlock != null)
//            {
////                System.out.println(airBlock);
//                blocksTraveledThrough.add(airBlock);
//            }

//            if (!positions.isEmpty())
//            {
            Vec lastPos = simPlayerPos /*positions.get(positions.size() - 1)*/;
            Vec currPos = simPlayer.getPosVec();

            MovingObjectPosition movingObjectPosition = mc.theWorld.rayTraceBlocks(new Vec3(lastPos.x, lastPos.y, lastPos.z), new Vec3(currPos.x, currPos.y, currPos.z), true, true, true);

            if (movingObjectPosition != null)
            {
//                System.out.println(movingObjectPosition);
                return movingObjectPosition;
            }

            if (mc.theWorld.checkBlockCollision(collisionBox))
            {
                System.out.println("collision");
            }

//                distTraveled += Math.sqrt(Math.pow((lastPos.x - simPlayerPos.x), 2) +
//                        Math.pow((lastPos.y - simPlayerPos.y), 2) +
//                        Math.pow((lastPos.z - simPlayerPos.z), 2));
//            }

//          mc.theWorld.checkBlockCollision(collisionBox);
//            BlockPos collisionBlock = Util.isBlockCollision(mc.theWorld, collisionBox);
//
//            if (collisionBlock != null && !mc.thePlayer.getEntityBoundingBox().intersectsWith(collisionBox))
//            {
//                collisionY = collisionBlock.getY();
////                positions.add(new Vec(simPlayerPos.x, collisionY, simPlayerPos.z));
//                break;
//            }
        }
        return null;
    }

    private void render(List<Vec> positions, Vec renderPos, Vec finalPos, MovingObjectPosition hit)
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

        // Draw trajectory end box
        if (positions.size() > 1 && hit != null)
        {
            drawEndBox(renderPos, finalPos, hit);
        }

//        // Debug
//        for (Vec pos : positions)
//        {
//            GL11.glTranslatef(pos.x - renderPos.x, pos.y - renderPos.y, pos.z - renderPos.z);
//            GL11.glColor4f(0.0F, 0.0F, 1.0F, 0.25F);
//            drawOutlinedBox(new AxisAlignedBB(
//                    pos.x - (playerW / 2), pos.y - (playerH / 2), pos.z - (playerW / 2),
//                    pos.x + (playerW / 2), pos.y + (playerH / 2), pos.z + (playerW / 2)));
//        }

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

    private void drawEndBox(Vec renderPos, Vec finalPos, MovingObjectPosition hit)
    {
        AxisAlignedBB box = new AxisAlignedBB(0, 0, 0, playerW, playerH, playerW);
        EntityPlayer closestPlayer = mc.theWorld.getClosestPlayer(finalPos.x, finalPos.y, finalPos.z, 4);
        Color boxColor = closestPlayer != null && !(closestPlayer instanceof EntityPlayerSP) ? Config.getBoxColorHit() : Config.getBoxColorMiss();

//            GL11.glBegin(GL11.GL_LINE_LOOP);
//            GL11.glVertex2d(box.minX, box.minZ);
//            GL11.glVertex2d(box.minX, box.maxZ);
//            GL11.glVertex2d(box.maxX, box.maxZ);
//            GL11.glVertex2d(box.maxX, box.minZ);
//            GL11.glEnd();

        GL11.glPushMatrix();

        GL11.glTranslated(hit.hitVec.xCoord - renderPos.x, hit.hitVec.yCoord - renderPos.y, hit.hitVec.zCoord - renderPos.z);

        switch (hit.sideHit)
        {
            case UP:
            case DOWN:
                GL11.glTranslated(box.maxX / -2F, 0, box.maxZ / -2F);
                break;

            case WEST:
                GL11.glTranslated(0, box.maxY / -2F, box.maxZ / -2F);
                break;
            case EAST:
                GL11.glTranslated(-box.maxX, box.maxY / -2F, box.maxZ / -2F);
                break;

            case NORTH:
                GL11.glTranslated(box.maxX / -2F, box.maxY / -2F, 0);
                break;
            case SOUTH:
                GL11.glTranslated(box.maxX / -2F, box.maxY / -2F, -box.maxZ);
                break;

        }

        GL11.glColor4f(boxColor.r, boxColor.g, boxColor.b, 0.25F);
        drawSolidBox(box);

        GL11.glColor4f(boxColor.r, boxColor.g, boxColor.b, 0.50F);
        drawOutlinedBox(box);

        if (hit.sideHit == EnumFacing.UP)
        {
            drawOutlinedBox(new AxisAlignedBB(
                    -4 + (box.maxX / 2.0F), 0, -4 + (box.maxZ / 2.0F),
                    4 + (box.maxX / 2.0F), 0.2, 4 + (box.maxZ / 2.0F)));
        }

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
