# Aqua-Thirst-hunger 🌊🥪

> [!NOTE]
> Este proyecto es un **fork** del original [HytaleHungerMod](https://github.com/Aex12/HytaleHungerMod) de **Aex12**.
> Un agradecimiento especial a Aex12 por la base sólida y a la comunidad de modding de Hytale por sus valiosos recursos.

---

**Aqua-Thirst-hunger** es un mod de supervivencia avanzado para Hytale que introduce mecánicas de **Hambre** y **Sed**. Expande el trabajo original con un enfoque en el realismo, la dificultad y la integración con otros mods de comida como *AndieChef*.

## ✨ Características Principales (¿Qué hay de nuevo?)

* **Sistema de Sed**: Un sistema de hidratación paralelo con su propia interfaz (HUD), lógica de agotamiento y efectos de deshidratación.
* **Integración con AndieChef**: Soporte nativo para comidas complejas (Sushi, Yakimeshi, Sake) con valores ajustados a mano.
* **Balance Hardcore**: Valores por defecto reducidos en un 75% para un verdadero desafío de supervivencia.
* **Hidratación por Frutas**: Consumir frutas otorga un bono de hidratación (multiplicador x5).
* **Envenenamiento por Carne Cruda**: Comer carne cruda conlleva un riesgo de envenenamiento (efecto Poison_T1).
* **Recarga Atómica de Configuración**: Actualiza tus ajustes dentro del juego con `/aquahunger reload` sin reiniciar el servidor.
* **Cantimplora Nativa**: Incluye una Cantimplora crafteable (desde el inventario o mesa de cocina) para restaurar la hidratación, esencial cuando no hay otros mods de comida.
* **Compatibilidad Universal**: Soporte para cualquier mod de comida; los objetos restauran sed automáticamente según su tier de rareza, a menos que se personalicen en la configuración.
* **Posicionamiento de HUD Mejorado**: Preajustes para barras de Hambre/Sed una al lado de la otra (BelowHotbarLeft/Right).

## 🛠️ Dependencias

* **[MultipleHUD](https://github.com/Buuz135/MHUD)**: **Obligatorio** para que las barras visuales se muestren correctamente junto a otros elementos de la interfaz.

## 📜 Comandos

* `/aquahunger`: Comando principal para gestión de hambre y recarga de configuración.
* `/aquathirst`: Gestión de niveles de sed.

---

## Características

* **Barra de Hambre**: Representación visual del nivel de hambre del jugador en el HUD.
* **Agotamiento de Hambre**: El hambre disminuye con el tiempo, influenciada por acciones como el uso de estamina o minar bloques.
* **Saturación**: Reserva por encima del valor del 100% que retrasa el agotamiento inicial.
* **Vista Previa de Restauración**: Previsualización en el HUD de los valores de restauración durante la animación de comer.
* **Efectos de Estado**: Velocidad reducida y sprint desactivado cuando el hambre es críticamente baja.
* **Daño por Inanición**: Pérdida de salud constante cuando el hambre llega a cero.
* **Zonas Seguras**: El agotamiento se pausa en áreas designadas como invulnerables.

## Acciones que afectan al hambre

* **Tasa Metabólica Basal**: Agotamiento lento incluso cuando el jugador está quieto.
* **Uso de Estamina**: Correr, bloquear y ataques cargados aceleran el agotamiento.
* **Golpear bloques**: Pequeño agotamiento por cada bloque golpeado, reducido al usar herramientas más eficientes.

## Configuración

Este mod crea archivos de configuración en `mods/Aqua-Thirst-hunger/`.

### Estadísticas de Hambre

* **Hambre Máxima**: 100 (no configurable).
* **Hambre Inicial/Reaparición**: Configurable (Por defecto: 50).
* **Tasas de Inanición**: Personalizables vía `HungerConfig.json`.

### Valores de Comida

* **Restauración por Tier**: Configura valores para niveles desde Común hasta Único en `FoodValuesConfig.json` (Hambre) y `ThirstFoodValuesConfig.json` (Sed).
* **Sobrescritura por Item**: Se pueden definir valores específicos para objetos individuales en ambos archivos de configuración.

## Rendimiento

Optimizado para servidores de alta población:

* Actualizaciones distribuidas entre múltiples ticks.
* Lógica basada en eventos siempre que sea posible.
* Actualizaciones parciales de la interfaz para minimizar el impacto en el TPS.

---

## Créditos y Licencia

Código original por [Aex12](https://github.com/Aex12). Mejorado por **jume**, **andiemg**, y **antigravity**.
Licenciado bajo GNU Affero General Public License.
