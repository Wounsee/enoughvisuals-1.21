# Полный список иконок EnoughVisuals

## Что присылать

- Один SVG на глиф, `viewBox="0 0 24 24"`.
- Имя файла — значение из колонки `Файл`, без переименований.
- Можно filled/bold, но внутренние отверстия и отрицательное пространство должны сохраняться на 16 px.
- Не использовать маски, фильтры, растр, текст, внешние CSS-стили и несколько цветов.
- Допустимы `path`, `circle`, `rect`, `polygon`; цвет `currentColor` или чёрный.
- Все SVG складываются в `scripts/font-assets/custom-icons/`. PUA-коды менять нельзя.

## Атлас приложения

`Используется` означает, что глиф уже вызывается клиентом. `Резерв` оставлен для ближайших экранов и тоже может быть заменён в том же TTF.

| Статус | PUA | Константа | Файл | Что должно быть изображено | Текущие места использования |
|---|---:|---|---|---|---|
| Используется | `U+F100` | `VISUAL` | `visual.svg` | visual category | `src/main/java/fun/enoughvisuals/ui/clickgui/MenuScreen.java:89`<br>`src/main/java/fun/enoughvisuals/ui/hud/BindsHud.java:158` |
| Используется | `U+F101` | `WAYPOINT` | `waypoint.svg` | waypoint and GPS | `src/main/java/fun/enoughvisuals/systems/gps/GpsSystem.java:291`<br>`src/main/java/fun/enoughvisuals/ui/clickgui/MenuScreen.java:90`<br>`src/main/java/fun/enoughvisuals/ui/clienttools/GpsScreen.java:18`<br>`src/main/java/fun/enoughvisuals/ui/clickgui/components/IntegratedToolPage.java:39` |
| Используется | `U+F102` | `SETTINGS` | `settings.svg` | settings and client category | `src/main/java/fun/enoughvisuals/ui/clickgui/MenuScreen.java:96`<br>`src/main/java/fun/enoughvisuals/ui/clickgui/components/ModuleCard.java:38` |
| Используется | `U+F103` | `UTILS` | `utils.svg` | utility category | `src/main/java/fun/enoughvisuals/ui/clickgui/MenuScreen.java:92`<br>`src/main/java/fun/enoughvisuals/ui/hud/BindsHud.java:163` |
| Используется | `U+F104` | `FRIENDS` | `friends.svg` | friends and players | `src/main/java/fun/enoughvisuals/ui/clickgui/MenuScreen.java:93`<br>`src/main/java/fun/enoughvisuals/ui/clienttools/FriendsScreen.java:21`<br>`src/main/java/fun/enoughvisuals/ui/clickgui/components/IntegratedToolPage.java:38` |
| Используется | `U+F105` | `BIND` | `bind.svg` | key bind | `src/main/java/fun/enoughvisuals/ui/clickgui/MenuScreen.java:95`<br>`src/main/java/fun/enoughvisuals/ui/hud/BindsHud.java:119`<br>`src/main/java/fun/enoughvisuals/ui/clickgui/components/ModuleCard.java:39` |
| Используется | `U+F106` | `CLIENT` | `client.svg` | client category compatibility alias | `src/main/java/fun/enoughvisuals/ui/hud/BindsHud.java:153` |
| Используется | `U+F107` | `PUZZLE` | `puzzle.svg` | modules | `src/main/java/fun/enoughvisuals/ui/clickgui/MenuScreen.java:97` |
| Используется | `U+F108` | `SEARCH` | `search.svg` | search | `src/main/java/fun/enoughvisuals/ui/clickgui/MenuScreen.java:98`<br>`src/main/java/fun/enoughvisuals/ui/clienttools/ClientToolScreen.java:28` |
| Используется | `U+F109` | `EVENTS` | `events.svg` | events | `src/main/java/fun/enoughvisuals/ui/hud/WatermarkHud.java:318`<br>`src/main/java/fun/enoughvisuals/ui/clickgui/components/EventsToolPage.java:193`<br>`src/main/java/fun/enoughvisuals/ui/clickgui/components/EventsToolPage.java:251`<br>`src/main/java/fun/enoughvisuals/ui/clickgui/components/IntegratedToolPage.java:43` |
| Используется | `U+F10A` | `INVENTORY` | `inventory.svg` | inventory layouts | `src/main/java/fun/enoughvisuals/ui/clickgui/components/IntegratedToolPage.java:41` |
| Используется | `U+F10B` | `COSMETICS` | `cosmetics.svg` | cosmetics | `src/main/java/fun/enoughvisuals/ui/clickgui/components/CosmeticsToolPage.java:88`<br>`src/main/java/fun/enoughvisuals/ui/clickgui/components/IntegratedToolPage.java:42` |
| Используется | `U+F10C` | `MACROS` | `macros.svg` | macros | `src/main/java/fun/enoughvisuals/ui/clickgui/components/IntegratedToolPage.java:40` |
| Резерв | `U+F10D` | `BOOK` | `book.svg` | book and documentation | — |
| Используется | `U+F10E` | `SAVE` | `save.svg` | save | `src/main/java/fun/enoughvisuals/ui/clienttools/ClientToolScreen.java:31` |
| Используется | `U+F10F` | `DELETE` | `delete.svg` | delete | `src/main/java/fun/enoughvisuals/systems/hud/ContextMenu.java:346`<br>`src/main/java/fun/enoughvisuals/ui/clienttools/ClientToolScreen.java:30`<br>`src/main/java/fun/enoughvisuals/ui/screen/MacrosScreen.java:154` |
| Используется | `U+F110` | `FOLDER` | `folder.svg` | open folder and configs | `src/main/java/fun/enoughvisuals/ui/clickgui/MenuScreen.java:91`<br>`src/main/java/fun/enoughvisuals/ui/clienttools/ClientToolScreen.java:33`<br>`src/main/java/fun/enoughvisuals/ui/clienttools/ConfigsScreen.java:17`<br>`src/main/java/fun/enoughvisuals/ui/clickgui/components/IntegratedToolPage.java:37` |
| Используется | `U+F111` | `ADD` | `add.svg` | add | `src/main/java/fun/enoughvisuals/ui/clienttools/ClientToolScreen.java:29` |
| Используется | `U+F112` | `CLOSE` | `close.svg` | close | `src/main/java/fun/enoughvisuals/ui/screen/AccountSwitcherScreen.java:273` |
| Используется | `U+F113` | `PLAY` | `play.svg` | play | `src/main/java/fun/enoughvisuals/ui/clickgui/components/EventsToolPage.java:347` |
| Резерв | `U+F114` | `CONNECT` | `connect.svg` | connect to server | — |
| Используется | `U+F115` | `LOCATION` | `location.svg` | current location | `src/main/java/fun/enoughvisuals/ui/clickgui/components/EventsToolPage.java:348` |
| Резерв | `U+F116` | `CLOCK` | `clock.svg` | time | — |
| Используется | `U+F117` | `HEART` | `heart.svg` | health | `src/main/java/fun/enoughvisuals/ui/hud/TargetHud.java:240`<br>`src/main/java/fun/enoughvisuals/ui/hud/TargetHud.java:246` |
| Используется | `U+F118` | `POTION` | `potion.svg` | effects | `src/main/java/fun/enoughvisuals/ui/hud/EffectsHud.java:112` |
| Используется | `U+F119` | `COOLDOWN` | `cooldown.svg` | cooldowns | `src/main/java/fun/enoughvisuals/ui/hud/CooldownsHud.java:100` |
| Используется | `U+F11A` | `DRAG` | `drag.svg` | drag handle | `src/main/java/fun/enoughvisuals/systems/hud/ContextMenu.java:358` |
| Используется | `U+F11B` | `DOWNLOAD` | `download.svg` | download | `src/main/java/fun/enoughvisuals/ui/clienttools/ClientToolScreen.java:32` |
| Резерв | `U+F11C` | `UPLOAD` | `upload.svg` | upload | — |
| Используется | `U+F11D` | `REFRESH` | `refresh.svg` | refresh | `src/main/java/fun/enoughvisuals/ui/clickgui/components/EventsToolPage.java:277` |
| Резерв | `U+F11E` | `FILTER` | `filter.svg` | filter | — |
| Резерв | `U+F11F` | `SORT` | `sort.svg` | sort | — |
| Используется | `U+F120` | `CHECK` | `check.svg` | success and selected | `src/main/java/fun/enoughvisuals/ui/clickgui/components/EventsToolPage.java:349` |
| Резерв | `U+F121` | `WARNING` | `warning.svg` | warning | — |
| Резерв | `U+F122` | `INFO` | `info.svg` | information | — |
| Резерв | `U+F123` | `LINK` | `link.svg` | link | — |
| Резерв | `U+F124` | `LOGOUT` | `logout.svg` | logout | — |
| Резерв | `U+F125` | `HOME` | `home.svg` | home | — |
| Резерв | `U+F126` | `PALETTE` | `palette.svg` | color and theme | — |
| Резерв | `U+F127` | `USER` | `user.svg` | user | — |
| Используется | `U+F128` | `SERVER` | `server.svg` | server | `src/main/java/fun/enoughvisuals/ui/clickgui/components/EventsToolPage.java:154` |
| Резерв | `U+F129` | `GAMEPAD` | `gamepad.svg` | game | — |
| Резерв | `U+F12A` | `CHAT` | `chat.svg` | chat | — |
| Резерв | `U+F12B` | `EDIT` | `edit.svg` | edit | — |
| Резерв | `U+F12C` | `COPY` | `copy.svg` | copy | — |
| Резерв | `U+F12D` | `ARROW_RIGHT` | `arrow_right.svg` | forward | — |
| Используется | `U+F12E` | `CHEVRON_DOWN` | `chevron_down.svg` | expand | `src/main/java/fun/enoughvisuals/systems/hud/ContextMenu.java:369` |
| Используется | `U+F12F` | `CHEVRON_RIGHT` | `chevron_right.svg` | next | `src/main/java/fun/enoughvisuals/systems/hud/ContextMenu.java:369` |
| Резерв | `U+F130` | `MUSIC` | `music.svg` | music | — |
| Резерв | `U+F131` | `GALLERY` | `gallery.svg` | gallery | — |
| Резерв | `U+F132` | `CAMERA` | `camera.svg` | camera | — |
| Резерв | `U+F133` | `LOCK` | `lock.svg` | locked | — |
| Резерв | `U+F134` | `UNLOCK` | `unlock.svg` | unlocked | — |
| Резерв | `U+F135` | `BELL` | `bell.svg` | notification | — |
| Резерв | `U+F136` | `SUN` | `sun.svg` | day | — |
| Резерв | `U+F137` | `MOON` | `moon.svg` | night | — |
| Резерв | `U+F138` | `SHIELD` | `shield.svg` | security | — |
| Резерв | `U+F139` | `KEY` | `key.svg` | key | — |
| Резерв | `U+F13A` | `TAG` | `tag.svg` | tag | — |
| Резерв | `U+F13B` | `STAR` | `star.svg` | favorite | — |
| Используется | `U+F13C` | `TARGET` | `target.svg` | target | `src/main/java/fun/enoughvisuals/ui/clienttools/ClientToolScreen.java:35` |
| Резерв | `U+F13D` | `LIST` | `list.svg` | list | — |
| Резерв | `U+F13E` | `GRID` | `grid.svg` | grid | — |
| Резерв | `U+F13F` | `WINDOW` | `window.svg` | window | — |
| Резерв | `U+F140` | `GLOBAL` | `global.svg` | global network | — |
| Резерв | `U+F141` | `ROCKET` | `rocket.svg` | launch | — |
| Резерв | `U+F142` | `BOX` | `box.svg` | package | — |
| Резерв | `U+F143` | `CODE` | `code.svg` | code | — |
| Резерв | `U+F144` | `MICROPHONE` | `microphone.svg` | voice | — |
| Резерв | `U+F145` | `SPARKLES` | `sparkles.svg` | visual effect | — |
| Резерв | `U+F146` | `LAYERS` | `layers.svg` | layers | — |
| Используется | `U+F147` | `PIN` | `pin.svg` | pin | `src/main/java/fun/enoughvisuals/ui/clienttools/ClientToolScreen.java:34` |
| Резерв | `U+F148` | `CURSOR` | `cursor.svg` | cursor | — |
| Резерв | `U+F149` | `TUNING` | `tuning.svg` | tuning | — |
| Резерв | `U+F14A` | `MENU` | `menu.svg` | more menu | — |
| Резерв | `U+F14B` | `DATABASE` | `database.svg` | data | — |
| Резерв | `U+F14C` | `ARCHIVE` | `archive.svg` | archive | — |
| Резерв | `U+F14D` | `DOCUMENT` | `document.svg` | document | — |
| Резерв | `U+F14E` | `BOLT` | `bolt.svg` | quick action | — |
| Резерв | `U+F14F` | `GRAPH` | `graph.svg` | statistics | — |

## Категории и самые заметные места

Это обязательные визуальные соответствия. Для них нельзя подставлять абстрактный квадрат или случайный glyph:

| Место | Нужная иконка | Файл |
|---|---|---|
| Категория `Визуалы` и HUD активных визуалов | глаз / visual | `visual.svg` |
| Категория `Утилиты` | сетка приложений / utilities | `utils.svg` |
| Категория `Клиент` | прежняя шестерёнка | `settings.svg` |
| Категория `Конфиги` | папка | `folder.svg` |
| Категория `Друзья` | люди / people | `friends.svg` |
| Категория `Метки`, GPS и waypoint HUD | GPS pin / map point | `waypoint.svg` |
| Категория `Макросы` | command / connected nodes | `macros.svg` |
| Категория `Инвентари` | книга | `inventory.svg` |
| Категория `Косметика` | палитра | `cosmetics.svg` |
| Категория `Ивенты` и Dynamic Island events | календарь | `events.svg` |
| HUD биндов | жирная клавиатура | `bind.svg` |
| HUD эффектов | зелье | `potion.svg` |
| HUD кулдаунов | часы / cooldown | `cooldown.svg` |
| Карточка события, переход на сервер | жирная Play | `play.svg` |
| Карточка события, метка на текущем сервере | GPS/location | `location.svg` |
| Заголовок Events | server rack | `server.svg` |
| Ссылки во всех UI | chain link | `link.svg` |

## Управляющие глифы и знаки

Их тоже нужно прислать SVG. После импорта обычные текстовые знаки будут заменены на PUA-глифы, чтобы стрелки и крестики везде имели одну толщину.

| PUA | Файл | Что изображено | Где используется / будет использоваться |
|---:|---|---|---|
| `U+F112` | `close.svg` | простой крестик без круга | закрытие аккаунта, модалок и экранов |
| `U+F111` | `add.svg` | плюс без круга | добавление друга, GPS, макроса и элемента; `ModuleCard` |
| `U+F154` | `minus.svg` | минус | сворачивание секции и парный знак к `add.svg`; `ModuleCard` |
| `U+F12D` | `arrow_right.svg` | стрелка вправо с древком | переход / продолжить / открыть |
| `U+F151` | `arrow_left.svg` | стрелка влево с древком | назад |
| `U+F152` | `arrow_up.svg` | стрелка вверх с древком | загрузка / перемещение вверх |
| `U+F153` | `arrow_down.svg` | стрелка вниз с древком | скачивание / перемещение вниз |
| `U+F12F` | `chevron_right.svg` | `>` без древка | breadcrumb `Клиент > ...`, вложенное меню, следующая страница |
| `U+F150` | `chevron_left.svg` | `<` без древка | предыдущая страница и возврат |
| `U+F12E` | `chevron_down.svg` | шеврон вниз | раскрытие списка и настройки HUD |
| `U+F155` | `chevron_up.svg` | шеврон вверх | сворачивание раскрытого списка |
| `U+F156` | `more_horizontal.svg` | три точки по горизонтали | контекстное меню / дополнительные действия |
| `U+F120` | `check.svg` | галочка без круга | выбранное, успешно, `Вы здесь` |
| `U+F121` | `warning.svg` | предупреждение | ошибки и опасные действия |
| `U+F11D` | `refresh.svg` | обновить | Events и повтор запроса |
| `U+F11A` | `drag.svg` | ручка перетаскивания | HUD editor |

Текущие буквальные места, которые будут мигрированы после получения SVG: `MenuScreen.java` breadcrumb `>` → `chevron_right.svg`; `ModuleCard.java` `+`/`−` → `add.svg`/`minus.svg`. Знак `?` в `FriendsScreen` — это fallback-аватар, он остаётся текстом Inter и иконкой не является.

## Отдельные фирменные/главные глифы

Эти глифы сейчас находятся в старых атласах `menu`, `clickgui` и `logo`. Они не входят в основной PUA-диапазон `F100–F14F`, поэтому их нужно присылать отдельными файлами.

| Атлас | PUA | Файл | Назначение | Использование |
|---|---:|---|---|---|
| `menu` | `U+F000` | `main_quit.svg` | выход из игры | `CustomMainMenuScreen` → `MainMenuButton` |
| `menu` | `U+F001` | `main_settings.svg` | настройки | `CustomMainMenuScreen` → `MainMenuButton` |
| `menu` | `U+F002` | `main_account.svg` | смена аккаунта | `CustomMainMenuScreen` → `MainMenuButton` |
| `menu` | `U+F003` | `main_multiplayer.svg` | мультиплеер / люди | `CustomMainMenuScreen` → `MainMenuButton` |
| `menu` | `U+F004` | `main_singleplayer.svg` | одиночная игра | `CustomMainMenuScreen` → `MainMenuButton` |
| `clickgui` | `U+F009` | `main_background.svg` | галерея/смена фона | `CustomMainMenuScreen.renderBackgroundPickerButton` |
| `logo` | `U+F002` | `enough_logo.svg` | фирменный знак EnoughVisuals | ClickGUI, Watermark, Globals/nametag |

## Обязательный минимум для первой поставки

Чтобы сразу заменить всё, что игрок видит чаще всего, сначала нужны: `visual.svg`, `waypoint.svg`, `settings.svg`, `utils.svg`, `friends.svg`, `bind.svg`, `search.svg`, `events.svg`, `inventory.svg`, `cosmetics.svg`, `macros.svg`, `folder.svg`, `save.svg`, `delete.svg`, `add.svg`, `close.svg`, `play.svg`, `connect.svg`, `location.svg`, `clock.svg`, `heart.svg`, `potion.svg`, `cooldown.svg`, `refresh.svg`, `check.svg`, `link.svg`, `logout.svg`, `home.svg`, `palette.svg`, `user.svg`, `server.svg`, `gamepad.svg`, `chat.svg`, `edit.svg`, `copy.svg`, `arrow_right.svg`, `arrow_left.svg`, `arrow_up.svg`, `arrow_down.svg`, `chevron_left.svg`, `chevron_down.svg`, `chevron_up.svg`, `chevron_right.svg`, `minus.svg`, `more_horizontal.svg`, `gallery.svg`, `camera.svg`, `pin.svg`, `target.svg`.
