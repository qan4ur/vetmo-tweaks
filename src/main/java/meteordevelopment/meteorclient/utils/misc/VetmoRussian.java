/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vetmo Tweaks: Russian display texts for modules, settings, setting groups, categories and enum values.
 * <p>
 * Only display fields (title / description) are changed. Internal names (module name, setting name,
 * group name, category name) stay English, so commands and saved configs keep working.
 * <p>
 * Keys: {@code "module"} for the module, {@code "module.setting"} for a setting, and
 * {@code "module.setting@Group"} when a module has two settings with the same name in different groups.
 * Anything missing keeps its English text.
 */
public final class VetmoRussian {
    private static final Map<String, String[]> ENTRIES = new HashMap<>();
    private static final Map<String, String> GROUPS = new HashMap<>();
    private static final Map<String, String> CATEGORIES = new HashMap<>();
    private static final Map<String, String> ENUMS = new HashMap<>();

    private VetmoRussian() {}

    // Applying

    /** Translates all registered modules, their settings and the categories. Call once after modules are created. */
    public static void apply() {
        List<String> missing = new ArrayList<>();
        int translated = 0;

        for (Category category : Modules.loopCategories()) {
            String t = CATEGORIES.get(category.name);
            if (t != null) category.title = t;
        }

        for (Module module : Modules.get().getAll()) {
            String[] e = ENTRIES.get(module.name);
            if (e != null) {
                module.title = e[0];
                if (e[1] != null) module.description = e[1];
                translated++;
            }
            else missing.add(module.name);

            translated += applySettings(module.name, module.settings, missing);
        }

        MeteorClient.LOG.info("Vetmo: applied {} Russian translations.", translated);
        if (!missing.isEmpty()) MeteorClient.LOG.debug("Vetmo: missing Russian translations: {}", missing);
    }

    /** Translates a standalone settings object (e.g. the BlockESP per-block config screen). */
    public static void applySettings(String prefix, Settings settings) {
        List<String> missing = new ArrayList<>();
        applySettings(prefix, settings, missing);
        if (!missing.isEmpty()) MeteorClient.LOG.debug("Vetmo: missing Russian translations: {}", missing);
    }

    private static int applySettings(String prefix, Settings settings, List<String> missing) {
        int translated = 0;

        for (SettingGroup group : settings) {
            for (Setting<?> setting : group) {
                String key = prefix + "." + setting.name;
                String[] e = ENTRIES.get(key + "@" + group.name);
                if (e == null) e = ENTRIES.get(key);

                if (e != null) {
                    setting.title = e[0];
                    if (e[1] != null) setting.description = e[1];
                    translated++;
                }
                else missing.add(key);
            }
        }

        return translated;
    }

    /** Display title for a setting group name. */
    public static String group(String name) {
        return GROUPS.getOrDefault(name, name);
    }

    /** Display text for a dropdown value. Only enum values are translated. */
    public static String value(Object value) {
        if (value == null) return "null";
        String str = value.toString();
        if (!(value instanceof Enum<?> e)) return str;

        String t = ENUMS.get(e.getDeclaringClass().getSimpleName() + "." + str);
        if (t == null) t = ENUMS.get(str);
        return t != null ? t : str;
    }

    // Table helpers

    private static void m(String key, String title, String description) {
        ENTRIES.put(key, new String[] { title, description });
    }

    private static void s(String key, String title, String description) {
        ENTRIES.put(key, new String[] { title, description });
    }

    static {
        // Categories
        CATEGORIES.put("Combat", "Бой");
        CATEGORIES.put("Player", "Игрок");
        CATEGORIES.put("Render", "Рендер");
        CATEGORIES.put("World", "Мир");

        // Setting groups
        GROUPS.put("General", "Основное");
        GROUPS.put("Colors", "Цвета");
        GROUPS.put("Appearance", "Внешний вид");
        GROUPS.put("Attack Indicator", "Индикатор атаки");
        GROUPS.put("Crits", "Криты");
        GROUPS.put("Custom Shape", "Своя форма");
        GROUPS.put("Death Position", "Место смерти");
        GROUPS.put("Dynamic", "Динамика");
        GROUPS.put("Fog", "Туман");
        GROUPS.put("HUD", "HUD");
        GROUPS.put("Items", "Предметы");
        GROUPS.put("Lightning", "Молния");
        GROUPS.put("Main Hand", "Основная рука");
        GROUPS.put("Off Hand", "Вторая рука");
        GROUPS.put("Opened Rendering", "Открытые контейнеры");
        GROUPS.put("Particles", "Частицы");
        GROUPS.put("Parts", "Элементы");
        GROUPS.put("Players", "Игроки");
        GROUPS.put("Render", "Отображение");
        GROUPS.put("Sky", "Небо");
        GROUPS.put("Sound", "Звук");
        GROUPS.put("Style", "Стиль");
        GROUPS.put("Time", "Время");
        GROUPS.put("Tracer", "Трейсер");

        // Enum values (key "Value" or "EnumClass.Value" for a specific enum)
        ENUMS.put("Smart", "Умный");
        ENUMS.put("Strict", "Строгий");
        ENUMS.put("Disabled", "Выкл");
        ENUMS.put("Hold", "Удержание");
        ENUMS.put("Press", "Нажатие");
        ENUMS.put("Pearl", "Жемчуг");
        ENUMS.put("XP", "Опыт");
        ENUMS.put("Rocket", "Фейерверк");
        ENUMS.put("Bow", "Лук");
        ENUMS.put("Gap", "Золотое яблоко");
        ENUMS.put("EGap", "Зачарованное яблоко");
        ENUMS.put("Chorus", "Хорус");
        ENUMS.put("AddFriend", "Добавить в друзья");
        ENUMS.put("Box", "Коробка");
        ENUMS.put("Wireframe", "Каркас");
        ENUMS.put("Shader", "Шейдер");
        ENUMS.put("Glow", "Свечение");
        ENUMS.put("Gamma", "Гамма");
        ENUMS.put("Potion", "Зелье");
        ENUMS.put("Luminance", "Освещённость");
        ENUMS.put("SKY", "Небо");
        ENUMS.put("BLOCK", "Блоки");
        ENUMS.put("Above", "Над предметом");
        ENUMS.put("OnTop", "Поверх");
        ENUMS.put("None", "Нет");
        ENUMS.put("Total", "Всего");
        ENUMS.put("Percentage", "Проценты");
        ENUMS.put("Gradient", "Градиент");
        ENUMS.put("Flat", "Один цвет");
        ENUMS.put("Lines", "Линии");
        ENUMS.put("Sides", "Стороны");
        ENUMS.put("Both", "Оба");
        ENUMS.put("Offscreen", "За экраном");
        ENUMS.put("Head", "Голова");
        ENUMS.put("Body", "Тело");
        ENUMS.put("Feet", "Ноги");
        ENUMS.put("Horizontal", "Горизонтально");
        ENUMS.put("Vertical", "Вертикально");
        ENUMS.put("Percent", "Проценты");
        ENUMS.put("Bar", "Полоска");
        ENUMS.put("Radial", "Радиально");
        ENUMS.put("Sweep", "Заливка");
        ENUMS.put("Number", "Число");
        ENUMS.put("Hearts", "Сердца");
        ENUMS.put("Cross", "Крест");
        ENUMS.put("Dot", "Точка");
        ENUMS.put("Circle", "Круг");
        ENUMS.put("Cross + Dot", "Крест + точка");
        ENUMS.put("Style.Custom", "Своя");
        ENUMS.put("Preset.Custom", "Своя");
        ENUMS.put("Classic 1.7", "Классика 1.7");
        ENUMS.put("Follow vanilla option", "Как в настройках игры");
        ENUMS.put("Always", "Всегда");
        ENUMS.put("Off", "Выкл");
        ENUMS.put("Vanilla", "Ванилла");
        ENUMS.put("Multiplier", "Множитель");
        ENUMS.put("FogMode.Disabled", "Без тумана");
        ENUMS.put("Dust", "Пыль");
        ENUMS.put("Crit", "Крит");
        ENUMS.put("Enchanted Hit", "Зачарованный удар");
        ENUMS.put("Heart", "Сердце");
        ENUMS.put("Flame", "Пламя");
        ENUMS.put("Soul Fire", "Огонь душ");
        ENUMS.put("Thunder", "Гром");
        ENUMS.put("Level Up", "Новый уровень");
        ENUMS.put("XP Orb", "Сфера опыта");
        ENUMS.put("Anvil", "Наковальня");
        ENUMS.put("Bell", "Колокол");
        ENUMS.put("Totem", "Тотем");
        ENUMS.put("Wither Spawn", "Появление визера");
        ENUMS.put("Dragon Growl", "Рык дракона");
        ENUMS.put("Blaze Death", "Смерть ифрита");
        ENUMS.put("Sine", "Плавно");
        ENUMS.put("Heartbeat", "Сердцебиение");
        ENUMS.put("Static", "Статично");
        ENUMS.put("Duration", "По длительности");
        ENUMS.put("Sort.Name", "По названию");
        ENUMS.put("Harmful", "Сначала вредные");
        ENUMS.put("Same", "Как остальные");
        ENUMS.put("Highlight", "Подсветка");
        ENUMS.put("Ignore", "Игнорировать");
        ENUMS.put("Chat", "Чат");
        ENUMS.put("Toast", "Всплывающее");
        ENUMS.put("Hide", "Скрыть");

        // ===================== Player =====================

        m("auto-totem", "Автототем", "Автоматически берёт тотем во вторую руку.");
        s("auto-totem.mode", "Режим", "Умный - тотем только при опасности. Строгий - тотем во второй руке всегда.");
        s("auto-totem.delay", "Задержка", "Тиков между перекладываниями тотема.");
        s("auto-totem.health", "Здоровье", "При каком здоровье брать тотем (умный режим).");
        s("auto-totem.elytra", "Элитры", "Всегда держать тотем во время полёта на элитрах.");
        s("auto-totem.fall", "Падение", "Брать тотем, если урон от падения может убить.");
        s("auto-totem.explosion", "Взрывы", "Брать тотем, если урон от взрыва может убить.");

        m("auto-clicker", "Автокликер", "Автоматически кликает за вас.");
        s("auto-clicker.while-in-screens", "В меню", "Кликать, даже когда открыт экран или меню.");
        s("auto-clicker.mode-left", "Режим ЛКМ", "Как кликать левой кнопкой.");
        s("auto-clicker.delay-left", "Задержка ЛКМ", "Задержка между кликами левой кнопкой в тиках.");
        s("auto-clicker.mode-right", "Режим ПКМ", "Как кликать правой кнопкой.");
        s("auto-clicker.delay-right", "Задержка ПКМ", "Задержка между кликами правой кнопкой в тиках.");

        m("middle-click-extra", "Действие на СКМ", "Выполняет действие при нажатии средней кнопки мыши.");
        s("middle-click-extra.mode", "Режим", "Какой предмет использовать по нажатию СКМ.");
        s("middle-click-extra.message", "Сообщение", "Написать игроку, когда вы добавляете его в друзья.");
        s("middle-click-extra.quick-swap", "Быстрая смена", "Использовать предметы из инвентаря через нажатия клавиш хотбара. Античит может это заметить.");
        s("middle-click-extra.swap-back", "Вернуть слот", "Вернуться в исходный слот после использования предмета.");
        s("middle-click-extra.notify", "Уведомлять", "Сообщать, если нужного предмета нет в хотбаре.");

        // ===================== Render =====================

        m("esp", "ESP", "Показывает сущности сквозь стены.");
        s("esp.mode", "Режим", "Способ отображения.");
        s("esp.outline-width", "Толщина обводки", "Толщина обводки шейдера.");
        s("esp.glow-multiplier", "Сила свечения", "Множитель эффекта свечения.");
        s("esp.ignore-self", "Без себя", "Не рисовать шейдер на себе.");
        s("esp.shape-mode", "Тип формы", "Как рисуются фигуры.");
        s("esp.fill-opacity", "Прозрачность заливки", "Непрозрачность заливки фигуры.");
        s("esp.fade-distance", "Дистанция затухания", "Расстояние до сущности, с которого цвет начинает затухать.");
        s("esp.entities", "Сущности", "Какие сущности показывать.");
        s("esp.distance-colors", "Цвет по дистанции", "Менять цвет в зависимости от расстояния.");
        s("esp.show-friend-colors", "Цвет друзей", "Показывать друзей их цветом вместо цвета по дистанции.");
        s("esp.players-color", "Цвет игроков", "Цвет других игроков.");
        s("esp.animals-color", "Цвет животных", "Цвет животных.");
        s("esp.water-animals-color", "Цвет водных животных", "Цвет водных животных.");
        s("esp.monsters-color", "Цвет монстров", "Цвет монстров.");
        s("esp.ambient-color", "Цвет фоновых мобов", "Цвет фоновых мобов (летучие мыши и т.п.).");
        s("esp.misc-color", "Цвет прочего", "Цвет остальных сущностей.");

        m("freecam", "Freecam", "Позволяет камере отлетать от игрока.");
        s("freecam.speed", "Скорость", "Скорость полёта во Freecam.");
        s("freecam.speed-scroll-sensitivity", "Скорость колесом", "Менять скорость колесом мыши. 0 - отключить.");
        s("freecam.stay-sneaking", "Оставаться крадущимся", "Если вы крались при включении, персонаж продолжит красться.");
        s("freecam.toggle-on-damage", "Выкл при уроне", "Выключать Freecam, когда вы получаете урон.");
        s("freecam.toggle-on-death", "Выкл при смерти", "Выключать Freecam после смерти.");
        s("freecam.toggle-on-log", "Выкл при выходе", "Выключать Freecam при отключении от сервера.");
        s("freecam.reload-chunks", "Перезагрузить чанки", "Отключает отсечение пещер.");
        s("freecam.show-hands", "Показывать руки", "Рисовать руки во Freecam.");
        s("freecam.rotate", "Поворачиваться", "Поворачивать персонажа к блоку или сущности, на которую вы смотрите.");
        s("freecam.static", "Статичная камера", "Отключает настройки, которые двигают обзор.");

        m("fullbright", "Полная яркость", "Делает мир светлым.");
        s("fullbright.mode", "Режим", "Способ осветления.");
        s("fullbright.light-type", "Тип света", "Какой свет использовать в режиме «Освещённость».");
        s("fullbright.minimum-light-level", "Мин. уровень света", "Минимальный уровень света в режиме «Освещённость».");

        m("nametags", "Неймтеги", "Настраиваемые таблички над игроками, предметами и другими сущностями.");
        s("nametags.entities", "Сущности", "Над какими сущностями рисовать таблички.");
        s("nametags.scale", "Размер", "Размер таблички.");
        s("nametags.ignore-self", "Без себя", "Не показывать свою табличку от третьего лица или во Freecam.");
        s("nametags.ignore-friends", "Без друзей", "Не показывать таблички друзей.");
        s("nametags.ignore-bots", "Без ботов", "Показывать таблички только не-ботов.");
        s("nametags.culling", "Ограничение", "Показывать ограниченное число табличек на ограниченной дистанции.");
        s("nametags.culling-range", "Дальность", "Показывать таблички только в пределах этого расстояния.");
        s("nametags.culling-count", "Количество", "Сколько табличек показывать максимум.");
        s("nametags.health", "Здоровье", "Показывать здоровье игрока.");
        s("nametags.gamemode", "Режим игры", "Показывать режим игры игрока.");
        s("nametags.distance", "Дистанция", "Показывать расстояние до игрока.");
        s("nametags.ping", "Пинг", "Показывать пинг игрока.");
        s("nametags.items", "Предметы", "Показывать броню и предметы в руках над табличкой.");
        s("nametags.item-spacing", "Отступ предметов", "Расстояние между предметами.");
        s("nametags.ignore-empty-slots", "Пропускать пустые", "Не оставлять место под пустые слоты.");
        s("nametags.durability", "Прочность", "Показывать прочность: числом, в процентах или никак.");
        s("nametags.display-enchants", "Зачарования", "Показывать зачарования на предметах.");
        s("nametags.shown-enchantments", "Список зачарований", "Какие зачарования показывать.");
        s("nametags.enchantment-position", "Позиция зачарований", "Где рисовать зачарования.");
        s("nametags.enchant-name-length", "Длина названия", "До скольких символов сокращать названия зачарований.");
        s("nametags.enchant-text-scale", "Размер текста зачарований", "Размер текста зачарований.");
        s("nametags.show-count", "Количество", "Показывать число предметов в стаке.");
        s("nametags.background-color", "Цвет фона", "Цвет фона таблички.");
        s("nametags.name-color", "Цвет имени", "Цвет имени на табличке.");
        s("nametags.ping-color", "Цвет пинга", "Цвет пинга на табличке.");
        s("nametags.gamemode-color", "Цвет режима игры", "Цвет режима игры на табличке.");
        s("nametags.distance-color-mode", "Цвет дистанции: режим", "Как окрашивать дистанцию на табличке.");
        s("nametags.distance-color", "Цвет дистанции", "Цвет дистанции на табличке.");

        m("storage-esp", "ESP хранилищ", "Показывает выбранные хранилища сквозь стены.");
        s("storage-esp.mode", "Режим", "Способ отображения.");
        s("storage-esp.storage-blocks", "Хранилища", "Какие хранилища показывать.");
        s("storage-esp.tracers", "Трейсеры", "Рисовать линии к хранилищам.");
        s("storage-esp.shape-mode", "Тип формы", "Как рисуются фигуры.");
        s("storage-esp.fill-opacity", "Прозрачность заливки", "Непрозрачность заливки фигуры.");
        s("storage-esp.width", "Толщина обводки", "Толщина обводки шейдера.");
        s("storage-esp.glow-multiplier", "Сила свечения", "Множитель эффекта свечения.");
        s("storage-esp.chest", "Сундук", "Цвет сундуков.");
        s("storage-esp.trapped-chest", "Сундук-ловушка", "Цвет сундуков-ловушек.");
        s("storage-esp.barrel", "Бочка", "Цвет бочек.");
        s("storage-esp.shulker", "Шалкер", "Цвет шалкеров.");
        s("storage-esp.ender-chest", "Эндер-сундук", "Цвет эндер-сундуков.");
        s("storage-esp.other", "Прочее", "Цвет печей, раздатчиков, выбрасывателей и воронок.");
        s("storage-esp.fade-distance", "Дистанция затухания", "Расстояние, на котором цвет начинает затухать.");
        s("storage-esp.hide-opened", "Скрывать открытые", "Не показывать уже открытые контейнеры.");
        s("storage-esp.opened-color", "Цвет открытых", "Другой цвет для открытых контейнеров вместо скрытия. При нулевой прозрачности выключено.");

        m("tracers", "Трейсеры", "Рисует линии к выбранным сущностям.");
        s("tracers.entities", "Сущности", "К каким сущностям рисовать линии.");
        s("tracers.ignore-self", "Без себя", "Не рисовать линию к себе от третьего лица или во Freecam.");
        s("tracers.ignore-friends", "Без друзей", "Не рисовать линии к друзьям.");
        s("tracers.show-invisible", "Невидимые", "Показывать невидимых сущностей.");
        s("tracers.style", "Стиль", "Как отображать трейсеры.");
        s("tracers.target", "Цель", "К какой части сущности вести линию.");
        s("tracers.stem", "Стержень", "Рисовать линию через центр цели.");
        s("tracers.max-distance", "Макс. дистанция", "Максимальное расстояние, на котором видны трейсеры.");
        s("tracers.distance-offscreen", "Отступ от центра", "Расстояние стрелок от центра экрана (режим «За экраном»).");
        s("tracers.size-offscreen", "Размер стрелок", "Размер стрелок в режиме «За экраном».");
        s("tracers.blink-offscreen", "Мигание", "Стрелки мигают.");
        s("tracers.blink-offscreen-speed", "Скорость мигания", "Скорость мигания стрелок.");
        s("tracers.distance-colors", "Цвет по дистанции", "Менять цвет линий в зависимости от расстояния.");
        s("tracers.show-friend-colors", "Цвет друзей", "Показывать друзей их цветом вместо цвета по дистанции.");
        s("tracers.players-colors", "Цвет игроков", "Цвет игроков.");
        s("tracers.animals-color", "Цвет животных", "Цвет животных.");
        s("tracers.water-animals-color", "Цвет водных животных", "Цвет водных животных.");
        s("tracers.monsters-color", "Цвет монстров", "Цвет монстров.");
        s("tracers.ambient-color", "Цвет фоновых мобов", "Цвет фоновых мобов (летучие мыши и т.п.).");
        s("tracers.misc-color", "Цвет прочего", "Цвет остальных сущностей.");

        m("waypoints", "Метки", "Позволяет создавать метки (вейпоинты).");
        s("waypoints.text-render-distance", "Дистанция текста", "Как далеко от центра экрана ещё показывается текст метки.");
        s("waypoints.waypoint-fade-distance", "Дистанция затухания", "Расстояние до метки, с которого она начинает исчезать.");
        s("waypoints.max-death-positions", "Мест смерти", "Сколько мест смерти сохранять. 0 - отключить.");
        s("waypoints.chat", "Сообщение в чат", "Писать в чат координаты после смерти.");

        m("xray", "Xray", "Показывает только выбранные блоки. Удобно для поиска руды.");
        s("xray.whitelist", "Блоки", "Какие блоки оставить видимыми.");
        s("xray.opacity", "Прозрачность", "Непрозрачность остальных блоков.");
        s("xray.exposed-only", "Только открытые", "Показывать только руду, соприкасающуюся с воздухом.");

        m("zoom", "Зум", "Приближает обзор.");
        s("zoom.zoom", "Приближение", "Насколько приближать.");
        s("zoom.scroll-sensitivity", "Зум колесом", "Менять приближение колесом мыши. 0 - отключить.");
        s("zoom.smooth", "Плавно", "Плавный переход.");
        s("zoom.cinematic", "Кинокамера", "Включает плавную кинематографическую камеру.");
        s("zoom.hide-HUD", "Скрыть HUD", "Скрывать интерфейс Minecraft.");
        s("zoom.show-hands", "Показывать руки", "Рисовать руки при зуме.");

        m("block-esp", "ESP блоков", "Показывает выбранные блоки сквозь стены.");
        s("block-esp.blocks", "Блоки", "Какие блоки искать.");
        s("block-esp.default-block-config", "Настройка по умолчанию", "Вид блоков по умолчанию.");
        s("block-esp.block-configs", "Настройки блоков", "Отдельный вид для каждого блока.");
        s("block-esp.tracers", "Трейсеры", "Рисовать линии к блокам.");

        // BlockESP per-block config screen (ESPBlockDataScreen)
        s("block-esp-block.shape-mode", "Тип формы", "Как рисуется фигура.");
        s("block-esp-block.line-color", "Цвет линий", "Цвет линий.");
        s("block-esp-block.side-color", "Цвет сторон", "Цвет сторон.");
        s("block-esp-block.tracer", "Трейсер", "Рисовать линию к этому блоку.");
        s("block-esp-block.tracer-color", "Цвет трейсера", "Цвет линии к блоку.");

        // ===================== World =====================

        m("stash-finder", "Поиск тайников", "Ищет хранилища в загруженных чанках. Сохраняет в папку vetmo-tweaks.");
        s("stash-finder.storage-blocks", "Хранилища", "Какие хранилища искать.");
        s("stash-finder.minimum-storage-count", "Мин. количество", "Сколько хранилищ должно быть в чанке, чтобы его записать.");
        s("stash-finder.minimum-distance", "Мин. дистанция", "Как далеко от спавна нужно быть, чтобы записывать чанки.");
        s("stash-finder.notifications", "Уведомления", "Уведомлять о найденных тайниках.");
        s("stash-finder.notification-mode", "Тип уведомлений", "Как уведомлять.");
    }
}
