package net.jonuuh.ltj;

import net.jonuuh.ltj.event.Controller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.lwjgl.input.Keyboard;

@Mod(
        modid = "ltj",
        version = "1.1.0",
        acceptedMinecraftVersions = "[1.8.9]"
)
public class LTJ
{
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final KeyBinding toggleKey;

    public LTJ()
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
