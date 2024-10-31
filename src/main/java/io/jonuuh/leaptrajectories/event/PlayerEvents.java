package io.jonuuh.leaptrajectories.event;

import io.jonuuh.leaptrajectories.Config;
import io.jonuuh.leaptrajectories.util.Util;
import io.jonuuh.leaptrajectories.util.Vec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemSpade;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PlayerEvents
{
    private final Minecraft mc;

    public PlayerEvents(Minecraft mc)
    {
        this.mc = mc;
    }

    @SubscribeEvent
    public void onClientChat(ClientChatReceivedEvent event)
    {
        String msg = event.message.getUnformattedText();
        // TODO: find a better way to detect a new game?
        if (msg.matches(".+6.+\\.")) // The walls fall in 6 minutes.
        {
            Config.setArrowMode(true);
            Util.addChat(mc.thePlayer, EnumChatFormatting.BLUE, "[LTJ] reset isArrowMode: " + Config.isArrowMode());
        }
    }

    // TODO: right clicking chests does not flip mode
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.entityPlayer != mc.thePlayer || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR /*|| !Util.isSpider(mc.thePlayer)*/)
        {
            return;
        }

        if (Util.getHeldItem(mc.thePlayer) instanceof ItemSpade)
        {
            Config.flipArrowMode();
            Util.addChat(mc.thePlayer, EnumChatFormatting.BLUE, "[LTJ] isArrowMode set to: " + Config.isArrowMode());
        }
    }

    private Vec lastPreTickMotion = new Vec();
    private int lastPreTickLevel = 0;
    private int lastExpLevelDiff = 0;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.player != mc.thePlayer || event.phase != TickEvent.Phase.START /*|| !Util.isSpider(mc.thePlayer)*/)
        {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        if (lastExpLevelDiff != -100)
        {
            lastExpLevelDiff = player.experienceLevel - lastPreTickLevel;
        }

        if (Config.isLeaping() && player.onGround)
        {
            lastExpLevelDiff = 0;
            Config.setLeaping(false);
            Util.addChat(player, EnumChatFormatting.BLUE, "[LTJ] stopped leap: "
                    + lastPreTickMotion + " " + player.experienceLevel + " " + event.phase + " " + player.onGround + " " + lastExpLevelDiff);
        }

        if (!Config.isLeaping() && !player.onGround && lastExpLevelDiff == -100 && Config.isArrowMode() && Util.usingLeapItem(player) && !player.isSneaking())
        {
            Config.setLeaping(true);
            // TODO: grabbing leap motion like this won't work if player is already airborne before leaping
            // TODO: try detecting a leap motion directly? (motion.dot(lookVec) = 1 && some threshold for minimum motion?)
            Util.addChat(player, EnumChatFormatting.BLUE, "[LTJ] started leap: "
                    + lastPreTickMotion + " " + player.experienceLevel + " " + event.phase + " " + player.onGround + " " + lastExpLevelDiff);
        }

        lastPreTickMotion = new Vec(player.motionX, player.motionY, player.motionZ);
        lastPreTickLevel = player.experienceLevel;
    }
}
