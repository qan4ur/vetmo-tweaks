<h1 align="center">🩸 Vetmo Tweaks</h1>

<p align="center">
Визуальный PvP-мод в красном стиле «кровавая луна» для <b>Minecraft 1.21.7 (Fabric)</b>.<br>
Форк <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a> от MeteorDevelopment, распространяется по лицензии GPL-3.0.
</p>

---

## Что это

Vetmo Tweaks — урезанный и перекрашенный Meteor Client с русским интерфейсом. В нём оставлен только небольшой набор модулей Meteor; визуальные модули живут в отдельном моде [Vetmos](https://github.com/qan4ur/vetmos).


- ClickGUI открывается на **правый Shift**. Тема тёмно-красная/алая, фон почти чёрный с красным оттенком.
- Префикс команд — точка (`.bind`, `.binds`, `.toggle`, `.settings`, `.reset` и другие).
- Конфиги лежат в папке `vetmo-tweaks` профиля игры. При первом запуске туда автоматически копируется всё из старой папки `meteor-client`: бинды, настройки, HUD, друзья, аккаунты, вейпоинты, стеши и профили. Старая папка не удаляется.

## Модули

### Из Meteor Client

| Модуль | Категория |
|---|---|
| Auto Totem | Combat |
| Auto Clicker | Player |
| Middle Click Extra | Player |
| Block ESP | Render |
| Storage ESP | Render |
| ESP | Render |
| Tracers | Render |
| Nametags | Render |
| Xray | Render |
| Freecam | Render |
| Zoom | Render |
| Fullbright | Render |
| Waypoints | Render (нужен для отображения вейпоинтов) |
| Stash Finder | World |

Системы Meteor тоже на месте: ClickGUI, настройки, бинды, команды, HUD, профили, друзья, вейпоинты, стеши, аккаунты и прокси. Кнопки Accounts и Proxies находятся на экране «По адресу».

### Визуалы

Визуальные модули (свой прицел, Target HUD, эффекты ударов, HUD-панели и т.д.) вынесены в отдельный легитный мод **[Vetmos](https://github.com/qan4ur/vetmos)**. Vetmo Tweaks содержит только функции Meteor.

## Скриншоты

<!-- Сюда можно добавить скриншоты: ![ClickGUI](docs/clickgui.png) -->

_Скоро._

## Установка

1. Установите [Fabric Loader](https://fabricmc.net/use/) для Minecraft **1.21.7**.
2. Скачайте [Fabric API](https://modrinth.com/mod/fabric-api) для 1.21.7 и положите его в папку `mods`.
3. Скачайте `vetmo-tweaks-1.21.7-1.3.0.jar` со страницы [Releases](https://github.com/qan4ur/vetmo-tweaks/releases) и положите его в папку `mods`.
4. **Не ставьте одновременно с Meteor Client:** это два клиента на одной основе, и вместе они не запустятся.

## Сборка из исходников

Нужна JDK 21. Если её нет, Gradle сам скачает её через toolchain (`gradle/gradle-daemon-jvm.properties`).

```
gradlew build
```

Готовый jar появится в `build/libs` (берите файл без `-all`).

## Лицензия

Vetmo Tweaks — форк **[Meteor Client](https://github.com/MeteorDevelopment/meteor-client)** (© MeteorDevelopment) и распространяется по той же лицензии **GNU GPL-3.0**. Полный текст лежит в файле [LICENSE](LICENSE). Исходный код Meteor Client изменён: удалена часть модулей, добавлен русский интерфейс, изменено оформление.
