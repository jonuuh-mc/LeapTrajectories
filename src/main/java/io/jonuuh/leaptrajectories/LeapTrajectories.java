package io.jonuuh.leaptrajectories;

import io.jonuuh.leaptrajectories.event.PlayerEvents;
import io.jonuuh.leaptrajectories.event.render.Renderer;
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
    @EventHandler
    public void FMLInit(FMLInitializationEvent event)
    {
        KeyBinding toggleKey =  new KeyBinding("debug", Keyboard.KEY_L, "LTJ");
        ClientRegistry.registerKeyBinding(toggleKey);

        MinecraftForge.EVENT_BUS.register(new Renderer());
        MinecraftForge.EVENT_BUS.register(new PlayerEvents(toggleKey));
    }
}
