# Plan de actualización — Aqua Thirst & Hunger → Hytale 0.7.x

**Fecha:** 29-oct-2026 · **Base:** `main` @ `cf46436` (mod 1.0.0) · **Objetivo:** dejar el mod funcional en el servidor actual de Hytale sin perder funcionalidades, corregir los bugs encontrados y rehacer la compatibilidad con los mods opcionales sobre APIs oficiales.

Todo lo que se afirma como "verificado" se comprobó compilando el código actual contra el `HytaleServer.jar` 0.7.2 instalado en esta máquina e inspeccionando su bytecode, o leyendo el código del mod. Lo marcado como **a verificar** depende de JARs de terceros que todavía no tenemos.

## Estado de ejecución

Plan aprobado con las decisiones recomendadas de la §12 (HUD solo nativo, sed máxima por nivel visible en el HUD, versión 1.1.0, AquaSanity en tarea aparte). Rama `update/hytale-0.7`.

| Fase | Estado |
|---|---|
| 0 — Build reproducible | Hecha |
| 1 — Migración de API | Hecha |
| 2 — HUD nativo | Hecha |
| 3 — Gameplay y config | Hecha |
| 4 — Integraciones | Hecha en código, contra las APIs de RPG Leveling 0.4.2, Endless Leveling 14.1.0 y MMO Skill Tree 1.7.4 (inspeccionadas, no ejecutadas; ver §6.1). Falta verificar en juego con cada mod |
| 5 — Documentación | Parcial: README y `CHANGELOG.md` actualizados |
| 6 — Verificación y release | Parcial: 27 pruebas unitarias; en servidores dedicados 0.7.2 y 0.7.5 el plugin carga su asset pack, pasa `--validate-assets`, se habilita y se apaga sin errores (solo con nuestro mod), y migra a 1.8.0 copias de configs reales de 1.6.0 y 1.6.1 sin perder valores. Falta la prueba en juego (§10.3) |

**Parche 0.7.5 (revisado el 5-nov-2026):** Maven retiró la 0.7.2, así que el build compila contra la 0.7.5. Las 139 clases del servidor que usa el mod tienen las mismas firmas en 0.7.2 y 0.7.5 (incluido el campo `EntityEffect.damageCalculatorCooldown` que se lee por reflexión), A pedido tuyo, el manifest pasa a `>=0.7.5 <0.8.0` y el mod a **1.1.1** (la 1.1.0 nunca se publicó). RPG Leveling, Endless Leveling y MMO Skill Tree no sacaron versiones nuevas desde las revisadas.

Bugs corregidos: B01–B27 (B23–B27 aparecieron en la fase 4, ver §7). Además se corrigieron tres problemas encontrados durante la implementación: life-per-hunger gastaba sed con la sed desactivada, el buff de stamina quitaba el buff vanilla de frutas/verduras, y las páginas de config truncaban decimales al cerrar (`1.5` → `1`).

---

## 1. Resumen ejecutivo

| Tema | Hallazgo |
|---|---|
| ¿Funciona hoy? | **No.** La 1.0.0 se compiló contra el servidor `2026.03.26`. Contra el servidor actual (0.7.2 instalado, 0.7.4 en Maven) el código da **27 errores de compilación en 10 archivos**. El JAR publicado lanzaría `NoSuchMethodError` al registrar el HUD en cuanto entra un jugador. |
| Problema de UI (HUDs que se pisaban) | **Resuelto en el motor.** Desde 0.5.1 Hytale soporta varios HUDs custom con clave (`CustomUIHud(PlayerRef, key)`, `HudManager.addCustomHud/removeCustomHud`), y cada paquete `CustomHud` lleva su `hudId`. Todo el paquete `aquahud` (≈1.180 líneas: bridge, contenedor y vigilante de paquetes) sobra, y su interceptor de paquetes ahora **rompería los HUD de otros mods**. MultipleHUD y AutoMultiHUD ya no hacen falta. |
| Compatibilidad improvisada | RPG Leveling **nunca se activa**: falta llamar a `initialize()`. MMO Skill Tree necesita "2 reinicios" porque el ID del manifest está mal escrito (`ziggfreed` vs `Ziggfreed`) y los archivos del árbol se escriben tarde. Endless Leveling depende de hacks de reflexión sobre su HUD interno que hoy serían dañinos. |
| Bugs | 22 problemas detectados (sección 7), 4 de severidad alta. El más visible: el JAR **no incluye** `ExternalFoodsDefaultConfig.json`, así que en instalaciones nuevas los 234 alimentos de otros mods quedan sin valores por defecto. |
| Propuesta | Versión **1.1.0** en 7 fases con criterios de aceptación, migración automática de configs y una matriz de pruebas en servidor 0.7.x. |

---

## 2. Situación de partida

- **Publicado:** `Aqua-Thirst-hunger-1.0.0.jar` en CurseForge (15-abr-2026). Es el mismo JAR que `build/libs/`; el repositorio no tiene cambios de código desde entonces.
- **Compilado contra:** `libs/HytaleServer.jar` = `2026.03.26-89796e57b` (Java 21).
- **Servidor actual:** `https://maven.hytale.com/release` publica `0.5.10, 0.6.9, 0.7.2, 0.7.3, 0.7.4` (la última es 0.7.4, del 22-oct-2026). Hytale pasó de versiones con fecha a semver, y las versiones con fecha ya no están en Maven. El cliente instalado aquí es `0.7.2` y el servidor ahora pide Java 25.
- **Manifest engañoso:** `build.gradle.kts` escribe como `ServerVersion` la última versión de Maven **en el momento de compilar** (hoy pondría `0.7.4`), aunque el código se compile contra el JAR de marzo. Así el manifest declara una compatibilidad que nunca se probó. En 0.7.x una discrepancia solo genera un aviso, pero el servidor ya advierte que omitir la versión "será un error duro en el futuro".
- **Sin diagnóstico:** todos los logs del mod están comentados (`logInfo/logWarning/logSevere/logDebug` no hacen nada), así que hoy no se pueden diagnosticar fallos en servidores de usuarios.

---

## 3. Inventario de funcionalidades (checklist de no regresión)

Todo esto debe seguir funcionando al terminar. Es también la lista de pruebas final.

**Mecánicas**
- [ ] Hambre 0–100 más saturación hasta 200 (barra doble), y sed 0–100.
- [ ] Desgaste por tiempo, por stamina gastada (sprint) y por golpes a bloques, con multiplicadores de las integraciones.
- [ ] Desgaste reducido al estar quieto ("AFK").
- [ ] Efectos `Hungry` (umbral), `Starving` (hambre 0, quita buffs) y `Dehydration` (sed 0); daño por inanición y por deshidratación.
- [ ] Life-per-hunger: regenera vida gastando hambre y sed, con pulso configurable.
- [ ] Buff de stamina (`FruitVeggie_Buff_T2`) con la sed por encima del umbral.
- [ ] Veneno al comer carne cruda (4 IDs).
- [ ] Sonidos de estómago y de tos al cruzar umbrales (30/25/22/18/15/10/5), con cooldown de 3 s.
- [ ] Saturación inicial para jugadores nuevos; hambre y sed mínimas o reiniciadas al reaparecer.
- [ ] Creativo: barras "creative" y rellenado automático al pasar a creativo.

**Comida y bebida**
- [ ] Cadena de resolución de valores: config externa del usuario → config externa por defecto (234 ítems) → `FoodValuesConfig` por ítem → asset `Item/Hungry/FoodValues` → valores de la interacción → valor por rareza (tier).
- [ ] Multiplicador de fruta para la sed (ResourceType `Fruits`).
- [ ] Vista previa de lo que se va a recuperar mientras se come, y limpieza al cancelar.
- [ ] Ítems: cantimplora, cantimplora pro (vacía/llena) y botella básica (vacía/llena) con sus recetas, rellenado en agua (`RefillContainer`) y en bancos (Well/Sink/Water).
- [ ] Overrides de `Consume_Charge_Food_T1/T2/T3` (+`_Inner`): comer falla al recibir daño, ralentiza y usa tiempos 2.0/2.5/….

**UI y comandos**
- [ ] HUD de hambre y de sed, 8 posiciones preset (+ Custom) e indicadores `+XP / -XP`.
- [ ] `/aquacheffconfig` → presets Easy/Normal/Hard y menú custom → editor de comidas (`/aquacheff`), posición de barras (`/aquacheffbars`), config de hambre (`/aquahungerconfig`), config de sed (`/aquathirstconfig`) y life-per-hunger.
- [ ] `/aquahunger [set <n> | set <jugador> <n> | stats]`, `/aquathirst [set …]`, `/aquahungerreload`, `/aquahungerok`, `/aquahungerreset`.
- [ ] Aviso de bienvenida de integraciones (bilingüe) y registro `notified_players.json`, sincronizado con AquaSanity.
- [ ] Récords del servidor (`/aquahunger stats`).
- [ ] Permisos automáticos para Adventure/Creative en modo SinglePlayer.

**Integraciones (opcionales)**
- [ ] RPG Leveling: XP con bonus o penalización según hambre y sed, menos consumo por nivel y aviso visual al subir de nivel.
- [ ] Endless Leveling: lo mismo, con detección de subida de nivel.
- [ ] MMO Skill Tree: árboles NUTRITION y THIRST (custom-skills, skill-tree, xp-maps), multiplicadores por recompensas, sistema "Glutton" (ítems al comer, o basura) y bonus de rehidratación.
- [ ] AquaSanity: suprime el aviso de bienvenida y comparte `notified_players.json`.

**Configuración**
- [ ] 6 archivos en `mods/Aqua-Thirst-hunger/`, con migración y backup (`*.vX.bak`) que conserva los valores del usuario.

---

## 4. Cambios de API de Hytale que rompen el mod

| Antes (2026.03.26) | Ahora (0.7.x) | Afecta a |
|---|---|---|
| `new CustomUIHud(playerRef)` | `new CustomUIHud(playerRef, key)` o `(playerRef, key, zOrder)` | `HHMHud`, `HHMThirstHud`, `AquaMultiHUD` |
| `HudManager.setCustomHud(ref, hud)` y `getCustomHud()` (un solo slot) | `addCustomHud(ref, hud)`, `removeCustomHud(ref, key)`, `getCustomHud(key)`, `getCustomHuds()` | `AquaHudBridge`, `AquaMultiHUD`, `AquaHudWatcher` |
| Paquete `CustomHud(clear, commands)` | `CustomHud(hudId, zOrder, clear, commands)`: cada HUD tiene su propio árbol en el cliente | lógica del watcher |
| `com.hypixel.hytale.math.vector.Vector3d/Vector3f` | **JOML**: `TransformComponent.getPosition()` devuelve `org.joml.Vector3d` | `StarveSystem`, `ThirstSystem`, `MMOSkillTreeBridge` |
| `ItemComponent.generateItemDrops(acc, items, pos, Vector3f)` | `generateItemDrops(acc, items, Vector3d, Rotation3fc)` (el 4º parámetro ahora es una rotación), más el helper `Player.giveItem(stack, ref, acc)` | `MMOSkillTreeBridge` (Glutton) |
| `AbstractCommand.canGeneratePermission()` | eliminado; ahora `requireNoPermission()` | `/aquahungerok`, `/aquahungerreset` |
| `Player.getInventory().getItemInHand()` (por reflexión) | el inventario vive en el ECS (`InventoryComponent`) | autocompletar del editor de comidas (falla en silencio) |
| Campo `JavaPlugin.init` (por reflexión) | ya no existe | `IntegrationManager.getPluginClassLoader` (además es código muerto) |
| — | El motor **borra todos los HUD custom** al cambiar de mundo (`Player.resetManagers → HudManager.resetHud`) | hay que volver a añadir los HUD en cada `PlayerReadyEvent` |
| — | Nuevos: modos de juego definidos como assets (`Spectator`, `SpectatorInteractive`, `HardcoreSpectator`), eventos `GameModeTypeEnterEvent/ExitEvent`, `RespawnEvent` y `LoadBefore` en el manifest | oportunidades (ver fases) |

**Sin cambios (verificado):** `UICommandBuilder.commands`, `EntityEffect.damageCalculatorCooldown`, `ActiveEntityEffect.entityEffectId` (aunque ya existe `getEntityEffectIndex()` para dejar de usar reflexión), `SoundUtil.playSoundEvent3d(ref, idx, x, y, z, bool, acc)`, `ChangeGameModeEvent`, `PlayerReadyEvent` y `PacketAdapters`. Los assets vanilla que el mod sobrescribe (`Consume_Charge_Food_*`) mantienen la misma estructura. Las plantillas de `Common.ui` que usan las páginas (`@TextButton`, `@TextField`, `@CheckBoxWithLabel` con `#CheckBox`, `@Container`, `@Title`, `@CancelTextButton`…) siguen existiendo en el cliente actual.

---

## 5. El problema de la UI: ¿se resolvió?

**Sí, en el motor.** El problema original era que Hytale solo permitía *un* HUD custom por jugador, y el último mod en llamar a `setCustomHud` borraba a los demás. Para sobrevivir, el mod construyó `aquahud` con 3 modos:

- `TRANSPARENT` (con AutoMultiHUD): escribe por reflexión en un mapa interno de AutoMultiHUD.
- `DELEGATED` (con MultipleHUD): registra el HUD por reflexión y reescribe los selectores con el prefijo `#MultipleHUD #id`.
- `COORDINATED` (sin gestor): contenedor propio `AquaMultiHUD`, "flat join" entre los mods de jume por reflexión entre classloaders, y `AquaHudWatcher`, que **intercepta los paquetes salientes de otros mods**, convierte `clear=true` en `clear=false`, "adopta" sus HUD, reclama el slot del `HudManager` por reflexión y, para Endless Leveling, saca su HUD de `ACTIVE_HUDS/DIRTY_HUDS`.

En 0.7.x cada HUD tiene su `hudId`, así que un `clear=true` solo limpia ese HUD. Consecuencias:

1. Nada de `aquahud` es necesario, y además ya no compila: el campo `customHud` del `HudManager` que se reclamaba por reflexión ahora es un mapa.
2. Si se portara tal cual, el watcher **anularía las reconstrucciones legítimas de otros mods** (HUDs en blanco), y el hack de Endless Leveling **dejaría su HUD sin actualizar**.
3. Las demoras de 500 ms para Endless Leveling, las supresiones de 300 ms y 3 s y los auto-rebuilds sobran.
4. La recomendación de la página de CurseForge ("usa AutoMultiHUD o desactiva MultipleHUD") deja de aplicar: el mod funcionará sin gestor de HUD. Si alguien mantiene esos mods instalados, no interferimos con ellos.

**Diseño nuevo (fase 2):** HUDs nativos con clave propia — `AquaThirstHunger_Hunger`, `AquaThirstHunger_Thirst` y `AquaThirstHunger_Notice` (el aviso de bienvenida pasa a ser un HUD independiente con zOrder mayor, que se quita con `removeCustomHud` al cerrarlo) — y un `HudService` que:

- añade los HUD en cada `PlayerReadyEvent` (login y cambio de mundo);
- marca los cambios con un "dirty flag" y los envía como máximo una vez por tick, en lugar del *throttle* actual que **descarta** actualizaciones (bug B03);
- aplica en caliente los cambios de posición y de config (`show()`), sin reiniciar el servidor;
- oculta los HUD en los modos espectador (`GameModeTypeEnterEvent`).

Se eliminan `AquaHudBridge`, `AquaMultiHUD`, `AquaHudWatcher`, `HUD/MultipleHUD.ui`, `HUD/HHMMultipleHUD.ui` y el duplicado sin uso `ui/WelcomeNotice.ui`.

---

## 6. Compatibilidad improvisada: auditoría por mod

### RPG Leveling And Stats/Skills (`Zuxaw:RPGLeveling`)
- **No funciona en 1.0.0.** `IntegrationManager.init()` crea el bridge y consulta `isAvailable()`, pero nadie llama a `initialize()`, así que `available` siempre es `false`: ni XP, ni reducción de consumo, ni aviso de nivel ([IntegrationManager.java:24](../src/main/java/mx/jume/aquahunger/compat/IntegrationManager.java#L24), [RPGLevelingBridge.java:24](../src/main/java/mx/jume/aquahunger/compat/RPGLevelingBridge.java#L24)).
- La detección también busca `com.zuxaw.rpgleveling.api`, un paquete que no existe (el real es `org.zuxaw.plugin.api`). `APIDiscovery` es código muerto.
- API usada, validada contra la 0.2.10: `RPGLevelingAPI.isAvailable/get/addXP(UUID,double,XPSource)/getPlayerLevel(UUID)/registerLevelUpListener` y `XPSource.create(String)`. **A verificar contra la versión actual (0.3.x).**

### Endless Leveling (`airijko:EndlessLeveling`)
- XP y nivel por reflexión (`EndlessLevelingAPI.get().grantXp(UUID,double)`, `getPlayerLevel(UUID)`). El level-up se detecta comparando niveles 200 ms después.
- La versión actual es la V11, y sus notas dicen que "toda ganancia de XP ahora lleva fuente": **probable cambio de firma. A verificar con el JAR actual**, igual que el `Group` exacto de su manifest.
- Hacks de HUD a eliminar: la demora de 500 ms en el registro del HUD ([HHMPlayerReady.java:51](../src/main/java/mx/jume/aquahunger/events/HHMPlayerReady.java#L51)) y la extracción del HUD de EL de `ACTIVE_HUDS/DIRTY_HUDS` ([AquaMultiHUD.java:305](../src/main/java/mx/jume/aquahud/AquaMultiHUD.java#L305)).

### MMO Skill Tree (`Ziggfreed:MMOSkillTree`)
- **ID mal escrito en el manifest:** declaramos `ziggfreed:MMOSkillTree`, pero el mod se registra como `Ziggfreed:MMOSkillTree` y `PluginIdentifier` distingue mayúsculas. La dependencia opcional se ignora y no hay orden de carga garantizado ([build.gradle.kts:70](../build.gradle.kts#L70)).
- **Causa de los "2 reinicios":** los archivos del árbol (`custom-skills.json`, `skill-tree.json`, `xp-maps.json`) se escriben en `start()`, cuando MMO ya leyó su config. Solución propuesta: declarar `LoadBefore: Ziggfreed:MMOSkillTree` y escribirlos en `setup()`, antes de que MMO cargue. Si la versión actual de MMO lo permite, mejor registrar las skills por API sin tocar sus archivos.
- La ruta `mods/mmoskilltree` está fija en ambos lados (MMO 0.14.1 la usa así). **MMO actual es la 1.5.x y requiere `ZiggfreedCommon`: verificar ruta, `schemaVersion` y formato.**
- `addXp(Store, Ref, …)` se llama desde dentro de un sistema ECS en pleno tick. Si MMO modifica componentes con el `Store` puede lanzar una excepción, que hoy se traga en silencio. Se moverá a `world.execute()`.
- El reparto de ítems del sistema Glutton usa la firma vieja de `generateItemDrops` (pasaba una velocidad como 4º parámetro); se cambiará a `Player.giveItem` más un drop con la API nueva.

### Semántica de `getSkillLevel("SURVIVAL")` (afecta a las tres)
[ThirstSystem.java:140](../src/main/java/mx/jume/aquahunger/systems/ThirstSystem.java#L140) sube la sed máxima +2.5 por cada nivel de "SURVIVAL". Pero RPG y Endless ignoran el `skillId` y devuelven el **nivel global**, y MMO no tiene ninguna skill "SURVIVAL" (solo NUTRITION y THIRST). Ejemplo con Endless nivel 50: la sed máxima pasa a 225, pero el HUD solo dibuja 0–100 y el porcentaje del buff de stamina se calcula sobre 100. El resultado es una reserva "invisible" y un buff casi permanente. **Requiere una decisión de diseño** (sección 12).

### AquaSanity (mod hermano)
Comparte el "flat join" de `aquahud` y `notified_players.json`. AquaSanity tampoco funcionará en 0.7.x hasta que migre a HUD nativo. Con HUDs nativos no hace falta coordinar HUDs entre los dos mods; solo se mantiene el contrato del archivo de notificados y la supresión del aviso. Se propone como tarea aparte.

### 6.1 Resultado de la fase 4: APIs verificadas

JARs descargados de CurseForge (archivos oficiales, versión para Hytale 0.7) a `libs/optional/`, inspeccionados con `javap`; no se ejecutaron.

| Mod (archivo) | ID real | Qué cambió | Cómo queda |
|---|---|---|---|
| RPG Leveling 0.4.2 (`RPGLeveling-0.4.2.jar`) | `Zuxaw:RPGLeveling` | Nada de lo que usamos: `isAvailable/get/addXP(UUID,double,XPSource)/getPlayerLevel/registerLevelUpListener`. `addXP` pasa la XP al hilo del mundo por su cuenta | Se llama a `initialize()` en nuestro `start()` (RPG ya arrancó, es dependencia opcional); listener de nivel con proxy completo, desregistrado al apagar |
| Endless Leveling 14.1.0 (`EndlessLeveling.jar`) | `Airijko:EndlessLevelingCore` (antes `airijko:EndlessLeveling`) | `grantXp(UUID,double)` y `getPlayerLevel(UUID)` siguen; nuevo `addLevelUpListener(Consumer<LevelUpEvent>)` | ID nuevo en el manifest; level-up por listener (el sondeo de 200 ms queda solo como respaldo si falta el método); `grantXp` sigue diferido con `world.execute` |
| MMO Skill Tree 1.7.4 (`MMOSkillTree-1.7.4.jar`) | `Ziggfreed:MMOSkillTree` | Lee `custom-skills.json`, `xp-maps.json` y `skill-tree.json` de `mods/mmoskilltree/` **en su `setup()`**, y descarta overrides de skills que no estén registradas en ese momento. `SkillTreeService`/`SkillReward` pasaron a `.skilltree`. `addXp` solo modifica el componente existente (seguro desde un tick) | `LoadBefore: Ziggfreed:MMOSkillTree` (y fuera de `OptionalDependencies`, que crearía un ciclo) y escritura de los 3 archivos en nuestro `setup()`: las skills existen desde el primer arranque. Las clases de MMO se cargan con su propio class loader (`OptionalPlugins`) |

Notas:
- **Por qué no un content pack para MMO:** MMO 1.6+ acepta skills y XP maps como assets (`Server/MMOSkillTree/CustomSkills/`), pero los nodos del árbol solo vienen de `skill-tree.json` del dueño y MMO ignora los overrides de skills que no conoce cuando lo lee en `setup()`, antes de cargar los assets. Con las skills en un pack, el árbol de NUTRITION y THIRST se perdería.
- MMO 1.7 sigue aceptando un override escrito como lista de tiers (nuestra plantilla); los que guarda él usan un mapa por tier. Los archivos solo se reescriben si cambia su contenido, y un archivo con JSON inválido no se toca.
- **Class loader de 0.7:** un plugin ve las clases de sus `Dependencies` y `OptionalDependencies`, no las de cualquier plugin cargado. Por eso la detección de mods va por ID de plugin (`PluginManager.getPlugin`) y las clases se piden al class loader del mod.
- **XP fuera del tick:** no hizo falta mover las llamadas. RPG ya difiere sus `addXP`, el `addXp` de MMO es seguro y la de Endless ya se difería.
- **"SURVIVAL" (B08):** el nivel que sube la sed máxima es ahora explícito: el nivel global de RPG Leveling o Endless Leveling (el mayor). MMO no tiene nivel global y no cuenta, igual que antes.
- **Posible mejora (no hecha):** Endless Leveling expone `HudSuppression.addCinematicListener` para sus cinemáticas; se podrían ocultar nuestras barras durante ellas, pero hay que verlo en juego primero.

### MultipleHUD y AutoMultiHUD
Ya no son necesarios. Se elimina toda la integración, incluida la escritura en `PlayerPacketTracker.incomingJarOverride` de AutoMultiHUD.

### Mods de comida y de agua
- `ExternalFoodsDefaultConfig.json` (234 ítems: vanilla, NoCube, Andiechef, Brewery, Food Galore…) **no se empaqueta en el JAR**. El código lo busca en la raíz del JAR ([HHMExternalFoodsConfig.java:228](../src/main/java/mx/jume/aquahunger/config/HHMExternalFoodsConfig.java#L228)), pero `build.gradle.kts` no lo copia. En instalaciones nuevas sale un log `CRITICAL` y no hay valores por defecto; las antiguas usan una copia vieja del disco.
- Infinite Water Supply (AvidPixel): la compatibilidad depende de IDs de bancos (`Well`, `Bench_Well`, `Bench_Sink`, `Bench_Water`) y de fluidos (`Water_Source`, `Water`). **Verificar que existan en 0.7.x y en la versión actual de ese mod.**
- El mod sobrescribe los assets vanilla `Consume_Charge_Food_T1/T2/T3` (+`_Inner`). Cualquier otro mod que sobrescriba los mismos archivos entra en conflicto (gana el último que carga). Se documentará; la API no ofrece otro punto de enganche para "al terminar de comer".

---

## 7. Bugs encontrados en la revisión

Severidad: **A** = rompe una función o provoca un crash · **M** = comportamiento incorrecto visible · **B** = menor o de mantenimiento.

| ID | Sev | Problema | Dónde | Corrección |
|---|---|---|---|---|
| B01 | A | No compila ni carga en 0.7.x (27 errores) | ver §4 y anexo A | fases 1–2 |
| B02 | A | `ExternalFoodsDefaultConfig.json` no se empaqueta | `build.gradle.kts` | moverlo a `src/main/resources` |
| B03 | M | El *throttle* de 200 ms **descarta** actualizaciones del HUD: tras comer, beber o curarse, el HUD puede quedarse con el valor viejo hasta el siguiente tick de desgaste (2 s por defecto, 30 s en Easy) | [HHMHud.java:181](../src/main/java/mx/jume/aquahunger/ui/HHMHud.java#L181), `HHMThirstHud` | dirty flag con envío por tick |
| B04 | A | RPG Leveling nunca se inicializa | `IntegrationManager` | §6 |
| B05 | M | `MaxHungerLevel` se muestra y edita en la UI pero se ignora: el componente lo fija en 200, así que el preset Hard (150) no se aplica | [HungerComponent.java:43](../src/main/java/mx/jume/aquahunger/components/HungerComponent.java#L43) | aplicar la config y escalar el HUD; al migrar, un config anterior a 1.8.0 con nivel inicial o de reaparición por encima de su máximo pasa a máximo 200, el tope que regía |
| B06 | M | `DamageIntervalSeconds` se edita en la UI pero se ignora: el daño por deshidratación se aplica en cada tick de desgaste (los presets coinciden, las configs personalizadas no) | `ThirstSystem` | temporizador propio |
| B07 | M | "AFK": estar quieto multiplica **todo** el desgaste por 0,1, incluido minar o luchar sin moverse | [StarveSystem.java:136](../src/main/java/mx/jume/aquahunger/systems/StarveSystem.java#L136), `ThirstSystem` | aplicarlo solo al metabolismo base y tras N s de inactividad real |
| B08 | M | Sed máxima ligada al nivel global (§6) | [ThirstSystem.java:140](../src/main/java/mx/jume/aquahunger/systems/ThirstSystem.java#L140) | decisión de diseño |
| B09 | M | `NullPointerException` al arrancar si `ThirstFoodValuesConfig.json` tiene `"ConfigVersion": null` | [ConfigMigrationManager.java:162](../src/main/java/mx/jume/aquahunger/config/ConfigMigrationManager.java#L162) | migración null-safe |
| B10 | M | Al reaparecer se quitan efectos usando el `Store` dentro de un sistema en procesamiento; debería usar el `CommandBuffer` (riesgo de excepción) | [OnDeathSystem.java:42](../src/main/java/mx/jume/aquahunger/systems/OnDeathSystem.java#L42) → `HHMUtils.setPlayerHungerLevel` | pasar el `CommandBuffer` o usar `RespawnEvent` |
| B11 | M | El multiplicador de coste de curación de las integraciones (`getHealCostMultiplier`) nunca se aplica | `HungerLifeSystem` | aplicarlo |
| B12 | M | Logs desactivados por completo | [AquaThirstHunger.java:173](../src/main/java/mx/jume/aquahunger/AquaThirstHunger.java#L173) | logger real, con `EnableDebugLogs` para el detalle |
| B13 | M | Cambiar la posición del HUD pide reiniciar el servidor | [AquaBarsPage.java:118](../src/main/java/mx/jume/aquahunger/ui/AquaBarsPage.java#L118) | aplicar en caliente |
| B14 | M | El autocompletar del ítem en mano del editor de comidas no funciona (reflexión a una API eliminada) | [AquaCheffPage.java:92](../src/main/java/mx/jume/aquahunger/ui/AquaCheffPage.java#L92) | API de inventario del ECS |
| B15 | M | `ServerVersion` del manifest = la última de Maven al compilar, no la versión probada | [build.gradle.kts:42](../build.gradle.kts#L42) | rango fijo y verificado |
| B16 | B | Páginas de config: "Cancelar" guarda; un número inválido aborta el resto de campos (un solo `try`); no se validan rangos | `Aqua*ConfigPage` | validar campo por campo, con Guardar y Cancelar reales |
| B17 | B | Cualquier ítem con "Canteen" en el ID (de cualquier mod) siempre da 0 de hambre | [FeedInteraction.java:63](../src/main/java/mx/jume/aquahunger/interactions/FeedInteraction.java#L63) | usar la config por ítem en vez de buscar texto |
| B18 | B | Las lambdas diferidas de los bridges guardan referencias a componentes, que pueden quedar obsoletas tras reaparecer o cambiar de mundo | bridges de RPG y Endless | releer por `PlayerRef` |
| B19 | B | Los constructores de copia de los componentes no copian temporizadores ni flags (la XP de resiliencia se puede duplicar) | [HungerComponent.java:72](../src/main/java/mx/jume/aquahunger/components/HungerComponent.java#L72), `ThirstComponent` | copiar todos los campos |
| B20 | B | En cada tick con sed > 0 se recorren y quitan los efectos de deshidratación, y se vuelve a aplicar el buff de stamina | `ThirstSystem` | actuar solo en las transiciones |
| B21 | B | Comandos sin registrar: `RpgStatusCommand`, `HungryHideCommand`, `HungryShowCommand` (código muerto) | `commands/` | registrarlos como subcomandos útiles o eliminarlos |
| B22 | B | Textos fijos en inglés en las páginas ("System Reloaded Successfully.", "WARNING: …") y traducciones a mano en `getCasualName` | `ui/`, `MMOSkillTreeBridge` | moverlos a `lang/` |
| B23 | A | Endless Leveling ahora se registra como `Airijko:EndlessLevelingCore`; con el ID viejo del manifest y el class loader de 0.7 el bridge nunca encuentra su API | `build.gradle.kts`, `EndlessLevelingBridge` | ID nuevo y carga por el class loader del mod |
| B24 | A | MMO 1.7 movió `SkillTreeService` y `SkillReward` a `com.ziggfreed.mmoskilltree.skilltree`: la lectura de recompensas falla, el sistema Glotón queda apagado y los bonos del árbol pasan al cálculo por nivel | `MMOSkillTreeBridge` | probar el paquete nuevo y el viejo |
| B25 | M | La recompensa final de Hidratación "Super Healthy" (`thirst_healthy_lifestyle`) no hace nada: el código buscaba `thirst_king_fitness` y `thirst_super_healthy`, que nunca existieron en el árbol | `MMOSkillTreeBridge` | ID real y prueba que cruza el código con la plantilla |
| B26 | M | En 0.7 el class loader de un plugin solo ve sus dependencias declaradas: la detección de AquaSanity por nombre de clase siempre falla y el aviso de bienvenida sale aunque AquaSanity esté instalado | `AquaThirstHunger`, `WelcomeNoticeManager` | detectar por ID de plugin (`PluginManager`) |
| B27 | B | El proxy del listener de RPG devolvía `null` en `equals`/`hashCode` (excepción si RPG guarda los listeners en un conjunto) y nunca se desregistraba | `RPGLevelingBridge` | proxy completo y `shutdown()` |

Además: los `CODEC` de Hytale de las clases de config no se usan (los archivos se leen con Gson), lo que duplica el mantenimiento; y `SuperlativeManager` se reinicia con el servidor (se podría persistir).

---

## 8. Arquitectura objetivo

```
AquaThirstHunger (plugin)
├─ setup():  config (+ migración 1.8.0) · componentes · sistemas · interacciones · comandos
│            · MMO: escribir el árbol antes de que cargue (LoadBefore)
├─ start():  IntegrationManager.bind() → log del estado de cada integración
├─ ui/
│   ├─ HungerHud, ThirstHud, NoticeHud   (CustomUIHud nativos con clave)
│   └─ HudService                         (añadir en PlayerReady, dirty flag por tick,
│                                          posición y config en caliente, ocultar en espectador)
├─ systems/  StarveSystem · ThirstSystem · HungerLifeSystem · OnBlockHit · Respawn
├─ compat/
│   ├─ IntegrationManager (adaptadores, MethodHandles en caché, XP vía world.execute)
│   ├─ RpgLevelingAdapter · EndlessLevelingAdapter · MmoSkillTreeAdapter
│   └─ AquaSanityContract (notified_players + aviso)
└─ config/   Gson + migraciones con backup (misma ruta de archivos que hoy)
```

Principios:
- Solo APIs oficiales del servidor: nada de interceptar paquetes de otros mods ni de reflexión sobre partes internas del motor.
- Cada integración opcional se resuelve por reflexión, con verificación de firmas al arrancar y degradación limpia: si otro mod cambia su API, se desactiva esa función, se registra en el log y el resto sigue funcionando.
- Cualquier modificación del ECS desde fuera de un sistema pasa por `world.execute`.

---

## 9. Plan por fases

Cada fase termina con build verde y un commit. Las fases 0–3 no dependen de JARs de terceros.

### Fase 0 — Build reproducible
- Compilar contra la API oficial: repositorio `https://maven.hytale.com/release` con `compileOnly("com.hypixel.hytale:Server:0.7.2")` (la mínima soportada), en lugar del JAR copiado en `libs/`.
- `ServerVersion` fijo como rango probado (por ejemplo `>=0.7.2 <0.8.0`; hay que confirmar la sintaxis de `SemverRange`) y la versión del mod (`1.1.0`) definida en un solo lugar.
- Empaquetar `ExternalFoodsDefaultConfig.json` (B02) moviéndolo a `src/main/resources/`.
- Corregir los IDs del manifest (`Ziggfreed:MMOSkillTree`; verificar `airijko:EndlessLeveling`).
- **Aceptación:** `./gradlew jar` limpio, y el JAR contiene el manifest correcto y el JSON por defecto.

### Fase 1 — Migración mecánica de API
- JOML (`org.joml.Vector3d`) en sistemas y Glutton; `requireNoPermission()`; entrega de ítems con `Player.giveItem` más el `generateItemDrops` nuevo; autocompletar del editor con el inventario del ECS (B14); comparar efectos por índice (`getEntityEffectIndex`) en vez de por reflexión.
- **Aceptación:** compila contra 0.7.2 y el gameplay ya no depende del paquete `aquahud`.

### Fase 2 — HUD nativo
- Implementar §5 (`HungerHud`, `ThirstHud`, `NoticeHud` y `HudService`) y eliminar `aquahud` y sus `.ui`.
- Cambio de modo de juego vía `ChangeGameModeEvent`, leyendo el modo en el tick siguiente. El watcher de `SetGameMode` solo se mantiene si las pruebas muestran que el evento no cubre `/gamemode`.
- Comprobar en pantalla las 8 posiciones preset; las constantes 702/351/176 asumían el layout del contenedor anterior y pueden necesitar recalibración.
- **Aceptación:** HUD correcto tras login, cambio de mundo, muerte, cambio de modo, `/aquahungerreload` y cambio de posición, sin reiniciar; sin parpadeos; convive con otro mod que tenga HUD custom.

### Fase 3 — Gameplay y config
- B03, B05–B07, B09–B12, B16–B20 y B22; constructores de copia; validaciones; logger.
- Migración `ConfigVersion 1.7.5 → 1.8.0` con backup: añadir los campos nuevos sin pisar los valores existentes del usuario.
- **Aceptación:** pruebas manuales de gameplay de §10, y configs antiguas de 1.0.0 que migran sin perder valores.

### Fase 4 — Integraciones (hecha en código, ver §6.1)
- Conseguir los JARs actuales de RPG Leveling, Endless Leveling y MMO Skill Tree (+ ZiggfreedCommon) en `libs/optional/` (ignorado por git; solo para inspeccionar la API, no se empaquetan).
- Rehacer los adaptadores (§6): inicialización real de RPG, firmas nuevas de EL V11, `LoadBefore` y escritura temprana para MMO (sin reinicios), XP fuera del tick y sin hacks de HUD.
- Comando `/aquahunger integrations` (solo op) que muestre qué integración y qué función están activas.
- Resolver la semántica de "SURVIVAL" (B08) según tu decisión.
- **Aceptación:** con cada mod presente o ausente, el servidor arranca sin errores, y el log y el comando muestran el estado correcto; XP y multiplicadores verificados en juego.

### Fase 5 — Documentación
- README y descripción de CurseForge: requisitos (Hytale ≥ 0.7.2), quitar la nota de AutoMultiHUD/MultipleHUD y el "reiniciar 2 veces", lista real de comandos y permisos, changelog de la 1.1.0.
- Documentar en `docs/` lo que hoy no está documentado: la cadena de resolución de valores, el formato de `ExternalFoodsConfig.json` (con el comodín `*`) y el contrato con AquaSanity.

### Fase 6 — Verificación y release
- Ver §10. Tag `v1.1.0`; la subida del JAR a CurseForge la haces tú.

---

## 10. Estrategia de pruebas

1. **Compilación** contra 0.7.2 (la mínima) y arranque con 0.7.4 (la última).
2. **Servidor dedicado local** con el mod: revisar el log en busca de errores de plugin y de validación de assets (ítems, interacciones, efectos, sonidos, recetas, bancos).
3. **Matriz en juego** (cliente 0.7.x):
   - Combinaciones: solo el mod · + RPG Leveling · + Endless Leveling · + MMO Skill Tree · + los tres · + otro mod con HUD custom · + AquaSanity actualizado.
   - Escenarios: primer login, relogin, cambio de mundo, muerte y reaparición (con reset activado y desactivado), creativo ↔ aventura, espectador, comer/beber/cancelar, carne cruda, cantimploras y botellas (rellenar en agua y en bancos), quieto/minando/sprint, life-per-hunger, presets, cada página de config, recarga y posiciones del HUD.
4. **Configs heredadas:** arrancar con los configs de 1.0.0 (y con uno sin `ConfigVersion`) y comprobar la migración y los backups.
5. Checklist de no regresión de §3 completa.

---

## 11. Riesgos

| Riesgo | Mitigación |
|---|---|
| Las APIs de los mods de terceros cambiaron (EL V11, MMO 1.5, RPG 0.3) | Inspeccionar los JARs actuales; reflexión con verificación de firma y degradación limpia |
| Las posiciones del HUD se ven distintas con el render nativo | Recalibrar en juego los 8 presets |
| Conflicto con otros mods que sobrescriben `Consume_Charge_Food_*` | Documentarlo y mantener los overrides al mínimo |
| Hytale sigue en Early Access (de 0.7.x a 0.8) | Rango de `ServerVersion` acotado, una sola dependencia de compilación fija y `docs/` al día |
| Pérdida de configs de usuarios | Misma ruta de archivos, migración con backup y prueba con configs reales de 1.0.0 |

---

## 12. Decisiones que necesito de ti

1. **Gestores de HUD:** eliminar por completo el soporte de MultipleHUD/AutoMultiHUD y usar solo el HUD nativo. *Recomendado: sí.*
2. **Sed máxima por nivel (B08):** (a) quitarla; (b) mantenerla, pero que el HUD y el buff usen el máximo real; (c) dejarla solo con MMO y con una skill que exista. *Recomendado: (b).*
3. **JARs de los mods opcionales** para la fase 4: me los pasas tú, o autorizas que los descargue de CurseForge. *Recomendado: autorizar la descarga; solo se inspeccionan, no se ejecutan.*
4. **Número de versión:** `1.1.0` (recomendado) o `2.0.0`, dado que pasa a requerir Hytale ≥ 0.7.
5. **AquaSanity:** migrarlo después en una tarea aparte (recomendado) o en paralelo.

---

## Anexo A — Errores de compilación contra 0.7.2

| Archivo | Errores | Causa |
|---|---|---|
| `aquahud/AquaHudBridge.java` | 5 | `setCustomHud` / `getCustomHud()` |
| `aquahud/AquaMultiHUD.java` | 7 | constructor de `CustomUIHud`, `getCustomHud()`, `setCustomHud` |
| `aquahud/AquaHudWatcher.java` | 2 | `getCustomHud()`, `setCustomHud` |
| `ui/HHMHud.java`, `ui/HHMThirstHud.java` | 2 | constructor de `CustomUIHud` |
| `systems/StarveSystem.java`, `systems/ThirstSystem.java` | 4 | `Vector3d` |
| `compat/MMOSkillTreeBridge.java` | 5 | `Vector3d` / `Vector3f` |
| `commands/AquaHungerOkCommand.java`, `commands/AquaHungerResetCommand.java` | 2 | `canGeneratePermission` |

## Anexo B — Archivos afectados

- **Eliminar:** `aquahud/*` (3 clases), `HUD/MultipleHUD.ui`, `HUD/HHMMultipleHUD.ui`, `resources/ui/WelcomeNotice.ui`, `test/APIDiscovery.java`. `mods/Aqua-Thirst-hunger/ExternalFoodsDefaultConfig.json` se mueve a `src/main/resources/`.
- **Crear:** `ui/HudService.java`, `ui/NoticeHud.java`, los adaptadores en `compat/` y la documentación en `docs/`.
- **Modificar:** buena parte de `systems/`, `ui/`, `compat/`, `config/` y `events/`, además de `build.gradle.kts` y el README.
