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
        CATEGORIES.put("Player", "Игрок");
        CATEGORIES.put("Render", "Рендер");
        CATEGORIES.put("World", "Мир");
        CATEGORIES.put("Visuals", "Визуалы");

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

        m("stash-finder", "Поиск тайников", "Ищет хранилища в загруженных чанках. Сохраняет в <папка minecraft>/meteor-client.");
        s("stash-finder.storage-blocks", "Хранилища", "Какие хранилища искать.");
        s("stash-finder.minimum-storage-count", "Мин. количество", "Сколько хранилищ должно быть в чанке, чтобы его записать.");
        s("stash-finder.minimum-distance", "Мин. дистанция", "Как далеко от спавна нужно быть, чтобы записывать чанки.");
        s("stash-finder.notifications", "Уведомления", "Уведомлять о найденных тайниках.");
        s("stash-finder.notification-mode", "Тип уведомлений", "Как уведомлять.");

        // ===================== Visuals =====================

        m("armor-hud", "HUD брони", "Показывает броню и предметы в руках с прочностью.");
        s("armor-hud.layout", "Расположение", "В ряд или в столбик.");
        s("armor-hud.durability", "Прочность", "Как показывать прочность.");
        s("armor-hud.main-hand", "Основная рука", "Показывать предмет в основной руке.");
        s("armor-hud.off-hand", "Вторая рука", "Показывать предмет во второй руке.");
        s("armor-hud.hide-empty", "Скрывать пустые", "Пропускать пустые слоты вместо пустой рамки.");
        s("armor-hud.warn-threshold", "Порог предупреждения", "Предметы с прочностью ниже этого % мигают красным.");
        s("armor-hud.blink", "Мигание", "Мигать предметами с низкой прочностью.");
        s("armor-hud.blink-speed", "Скорость мигания", "Миганий в секунду.");
        s("armor-hud.background@General", "Фон", "Рисовать панель под HUD.");
        s("armor-hud.text-shadow", "Тень текста", "Рисовать текст с тенью.");
        s("armor-hud.scale", "Размер", "Размер HUD (колесо мыши над элементом в редакторе HUD).");
        s("armor-hud.background@Colors", "Фон", "Цвет фона панели.");
        s("armor-hud.accent", "Акцент", "Акцентная линия и рамки слотов.");
        s("armor-hud.full-durability", "Полная прочность", "Цвет текста и полоски при полной прочности.");
        s("armor-hud.low-durability", "Низкая прочность", "Цвет текста и полоски при нулевой прочности.");
        s("armor-hud.warning", "Предупреждение", "Цвет мигания предметов ниже порога.");
        s("armor-hud.text", "Текст", "Цвет количества предметов.");

        m("block-outline", "Обводка блока", "Свой цвет, толщина и заливка обводки выбранного блока.");
        s("block-outline.line-width", "Толщина", "Толщина обводки в пикселях (примерно). 1 - обычные тонкие линии.");
        s("block-outline.fill", "Заливка", "Заливать выбранный блок полупрозрачным цветом.");
        s("block-outline.through-walls", "Сквозь стены", "Рисовать обводку поверх всего, а не с проверкой глубины, как в ванилле.");
        s("block-outline.line-color", "Цвет линии", "Цвет обводки.");
        s("block-outline.fill-color", "Цвет заливки", "Цвет заливки.");

        m("combo-counter", "Счётчик комбо", "Считает ваши удары подряд.");
        s("combo-counter.players-only", "Только игроки", "Считать только удары по игрокам.");
        s("combo-counter.timeout", "Сброс через", "Через сколько секунд без попаданий комбо сбрасывается.");
        s("combo-counter.reset-on-hurt", "Сброс при уроне", "Сбрасывать комбо, когда вы получаете урон.");
        s("combo-counter.min-combo", "Мин. комбо", "Показывать счётчик начиная с этого числа ударов.");
        s("combo-counter.label@General", "Подпись", "Текст после числа.");
        s("combo-counter.background@General", "Фон", "Рисовать панель под счётчиком.");
        s("combo-counter.pop-strength", "Сила увеличения", "Насколько число увеличивается при росте комбо.");
        s("combo-counter.preview-in-chat", "Превью в чате", "Показывать пример, пока открыт чат.");
        s("combo-counter.show-in-screens", "В меню", "Показывать, даже когда открыт экран (кроме чата).");
        s("combo-counter.scale", "Размер", "Размер счётчика. Можно менять колесом мыши в редакторе HUD.");
        s("combo-counter.number", "Число", "Цвет числа комбо.");
        s("combo-counter.flash", "Вспышка", "Цвет вспышки числа при новом ударе.");
        s("combo-counter.label@Colors", "Подпись", "Цвет текста подписи.");
        s("combo-counter.background@Colors", "Фон", "Цвет фона панели.");
        s("combo-counter.outline", "Обводка", "Цвет обводки панели.");
        s("combo-counter.glow", "Свечение", "Свечение вдоль верхнего края панели.");
        s("combo-counter.shadow", "Тень", "Мягкая тень под панелью.");

        m("cooldowns-hud", "HUD перезарядок", "Показывает перезарядку предметов (жемчуг, хорус, тотем, щит...).");
        s("cooldowns-hud.items", "Предметы", "Для каких предметов показывать перезарядку.");
        s("cooldowns-hud.layout", "Расположение", "В ряд или в столбик.");
        s("cooldowns-hud.overlay@General", "Наложение", "Как показывать прогресс поверх иконки.");
        s("cooldowns-hud.bar", "Полоска", "Рисовать полоску прогресса под предметом.");
        s("cooldowns-hud.seconds", "Секунды", "Показывать примерное число оставшихся секунд.");
        s("cooldowns-hud.background@General", "Фон", "Рисовать панель под HUD.");
        s("cooldowns-hud.text-shadow", "Тень текста", "Рисовать текст с тенью.");
        s("cooldowns-hud.scale", "Размер", "Размер HUD (колесо мыши над элементом в редакторе HUD).");
        s("cooldowns-hud.background@Colors", "Фон", "Цвет фона панели.");
        s("cooldowns-hud.accent", "Акцент", "Акцентная линия и полоска прогресса.");
        s("cooldowns-hud.overlay@Colors", "Наложение", "Цвет оставшейся перезарядки поверх иконки.");
        s("cooldowns-hud.text", "Текст", "Цвет текста секунд.");

        m("crosshair-hp-indicator", "HP у прицела", "Показывает здоровье и поглощение рядом с прицелом.");
        s("crosshair-hp-indicator.mode", "Режим", "Показывать здоровье числом или сердцами.");
        s("crosshair-hp-indicator.absorption", "Поглощение", "Показывать поглощение под здоровьем (только если оно есть).");
        s("crosshair-hp-indicator.low-health-color@General", "Цвет при низком HP", "Менять цвет числа, когда здоровье на пороге или ниже.");
        s("crosshair-hp-indicator.low-health-threshold", "Порог низкого HP", "Здоровье, с которого используется цвет низкого HP.");
        s("crosshair-hp-indicator.text-shadow", "Тень текста", "Рисовать текст с тенью.");
        s("crosshair-hp-indicator.scale", "Размер", "Размер индикатора (колесо мыши над элементом в редакторе HUD).");
        s("crosshair-hp-indicator.health-color", "Цвет здоровья", "Цвет числа здоровья.");
        s("crosshair-hp-indicator.low-health-color@Colors", "Цвет низкого HP", "Цвет числа здоровья при низком HP.");
        s("crosshair-hp-indicator.absorption-color", "Цвет поглощения", "Цвет числа поглощения.");

        m("custom-crosshair", "Свой прицел", "Заменяет ванильный прицел и индикатор атаки на настраиваемые, без инверсии цвета.");
        s("custom-crosshair.resource-pack-textures", "Текстуры ресурспака", "Рисовать прицел и индикатор из текущего ресурспака в их цветах (без инверсии) вместо фигур ниже.");
        s("custom-crosshair.style", "Стиль", "Форма прицела.");
        s("custom-crosshair.size", "Размер", "Длина линий креста или радиус круга (пиксели GUI).");
        s("custom-crosshair.gap", "Зазор", "Пустое место между центром и линиями (пиксели GUI).");
        s("custom-crosshair.thickness", "Толщина", "Толщина линий (пиксели GUI).");
        s("custom-crosshair.dot-size", "Размер точки", "Размер точки в центре (пиксели GUI).");
        s("custom-crosshair.outline@General", "Обводка", "Рисовать обводку вокруг прицела.");
        s("custom-crosshair.outline-width", "Толщина обводки", "Толщина обводки (пиксели GUI).");
        s("custom-crosshair.opacity", "Непрозрачность", "Непрозрачность прицела и индикатора атаки.");
        s("custom-crosshair.scale", "Масштаб", "Масштаб HUD (колесо мыши над элементом в редакторе HUD). 1 - как в ванилле.");
        s("custom-crosshair.top-length", "Верхняя линия", "Длина верхней линии (0 - без неё, получится буква Т).");
        s("custom-crosshair.bottom-length", "Нижняя линия", "Длина нижней линии (0 - без неё).");
        s("custom-crosshair.left-length", "Левая линия", "Длина левой линии (0 - без неё).");
        s("custom-crosshair.right-length", "Правая линия", "Длина правой линии (0 - без неё).");
        s("custom-crosshair.center-dot", "Точка в центре", "Рисовать точку в центре.");
        s("custom-crosshair.attack-cooldown-spread", "Разлёт при перезарядке", "Раздвигать прицел, пока атака перезаряжается.");
        s("custom-crosshair.spread-amount", "Сила разлёта", "Насколько раздвигается прицел при нулевом заряде атаки (пиксели GUI).");
        s("custom-crosshair.target-color@Dynamic", "Цвет на цели", "Менять цвет прицела при наведении на цель.");
        s("custom-crosshair.players-only", "Только игроки", "Цвет на цели только для игроков (иначе любая живая сущность).");
        s("custom-crosshair.mode", "Режим", "Когда рисовать полоску перезарядки атаки под прицелом.");
        s("custom-crosshair.offset", "Отступ", "Расстояние от центра прицела до верха полоски (пиксели GUI). 10 - как в ванилле.");
        s("custom-crosshair.width", "Ширина", "Ширина полоски без обводки (пиксели GUI).");
        s("custom-crosshair.height", "Высота", "Высота полоски без обводки (пиксели GUI).");
        s("custom-crosshair.outline@Attack Indicator", "Обводка", "Рисовать обводку в 1 пиксель вокруг полоски.");
        s("custom-crosshair.flash-when-full", "Вспышка при заряде", "Коротко мигать полоской, когда атака полностью заряжена.");
        s("custom-crosshair.color", "Цвет", "Цвет прицела.");
        s("custom-crosshair.target-color@Colors", "Цвет на цели", "Цвет прицела при наведении на цель.");
        s("custom-crosshair.outline-color", "Цвет обводки", "Цвет обводки прицела.");
        s("custom-crosshair.bar-color", "Цвет полоски", "Цвет заполнения индикатора во время зарядки.");
        s("custom-crosshair.bar-full-color", "Цвет полного заряда", "Цвет индикатора при полном заряде.");
        s("custom-crosshair.bar-flash-color", "Цвет вспышки", "Цвет вспышки полоски при полном заряде.");
        s("custom-crosshair.bar-background", "Фон полоски", "Цвет пустой части индикатора.");
        s("custom-crosshair.bar-outline-color", "Обводка полоски", "Цвет обводки индикатора атаки.");

        m("custom-sky-fog", "Небо и туман", "Свой цвет неба, цвет и дальность тумана и время суток на клиенте.");
        s("custom-sky-fog.custom-sky-color", "Свой цвет неба", "Заменить цвет неба.");
        s("custom-sky-fog.sky-color", "Цвет неба", "Цвет неба.");
        s("custom-sky-fog.custom-fog-color", "Свой цвет тумана", "Заменить цвет тумана и горизонта (на воздухе).");
        s("custom-sky-fog.fog-color", "Цвет тумана", "Цвет тумана.");
        s("custom-sky-fog.fog-distance", "Дальность тумана", "Как в ванилле, с множителем или без тумана на воздухе.");
        s("custom-sky-fog.distance-multiplier", "Множитель дальности", "Умножает начало и конец тумана. Больше 1 - туман дальше.");
        s("custom-sky-fog.affect-fluids", "В жидкостях", "Менять дальность тумана также под водой, в лаве и в рыхлом снегу.");
        s("custom-sky-fog.fixed-time", "Своё время", "Показывать мир в заданное время суток (только на клиенте).");
        s("custom-sky-fog.time", "Время", "Время суток в тиках. 1000 - утро, 6000 - полдень, 13000 - закат, 18000 - полночь.");

        m("glint-color", "Цвет зачарования", "Перекрашивает блеск зачарования на предметах и броне.");
        s("glint-color.items", "Предметы", "Перекрашивать блеск на предметах (в руке, на земле, в интерфейсе).");
        s("glint-color.armor-and-entities", "Броня и сущности", "Перекрашивать блеск на надетой броне, элитрах, трезубцах и щитах.");
        s("glint-color.brightness", "Яркость", "Яркость окрашенного блеска.");
        s("glint-color.item-color", "Цвет предметов", "Цвет блеска на предметах.");
        s("glint-color.armor-uses-item-color", "Броня как предметы", "Использовать цвет предметов и для брони.");
        s("glint-color.armor-color", "Цвет брони", "Цвет блеска на броне и сущностях.");

        m("hit-color", "Цвет удара", "Меняет красный оттенок раненых сущностей на свой цвет.");
        s("hit-color.hit-color", "Цвет удара", "Оттенок сущности при получении урона. Альфа задаёт силу.");

        m("hit-particles", "Частицы удара", "Создаёт свои частицы на клиенте при ударе по сущности.");
        s("hit-particles.particle", "Частица", "Тип частиц.");
        s("hit-particles.normal-hits", "Обычные удары", "Создавать частицы при обычных (не крит) ударах.");
        s("hit-particles.amount", "Количество", "Частиц за обычный удар.");
        s("hit-particles.spread", "Разброс", "Как далеко от хитбокса цели появляются частицы.");
        s("hit-particles.speed", "Скорость", "Начальная скорость частиц.");
        s("hit-particles.size", "Размер", "Размер частиц пыли.");
        s("hit-particles.crits", "Криты", "Создавать частицы при критах.");
        s("hit-particles.crit-amount", "Количество при крите", "Частиц за крит.");
        s("hit-particles.crit-particle", "Частица крита", "Тип частиц для критов.");
        s("hit-particles.color", "Цвет", "Цвет пыли при обычных ударах.");
        s("hit-particles.crit-color", "Цвет крита", "Цвет пыли при критах.");

        m("keystrokes", "Нажатия клавиш", "Показывает клавиши движения, кнопки мыши и CPS.");
        s("keystrokes.movement-keys", "Клавиши движения", "Показывать клавиши W A S D.");
        s("keystrokes.mouse-buttons", "Кнопки мыши", "Показывать левую и правую кнопки мыши.");
        s("keystrokes.cps", "CPS", "Показывать клики в секунду под кнопками мыши.");
        s("keystrokes.space-bar", "Пробел", "Показывать клавишу прыжка.");
        s("keystrokes.sneak-key", "Присед", "Показывать клавишу приседа рядом с пробелом.");
        s("keystrokes.bound-key-names", "Реальные клавиши", "Подписывать клавиши тем, на что они назначены, вместо W A S D.");
        s("keystrokes.key-size", "Размер клавиши", "Размер одной клавиши.");
        s("keystrokes.gap", "Зазор", "Расстояние между клавишами.");
        s("keystrokes.fade-speed", "Скорость затухания", "Как быстро клавиша гаснет после отпускания.");
        s("keystrokes.show-in-screens", "В меню", "Показывать, даже когда открыт экран (кроме чата).");
        s("keystrokes.scale", "Размер", "Общий размер. Можно менять колесом мыши в редакторе HUD.");
        s("keystrokes.key", "Клавиша", "Фон отпущенной клавиши.");
        s("keystrokes.pressed", "Нажатая", "Фон нажатой клавиши.");
        s("keystrokes.outline", "Обводка", "Обводка клавиш.");
        s("keystrokes.glow", "Свечение", "Свечение вокруг нажатых клавиш.");
        s("keystrokes.text", "Текст", "Цвет подписи отпущенной клавиши.");
        s("keystrokes.pressed-text", "Текст нажатой", "Цвет подписи нажатой клавиши.");
        s("keystrokes.shadow", "Тень", "Мягкая тень под каждой клавишей.");

        m("kill-effect", "Эффект убийства", "Проигрывает эффект на клиенте, когда умирает игрок, которого вы били.");
        s("kill-effect.players-only", "Только игроки", "Срабатывать только на игроках (иначе на любой живой сущности).");
        s("kill-effect.attack-window", "Окно атаки", "Сколько секунд после вашего удара смерть засчитывается как ваше убийство.");
        s("kill-effect.lightning", "Молния", "Бьёт декоративной молнией (только на клиенте) в жертву.");
        s("kill-effect.particles", "Частицы", "Взрыв колец пыли вокруг жертвы.");
        s("kill-effect.particle-color", "Цвет частиц", "Цвет пыли.");
        s("kill-effect.particles-per-ring", "Частиц в кольце", "Количество частиц в каждом кольце.");
        s("kill-effect.rings", "Колец", "Сколько колец по высоте жертвы.");
        s("kill-effect.radius", "Радиус", "Начальный радиус колец.");
        s("kill-effect.burst-speed", "Скорость разлёта", "Скорость разлёта частиц наружу.");
        s("kill-effect.particle-size", "Размер частиц", "Размер частиц пыли.");
        s("kill-effect.sound", "Звук", "Проигрывать звук, который слышите только вы.");
        s("kill-effect.sound-type", "Тип звука", "Какой звук проигрывать.");
        s("kill-effect.volume", "Громкость", "Громкость звука.");
        s("kill-effect.pitch", "Тон", "Высота звука.");

        m("low-hp-warning", "Низкое HP", "Пульсирующая красная виньетка по краям экрана при низком здоровье.");
        s("low-hp-warning.threshold", "Порог", "Здоровье (в половинках сердец), ниже которого появляется предупреждение. 8 = 4 сердца.");
        s("low-hp-warning.include-absorption", "Учитывать поглощение", "Считать сердца поглощения здоровьем.");
        s("low-hp-warning.scale-with-health", "Зависит от HP", "Чем меньше здоровья, тем сильнее виньетка.");
        s("low-hp-warning.ignore-creative", "Не в креативе", "Не показывать в творческом режиме и в режиме наблюдателя.");
        s("low-hp-warning.show-in-screens", "В меню", "Показывать, даже когда открыт экран (кроме чата).");
        s("low-hp-warning.pulse-mode", "Тип пульсации", "Как пульсирует виньетка.");
        s("low-hp-warning.pulse-speed", "Скорость пульсации", "Пульсаций в секунду.");
        s("low-hp-warning.faster-when-lower", "Быстрее при низком HP", "Пульсировать быстрее по мере падения здоровья.");
        s("low-hp-warning.min-pulse", "Мин. яркость", "Наименьшая интенсивность между пульсами (0-1).");
        s("low-hp-warning.max-alpha", "Макс. непрозрачность", "Наибольшая непрозрачность у самого края экрана.");
        s("low-hp-warning.thickness", "Толщина", "Как далеко виньетка заходит в экран (пиксели GUI).");
        s("low-hp-warning.color", "Цвет", "Цвет виньетки (прозрачность задаёт «Макс. непрозрачность»).");
        s("low-hp-warning.screen-tint", "Тонировка экрана", "Слегка окрашивать весь экран во время пульса.");
        s("low-hp-warning.tint-alpha", "Сила тонировки", "Наибольшая непрозрачность тонировки экрана.");

        m("potion-hud", "HUD эффектов", "Список активных эффектов с таймерами.");
        s("potion-hud.hide-vanilla-icons", "Скрыть ванильные иконки", "Скрывать ванильные иконки эффектов в правом верхнем углу.");
        s("potion-hud.icons", "Иконки", "Рисовать иконку эффекта.");
        s("potion-hud.level", "Уровень", "Показывать уровень эффекта римскими цифрами.");
        s("potion-hud.show-ambient", "Фоновые эффекты", "Показывать эффекты от маяков и проводников.");
        s("potion-hud.respect-hidden", "Учитывать скрытые", "Пропускать эффекты, у которых сервер скрыл иконку.");
        s("potion-hud.sort", "Сортировка", "Порядок эффектов.");
        s("potion-hud.blink-below", "Мигать при", "Мигать эффектом, когда осталось меньше стольких секунд.");
        s("potion-hud.effect-color-bar", "Цветная полоска", "Тонкая полоска сбоку строки в цвете эффекта.");
        s("potion-hud.text-shadow", "Тень текста", "Рисовать текст с тенью.");
        s("potion-hud.scale", "Размер", "Размер HUD (колесо мыши над элементом в редакторе HUD).");
        s("potion-hud.background", "Фон", "Цвет фона панели.");
        s("potion-hud.accent", "Акцент", "Цвет акцентной линии.");
        s("potion-hud.name", "Название", "Цвет названия эффекта.");
        s("potion-hud.harmful-name", "Вредные эффекты", "Цвет названия вредных эффектов.");
        s("potion-hud.timer", "Таймер", "Цвет таймера.");
        s("potion-hud.blink", "Мигание", "Цвет мигания заканчивающихся эффектов.");

        m("swing-animations", "Анимации удара", "Положение, размер и взмах предмета от первого лица (как в 1.7/1.8).");
        s("swing-animations.preset", "Пресет", "«Классика 1.7» включает старые анимации: взмах 6 тиков, без покачивания при смене и без провала после удара.");
        s("swing-animations.swing-duration", "Длительность взмаха", "Длина взмаха от первого лица в тиках (в ванилле 6, больше - медленнее).");
        s("swing-animations.ignore-haste-fatigue", "Игнорировать спешку", "Не менять длительность взмаха от Спешки и Усталости.");
        s("swing-animations.no-swap-animation", "Без анимации смены", "Убирает опускание/подъём предмета при смене (как в 1.7/1.8).");
        s("swing-animations.no-cooldown-dip", "Без провала после удара", "Убирает опускание предмета после каждого удара из-за перезарядки (как в 1.8).");
        s("swing-animations.affect-empty-hand", "Пустая рука", "Применять положение/поворот/размер основной руки и к пустой руке.");
        s("swing-animations.scale@Main Hand", "Размер", "Размер предмета в основной руке.");
        s("swing-animations.offset@Main Hand", "Смещение", "Смещение X / Y / Z предмета в основной руке.");
        s("swing-animations.rotation@Main Hand", "Поворот", "Поворот X / Y / Z предмета в основной руке в градусах.");
        s("swing-animations.scale@Off Hand", "Размер", "Размер предмета во второй руке.");
        s("swing-animations.offset@Off Hand", "Смещение", "Смещение X / Y / Z предмета во второй руке.");
        s("swing-animations.rotation@Off Hand", "Поворот", "Поворот X / Y / Z предмета во второй руке в градусах.");

        m("target-hud", "Target HUD", "Панель со здоровьем и снаряжением игрока, с которым вы сражаетесь.");
        s("target-hud.players-only", "Только игроки", "Следить только за игроками. Иначе показывается любая атакованная сущность.");
        s("target-hud.crosshair-target", "Цель под прицелом", "Также брать сущность под прицелом.");
        s("target-hud.timeout", "Сброс через", "Через сколько секунд без боя цель сбрасывается.");
        s("target-hud.range", "Дальность", "Сбрасывать цель, если она дальше этого расстояния.");
        s("target-hud.preview-in-chat", "Превью в чате", "Показывать себя как пример, пока открыт чат.");
        s("target-hud.show-in-screens", "В меню", "Показывать, даже когда открыт экран (инвентарь, меню).");
        s("target-hud.animation-speed", "Скорость анимации", "Скорость анимации полоски здоровья и появления.");
        s("target-hud.head", "Голова", "Показывать лицо скина игрока.");
        s("target-hud.name", "Имя", "Показывать имя цели.");
        s("target-hud.health-bar", "Полоска здоровья", "Показывать анимированную полоску здоровья.");
        s("target-hud.damage-trail", "След урона", "Отстающая полоска, показывающая недавно потерянное здоровье.");
        s("target-hud.health-number", "Число здоровья", "Показывать здоровье числом.");
        s("target-hud.absorption@Parts", "Поглощение", "Показывать сердца поглощения на полоске и в числе.");
        s("target-hud.armor", "Броня", "Показывать надетую броню цели.");
        s("target-hud.held-item", "Предмет в руке", "Показывать предмет в основной руке цели рядом с бронёй.");
        s("target-hud.durability", "Прочность", "Показывать полоски прочности на иконках.");
        s("target-hud.distance", "Дистанция", "Показывать расстояние до цели.");
        s("target-hud.scale", "Размер", "Размер панели. Можно менять колесом мыши в редакторе HUD.");
        s("target-hud.background", "Фон", "Цвет фона панели.");
        s("target-hud.outline", "Обводка", "Цвет обводки панели.");
        s("target-hud.glow", "Свечение", "Свечение «кровавой луны» вдоль верхнего края.");
        s("target-hud.shadow", "Тень", "Мягкая тень под панелью.");
        s("target-hud.bar-start", "Начало полоски", "Левый цвет полоски здоровья.");
        s("target-hud.bar-end", "Конец полоски", "Правый цвет полоски здоровья.");
        s("target-hud.trail", "След урона", "Цвет следа урона.");
        s("target-hud.absorption@Colors", "Поглощение", "Цвет поглощения на полоске.");
        s("target-hud.bar-background", "Фон полоски", "Пустая часть полоски здоровья.");
        s("target-hud.text", "Текст", "Основной цвет текста.");
        s("target-hud.secondary-text", "Доп. текст", "Цвет второстепенного текста (число здоровья, дистанция).");
        s("target-hud.accent", "Акцент", "Акцентный цвет (рамка головы, вспышка при уроне).");

        m("totem-pop-counter", "Счётчик тотемов", "Считает сработавшие тотемы у игроков и показывает над головой.");
        s("totem-pop-counter.ignore-self", "Без себя", "Не считать свои тотемы.");
        s("totem-pop-counter.friends", "Друзья", "Как показывать друзей: как всех, цветом друзей или не показывать.");
        s("totem-pop-counter.chat-message", "Сообщение в чат", "Писать в чат, когда у кого-то сработал тотем.");
        s("totem-pop-counter.death-message", "Сообщение о смерти", "Писать в чат, когда игрок умер после тотемов.");
        s("totem-pop-counter.above-head", "Над головой", "Показывать счётчик тотемов над головами игроков.");
        s("totem-pop-counter.totem-icon", "Иконка тотема", "Рисовать маленькую иконку тотема рядом с числом.");
        s("totem-pop-counter.float-up", "Всплывающее -1", "При каждом тотеме вверх всплывает и исчезает «-1».");
        s("totem-pop-counter.scale", "Размер", "Размер значка.");
        s("totem-pop-counter.height", "Высота", "Высота над головой игрока (в блоках). Поднимите, если перекрывает неймтеги.");
        s("totem-pop-counter.range", "Дальность", "Максимальное расстояние, на котором видны значки.");
        s("totem-pop-counter.background", "Фон", "Фон значка.");
        s("totem-pop-counter.accent", "Акцент", "Цвет числа и акцентной линии.");
        s("totem-pop-counter.friend", "Друг", "Акцентный цвет для друзей.");
        s("totem-pop-counter.pop-flash", "Вспышка", "Цвет вспышки сразу после тотема.");

        m("world-border", "Граница мира", "Скрывает границу мира или рисует её статичным своим цветом.");
        s("world-border.mode", "Режим", "Скрыть - не рисовать границу. Статично - свой цвет без бегущей анимации.");
        s("world-border.color", "Цвет", "Цвет границы (также когда граница растёт или сужается).");
        s("world-border.alpha", "Непрозрачность", "Максимальная непрозрачность границы.");
        s("world-border.fade-with-distance", "Затухание по дистанции", "Проявлять границу по мере приближения, как в ванилле. Выкл - постоянная непрозрачность.");
    }
}
