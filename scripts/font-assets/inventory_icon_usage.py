#!/usr/bin/env python3
"""Generate the complete custom icon handoff list with live Java usages."""

from __future__ import annotations

import json
import re
from pathlib import Path


def main() -> None:
    root = Path(__file__).resolve().parents[2]
    manifest = json.loads((Path(__file__).with_name("solar-icons.json")).read_text(encoding="utf-8"))
    java_root = root / "src" / "main" / "java"
    references: dict[str, list[str]] = {}
    pattern = re.compile(r"SolarIcons\.([A-Z0-9_]+)")

    for path in java_root.rglob("*.java"):
        if path.name == "SolarIcons.java":
            continue
        relative = path.relative_to(root).as_posix()
        for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            for constant in pattern.findall(line):
                references.setdefault(constant, []).append(f"`{relative}:{line_number}`")

    output = Path(__file__).with_name("ICON-REQUESTS.md")
    lines = [
        "# Полный список иконок EnoughVisuals",
        "",
        "## Что присылать",
        "",
        "- Один SVG на глиф, `viewBox=\"0 0 24 24\"`.",
        "- Имя файла — значение из колонки `Файл`, без переименований.",
        "- Можно filled/bold, но внутренние отверстия и отрицательное пространство должны сохраняться на 16 px.",
        "- Не использовать маски, фильтры, растр, текст, внешние CSS-стили и несколько цветов.",
        "- Допустимы `path`, `circle`, `rect`, `polygon`; цвет `currentColor` или чёрный.",
        "- Все SVG складываются в `scripts/font-assets/custom-icons/`. PUA-коды менять нельзя.",
        "",
        "## Атлас приложения",
        "",
        "`Используется` означает, что глиф уже вызывается клиентом. `Резерв` оставлен для ближайших экранов и тоже может быть заменён в том же TTF.",
        "",
        "| Статус | PUA | Константа | Файл | Что должно быть изображено | Текущие места использования |",
        "|---|---:|---|---|---|---|",
    ]

    for icon in manifest["icons"]:
        constant = icon["constant"]
        refs = references.get(constant, [])
        status = "Используется" if refs else "Резерв"
        usage = "<br>".join(refs) if refs else "—"
        filename = constant.lower() + ".svg"
        purpose = icon["purpose"]
        lines.append(
            f"| {status} | `U+{icon['codepoint']}` | `{constant}` | `{filename}` | {purpose} | {usage} |"
        )

    lines.extend([
        "",
        "## Категории и самые заметные места",
        "",
        "Это обязательные визуальные соответствия. Для них нельзя подставлять абстрактный квадрат или случайный glyph:",
        "",
        "| Место | Нужная иконка | Файл |",
        "|---|---|---|",
        "| Категория `Визуалы` и HUD активных визуалов | глаз / visual | `visual.svg` |",
        "| Категория `Утилиты` | сетка приложений / utilities | `utils.svg` |",
        "| Категория `Клиент` | прежняя шестерёнка | `settings.svg` |",
        "| Категория `Конфиги` | папка | `folder.svg` |",
        "| Категория `Друзья` | люди / people | `friends.svg` |",
        "| Категория `Метки`, GPS и waypoint HUD | GPS pin / map point | `waypoint.svg` |",
        "| Категория `Макросы` | command / connected nodes | `macros.svg` |",
        "| Категория `Инвентари` | книга | `inventory.svg` |",
        "| Категория `Косметика` | палитра | `cosmetics.svg` |",
        "| Категория `Ивенты` и Dynamic Island events | календарь | `events.svg` |",
        "| HUD биндов | жирная клавиатура | `bind.svg` |",
        "| HUD эффектов | зелье | `potion.svg` |",
        "| HUD кулдаунов | часы / cooldown | `cooldown.svg` |",
        "| Карточка события, переход на сервер | жирная Play | `play.svg` |",
        "| Карточка события, метка на текущем сервере | GPS/location | `location.svg` |",
        "| Заголовок Events | server rack | `server.svg` |",
        "| Ссылки во всех UI | chain link | `link.svg` |",
        "",
        "## Управляющие глифы и знаки",
        "",
        "Их тоже нужно прислать SVG. После импорта обычные текстовые знаки будут заменены на PUA-глифы, "
        "чтобы стрелки и крестики везде имели одну толщину.",
        "",
        "| PUA | Файл | Что изображено | Где используется / будет использоваться |",
        "|---:|---|---|---|",
        "| `U+F112` | `close.svg` | простой крестик без круга | закрытие аккаунта, модалок и экранов |",
        "| `U+F111` | `add.svg` | плюс без круга | добавление друга, GPS, макроса и элемента; `ModuleCard` |",
        "| `U+F154` | `minus.svg` | минус | сворачивание секции и парный знак к `add.svg`; `ModuleCard` |",
        "| `U+F12D` | `arrow_right.svg` | стрелка вправо с древком | переход / продолжить / открыть |",
        "| `U+F151` | `arrow_left.svg` | стрелка влево с древком | назад |",
        "| `U+F152` | `arrow_up.svg` | стрелка вверх с древком | загрузка / перемещение вверх |",
        "| `U+F153` | `arrow_down.svg` | стрелка вниз с древком | скачивание / перемещение вниз |",
        "| `U+F12F` | `chevron_right.svg` | `>` без древка | breadcrumb `Клиент > ...`, вложенное меню, следующая страница |",
        "| `U+F150` | `chevron_left.svg` | `<` без древка | предыдущая страница и возврат |",
        "| `U+F12E` | `chevron_down.svg` | шеврон вниз | раскрытие списка и настройки HUD |",
        "| `U+F155` | `chevron_up.svg` | шеврон вверх | сворачивание раскрытого списка |",
        "| `U+F156` | `more_horizontal.svg` | три точки по горизонтали | контекстное меню / дополнительные действия |",
        "| `U+F120` | `check.svg` | галочка без круга | выбранное, успешно, `Вы здесь` |",
        "| `U+F121` | `warning.svg` | предупреждение | ошибки и опасные действия |",
        "| `U+F11D` | `refresh.svg` | обновить | Events и повтор запроса |",
        "| `U+F11A` | `drag.svg` | ручка перетаскивания | HUD editor |",
        "",
        "Текущие буквальные места, которые будут мигрированы после получения SVG: "
        "`MenuScreen.java` breadcrumb `>` → `chevron_right.svg`; "
        "`ModuleCard.java` `+`/`−` → `add.svg`/`minus.svg`. "
        "Знак `?` в `FriendsScreen` — это fallback-аватар, он остаётся текстом Inter и иконкой не является.",
        "",
        "## Отдельные фирменные/главные глифы",
        "",
        "Эти глифы сейчас находятся в старых атласах `menu`, `clickgui` и `logo`. "
        "Они не входят в основной PUA-диапазон `F100–F14F`, поэтому их нужно присылать отдельными файлами.",
        "",
        "| Атлас | PUA | Файл | Назначение | Использование |",
        "|---|---:|---|---|---|",
        "| `menu` | `U+F000` | `main_quit.svg` | выход из игры | `CustomMainMenuScreen` → `MainMenuButton` |",
        "| `menu` | `U+F001` | `main_settings.svg` | настройки | `CustomMainMenuScreen` → `MainMenuButton` |",
        "| `menu` | `U+F002` | `main_account.svg` | смена аккаунта | `CustomMainMenuScreen` → `MainMenuButton` |",
        "| `menu` | `U+F003` | `main_multiplayer.svg` | мультиплеер / люди | `CustomMainMenuScreen` → `MainMenuButton` |",
        "| `menu` | `U+F004` | `main_singleplayer.svg` | одиночная игра | `CustomMainMenuScreen` → `MainMenuButton` |",
        "| `clickgui` | `U+F009` | `main_background.svg` | галерея/смена фона | `CustomMainMenuScreen.renderBackgroundPickerButton` |",
        "| `logo` | `U+F002` | `enough_logo.svg` | фирменный знак EnoughVisuals | ClickGUI, Watermark, Globals/nametag |",
        "",
        "## Обязательный минимум для первой поставки",
        "",
        "Чтобы сразу заменить всё, что игрок видит чаще всего, сначала нужны: "
        "`visual.svg`, `waypoint.svg`, `settings.svg`, `utils.svg`, `friends.svg`, `bind.svg`, "
        "`search.svg`, `events.svg`, `inventory.svg`, `cosmetics.svg`, `macros.svg`, `folder.svg`, "
        "`save.svg`, `delete.svg`, `add.svg`, `close.svg`, `play.svg`, `connect.svg`, `location.svg`, "
        "`clock.svg`, `heart.svg`, `potion.svg`, `cooldown.svg`, `refresh.svg`, `check.svg`, `link.svg`, "
        "`logout.svg`, `home.svg`, `palette.svg`, `user.svg`, `server.svg`, `gamepad.svg`, `chat.svg`, "
        "`edit.svg`, `copy.svg`, `arrow_right.svg`, `arrow_left.svg`, `arrow_up.svg`, `arrow_down.svg`, "
        "`chevron_left.svg`, `chevron_down.svg`, `chevron_up.svg`, `chevron_right.svg`, `minus.svg`, "
        "`more_horizontal.svg`, `gallery.svg`, "
        "`camera.svg`, `pin.svg`, `target.svg`.",
        "",
    ])
    output.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {output} ({len(manifest['icons'])} atlas glyphs)")


if __name__ == "__main__":
    main()
