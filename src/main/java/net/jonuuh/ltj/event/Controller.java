package net.jonuuh.ltj.event;

import net.jonuuh.ltj.event.render.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

public class Controller
{
    private final Minecraft mc;
    private final KeyBinding toggleKey;
    private final Renderer renderer;
    private final PlayerEvents playerEvents;
    private boolean toggled = true;

    public Controller(Minecraft mc, KeyBinding toggleKey)
    {
        this.mc = mc;
        this.toggleKey = toggleKey;
        this.renderer = new Renderer(mc);
        this.playerEvents = new PlayerEvents(mc);
        MinecraftForge.EVENT_BUS.register(renderer);
        MinecraftForge.EVENT_BUS.register(playerEvents);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (toggleKey.isPressed())
        {
            if (toggled)
            {
                MinecraftForge.EVENT_BUS.unregister(renderer);
                MinecraftForge.EVENT_BUS.unregister(playerEvents);
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_BLUE + "[LTJ] Unregistered"));
            }
            else
            {
                MinecraftForge.EVENT_BUS.register(renderer);
                MinecraftForge.EVENT_BUS.register(playerEvents);
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_BLUE + "[LTJ] Registered"));
            }
            toggled = !toggled;

//            Vec leapMotion = Util.getLeapMotion(mc.thePlayer);
//            mc.thePlayer.setVelocity(leapMotion.x, leapMotion.y, leapMotion.z);
//            Config.setLeaping(true);
        }
    }
}
