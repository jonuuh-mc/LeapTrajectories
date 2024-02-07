package net.jonuuh.ltj.event.render;

import net.jonuuh.ltj.util.Vec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;

class SimulatedPlayer
{
    private final Minecraft mc;
    private final EntityPlayerSP player;
    private final Vec pos;
    private final Vec motion;
    private final float moveForward;
    private final float moveStrafe;
    private boolean onGround;

    SimulatedPlayer(Minecraft mc, EntityPlayerSP player, Vec initPos, Vec initMotion)
    {
        this.mc = mc;
        this.player = player;
        this.pos = initPos;
        this.motion = initMotion;
        this.moveForward = player.moveForward;
        this.moveStrafe = player.moveStrafing;
        this.onGround = player.onGround;
    }

    Vec getPosVec()
    {
        return pos.cloneVec();
    }

    /**
     * EntityLivingBase line 1589 moveEntityWithHeading
     */
    void move()
    {
        float xzFrict = (!onGround) ? 0.91F : mc.theWorld.getBlockState(new BlockPos(
                Math.floor(player.posX),
                Math.floor(player.getEntityBoundingBox().minY) - 1,
                Math.floor(player.posZ))).getBlock().slipperiness * 0.91F;

        float friction = (!onGround) ? player.jumpMovementFactor : player.getAIMoveSpeed() * (0.16277136F / (xzFrict * xzFrict * xzFrict));
        float f = moveStrafe * moveStrafe + moveForward * moveForward;

        if (f >= 1.0E-4F)
        {
            f = (float) Math.sqrt(f);
            f = friction / Math.max(f, 1.0F);
            double f1 = Math.sin(Math.toRadians(player.rotationYaw));
            double f2 = Math.cos(Math.toRadians(player.rotationYaw));

            motion.x += (double) ((moveStrafe * f) * f2 - (moveForward * f) * f1);
            motion.z += (double) ((moveForward * f) * f2 + (moveStrafe * f) * f1);
        }

        xzFrict = (!onGround) ? 0.91F : mc.theWorld.getBlockState(new BlockPos(
                Math.floor(player.posX),
                Math.floor(player.getEntityBoundingBox().minY) - 1,
                Math.floor(player.posZ))).getBlock().slipperiness * 0.91F;

        pos.x += motion.x;
        pos.y += motion.y;
        pos.z += motion.z;

        motion.y -= 0.08F;
        motion.y *= 0.9800000190734863F;
        motion.x *= xzFrict;
        motion.z *= xzFrict;
        onGround = false;
    }
}
