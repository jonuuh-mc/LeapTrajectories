package io.jonuuh.leaptrajectories;

import io.jonuuh.leaptrajectories.event.Controller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "leaptrajectories", version = "1.2.0", acceptedMinecraftVersions = "[1.8.9]")
public class LeapTrajectories
{
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final KeyBinding toggleKey;

    public LeapTrajectories()
    {
        this.toggleKey =  new KeyBinding("LTJToggle", Keyboard.KEY_L, "LTJ");
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        ClientRegistry.registerKeyBinding(toggleKey);
        MinecraftForge.EVENT_BUS.register(new Controller(mc, toggleKey));
    }
}
