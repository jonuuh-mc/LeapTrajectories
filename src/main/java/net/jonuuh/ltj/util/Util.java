package net.jonuuh.ltj.util;

import net.jonuuh.ltj.Config;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

public class Util
{
    public static void addChat(EntityPlayerSP player, EnumChatFormatting color, String msg)
    {
        player.addChatMessage(new ChatComponentText(color + msg));
    }

    public static boolean isSpider(EntityPlayerSP player)
    {
        // TODO: doesn't work in mw duels
        return player.getDisplayName().getUnformattedText().contains("[SPI]");
    }

    public static boolean holdingLeapItem(EntityPlayerSP player)
    {
        Item heldItem = getHeldItem(player);
        return heldItem instanceof ItemBow || heldItem instanceof ItemSword;
    }

    public static boolean usingLeapItem(EntityPlayerSP player)
    {
        Item heldItem = getHeldItem(player);
        return (heldItem instanceof ItemBow && player.isSwingInProgress) || (heldItem instanceof ItemSword && player.isUsingItem());
    }

    public static boolean canLeap(EntityPlayerSP player)
    {
        return holdingLeapItem(player) && Config.isArrowMode() && !player.isSneaking() && player.experienceLevel == 100;
    }

    public static Item getHeldItem(EntityPlayerSP player)
    {
        return player.getHeldItem() != null ? player.getHeldItem().getItem() : null;
    }

    public static Vec getLeapMotion(EntityPlayerSP player)
    {
        // TODO: rotation pitch motion probably a bit off
        // TODO: x, z, and pitch numbers depend on ping (some number based on ping / 8000.0) (NetHandlerPlayClient.java line 494 handleEntityVelocity?)
        // TODO: grab player motion on leap and use as future leap motion (also for current leap trajectory? correct it midair?)
        return new Vec(
                (float) -Math.sin(Math.toRadians(player.rotationYaw)) * (player.onGround ? 1.310375F : 2.183875F),
                (float) Math.max(Math.sin(-Math.toRadians(player.rotationPitch)) * 1.8815F, 0.686F),
                (float) Math.cos(Math.toRadians(player.rotationYaw)) * (player.onGround ? 1.310375F : 2.183875F));
    }

    public static BlockPos isBlockCollision(WorldClient world, AxisAlignedBB bb)
    {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for (int x = MathHelper.floor_double(bb.minX); x <= MathHelper.floor_double(bb.maxX); ++x)
        {
            for (int y = MathHelper.floor_double(bb.minY); y <= MathHelper.floor_double(bb.maxY); ++y)
            {
                for (int z = MathHelper.floor_double(bb.minZ); z <= MathHelper.floor_double(bb.maxZ); ++z)
                {
                    Block block = world.getBlockState(mutableBlockPos.set(x, y, z)).getBlock();
                    BlockPos blockPos = new BlockPos(x, y, z);

                    if (!block.isAir(world, blockPos) && block.getSelectedBoundingBox(world, blockPos).intersectsWith(bb))
                    {
                        return blockPos;
                    }
                }
            }
        }
        return null;
    }
}
