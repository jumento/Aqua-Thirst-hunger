package mx.jume.aquahud;

import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.logger.HytaleLogger;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Container que soporta N HUDs hijos bajo #MultipleHUD.
 * Solo se usa en modo COORDINATED. Basado en MultipleCustomUIHud de MHUD
 * con thread safety, updateChild(), y registro diferido (pendingPlayer + activated).
 *
 * ARQUITECTURA PLANA: Un solo container por jugador. El segundo mod se une
 * como hijo directo via reflexion cross-classloader. Sin nesting, sin wrapped awareness.
 */
public class AquaMultiHUD extends CustomUIHud {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Reflexión cacheada — clases del ENGINE (classloader compartido), seguro cachear
    private static Method BUILD_METHOD;
    private static Field COMMANDS_FIELD;

    static {
        try {
            BUILD_METHOD = CustomUIHud.class.getDeclaredMethod("build", UICommandBuilder.class);
            BUILD_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            BUILD_METHOD = null;
            // LOGGER.at(Level.SEVERE).log("[AquaMultiHUD] Could not find method 'build' in CustomUIHud");
        }
        try {
            COMMANDS_FIELD = UICommandBuilder.class.getDeclaredField("commands");
            COMMANDS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            COMMANDS_FIELD = null;
            // LOGGER.at(Level.SEVERE).log("[AquaMultiHUD] Could not find field 'commands' in UICommandBuilder");
        }
    }

    // Reflexión para reclaim silencioso del slot en HudManager
    private static volatile Field hudManagerCustomHudField;
    private static volatile boolean hudManagerFieldResolved = false;

    // Un container por jugador (campos estáticos separados por classloader)
    private static final Map<PlayerRef, AquaMultiHUD> playerContainerMap =
            Collections.synchronizedMap(new WeakHashMap<>());

    private volatile Player pendingPlayer = null;
    volatile Player player = null;
    private volatile boolean activated = false;
    private volatile long suppressUntilNanos = 0;
    private volatile int autoRebuildCount = 0;
    private static final int MAX_AUTO_REBUILDS = 2;
    long lastShowNanos = 0;
    private static final long SHOW_DEBOUNCE_NS = 50_000_000L; // 50ms
    final ConcurrentHashMap<String, String> normalizedIds = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, CustomUIHud> customHuds = new ConcurrentHashMap<>();

    public AquaMultiHUD(@NonNullDecl PlayerRef playerRef) {
        super(playerRef);
        playerContainerMap.put(playerRef, this);
    }

    public boolean isActivated() {
        return activated;
    }

    // ========== LIFECYCLE ==========

    /**
     * Obtiene o crea el container para un jugador.
     * NO llama setCustomHud() — eso se difiere al primer add().
     */
    public static AquaMultiHUD getOrCreate(Player player, PlayerRef playerRef) {
        AquaMultiHUD container = playerContainerMap.get(playerRef);
        if (container != null) {
            if (container.activated) {
                if (player.getHudManager().getCustomHud() == container) {
                    return container;
                }
                // Container fue reemplazado por otro HUD — crear nuevo
            } else {
                return container; // Aún pendiente
            }
        }

        container = new AquaMultiHUD(playerRef);
        container.pendingPlayer = player;

        // FIX 14: Verificar si hay un HUD de tercero activo
        CustomUIHud existingHud = player.getHudManager().getCustomHud();
        if (existingHud != null && !"AquaMultiHUD".equals(existingHud.getClass().getSimpleName())) {
            String adoptId = "Adopted_" + existingHud.getClass().getSimpleName();
            container.adoptHud(adoptId, existingHud);
        }

        return container;
    }

    public static AquaMultiHUD getContainer(PlayerRef playerRef) {
        return playerContainerMap.get(playerRef);
    }

    public static void removePlayer(PlayerRef playerRef) {
        playerContainerMap.remove(playerRef);
    }

    // ========== SUPRESIÓN ==========

    public void suppress(long durationMs) {
        this.suppressUntilNanos = System.nanoTime() + (durationMs * 1_000_000L);
    }

    public void suppressAndReset(long durationMs) {
        this.suppressUntilNanos = System.nanoTime() + (durationMs * 1_000_000L);
        this.autoRebuildCount = 0;
        this.lastShowNanos = 0; // Permitir que el siguiente show() sea instantáneo
    }

    public static boolean hasChildren() {
        for (AquaMultiHUD container : playerContainerMap.values()) {
            if (!container.customHuds.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ========== BUILD ==========

    /**
     * Build completo: root + todos los hijos.
     * El motor llama esto internamente al ejecutar setCustomHud().
     */
    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("HUD/MultipleHUD.ui");
        for (String identifier : customHuds.keySet()) {
            String normalizedId = normalizedIds.get(identifier);
            CustomUIHud hud = customHuds.get(identifier);
            if (normalizedId != null && hud != null) {
                buildHud(uiCommandBuilder, normalizedId, hud, false);
            }
        }
    }

    // ========== SHOW ==========

    /**
     * Reconstrucción completa con clear=true.
     * Arquitectura plana: somos siempre el dueño. Sin wrapped awareness.
     * Debounce de 100ms contra tormenta de paquetes.
     * NUNCA limpia supresión.
     */
    @Override
    public void show() {
        if (!activated) return;

        // Debounce — prevenir tormenta de paquetes dobles
        long now = System.nanoTime();
        if (this.suppressUntilNanos == 0 && now - lastShowNanos < SHOW_DEBOUNCE_NS) {
            // LOGGER.at(Level.INFO).log("[AquaMultiHUD] show() DEBOUNCED thread=" + Thread.currentThread().getName());
            return;
        }
        lastShowNanos = now;

        // LOGGER.at(Level.INFO).log("[AquaMultiHUD] show() FULL REBUILD (clear=true) children="
                // + customHuds.keySet() + " thread=" + Thread.currentThread().getName());
        UICommandBuilder commandBuilder = new UICommandBuilder();
        this.build(commandBuilder);  // build() ya incluye root + hijos
        this.update(true, commandBuilder);
    }

    // ========== ADD ==========

    /**
     * Agrega o reemplaza un hijo.
     * Primera activación: setCustomHud() + show() (root + hijos).
     * Posterior con instancia diferente: show() completo (invariante #16).
     * Posterior normal: update incremental.
     */
    public void add(@NonNullDecl String identifier, @NonNullDecl CustomUIHud hud) {
        String normalizedId = normalizedIds.computeIfAbsent(identifier,
                i -> i.replaceAll("[^a-zA-Z0-9]", ""));
        CustomUIHud existingHud = customHuds.get(identifier);
        if (existingHud != hud) {
            customHuds.put(identifier, hud);
        }

        if (!activated && pendingPlayer != null) {
            // Primera activación: setCustomHud() llama build() internamente.
            // build() incluye root + todos los hijos. NO llamar show() extra.
            activated = true;
            this.player = pendingPlayer;
            pendingPlayer = null;
            // LOGGER.at(Level.INFO).log("[AquaMultiHUD] FIRST ACTIVATION for " + identifier
                    // + " children=" + customHuds.keySet()
                    // + " thread=" + Thread.currentThread().getName());
            this.player.getHudManager().setCustomHud(getPlayerRef(), this);
            // NO show(). setCustomHud() ya llama build() internamente.
            return;
        }

        // Activación posterior — SIEMPRE incremental, NUNCA show()
        // LOGGER.at(Level.INFO).log("[AquaMultiHUD] ADD CHILD " + identifier
                // + " (incremental) children=" + customHuds.keySet()
                // + " thread=" + Thread.currentThread().getName());
        UICommandBuilder commandBuilder = new UICommandBuilder();
        buildHud(commandBuilder, normalizedId, hud, existingHud != null);
        update(false, commandBuilder);
    }

    /**
     * Actualiza la referencia de un hijo sin enviar paquetes.
     * Para uso via reflexión desde AquaHudBridge.reRegister.
     */
    public void updateHudReference(@NonNullDecl String identifier, @NonNullDecl CustomUIHud hud) {
        customHuds.put(identifier, hud);
        normalizedIds.computeIfAbsent(identifier, i -> i.replaceAll("[^a-zA-Z0-9]", ""));
    }

    /**
     * Marca que se acaba de enviar un paquete completo (vía setCustomHud).
     * Evita que show() envíe un segundo paquete redundante.
     */
    public void markShowSent() {
        this.lastShowNanos = System.nanoTime();
    }

    /**
     * Reclamar el slot del HudManager SIN generar paquete.
     * Cambia el campo interno via reflexión → getCustomHud() devuelve this.
     * No llama build(), no envía clear=true. Silencioso.
     *
     * @return true si se reclamó exitosamente, false si falló reflexión.
     */
    boolean reclaimSlotSilently() {
        if (player == null) return false;
        try {
            Object hudManager = player.getHudManager();
            Field f = resolveHudManagerField(hudManager);
            if (f != null) {
                f.set(hudManager, this);
                return true;
            }
        } catch (Exception e) {
            // LOGGER.at(Level.WARNING).log("[AquaMultiHUD] Silent reclaim failed: " + e.getMessage());
        }
        return false;
    }

    private static Field resolveHudManagerField(Object hudManager) {
        if (hudManagerFieldResolved) return hudManagerCustomHudField;
        synchronized (AquaMultiHUD.class) {
            if (hudManagerFieldResolved) return hudManagerCustomHudField;
            try {
                for (Field f : hudManager.getClass().getDeclaredFields()) {
                    if (f.getType() == CustomUIHud.class) {
                        f.setAccessible(true);
                        hudManagerCustomHudField = f;
                        // LOGGER.at(Level.INFO).log("[AquaMultiHUD] Resolved HudManager field: " + f.getName());
                        break;
                    }
                }
            } catch (Exception e) {
                // LOGGER.at(Level.SEVERE).log("[AquaMultiHUD] Could not resolve HudManager field: " + e.getMessage());
            }
            hudManagerFieldResolved = true;
            return hudManagerCustomHudField;
        }
    }

    /**
     * Registra un HUD como hijo SIN enviar paquetes.
     * Para adoptar HUDs de terceros (ej: RPGLeveling).
     */
    public void adoptHud(String identifier, CustomUIHud hud) {
        normalizedIds.computeIfAbsent(identifier, i -> i.replaceAll("[^a-zA-Z0-9]", ""));
        customHuds.put(identifier, hud);
        tryRemoveFromEndlessActiveHuds(hud);
    }

    /**
     * Si el HUD adoptado es de EndlessLeveling, eliminarlo de ACTIVE_HUDS y DIRTY_HUDS
     * para que HudRefreshSystem no envíe updates incrementales que crashean el cliente.
     */
    private void tryRemoveFromEndlessActiveHuds(CustomUIHud hud) {
        if (!"com.airijko.endlessleveling.ui.PlayerHud".equals(hud.getClass().getName())) return;
        try {
            Class<?> phClass = hud.getClass();
            Field activeField = phClass.getDeclaredField("ACTIVE_HUDS");
            activeField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<java.util.UUID, ?> activeHuds =
                    (ConcurrentHashMap<java.util.UUID, ?>) activeField.get(null);
            java.util.UUID removedUuid = null;
            for (Map.Entry<java.util.UUID, ?> entry : activeHuds.entrySet()) {
                if (entry.getValue() == hud) {
                    removedUuid = entry.getKey();
                    break;
                }
            }
            if (removedUuid != null) {
                activeHuds.remove(removedUuid);
                try {
                    Field dirtyField = phClass.getDeclaredField("DIRTY_HUDS");
                    dirtyField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.util.Set<java.util.UUID> dirtyHuds =
                            (java.util.Set<java.util.UUID>) dirtyField.get(null);
                    dirtyHuds.remove(removedUuid);
                } catch (Exception ignored) {}
                // LOGGER.at(Level.INFO).log("[AquaMultiHUD] Removed " + removedUuid
                        // + " from EL ACTIVE_HUDS+DIRTY_HUDS — HudRefreshSystem will skip");
            }
        } catch (NoSuchFieldException e) {
            // Not a recognized EndlessLeveling HUD structure — ignore
        } catch (Exception e) {
            // LOGGER.at(Level.WARNING).log("[AquaMultiHUD] Failed to remove from EL ACTIVE_HUDS: " + e.getMessage());
        }
    }

    // ========== REMOVE ==========

    public void remove(@NonNullDecl String identifier) {
        String normalizedId = normalizedIds.get(identifier);
        if (normalizedId == null) return;
        normalizedIds.remove(identifier);
        customHuds.remove(identifier);
        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.remove("#MultipleHUD #" + normalizedId);
        update(false, commandBuilder);
    }

    // ========== UPDATE CHILD ==========

    /**
     * Update incremental con prefijado de selectores.
     * Arquitectura plana: sin wrapped awareness, sin proxyUpdate.
     * Supresión temporal + auto-rebuild.
     */
    @SuppressWarnings("unchecked")
    public void updateChild(@NonNullDecl String identifier, @NonNullDecl UICommandBuilder partialCommands) {
        // === CHEQUEO DE DESREGISTRO ===
        if (player != null) {
            CustomUIHud current = player.getHudManager().getCustomHud();
            if (current != this) {
                // Verificar si el HUD actual es un hijo adoptado (ej: RPG via show())
                String potentialAdoptId = current != null ? "Adopted_" + current.getClass().getSimpleName() : "";
                // LOGGER.at(Level.WARNING).log("[AquaMultiHUD] SLOT TAKEN by "
                        // + (current != null ? current.getClass().getName() : "null")
                        // + " adoptId=" + potentialAdoptId
                        // + " isAdopted=" + customHuds.containsKey(potentialAdoptId)
                        // + " children=" + customHuds.keySet());
                if (current != null && customHuds.containsKey(potentialAdoptId)) {
                    // Hijo adoptado tomó el slot via show() — reclaim silencioso, continuar
                    reclaimSlotSilently();
                } else if (current != null) {
                    // HUD desconocido tomó el slot — adoptar para tracking, suprimir largo
                    adoptHud(potentialAdoptId, current);
                    // LOGGER.at(Level.INFO).log("[AquaMultiHUD] ADOPTED " + potentialAdoptId
                            // + " — silent reclaim + suppress 3s");
                    reclaimSlotSilently();
                    suppress(3000);
                    return;
                }
            }
        }

        // === SUPRESIÓN ACTIVA ===
        if (suppressUntilNanos > 0) {
            if (System.nanoTime() < suppressUntilNanos) {
                return; // Aún suprimido
            }

            // Expiró — auto-rebuild (si no excedimos MAX)
            if (autoRebuildCount >= MAX_AUTO_REBUILDS) {
                suppressUntilNanos = 0;
                // Caer al path normal
            } else {
                autoRebuildCount++;
                if (player != null && player.getHudManager().getCustomHud() != this) {
                    CustomUIHud curr = player.getHudManager().getCustomHud();
                    String potAdoptId = curr != null ? "Adopted_" + curr.getClass().getSimpleName() : "";
                    if (curr != null && customHuds.containsKey(potAdoptId)) {
                        // Hijo adoptado tiene el slot — reclaim silencioso, continuar rebuild
                        reclaimSlotSilently();
                    } else if (curr != null) {
                        // HUD desconocido — adoptar para tracking, suprimir largo
                        adoptHud(potAdoptId, curr);
                        // LOGGER.at(Level.INFO).log("[AquaMultiHUD] ADOPTED (auto-rebuild) " + potAdoptId
                                // + " — suppress 3s");
                        reclaimSlotSilently();
                        suppress(3000);
                        return;
                    }
                }
                suppress(300);
                show();
                return;
            }
        }

        // === PATH NORMAL ===
        String normalizedId = normalizedIds.get(identifier);
        if (normalizedId == null || COMMANDS_FIELD == null) return;
        try {
            List<CustomUICommand> srcCmds = (List<CustomUICommand>) COMMANDS_FIELD.get(partialCommands);
            if (srcCmds == null || srcCmds.isEmpty()) return;

            String prefix = "#MultipleHUD #" + normalizedId;
            UICommandBuilder outBuilder = new UICommandBuilder();
            List<CustomUICommand> outCmds = (List<CustomUICommand>) COMMANDS_FIELD.get(outBuilder);

            for (CustomUICommand cmd : srcCmds) {
                CustomUICommand prefixed = new CustomUICommand(cmd.type,
                    cmd.selector == null ? prefix : prefix + " " + cmd.selector,
                    cmd.data, cmd.text);
                outCmds.add(prefixed);
            }

            this.update(false, outBuilder);
        } catch (IllegalAccessException e) {
            // LOGGER.at(Level.SEVERE).log("[AquaMultiHUD] Error in updateChild: " + e.getMessage());
        }
    }

    // ========== REINJECT ==========

    /**
     * Reinyección tras convertir clear=true a clear=false (AquaHudWatcher).
     * DOM del cliente NO fue borrado → grupos YA EXISTEN.
     * Usa hudExists=true (Clear contenido + rebuild) para cada hijo.
     * NO incluye root (MultipleHUD.ui) — ya existe en el DOM.
     */
    public void reinject(UICommandBuilder commandBuilder) {
        if (!activated) return;
        for (String identifier : customHuds.keySet()) {
            String normalizedId = normalizedIds.get(identifier);
            CustomUIHud hud = customHuds.get(identifier);
            if (normalizedId != null && hud != null) {
                buildHud(commandBuilder, normalizedId, hud, true);
            }
        }
    }

    // ========== BUILD HUD ==========

    /**
     * Construye un hijo dentro del container.
     *
     * Arquitectura plana: los hijos son siempre HUDs regulares (SanityHud, HHMHud, etc.)
     * NUNCA otra instancia de AquaMultiHUD. Sin Native Bypass.
     */
    static void buildHud(
            @Nonnull UICommandBuilder uiCommandBuilder,
            @NonNullDecl String normalizedId,
            @Nonnull CustomUIHud hud,
            boolean hudExists) {
        try {
            if (BUILD_METHOD == null || COMMANDS_FIELD == null) return;

            PrefixedUICommandBuilder singleHudBuilder = new PrefixedUICommandBuilder(normalizedId);
            if (hudExists) {
                singleHudBuilder.addCustomCommand(CustomUICommandType.Clear, singleHudBuilder.getPrefix(), null);
            } else {
                singleHudBuilder.addCustomCommand(CustomUICommandType.AppendInline, "#MultipleHUD",
                        "Group #" + normalizedId + " {}");
            }
            BUILD_METHOD.invoke(hud, singleHudBuilder);
            singleHudBuilder.appendCommandsTo(uiCommandBuilder);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update incremental de un HUD adoptado: solo propiedades, sin rebuild DOM.
     * Llama build() pero filtra Append/AppendInline — evita Clear y recreación
     * de estructura. Solo envía Set y otros comandos de propiedades.
     * Previene flicker de boss bar causado por destroy/rebuild cada ~150ms.
     */
    @SuppressWarnings("unchecked")
    static void updateAdoptedHudData(UICommandBuilder outBuilder, String normalizedId, CustomUIHud hud) {
        try {
            if (BUILD_METHOD == null || COMMANDS_FIELD == null) return;

            UICommandBuilder tempBuilder = new UICommandBuilder();
            BUILD_METHOD.invoke(hud, tempBuilder);
            List<CustomUICommand> srcCmds = (List<CustomUICommand>) COMMANDS_FIELD.get(tempBuilder);
            if (srcCmds == null || srcCmds.isEmpty()) return;

            String prefix = "#MultipleHUD #" + normalizedId;
            List<CustomUICommand> outCmds = (List<CustomUICommand>) COMMANDS_FIELD.get(outBuilder);

            for (CustomUICommand cmd : srcCmds) {
                // Filtrar comandos estructurales — solo enviar propiedades
                if (cmd.type == CustomUICommandType.Append || cmd.type == CustomUICommandType.AppendInline) {
                    continue;
                }
                String selector = cmd.selector == null ? prefix : prefix + " " + cmd.selector;
                outCmds.add(new CustomUICommand(cmd.type, selector, cmd.data, cmd.text));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            // LOGGER.at(Level.SEVERE).log("[AquaMultiHUD] Error in updateAdoptedHudData: " + e.getMessage());
        }
    }

    // ========== PREFIXED BUILDER ==========

    /**
     * Idéntico a MHUD PrefixedUICommandBuilder.
     * Captura comandos generados por build() de un hijo y les añade prefijo.
     */
    private static class PrefixedUICommandBuilder extends UICommandBuilder {
        private final List<CustomUICommand> wrappedCommands = new ArrayList<>();
        private final String prefix;

        public PrefixedUICommandBuilder(@NonNullDecl String id) {
            this.prefix = "#MultipleHUD #" + id;
        }

        public String getPrefix() {
            return this.prefix;
        }

        @SuppressWarnings("unchecked")
        private void prefixCommands() throws IllegalAccessException {
            if (COMMANDS_FIELD == null) return;
            final List<CustomUICommand> commands = (List<CustomUICommand>) COMMANDS_FIELD.get(this);

            if (commands != null) {
                for (CustomUICommand command : commands) {
                    if (command != null) {
                        if (command.selector == null) {
                            command.selector = this.prefix;
                        } else {
                            command.selector = this.prefix + " " + command.selector;
                        }
                        wrappedCommands.add(command);
                    }
                }
                commands.clear();
            }
        }

        @Override
        @Nonnull
        public CustomUICommand[] getCommands() {
            try {
                this.prefixCommands();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            CustomUICommand[] commands = wrappedCommands.toArray(new CustomUICommand[0]);
            wrappedCommands.clear();
            return commands;
        }

        @SuppressWarnings("unchecked")
        void appendCommandsTo(@Nonnull UICommandBuilder builder) throws IllegalAccessException {
            this.prefixCommands();
            if (COMMANDS_FIELD == null) return;
            final List<CustomUICommand> commands = (List<CustomUICommand>) COMMANDS_FIELD.get(builder);
            if (commands != null) {
                commands.addAll(this.wrappedCommands);
            }
            this.wrappedCommands.clear();
        }

        void addCustomCommand(@Nonnull CustomUICommandType type, @Nullable String selector,
                @Nullable String document) {
            this.wrappedCommands.add(new CustomUICommand(type, selector, null, document));
        }
    }
}
