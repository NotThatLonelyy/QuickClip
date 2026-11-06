package dev.quickclip;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = QuickClipMod.MOD_ID,
    name = QuickClipMod.NAME,
    version = QuickClipMod.VERSION,
    acceptedMinecraftVersions = "[1.8.9]",
    clientSideOnly = true
)
public final class QuickClipMod {
    public static final String MOD_ID = "quickclip";
    public static final String NAME = "QuickClip";
    public static final String VERSION = BuildInfo.VERSION;
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @Mod.Instance(MOD_ID)
    public static QuickClipMod INSTANCE;

    @Mod.EventHandler
    public void preInitialize(FMLPreInitializationEvent event) {
        if (Loader.isModLoaded("recordable")) {
            throw new IllegalStateException(
                    "Both QuickClip and the old Record-able Forge port are "
                            + "installed. Remove the recordable-*.jar before "
                            + "launching QuickClip.");
        }
        java.io.File gameDir =
            event.getModConfigurationDirectory().getParentFile();
        java.io.File quickClipConfigDir =
            new java.io.File(new java.io.File(gameDir, "QuickClip"), "config");
        quickClipConfigDir.mkdirs();
        QuickClipConfig.initialize(gameDir, quickClipConfigDir);
        LOGGER.info("QuickClip Forge 1.8.9 configuration loaded from {}",
            QuickClipConfig.getConfigPath());
    }

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        ModCompatibilityChecker.checkAndLog();
        RecordingManager.getInstance().initialize();
        QuickClipClientEvents.register();
        DefaultWatermark.ensureSeeded();
        ClipLibrary.ensureLayout(QuickClipConfig.get());
        LOGGER.info("QuickClip Forge 1.8.9 initialized.");
        Runtime.getRuntime().addShutdownHook(new Thread(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        LOGGER.info(
                                "JVM shutdown hook checking QuickClip media.");
                        RecordingManager.getInstance().emergencyShutdown();
                    } catch (Throwable throwable) {
                        LOGGER.error(
                            "Emergency recording shutdown failed.",
                            throwable);
                    } finally {
                        QuickClipConfig.get().save();
                    }
                }
            },
            "QuickClip-ConfigShutdown"));
    }
}
