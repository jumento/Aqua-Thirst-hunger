package mx.jume.aquahud;

import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.PluginBase;

/**
 * Punto de entrada unico para todos los mods de jume.
 * Encapsula deteccion de gestores externos y routing de operaciones HUD.
 *
 * Arquitectura PLANA: solo UN container por jugador. El segundo mod
 * detecta el container existente via getCustomHud() + getSimpleName()
 * y se une como hijo directo via reflexion cross-classloader.
 */
public final class AquaHudBridge {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static volatile Mode mode = null;
    private static final Object detectLock = new Object();

    // MHUD real (modo DELEGATED)
    private static Object mhudInstance = null;
    private static Method mhudSetCustomHud = null;
    private static Method mhudHideCustomHud = null;

    // AutoMultiHud JAR override (modo TRANSPARENT)
    // Referencia cacheada al mapa PlayerPacketTracker.incomingJarOverride.
    @SuppressWarnings("unchecked")
    private static Map<String, String> amhJarOverrideMap = null;
    // JAR path del mod que invocó detect(). Cada classloader tiene su propia copia.
    private static String ownJarPath = null;

    // Cache cross-classloader para container externo (modo COORDINATED, flat join)
    // Cuando nos unimos al container de otro mod, cacheamos la referencia y Methods.
    static class ExternalCache {
        CustomUIHud container;
        Method addMethod;
        Method updateHudReferenceMethod;
        Method updateChildMethod;
        Method suppressAndResetMethod;
        Method showMethod;
    }

    private static final java.util.Map<PlayerRef, ExternalCache> externalCacheMap =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static final Field COMMANDS_FIELD;
    static {
        try {
            COMMANDS_FIELD = UICommandBuilder.class.getDeclaredField("commands");
            COMMANDS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Cannot access UICommandBuilder.commands", e);
        }
    }

    public enum Mode {
        TRANSPARENT,  // AutoMultiHud presente — cada mod actúa independiente
        DELEGATED,    // MHUD real presente — delegar registro a MHUD
        COORDINATED   // Ningún gestor externo — AquaMultiHUD propio
    }

    public static Mode getMode() {
        return mode;
    }

    // ========== DETECCIÓN ==========

    private static void setJarOverride(PlayerRef playerRef, String hudIdentifier) {
        if (amhJarOverrideMap == null || ownJarPath == null) return;
        amhJarOverrideMap.put(playerRef.getUsername(), ownJarPath + "#" + hudIdentifier);
    }

    public static void detect(PluginBase plugin) {
        if (ownJarPath == null) {
            try {
                CodeSource src = plugin.getClass().getProtectionDomain().getCodeSource();
                if (src != null) {
                    ownJarPath = src.getLocation().getFile();
                    // LOGGER.at(Level.INFO).log("[AquaHudBridge] Own JAR path: " + ownJarPath);
                }
            } catch (Exception e) {
                // LOGGER.at(Level.WARNING).log("[AquaHudBridge] Failed to resolve own JAR path: " + e.getMessage());
            }
        }

        if (mode != null) return;
        synchronized (detectLock) {
            if (mode != null) return;

            // Paso 1: AutoMultiHud
            try {
                Class.forName("com.dairymoose.auto_multi_hud.AutoMultiHud");
                cacheAmhJarOverride();
                mode = Mode.TRANSPARENT;
                // LOGGER.at(Level.INFO).log("[AquaHudBridge] Mode: TRANSPARENT (AutoMultiHud detected)");
                return;
            } catch (ClassNotFoundException ignored) {}

            // Paso 2: MHUD real via PluginManager
            try {
                com.hypixel.hytale.common.plugin.PluginIdentifier mhudId =
                        com.hypixel.hytale.common.plugin.PluginIdentifier.fromString("Buuz135:MultipleHUD");
                if (com.hypixel.hytale.server.core.plugin.PluginManager.get().getPlugin(mhudId) != null) {
                    cacheMhudReflection();
                    mode = Mode.DELEGATED;
                    // LOGGER.at(Level.INFO).log("[AquaHudBridge] Mode: DELEGATED (MHUD detected via PluginManager)");
                    return;
                }
            } catch (Exception ignored) {}

            // Paso 3: Nada externo
            mode = Mode.COORDINATED;
            // LOGGER.at(Level.INFO).log("[AquaHudBridge] Mode: COORDINATED (no external HUD manager)");
        }
    }

    @SuppressWarnings("unchecked")
    private static void cacheAmhJarOverride() {
        try {
            Class<?> trackerClass = Class.forName("com.dairymoose.auto_multi_hud.PlayerPacketTracker");
            Field field = trackerClass.getField("incomingJarOverride");
            amhJarOverrideMap = (Map<String, String>) field.get(null);
            // LOGGER.at(Level.INFO).log("[AquaHudBridge] Cached AutoMultiHud incomingJarOverride map");
        } catch (Exception e) {
            amhJarOverrideMap = null;
            // LOGGER.at(Level.WARNING).log("[AquaHudBridge] Failed to cache AMH jarOverride: " + e.getMessage());
        }
    }

    public static void recheckMhud() {
        if (mode != Mode.COORDINATED) return;
        try {
            com.hypixel.hytale.common.plugin.PluginIdentifier mhudId =
                    com.hypixel.hytale.common.plugin.PluginIdentifier.fromString("Buuz135:MultipleHUD");
            if (com.hypixel.hytale.server.core.plugin.PluginManager.get().getPlugin(mhudId) != null) {
                if (!AquaMultiHUD.hasChildren()) {
                    cacheMhudReflection();
                    mode = Mode.DELEGATED;
                    // LOGGER.at(Level.INFO).log("[AquaHudBridge] Late-detected MHUD — switching to DELEGATED");
                }
            }
        } catch (Exception ignored) {}
    }

    private static void cacheMhudReflection() {
        try {
            Class<?> mhudClass = Class.forName("com.buuz135.mhud.MultipleHUD");
            mhudInstance = mhudClass.getMethod("getInstance").invoke(null);
            mhudSetCustomHud = mhudClass.getMethod("setCustomHud",
                    Player.class, PlayerRef.class, String.class, CustomUIHud.class);
            mhudHideCustomHud = mhudClass.getMethod("hideCustomHud", Player.class, String.class);
        } catch (Exception e) {
            // LOGGER.at(Level.WARNING).log("[AquaHudBridge] Failed to cache MHUD reflection: " + e.getMessage());
            mhudInstance = null;
            mhudSetCustomHud = null;
            mhudHideCustomHud = null;
        }
    }

    // ========== Cache cross-classloader ==========

    /**
     * Cachea reflexion sobre el container externo (de otro classloader).
     * Se llama en register() cuando detectamos container existente.
     */
    static void cacheExternalContainer(PlayerRef playerRef, CustomUIHud container) {
        try {
            Class<?> clazz = container.getClass();
            ExternalCache cache = new ExternalCache();
            cache.container = container;
            cache.addMethod = clazz.getMethod("add", String.class, CustomUIHud.class);
            cache.updateHudReferenceMethod = clazz.getMethod("updateHudReference", String.class, CustomUIHud.class);
            cache.updateChildMethod = clazz.getMethod("updateChild", String.class, UICommandBuilder.class);
            cache.suppressAndResetMethod = clazz.getMethod("suppressAndReset", long.class);
            cache.showMethod = clazz.getMethod("show");
            externalCacheMap.put(playerRef, cache);
            // LOGGER.at(Level.INFO).log("[AquaHudBridge] Cached external container for " + playerRef.getUsername() + ": " + clazz.getName());
        } catch (Exception e) {
            // LOGGER.at(Level.WARNING).log("[AquaHudBridge] Failed to cache external container for " + playerRef.getUsername() + ": " + e.getMessage());
        }
    }

    // ========== REGISTRO ==========

    public static void register(Player player, PlayerRef playerRef, String hudIdentifier, CustomUIHud hud) {
        if (mode == null) {
            // LOGGER.at(Level.WARNING).log("[AquaHudBridge] register() called before detect()!");
            return;
        }

        switch (mode) {
            case TRANSPARENT:
                setJarOverride(playerRef, hudIdentifier);
                player.getHudManager().setCustomHud(playerRef, hud);
                break;

            case DELEGATED:
                try {
                    mhudSetCustomHud.invoke(mhudInstance, player, playerRef, hudIdentifier, hud);
                } catch (Exception e) {
                    // LOGGER.at(Level.WARNING).log("[AquaHudBridge] MHUD register failed: " + e.getMessage());
                }
                break;

            case COORDINATED:
                // FLAT JOIN: detectar si ya existe un AquaMultiHUD como CustomHud actual
                CustomUIHud current = player.getHudManager().getCustomHud();
                if (current != null && current.getClass().getSimpleName().equals("AquaMultiHUD")) {
                    // Otro mod ya creó el container. Unirse como hijo via reflexión.
                    try {
                        Method addMethod = current.getClass().getMethod("add", String.class, CustomUIHud.class);
                        addMethod.invoke(current, hudIdentifier, hud);
                        // Cachear para updates futuros
                        cacheExternalContainer(playerRef, current);
                        // LOGGER.at(Level.INFO).log("[AquaHudBridge] FLAT JOIN: registered '" + hudIdentifier
                                // + "' in external container for " + playerRef.getUsername());
                    } catch (Exception e) {
                        // LOGGER.at(Level.WARNING).log("[AquaHudBridge] FLAT JOIN failed, creating own container: " + e.getMessage());
                        AquaMultiHUD.getOrCreate(player, playerRef).add(hudIdentifier, hud);
                    }
                } else {
                    // Somos el primer mod. Crear nuestro container.
                    AquaMultiHUD.getOrCreate(player, playerRef).add(hudIdentifier, hud);
                }
                break;
        }
    }

    // ========== RE-REGISTRO (World Travel) ==========

    /**
     * Re-registra un HUD existente sin crear nueva instancia.
     * Para world travel: el container persiste, el DOM fue destruido.
     */
    public static void reRegister(Player player, PlayerRef playerRef, String hudIdentifier, CustomUIHud hud) {
        if (mode == null) return;

        switch (mode) {
            case TRANSPARENT:
                setJarOverride(playerRef, hudIdentifier);
                hud.show();
                break;

            case DELEGATED:
                try {
                    mhudSetCustomHud.invoke(mhudInstance, player, playerRef, hudIdentifier, hud);
                } catch (Exception e) {
                    // LOGGER.at(Level.WARNING).log("[AquaHudBridge] MHUD reRegister failed: " + e.getMessage());
                }
                break;

            case COORDINATED:
                ExternalCache cache = externalCacheMap.get(playerRef);
                if (cache != null) {
                    // FLAT JOIN: Actualizar referencia sin enviar paquetes
                    try {
                        Method method = cache.updateHudReferenceMethod != null ? cache.updateHudReferenceMethod : cache.addMethod;
                        method.invoke(cache.container, hudIdentifier, hud);
                    } catch (Exception e) {
                        // LOGGER.at(Level.WARNING).log("[AquaHudBridge] External reRegister failed: " + e.getMessage());
                    }
                } else {
                    AquaMultiHUD container = AquaMultiHUD.getContainer(playerRef);
                    if (container != null && container.isActivated()) {
                        // Actualizar referencia sin enviar paquetes redundantes
                        container.updateHudReference(hudIdentifier, hud);
                        
                        // Si el motor desregistró el container, re-registrar
                        if (player.getHudManager().getCustomHud() != container) {
                            container.player = player;
                            player.getHudManager().setCustomHud(playerRef, container);
                            // Marcar como enviado para que el rebuildDeferred posterior sea debouncado
                            container.markShowSent();
                        }
                    } else {
                        register(player, playerRef, hudIdentifier, hud);
                    }
                }
                break;
        }
    }

    // ========== UPDATE ==========

    @SuppressWarnings("unchecked")
    public static void update(PlayerRef playerRef, String hudIdentifier, UICommandBuilder commands, CustomUIHud hud) {
        if (mode == null) return;

        switch (mode) {
            case TRANSPARENT:
                setJarOverride(playerRef, hudIdentifier);
                hud.update(false, commands);
                break;

            case DELEGATED:
                try {
                    List<CustomUICommand> cmds = (List<CustomUICommand>) COMMANDS_FIELD.get(commands);
                    String normalizedId = hudIdentifier.replaceAll("[^a-zA-Z0-9]", "");
                    String prefix = "#MultipleHUD #" + normalizedId;
                    for (CustomUICommand cmd : cmds) {
                        if (cmd.selector == null) {
                            cmd.selector = prefix;
                        } else {
                            cmd.selector = prefix + " " + cmd.selector;
                        }
                    }
                } catch (IllegalAccessException e) {
                    // LOGGER.at(Level.SEVERE).log("[AquaHudBridge] Prefix error: " + e.getMessage());
                }
                hud.update(false, commands);
                break;

            case COORDINATED:
                ExternalCache cache = externalCacheMap.get(playerRef);
                if (cache != null && cache.updateChildMethod != null) {
                    // Somos invitados — routing via reflexión cacheada
                    try {
                        cache.updateChildMethod.invoke(cache.container, hudIdentifier, commands);
                    } catch (Exception e) {
                        // LOGGER.at(Level.WARNING).log("[AquaHudBridge] External updateChild failed: " + e.getMessage());
                    }
                } else {
                    // Somos dueños del container
                    AquaMultiHUD container = AquaMultiHUD.getContainer(playerRef);
                    if (container != null) {
                        container.updateChild(hudIdentifier, commands);
                    } else {
                        // Container no disponible aún (race condition al inicio).
                        // NO enviar sin prefijo — los elementos están dentro de #MultipleHUD.
                        // Enviar sin prefijo causa crash: "Selected element not found".
                        // Descartar el update. Se recuperará en el siguiente tick.
                        // LOGGER.at(Level.WARNING).log("[AquaHudBridge] update() COORDINATED but container=null for: "
                                // + hudIdentifier + " — update dropped (will retry next tick)");
                    }
                }
                break;
        }
    }

    // ========== REMOVE ==========

    public static void remove(Player player, PlayerRef playerRef, String hudIdentifier) {
        if (mode == null) return;

        switch (mode) {
            case TRANSPARENT:
                setJarOverride(playerRef, hudIdentifier);
                player.getHudManager().setCustomHud(playerRef, null);
                break;

            case DELEGATED:
                try {
                    mhudHideCustomHud.invoke(mhudInstance, player, hudIdentifier);
                } catch (Exception e) {
                    // LOGGER.at(Level.WARNING).log("[AquaHudBridge] MHUD remove failed: " + e.getMessage());
                }
                break;

            case COORDINATED:
                AquaMultiHUD container = AquaMultiHUD.getContainer(playerRef);
                if (container != null) {
                    container.remove(hudIdentifier);
                }
                break;
        }
    }

    // ========== INVALIDATE DOM ==========

    /**
     * Marcar el DOM como inválido. Llamar síncronamente desde accept()
     * cuando se detecta un evento que limpiará el DOM.
     */
    public static void invalidateDom(PlayerRef playerRef) {
        if (mode != Mode.COORDINATED) return;

        ExternalCache cache = externalCacheMap.get(playerRef);
        if (cache != null && cache.suppressAndResetMethod != null) {
            // Somos invitados — suprimir via reflexión
            try {
                cache.suppressAndResetMethod.invoke(cache.container, 300L);
            } catch (Exception e) {
                // LOGGER.at(Level.WARNING).log("[AquaHudBridge] External suppressAndReset failed: " + e.getMessage());
            }
        } else {
            // Somos dueños o no hay container externo
            AquaMultiHUD container = AquaMultiHUD.getContainer(playerRef);
            if (container != null) {
                container.suppressAndReset(300);
            }
        }
    }

    // ========== REBUILD ==========

    public static void rebuildAll(PlayerRef playerRef) {
        if (mode != Mode.COORDINATED) return;
        AquaMultiHUD container = AquaMultiHUD.getContainer(playerRef);
        if (container != null) {
            container.show();
        }
    }

    /**
     * Rebuild inmediato que mantiene la supresión activa.
     * NO usa world.execute() anidado — ejecuta inmediatamente.
     */
    public static void rebuildAllDeferred(PlayerRef playerRef, World world) {
        if (mode != Mode.COORDINATED) return;

        ExternalCache cache = externalCacheMap.get(playerRef);
        if (cache != null && cache.showMethod != null) {
            try {
                cache.showMethod.invoke(cache.container);
            } catch (Exception e) {
                // LOGGER.at(Level.WARNING).log("[AquaHudBridge] External show() failed: " + e.getMessage());
            }
        } else {
            AquaMultiHUD container = AquaMultiHUD.getContainer(playerRef);
            if (container != null) {
                container.show();
            }
        }
    }

    // ========== TRANSPARENT GAMEMODE REBUILD ==========

    /**
     * Fuerza un rebuild completo de un HUD en modo TRANSPARENT.
     * Se usa cuando cambia el GameMode: hud.setGameMode() solo actualiza el campo interno,
     * pero build() (que genera los comandos de visibilidad del overlay creativo) solo se
     * ejecuta con show(). En TRANSPARENT, rebuildAllDeferred() es no-op, así que hay que
     * forzar el show() manualmente con el jarOverride correcto.
     *
     * En modos DELEGATED/COORDINATED esto es no-op (rebuildAllDeferred ya lo maneja).
     */
    public static void forceTransparentRebuild(PlayerRef playerRef, String hudIdentifier, CustomUIHud hud) {
        if (mode != Mode.TRANSPARENT) return;
        setJarOverride(playerRef, hudIdentifier);
        hud.show();
    }

    // ========== DISCONNECT ==========

    public static void onPlayerDisconnect(PlayerRef playerRef) {
        AquaMultiHUD.removePlayer(playerRef);
        externalCacheMap.remove(playerRef);
    }
}
