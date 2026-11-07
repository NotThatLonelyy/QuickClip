package dev.quickclip;

import dev.quickclip.screen.FfmpegWelcomeScreen;
import dev.quickclip.screen.QuickClipHomeButton;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AchievementEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Keybindings and Forge event bridges for the fixed 1.8.9 client target.
 */
public final class QuickClipClientEvents {
    private static final String CATEGORY =
            "key.categories.quickclip.main";
    private static final int HOME_BUTTON_ID = 0x524543;
    private static final int[] FKEY_PRIORITY_ORDER = {
        Keyboard.KEY_F6,
        Keyboard.KEY_F7,
        Keyboard.KEY_F8,
        Keyboard.KEY_F9,
        Keyboard.KEY_F10,
        Keyboard.KEY_F11,
        Keyboard.KEY_F12,
        Keyboard.KEY_F4
    };
    private static final QuickClipClientEvents INSTANCE =
            new QuickClipClientEvents();

    private final AutoRecordManager autoRecordManager =
            new AutoRecordManager();

    private KeyBinding toggleRecording;
    private KeyBinding pauseResume;
    private KeyBinding openSettings;
    private KeyBinding openVideos;
    private KeyBinding addBookmark;
    private KeyBinding pushToTalk;
    private KeyBinding saveReplay;
    private KeyBinding toggleCensor;
    private KeyBinding openCensorEditor;
    private boolean missingFfmpegNoticeShown;
    private boolean ffmpegWelcomeCheckStarted;
    private volatile boolean worldUnloadCheckPending;
    private QuickClipHomeButton homeButton;
    private GuiScreen initializedMainMenu;

    private QuickClipClientEvents() {
    }

    public static void register() {
        INSTANCE.registerKeybindings();
        AutoClipManager.getInstance().initialize();
        FMLCommonHandler.instance().bus().register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    private void registerKeybindings() {
        QuickClipConfig config = QuickClipConfig.get();
        Set<Integer> claimedKeys = collectClaimedKeyBindings();
        openSettings = register(
                "key.quickclip.open_settings",
                resolveHotkeyDefault(
                    config.hotkeyOpenSettings,
                    QuickClipConfig.DEFAULT_HOTKEY_OPEN_SETTINGS,
                    claimedKeys));
        openVideos = register(
                "key.quickclip.open_video_collection",
                resolveHotkeyDefault(
                    config.hotkeyOpenVideoCollection,
                    QuickClipConfig.DEFAULT_HOTKEY_OPEN_VIDEO_COLLECTION,
                    claimedKeys));
        saveReplay = register(
                "key.quickclip.save_replay_buffer",
                config.hotkeySaveReplayBuffer);
    }

    private static KeyBinding register(String description, int keyCode) {
        KeyBinding binding = new KeyBinding(
                description,
                keyCode,
                CATEGORY);
        ClientRegistry.registerKeyBinding(binding);
        return binding;
    }

    private static Set<Integer> collectClaimedKeyBindings() {
        Set<Integer> claimed = new LinkedHashSet<Integer>();
        claimed.add(Integer.valueOf(Keyboard.KEY_F1));
        claimed.add(Integer.valueOf(Keyboard.KEY_F2));
        claimed.add(Integer.valueOf(Keyboard.KEY_F3));
        claimed.add(Integer.valueOf(Keyboard.KEY_F5));
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null
                    && minecraft.gameSettings != null
                    && minecraft.gameSettings.keyBindings != null) {
                for (KeyBinding binding
                        : minecraft.gameSettings.keyBindings) {
                    int code = binding.getKeyCode();
                    if (code != Keyboard.KEY_NONE) {
                        claimed.add(Integer.valueOf(code));
                    }
                }
            }
        } catch (Throwable throwable) {
            QuickClipMod.LOGGER.debug(
                "Could not inspect existing key bindings.",
                throwable);
        }
        return claimed;
    }

    private static int resolveHotkeyDefault(
            int configured,
            int compileDefault,
            Set<Integer> claimed) {
        if (configured == Keyboard.KEY_NONE
                && compileDefault == Keyboard.KEY_NONE) {
            return Keyboard.KEY_NONE;
        }
        if (configured != compileDefault) {
            if (configured != Keyboard.KEY_NONE) {
                claimed.add(Integer.valueOf(configured));
            }
            return configured;
        }
        if (compileDefault != Keyboard.KEY_NONE
                && !claimed.contains(Integer.valueOf(compileDefault))) {
            claimed.add(Integer.valueOf(compileDefault));
            return compileDefault;
        }
        for (int candidate : FKEY_PRIORITY_ORDER) {
            if (!claimed.contains(Integer.valueOf(candidate))) {
                claimed.add(Integer.valueOf(candidate));
                return candidate;
            }
        }
        return Keyboard.KEY_NONE;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (worldUnloadCheckPending) {
            worldUnloadCheckPending = false;
            if (minecraft.theWorld == null
                    || minecraft.currentScreen instanceof GuiMainMenu) {
                requestSessionExit("client world unload");
            }
        }
        ensureInitialMainMenuEvents(minecraft);
        RecordingManager manager = RecordingManager.getInstance();
        while (openSettings.isPressed()) {
            openScreen(
                    "dev.quickclip.screen.QuickClipSettingsScreen",
                    minecraft.currentScreen);
        }
        while (openVideos.isPressed()) {
            if (!openScreen(
                    "dev.quickclip.screen.ClipFoldersScreen",
                    minecraft.currentScreen)) {
                PlatformUtils.open(
                        QuickClipConfig.get().getOutputDirectory());
            }
        }
        while (saveReplay.isPressed()) {
            if (!invokeReplaySave()) {
                QuickClipMessages.send(
                        ChatCategory.REPLAY_BUFFER,
                        "Replay buffer is not active.");
            }
        }
        boolean pttHeld = pushToTalk != null
                && GameSettings.isKeyDown(pushToTalk);
        manager.setPushToTalkActive(pttHeld);
        autoRecordManager.onClientTick();
        ReplayCompatBridge.onClientTick(minecraft);
        AutoClipManager.getInstance().onClientTick();
        PerformanceMetrics.getInstance().onClientTick();
        PerformanceOptimizer.getInstance().onClientTick();

        if (!missingFfmpegNoticeShown
                && RecordingManager.isInGameState(minecraft)) {
            FfmpegBundleManager.Status status =
                    FfmpegBundleManager.getStatus();
            if (status == FfmpegBundleManager.Status.CHECKING
                    || status == FfmpegBundleManager.Status.DOWNLOADING) {
                return;
            }
            missingFfmpegNoticeShown = true;
            if (status != FfmpegBundleManager.Status.AVAILABLE) {
                QuickClipMessages.send(
                        ChatCategory.WARNINGS,
                        "FFmpeg is missing. Press "
                                + Keyboard.getKeyName(
                                        openSettings.getKeyCode())
                                + " to open QuickClip settings.");
            }
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            LiveEffectsRenderer.renderHud(event.resolution);
            RecordingOverlay.render(event.resolution);
        }
    }

    @SubscribeEvent
    public void onDrawScreenPost(
            GuiScreenEvent.DrawScreenEvent.Post event) {
        LiveEffectsRenderer.renderScreen(
                new net.minecraft.client.gui.ScaledResolution(
                        Minecraft.getMinecraft()));
        if (event.gui instanceof GuiMainMenu && homeButton != null) {
            homeButton.drawTooltip(
                    Minecraft.getMinecraft(),
                    event.mouseX,
                    event.mouseY);
        }
    }

    @SubscribeEvent
    public void onConnected(
            FMLNetworkEvent.ClientConnectedToServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                autoRecordManager.onConnected();
            }
        });
    }

    @SubscribeEvent
    public void onDisconnected(
            FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        requestSessionExit("Forge network disconnect");
    }

    /**
     * Forge's network event can be delivered after the client has stopped
     * accepting scheduled work. Handle it immediately when it already runs on
     * Minecraft's thread, and marshal only the genuine network-thread case.
     */
    private void requestSessionExit(final String trigger) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                RecordingManager manager =
                        RecordingManager.getInstance();
                if (manager.isRecording() || manager.isPaused()) {
                    QuickClipMod.LOGGER.info(
                            "Recording lifecycle exit detected: {}.",
                            trigger);
                }
                AutoClipManager.getInstance().reset();
                ReplayCompatBridge.resetPlaybackState();
                PerformanceMetrics.getInstance().reset();
                PerformanceOptimizer.getInstance().reset();
                autoRecordManager.onDisconnected();
            }
        };
        if (minecraft == null
                || minecraft.isCallingFromMinecraftThread()) {
            task.run();
        } else {
            minecraft.addScheduledTask(task);
        }
    }

    /**
     * A client-world unload is earlier and more deterministic than the network
     * callback when returning to the title screen. The manager state makes
     * duplicate Forge/world/menu notifications idempotent.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world == null || !event.world.isRemote) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null
                && minecraft.theWorld != null
                && minecraft.theWorld != event.world) {
            return;
        }
        /*
         * Forge posts Unload before Minecraft assigns the replacement world.
         * Checking on the next client tick prevents a dimension/server-world
         * replacement from looking like a trip to the title screen.
         */
        worldUnloadCheckPending = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerAttack(AttackEntityEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.entityPlayer == minecraft.thePlayer) {
            AutoClipManager.getInstance()
                    .onPlayerAttackEntity(event.target);
        }
    }

    @SubscribeEvent
    public void onAchievement(AchievementEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.entityPlayer != minecraft.thePlayer
                || event.achievement == null) {
            return;
        }
        AutoClipManager.getInstance().onAchievementEarned(
                event.achievement.getStatName().getUnformattedText());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.ammount <= 0.0F) {
            return;
        }
        AutoClipManager.getInstance().onLivingAttackCandidate(
                event.entityLiving,
                event.source,
                event.ammount);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null
                || event.source == null
                || event.source.getEntity() != minecraft.thePlayer
                || (!(event.entityLiving instanceof EntityDragon)
                    && !(event.entityLiving instanceof EntityWither))) {
            return;
        }
        AutoClipManager.getInstance().onBossKilled(
                event.entityLiving.getName());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMainMenuInitialized(
            GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiMainMenu)) {
            return;
        }

        RecordingManager manager = RecordingManager.getInstance();
        if (manager.isRecording() || manager.isPaused()) {
            requestSessionExit("title screen");
        }
        initializedMainMenu = event.gui;
        homeButton = null;
        QuickClipConfig config = QuickClipConfig.get();
        if (config.showHomeButton) {
            int x = event.gui.width / 2 + 104;
            int y = event.gui.height / 4 + 72;
            homeButton = new QuickClipHomeButton(
                    HOME_BUTTON_ID,
                    x,
                    y);
            event.buttonList.add(homeButton);
        }
        scheduleFfmpegWelcome(event.gui);
    }

    /**
     * Forge can construct and initialize the first title screen before client
     * mods receive FMLInitializationEvent. Reinitializing that same screen
     * once makes its normal Forge GUI events observable without replacing a
     * custom GuiMainMenu subclass installed by another mod.
     */
    private void ensureInitialMainMenuEvents(Minecraft minecraft) {
        GuiScreen current = minecraft == null
                ? null
                : minecraft.currentScreen;
        if (!(current instanceof GuiMainMenu)
                || current == initializedMainMenu
                || current.width <= 0
                || current.height <= 0) {
            return;
        }
        initializedMainMenu = current;
        current.setWorldAndResolution(
                minecraft,
                current.width,
                current.height);
    }

    @SubscribeEvent
    public void onGuiAction(
            GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.gui instanceof GuiMainMenu
                && event.button != null
                && event.button == homeButton
                && event.button.id == HOME_BUTTON_ID) {
            Minecraft.getMinecraft().displayGuiScreen(
                    new dev.quickclip.screen.ClipFoldersScreen(event.gui));
        }
    }

    private void scheduleFfmpegWelcome(final GuiScreen parent) {
        final QuickClipConfig config = QuickClipConfig.get();
        if (config.ffmpegFirstRunShown || ffmpegWelcomeCheckStarted) {
            return;
        }

        ffmpegWelcomeCheckStarted = true;
        CompletableFuture
                .supplyAsync(FfmpegBundleManager::detectFfmpeg)
                .whenComplete((status, failure) -> {
                    Minecraft minecraft = Minecraft.getMinecraft();
                    if (minecraft == null) {
                        ffmpegWelcomeCheckStarted = false;
                        return;
                    }
                    minecraft.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            if (failure == null
                                    && status != null
                                    && status.isFound()) {
                                config.ffmpegFirstRunShown = true;
                                config.save();
                                return;
                            }
                            GuiScreen current = minecraft.currentScreen;
                            if (current instanceof GuiMainMenu) {
                                minecraft.displayGuiScreen(
                                        new FfmpegWelcomeScreen(current));
                            } else {
                                ffmpegWelcomeCheckStarted = false;
                            }
                        }
                    });
                });
    }

    private static boolean openScreen(
            String className,
            GuiScreen parent) {
        try {
            Class<?> screenClass = Class.forName(className);
            if (parent != null && screenClass.isInstance(parent)) {
                return true;
            }
            Constructor<?> constructor =
                    screenClass.getConstructor(GuiScreen.class);
            Object screen = constructor.newInstance(parent);
            Minecraft.getMinecraft().displayGuiScreen(
                    (GuiScreen) screen);
            return true;
        } catch (ClassNotFoundException missing) {
            QuickClipMessages.send(
                    ChatCategory.GENERAL,
                    "That QuickClip screen is not available yet.");
            return false;
        } catch (Throwable throwable) {
            QuickClipMod.LOGGER.warn(
                    "Unable to open QuickClip screen {}.",
                    className,
                    throwable);
            return false;
        }
    }

    private static boolean invokeReplaySave() {
        try {
            Class<?> replayClass =
                    Class.forName("dev.quickclip.ReplayBuffer");
            Object instance = replayClass
                    .getMethod("getInstance")
                    .invoke(null);
            Boolean active = (Boolean) replayClass
                    .getMethod("isActive")
                    .invoke(instance);
            if (!active.booleanValue()) return false;
            replayClass.getMethod("saveBuffer").invoke(instance);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
