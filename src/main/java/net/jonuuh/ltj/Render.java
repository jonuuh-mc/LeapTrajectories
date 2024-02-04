package net.jonuuh.ltj;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class Render
{
    private final Minecraft mc;
    private final KeyBinding leapKey;
    private final Color lineColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    private final Color boxColorHit = new Color(1.0F, 0.0F, 0.0F);
    private final Color boxColorMiss = new Color(0.0F, 1.0F, 0.0F);
    private final float playerW = 0.6F;
    private final float playerH = 1.8F;
    private final float leapRadius = 8.0F;
    private boolean isArrowMode = true;
    private boolean leaping = false;
    private List<Vec> lastPositions;
    private Vec lastFinalPos;
    private int lastCollisionY;

    public Render(Minecraft mc, KeyBinding leapKey)
    {
        this.mc = mc;
        this.leapKey = leapKey;
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event)
    {
        // TODO: reset leap mode when starting mw game ("the walls are about to come down" msg?)
        if (event.message.getUnformattedText().indexOf("Your primary Leap skill switched to") == 0)
        {
            isArrowMode = !isArrowMode;
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_GREEN + "[LTJ] switched leap mode to: " + isArrowMode));
        }
    }

    // TODO: rendergameoverlay fall dmg from leap (and dmg against entities in range?)
    // TODO: breadcrumbs to track real leap motion?

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || event.player != player || event.phase == TickEvent.Phase.END)
        {
            return;
        }

        if (player.onGround)
        {
            leaping = false;
        }

        Item heldItem = player.getHeldItem() != null ? player.getHeldItem().getItem() : null;

        if ((!leaping && player.experienceLevel == 100 && isArrowMode && mc.gameSettings.thirdPersonView != 2 && !player.isSneaking())
                && ((heldItem instanceof ItemBow && player.isSwingInProgress) || (heldItem instanceof ItemSword && player.isUsingItem())))
        {
//            Vec leapMotion = getLeapMotion(player);
//            player.addVelocity(leapMotion.x, leapMotion.y, leapMotion.z);
//            player.removeExperienceLevel(100);
            leaping = true;
        }
    }

    private Vec getLeapMotion(EntityPlayerSP player)
    {
        return new Vec(
                (float) -Math.sin(Math.toRadians(player.rotationYaw)) * (player.onGround ? 1.310375F : 2.183875F),
                (float) Math.max(Math.sin(-Math.toRadians(player.rotationPitch)) * 1.8815F, 0.686F),
                (float) Math.cos(Math.toRadians(player.rotationYaw)) * (player.onGround ? 1.310375F : 2.183875F));
    }

    // should be called 100 times a second (5x tick speed, every 10ms)
    @SubscribeEvent
    public void render(RenderWorldLastEvent event)
    {
        EntityPlayerSP player = mc.thePlayer;

        if (leaping)
        {
            doRender(lastPositions, getRenderPositions(player, event.partialTicks), lastFinalPos, lastCollisionY);
            return;
        }

        if (!canRenderTrajectory(player))
        {
            return;
        }

        List<Vec> positions = new ArrayList<>();
        Vec renderPos = getRenderPositions(player, event.partialTicks);
        Vec leapMotion = getLeapMotion(player);
        float strafe = player.moveStrafing;
        float forward = player.moveForward;
        int collisionY = 0;

        SimulatedPlayer simPlayer = new SimulatedPlayer(
                renderPos.x - (float) Math.cos(Math.toRadians(player.rotationYaw)) * 0.0F,
                renderPos.y + player.height / 2,
                renderPos.z - (float) Math.sin(Math.toRadians(player.rotationYaw)) * 0.0F,
                leapMotion.x,
                leapMotion.y + (!player.onGround ? player.motionY + 0.0784000015258789F : player.motionY),
                leapMotion.z,
                player.onGround);

        // Calculate trajectory (player's future positions)
        while (simPlayer.posY > 0.0F)
        {
            positions.add(simPlayer.getPosVec());

            AxisAlignedBB collisionBox = new AxisAlignedBB(
                    simPlayer.posX - (playerW / 2), simPlayer.posY - (playerH / 2), simPlayer.posZ - (playerW / 2),
                    simPlayer.posX + (playerW / 2), simPlayer.posY + (playerH / 2), simPlayer.posZ + (playerW / 2));

            simPlayer.move(strafe, forward);

            BlockPos collisionBlock = isBlockCollision(collisionBox);
            if (collisionBlock != null && !mc.thePlayer.getEntityBoundingBox().intersectsWith(collisionBox))
            {
                collisionY = collisionBlock.getY();
                break;
            }
        }

        lastPositions = positions;
        lastFinalPos = simPlayer.getPosVec();
        lastCollisionY = collisionY;

        doRender(positions, renderPos, simPlayer.getPosVec(), collisionY);
    }

    private void doRender(List<Vec> positions, Vec renderPos, Vec finalPos, int collisionY)
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
//            GL11.glColor4f(1.0F, 0.333F, 1.0F, 1.0F);
//            Vec pos = positions.get(positions.size() - 1);
//            drawOutlinedBox(new AxisAlignedBB(
//                    pos.x - (playerW / 2), pos.y - (playerH / 2), pos.z - (playerW / 2),
//                    pos.x + (playerW / 2), pos.y + (playerH / 2), pos.z + (playerW / 2)));
        }

        // GL reset
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private boolean canRenderTrajectory(EntityPlayerSP player)
    {
        if (!isArrowMode || player.experienceLevel != 100 || mc.gameSettings.thirdPersonView == 2 || player.isSneaking())
        {
            return false;
        }

        Item heldItem = player.getHeldItem() != null ? player.getHeldItem().getItem() : null;
        if (!(heldItem instanceof ItemBow) && !(heldItem instanceof ItemSword))
        {
            return false;
        }

        return true;
//        // TODO: find a more generic solution (any language?)
//        NBTTagCompound tagCompound = player.getHeldItem().getTagCompound();
//        if (tagCompound != null)
//        {
//            if (tagCompound.getTag("display") != null)
//            {
//                String display = tagCompound.getTag("display").toString();
//                if (display.contains("Name:"))
//                {
//                    return display.substring(display.indexOf("Name:")).contains("Spider");
//                }
//            }
//        }
//        return false;
    }

    private Vec getRenderPositions(EntityPlayerSP player, float partialTicks)
    {
        return new Vec(
                (float) (player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks),
                (float) (player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks),
                (float) (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks));
    }

    private void drawEndBox(Vec renderPos, Vec finalPos, int collisionY)
    {
        AxisAlignedBB playerBox = new AxisAlignedBB(0, 0, 0, playerW, playerH, playerW);
//        AxisAlignedBB leapBox = new AxisAlignedBB(0, 0, 0, leapRadius, 0.2, leapRadius);
        EntityPlayer closestPlayer = mc.theWorld.getClosestPlayer(finalPos.x, finalPos.y, finalPos.z, leapRadius / 2);
        Color boxColor = closestPlayer != null && !(closestPlayer instanceof EntityPlayerSP) ? boxColorHit : boxColorMiss;
        double renderX = (finalPos.x - renderPos.x) - (playerBox.maxX / 2.0F);
        double renderY = collisionY + 1 - renderPos.y;
        double renderZ = (finalPos.z - renderPos.z) - (playerBox.maxZ / 2.0F);

        GL11.glPushMatrix();
        GL11.glTranslated(renderX, renderY, renderZ);
        GL11.glColor4f(boxColor.r, boxColor.g, boxColor.b, 0.25F);
        drawSolidBox(playerBox);
        GL11.glColor4f(boxColor.r, boxColor.g, boxColor.b, 0.75F);
        drawOutlinedBox(playerBox);
//        GL11.glTranslated((-leapRadius / 2) + playerW / 2, 0, (-leapRadius / 2) + playerW / 2);
//        drawOutlinedHex(leapBox);
        GL11.glPopMatrix();
    }

    private BlockPos isBlockCollision(AxisAlignedBB bb)
    {
        int i = MathHelper.floor_double(bb.minX);
        int j = MathHelper.floor_double(bb.maxX);
        int k = MathHelper.floor_double(bb.minY);
        int l = MathHelper.floor_double(bb.maxY);
        int i1 = MathHelper.floor_double(bb.minZ);
        int j1 = MathHelper.floor_double(bb.maxZ);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for (int k1 = i; k1 <= j; ++k1)
        {
            for (int l1 = k; l1 <= l; ++l1)
            {
                for (int i2 = i1; i2 <= j1; ++i2)
                {
                    Block block = mc.theWorld.getBlockState(mutableBlockPos.set(k1, l1, i2)).getBlock();
                    BlockPos blockPos = new BlockPos(k1, l1, i2);

                    if (!block.isAir(mc.theWorld, blockPos) && block.getSelectedBoundingBox(mc.theWorld, blockPos).intersectsWith(bb))
                    {
                        return blockPos;
                    }
                }
            }
        }
        return null;
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

    // bb min positions must be 0
    private void drawOutlinedHex(AxisAlignedBB bb)
    {
        double thirdX = bb.maxX / 3;
        double thirdZ = bb.maxZ / 3;

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(bb.minX, bb.minY, thirdZ);
        GL11.glVertex3d(thirdX, bb.minY, bb.minZ);

        GL11.glVertex3d(thirdX, bb.minY, bb.minZ);
        GL11.glVertex3d(thirdX * 2, bb.minY, bb.minZ);

        GL11.glVertex3d(thirdX * 2, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, thirdZ);

        GL11.glVertex3d(bb.maxX, bb.minY, thirdZ);
        GL11.glVertex3d(bb.maxX, bb.minY, thirdZ * 2);

        GL11.glVertex3d(bb.maxX, bb.minY, thirdZ * 2);
        GL11.glVertex3d(thirdX * 2, bb.minY, bb.maxZ);

        GL11.glVertex3d(thirdX * 2, bb.minY, bb.maxZ);
        GL11.glVertex3d(thirdX, bb.minY, bb.maxZ);

        GL11.glVertex3d(thirdX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, thirdZ * 2);

        GL11.glVertex3d(bb.minX, bb.minY, thirdZ * 2);
        GL11.glVertex3d(bb.minX, bb.minY, thirdZ);
        //////////
        GL11.glVertex3d(bb.minX, bb.minY, thirdZ);
        GL11.glVertex3d(bb.minX, bb.maxY, thirdZ);

        GL11.glVertex3d(thirdX, bb.minY, bb.minZ);
        GL11.glVertex3d(thirdX, bb.maxY, bb.minZ);

        GL11.glVertex3d(thirdX * 2, bb.minY, bb.minZ);
        GL11.glVertex3d(thirdX * 2, bb.maxY, bb.minZ);

        GL11.glVertex3d(bb.maxX, bb.minY, thirdZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, thirdZ);

        GL11.glVertex3d(bb.maxX, bb.minY, thirdZ * 2);
        GL11.glVertex3d(bb.maxX, bb.maxY, thirdZ * 2);

        GL11.glVertex3d(thirdX * 2, bb.minY, bb.maxZ);
        GL11.glVertex3d(thirdX * 2, bb.maxY, bb.maxZ);

        GL11.glVertex3d(thirdX, bb.minY, bb.maxZ);
        GL11.glVertex3d(thirdX, bb.maxY, bb.maxZ);

        GL11.glVertex3d(bb.minX, bb.minY, thirdZ * 2);
        GL11.glVertex3d(bb.minX, bb.maxY, thirdZ * 2);
        //////////
        GL11.glVertex3d(bb.minX, bb.maxY, thirdZ);
        GL11.glVertex3d(thirdX, bb.maxY, bb.minZ);

        GL11.glVertex3d(thirdX, bb.maxY, bb.minZ);
        GL11.glVertex3d(thirdX * 2, bb.maxY, bb.minZ);

        GL11.glVertex3d(thirdX * 2, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, thirdZ);

        GL11.glVertex3d(bb.maxX, bb.maxY, thirdZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, thirdZ * 2);

        GL11.glVertex3d(bb.maxX, bb.maxY, thirdZ * 2);
        GL11.glVertex3d(thirdX * 2, bb.maxY, bb.maxZ);

        GL11.glVertex3d(thirdX * 2, bb.maxY, bb.maxZ);
        GL11.glVertex3d(thirdX, bb.maxY, bb.maxZ);

        GL11.glVertex3d(thirdX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, thirdZ * 2);

        GL11.glVertex3d(bb.minX, bb.maxY, thirdZ * 2);
        GL11.glVertex3d(bb.minX, bb.maxY, thirdZ);
        GL11.glEnd();
    }

    private static class Vec
    {
        private float x;
        private float y;
        private float z;

        private Vec(float x, float y, float z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void addX(float x)
        {
            this.x += x;
        }

        private void addY(float y)
        {
            this.y += y;
        }

        private void addZ(float z)
        {
            this.z += z;
        }

        private Vec addVec(Vec vec)
        {
            return new Vec(this.x + vec.x, this.y + vec.y, this.z + vec.z);
//            this.x += vec.x;
//            this.y += vec.y;
//            this.z += vec.z;
        }

        private Vec multiply(float num)
        {
            return new Vec(this.x * num, this.y * num, this.z * num);
//            this.x *= num;
//            this.y *= num;
//            this.z *= num;
        }

        public float getDistanceTo(Vec vec)
        {
            float dx = this.x - vec.x;
            float dy = this.y - vec.y;
            float dz = this.z - vec.z;
            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        public float length()
        {
            return (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        }
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

    private class SimulatedPlayer
    {
        private float posX;
        private float posY;
        private float posZ;
        private float motionX;
        private float motionY;
        private float motionZ;
        private boolean onGround;

        private SimulatedPlayer(double posX, double posY, double posZ, double motionX, double motionY, double motionZ, boolean onGround)
        {
            this.posX = (float) posX;
            this.posY = (float) posY;
            this.posZ = (float) posZ;
            this.motionX = (float) motionX;
            this.motionY = (float) motionY;
            this.motionZ = (float) motionZ;
            this.onGround = onGround;
        }

        private Vec getPosVec()
        {
            return new Vec(posX, posY, posZ);
        }

        /**
         * EntityLivingBase line 1589 moveEntityWithHeading
         */
        private void move(float strafe, float forward)
        {
            float xzFrict = (!onGround) ? 0.91F : mc.theWorld.getBlockState(new BlockPos(
                    Math.floor(mc.thePlayer.posX),
                    Math.floor(mc.thePlayer.getEntityBoundingBox().minY) - 1,
                    Math.floor(mc.thePlayer.posZ))).getBlock().slipperiness * 0.91F;

            float friction = (!onGround) ? mc.thePlayer.jumpMovementFactor : mc.thePlayer.getAIMoveSpeed() * (0.16277136F / (xzFrict * xzFrict * xzFrict));
            float f = strafe * strafe + forward * forward;

            if (f >= 1.0E-4F)
            {
                f = (float) Math.sqrt(f);

                if (f < 1.0F)
                {
                    f = 1.0F;
                }

                f = friction / f;
                strafe = strafe * f;
                forward = forward * f;
                double f1 = Math.sin(Math.toRadians(mc.thePlayer.rotationYaw));
                double f2 = Math.cos(Math.toRadians(mc.thePlayer.rotationYaw));
                motionX += (double) (strafe * f2 - forward * f1);
                motionZ += (double) (forward * f2 + strafe * f1);
            }

            xzFrict = (!onGround) ? 0.91F : mc.theWorld.getBlockState(new BlockPos(
                    Math.floor(mc.thePlayer.posX),
                    Math.floor(mc.thePlayer.getEntityBoundingBox().minY) - 1,
                    Math.floor(mc.thePlayer.posZ))).getBlock().slipperiness * 0.91F;

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            motionY -= 0.08F;
            motionY *= 0.9800000190734863F;
            motionX *= xzFrict;
            motionZ *= xzFrict;
            onGround = false;

//        if (mc.theWorld.isRemote && (!mc.theWorld.isBlockLoaded(new BlockPos((int)player.posX, 0, (int)player.posZ)) || !mc.theWorld.getChunkFromBlockCoords(new BlockPos((int)player.posX, 0, (int)player.posZ)).isLoaded()))
//        {
//            if (player.posY > 0.0D)
//            {
//                player.motionY = -0.1F;
//            }
//            else
//            {
//                player.motionY = 0.0F;
//            }
//        }
//        else
//        {
//            motionY -= 0.08F;
//        }
//        motionY -= 0.0784000015258789F;
        }
    }
}
