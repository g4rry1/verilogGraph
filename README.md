# VerilogGraph

Интерактивный граф видов токенов SystemVerilog на Kotlin/Swing. В левой панели — все 352 `TokenKind` из закреплённой ревизии [slang](https://github.com/MikePopoloski/slang). В центре — вершина и непосредственные допустимые продолжения; щелчок по вершине продвигает просмотр. Для `module` сохраняется грамматический контекст `module_declaration`, так что каждый следующий шаг относится к тому же префиксу конструкции.

![Граф допустимых продолжений module](docs/module-preview.png)

## Источники

- [IEEE Std 1800-2023](https://ieeexplore.ieee.org/document/10458102), Annex A — формальный синтаксис. [Страница IEEE](https://standards.ieee.org/ieee/1800/7743/) подтверждает редакцию и бесплатный доступ через IEEE Get. Из [публичной копии PDF](https://www.iccircle.com/static/upload/img20240319175450.pdf) извлечены правила вместе с цветовым выделением буквальных терминалов.
- [`scripts/grammar.txt`](https://github.com/MikePopoloski/slang/blob/dcc7ee97fb14f9be003de71e100d0c9601a993c9/scripts/grammar.txt) в slang — дополнительная текстовая транскрипция Annex A для сравнения.
- [`scripts/tokenkinds.txt`](https://github.com/MikePopoloski/slang/blob/dcc7ee97fb14f9be003de71e100d0c9601a993c9/scripts/tokenkinds.txt), [`LexerFacts.cpp`](https://github.com/MikePopoloski/slang/blob/dcc7ee97fb14f9be003de71e100d0c9601a993c9/source/parsing/LexerFacts.cpp) и [`Lexer.cpp`](https://github.com/MikePopoloski/slang/blob/dcc7ee97fb14f9be003de71e100d0c9601a993c9/source/parsing/Lexer.cpp) — виды токенов, фиксированные написания и разбиение составных лексем.

Конкретная ревизия slang и SHA-256 PDF указаны в [`SOURCE.txt`](src/main/resources/SOURCE.txt). Извлечённые данные включены в `src/main/resources`; PDF стандарта в репозитории нет. Импорт повторяется командами `python3 tools/import_slang.py /path/to/slang` и `python3 tools/extract_ieee_pdf.py /path/to/ieee1800-2023.pdf` (для второй требуется `pdftohtml`).

## Запуск

Требуются JDK 17+ и доступ к Maven Central для первого получения плагина Kotlin.

```bash
./gradlew run
./gradlew check
./gradlew run --args='--check'
./gradlew run --args='--audit'
./gradlew run --args='--export-dot graph.dot'
./gradlew run --args='--preview preview.png'
./gradlew installDist
build/install/veriloggraph/bin/veriloggraph
```

`./gradlew check` запускает проверки графа автоматически. `--check` проверяет путь `module → Identifier → ; → endmodule`, вариант с `automatic`, буквальные скобки, числовую основу и системные проверки времени. `--audit` печатает обозначения, которые нельзя однозначно сопоставить с токеном. DOT экспортирует весь полученный граф с правилом происхождения на каждом ребре.
`--preview` сохраняет изображение сценария `module → …` без запуска оконной системы.

## Как устроены переходы

Импортёр читает правила Annex A из PDF, сохраняет выделенные в нём буквальные терминалы, преобразует альтернативы, необязательные части и повторения в продукции, затем сопоставляет терминалы с реальными `TokenKind` slang. Граф строится из множеств первых и последних токенов продукций. Пошаговый просмотр `module_declaration` использует распознаватель префиксов Earley, поэтому после выбора `module` выводятся `automatic`, `static` и `Identifier`, а после `module Identifier` — `#`, `(`, `;` и `import`.

При выборе другого токена из списка показывается объединение непосредственных переходов во всей грамматике. В этом режиме каждое ребро обосновано отдельно; составная цепочка может относиться к разным контекстам. Правила и разделы Annex A показаны под графом.

## Границы точности

Два заголовка правил в опубликованном PDF набраны как `:: =` и `:=`. Импортёр нормализует только эти разделители до `::=`, сохраняет текст правил и выводит предупреждения. Обозначения, которые нельзя однозначно сопоставить с `TokenKind`, перечислены в `--audit`; из корневой грамматики достижимо одно такое обозначение — `_` внутри записи десятичного числа. Для числовых значений после `IntegerBase` включены только однотокенные варианты, прямо подтверждённые правилами Annex A и lexer slang. Продукции с неразрешёнными символами исключаются из графа. Поэтому глобальный граф пока не является полным формальным доказательством всех допустимых текстов SystemVerilog. Проверенный сценарий `module` проходит отдельные проверки.

## История изменений

В этой рабочей среде каталог `.git` примонтирован только для чтения, поэтому коммиты проекта хранятся в `.history` и доступны через `tools/git-local.sh log --oneline`. Каталог `.history` не входит в экспорт исходников.
