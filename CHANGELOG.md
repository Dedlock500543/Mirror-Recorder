# История изменений / Changelog

Формат по [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/), версии по [SemVer](https://semver.org/lang/ru/).

## [1.0.1]

### Исправлено / Fixed

- Сборка снова компилируется: метод выброса предмета в маппинге 1.12.2 называется `dropItem(boolean)` — имя `dropOneItem` осталось от маппингов эпохи 1.8.
  *The build compiles again: the drop method in the 1.12.2 mapping is `dropItem(boolean)` — `dropOneItem` was a 1.8-era mapping name.*
- Клики по собственным окнам мода и по окну чата больше не записываются как игровые GUI-клики и не могут быть «проиграны» в такие окна при повторе — теперь действует тот же фильтр, что и у клавиш.
  *Clicks in the mod's own windows and the chat window are no longer recorded as GUI clicks and cannot be replayed into them — same filter as keyboard input.*
- Достижение лимита 72 000 кадров теперь останавливает запись с сообщением в чат (раньше — молча, только звук остановки).
  *Hitting the 72 000-frame limit now stops the recording with a chat message instead of stopping silently.*
- Повтор больше не зависает навсегда, если ввод не применяется к игроку (например, спектатор за другой сущностью): через полсекунды мод сам останавливает повтор с сообщением.
  *Playback no longer hangs forever when input cannot reach the player (e.g. spectating another entity): it stops itself with a message after half a second.*
- Дубликат слота получает имя «<имя> (копия)» вместо точной копии имени — в списке больше нет неотличимых близнецов; при копии копии пометка не накапливается.
  *Duplicating a slot names the copy "<name> (copy)" instead of cloning the name; copying a copy does not stack the suffix.*
- Очередь клавиш окон при переполнении отклоняет новое событие — так же, как очередь кликов, — а не выбрасывает старейшее: порядок уже стоящих событий не ломается.
  *The GUI key queue now rejects new events on overflow like the click queue instead of dropping the oldest one.*
- `getSlotStartDelay` для слота без сохранённых настроек возвращает текущую задержку, а не жёсткую «3»: пустой слот не подменяет настройки.
  *`getSlotStartDelay` now falls back to the current delay instead of a hardcoded 3 for slots without saved settings.*
- Убран мёртвый код (всегда нулевые смещения опоры вращения, недостижимая интерполяция `viewBlend`, неиспользуемый `carryMask`, пустые `cycleD*`) — поведение не изменилось, код стал честнее.
  *Dead code removed (always-zero rotation offsets, unreachable `viewBlend` interpolation, unused `carryMask`, empty `cycleD*`) with no behavior change.*

### Изменено / Changed

- Версия ForgeGradle зафиксирована на `3.0.197` вместо плавающего `3.+` — сборка воспроизводима и не зависит от будущих релизов 3.x.
  *ForgeGradle pinned to `3.0.197` instead of the floating `3.+` so builds stay reproducible.*
- Добавлен `.gitignore` (build/run/.gradle/IDE-файлы); лицензия теперь в одном файле `LICENSE.txt`.
  *Added a `.gitignore` (build/run/.gradle/IDE files); the license is now a single `LICENSE.txt` file.*

## [1.0.0]

### Безопасность / Security

- Записи, загруженные из файла, помечаются флагом `Imported`; чат и команды из таких записей больше не отправляются. В чат выводится одноразовое предупреждение.
  *Imported recordings are flagged and can no longer send chat messages or commands.*
- Чтение `.nbt`/`.mrr` ограничено по объёму распакованных данных (`NBTSizeTracker`, 256 МБ), а не только по размеру сжатого файла: специально сжатый файл больше не вызовет OutOfMemory.
  *Decompression is now bounded, not just the compressed file size.*

### Исправлено / Fixed

- Запись и повтор больше не обрываются при смене мира (лобби → арена на серверах-сетях): при выгрузке мира делается промежуточное сохранение на диск, при загрузке нового мира запись продолжается в тот же файл, а кадр-граница помечается флагом — повтор на нём пересобирает опоры поворота и стабилизации и скидывает мёртвые очереди окон. Если новый мир не загрузился за 30 секунд (выход в меню, дисконнект), запись останавливается и сохраняется, об этом пишется в чат.
  *Recording and playback now survive world transfers (lobby → arena): a checkpoint is saved on unload, recording continues into the same file, and the boundary frame tells playback to re-anchor. If no new world loads within 30 s, the recording stops and is saved.*
- Выброс целого стака в мире (Ctrl + клавиша выброса) теперь повторяется как выброс стака. Ваниль читает физический Ctrl в момент повтора, поэтому выброс вызывается напрямую с флагом, записанным в кадр. В окнах (инвентарь, сундук) выброс стака уже повторялся корректно.
  *Throwing a whole stack in the world (Ctrl+drop) now replays as a stack drop.*
- Печать на табличке и в книге больше не ломается при повторе: на неконтейнерных экранах клавиши (включая E, Q, цифры и стрелки) — это текст, а не действия слотов. Раньше в этой сборке табличка закрывалась бы на первой же букве E. Плюс при старте записи теперь очищаются «зависшие» состояния кликов и клавиш от прошлой остановки посреди тика.
  *Typing on signs and in books no longer breaks during playback: on non-container screens keys are text input, not slot actions. Recording start also clears stale click/key state.*
- Вход в мир больше не требует обязательного клика для захвата мыши: если захват не случился вовремя (окно было неактивно в момент загрузки — особенность ванильного `setIngameFocus`, который работает только при активном окне), мод возвращает его сам, как только окно активно, мир загружен и нет открытого экрана.
  *Joining a world no longer needs a click to capture the mouse: if the capture was missed, the mod restores it once the window is active and no screen is open.*
- Клавиатура внутри чужих окон теперь записывается и повторяется: Q над слотом выбрасывает предмет (Ctrl+Q — стак), цифры 1–9 меняют слот с хотбаром, клавиша выбора блока клонирует предмет, E и ESC закрывают окно в тот же тик, что и в записи, а текст печатается в наковальне, табличке, книге и поиске креатива. Действие привязывается к смыслу клавиши в момент записи, поэтому смена привязки между записью и повтором не мешает.
  *Keyboard input inside windows is now recorded and replayed: drop, hotbar swap, pick-block, window close and typing in anvils, signs and books.*
- Убраны «фантомные» действия после закрытия окна: нажатия Q/E/F, записанные внутри окна, больше не накапливаются в очереди клавиш и не выстреливают в мире после её закрытия (предмет мог сам выпасть из руки, инвентарь — сам открыться). При остановке повтора очереди этих клавиш теперь тоже очищаются.
  *Key presses recorded inside a window no longer fire in the world after the window closes.*
- Книга и табличка, закрытые через ESC, теперь закрываются и при повторе; табличка уходит на сервер с восстановленным текстом. Экран смерти мод не трогает.
  *Books and signs closed with ESC now close during playback as well.*
- Средний клик в контейнере записывается и повторяется как CLONE независимо от привязки pick-block (актуально для креатива).
  *Middle click in containers is recorded and replayed as CLONE regardless of the pick-block binding.*
- Клавиши мода в «Управлении» больше не сбрасываются. Локализованное название больше не записывается в `keyDescription`: ваниль хранит привязку в `options.txt` строкой `key_<keyDescription>`, и подменённое название приводило к тому, что при следующем запуске игра не находила свою строку. Перевод теперь подставляется в таблицу локализации, а испорченное прошлыми версиями описание восстанавливается автоматически (привязку после обновления нужно выставить один раз).
  *Mod keybinds no longer reset: the localized label is never written into `keyDescription`, so `options.txt` keeps the vanilla key.*
- Кадры теперь запоминают открытый экран, и при воспроизведении окно, которого в записи в этот момент не было (интерфейс жителя на BedWars, сундук, инвентарь), закрывается автоматически. Расхождение должно держаться 10 тиков, чтобы сетевая задержка не закрывала только что открытое окно. Старые записи (формат 2) ориентируются на записанные клики вперёд. Чат и интерфейс мода не закрываются никогда.
  *Playback now mirrors the recorded screen state, so a server GUI left open (villager shop, chest) is closed by the mod instead of hanging until the end.*
- Shift-клик в контейнерах воспроизводится как настоящее быстрое перемещение (`QUICK_MOVE`), а не как обычный клик: перекладывание предметов в эндер-сундук и обратно снова работает. Раньше ваниль проверяла физическую клавишу Shift, которая при повторе не нажата, и предмет просто вис на курсоре.
  *Recorded shift-clicks are replayed as a real QUICK_MOVE, so moving items into an ender chest works during playback.*
- Выбор пустого слота больше не сбрасывает настройки, включая «Задержку перед стартом»: настройки слота применяются только если они действительно сохранены в файле слота, а отсутствующие ключи сохраняют текущее значение.
  *Selecting an empty slot no longer wipes settings such as the start delay.*
- Формат записей поднят до версии 3 (открытый экран и Shift на клике). Старые записи читаются без изменений, импорт и восстановление из корзины сохраняют исходную версию файла.
  *Recording format is now version 3 and stays backward compatible.*
- Остановка длинной записи больше не подвешивает клиент на много секунд: убрано повторное сохранение при ошибке, чтение старого файла ради имени слота и полный повторный разбор NBT при проверке записи; проверка теперь потоковая, сжатие — быстрое. Атомарная замена файла и `.bak` сохранены.
  *Stopping a long recording no longer freezes the client for seconds.*
- Один повреждённый кадр больше не превращает всю запись в «пустой слот»: битые кадры пропускаются, предпочтение отдаётся целому `.nbt`, затем целому `.bak`, затем более полному частичному варианту. Расхождение `FrameCount` с фактическим числом кадров больше не фатально.
  *A single damaged frame no longer discards the whole recording.*
- Кадры воспроизведённых слотов больше не копятся в памяти до выхода из игры: кэш ограничен тремя слотами (LRU) и освобождается при выгрузке мира. Занятый слот и слот с несохранённой записью не вытесняются никогда.
  *Frame cache is now LRU-bounded instead of growing until the game exits.*
- Клики в игровых окнах записываются дополнительно как смещение курсора от центра экрана (`GuiCX`/`GuiCY`) и воспроизводятся от центра: при другом размере окна клик попадает в тот же слот, а не в соседний. Старые записи читаются по-старому.
  *GUI clicks are now stored relative to the screen centre, so a different window size keeps the same slot.*
- HUD в окнах мода перетаскивается только с Shift, поэтому он больше не перехватывает клики по кнопкам под ним.
  *The status HUD no longer swallows clicks on the buttons underneath.*
- Переключение слота, сохранение настроек слота, переименование и переключение режима «Цикл» не проверяют каждый кадр записи — только структуру файла.
  *Slot selection and slot-settings writes no longer re-validate every frame.*
- Ошибка сохранения настроек больше не теряется: окно остаётся открытым и показывает сообщение.
  *A failed settings save now keeps the screen open with the error visible.*

### Документация / Docs

- Справка в игре: `/mirror gui` вместо `/mirror` (все пять языков); язык переключается в Настройках → Интерфейс, а не на главном экране (ru/en).
- README: пять языков интерфейса вместо двух; раздел в «Управлении» не переводится; стоп-при-движении включает пробел; добавлены предупреждения про импорт и про клики в игровых окнах.
- Добавлен этот CHANGELOG.

### Прочее / Other

- Убраны 5 неиспользуемых импортов (`GuiMirrorHelp`, `UiTheme`, `VisualPreview`).
- Формат файлов не менялся (`Version` = 2): старые записи читаются без изменений, новые поля необязательные.
  *File format unchanged (`Version` = 2); older recordings load as before.*

### Известные ограничения / Known limitations

- Сохранение всё ещё идёт в игровом потоке — теперь заметно быстрее, но на часовой записи короткая пауза остаётся. Асинхронная запись не включена сознательно: без блокировок по слотам она создаёт риск потери данных.
- `mcmod.info`: `url` ведёт на репозиторий GitHub; `updateUrl`/`updateJSON` не добавлены.
- `ForgeGradle:3.+` не закреплён: жёсткая версия без проверки сборки может сломать сборку у вас.
- Стабилизация, автовозврат и визуализация маршрута не трогались.
