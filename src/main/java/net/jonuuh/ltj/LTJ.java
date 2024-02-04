package net.jonuuh.ltj;

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
        version = "1.0.0",
        acceptedMinecraftVersions = "[1.8.9]"
)
public class LTJ
{
    private final Minecraft mc = Minecraft.getMinecraft();
    private final KeyBinding leapKey;

    public LTJ()
    {
        this.leapKey =  new KeyBinding("LTJLeap", Keyboard.KEY_L, "LTJ");;
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        ClientRegistry.registerKeyBinding(leapKey);
        MinecraftForge.EVENT_BUS.register(new Render(mc, leapKey));
//        MinecraftForge.EVENT_BUS.register(new Misc(mc));
    }
}
