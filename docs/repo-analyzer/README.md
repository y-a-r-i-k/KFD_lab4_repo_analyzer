# Анализатор репозитория

Утилита на Kotlin принимает путь к каталогу и выводит два отчёта:

1. **По расширениям** — для `.kt`, `.kts`, `.md`: число файлов и суммарное число строк, расширения по алфавиту.
2. **Три самых длинных файла** — относительный путь и число строк; больше строк — выше, при равенстве — по пути по алфавиту.

Каталоги `.git`, `.gradle`, `build` и символические ссылки пропускаются.
Если путь не существует или указывает на файл, программа печатает ошибку в stderr и завершается с кодом 1
(без аргументов — код 2).

## Модули

```text
:cli  --implementation-->  :core
```

- `:core` — обход файлов, подсчёт строк, сортировка (`core/src/main/kotlin/analyzer/RepoAnalyzer.kt`).
  Тесты расчётов: `core/src/test/kotlin/analyzer/RepoAnalyzerTest.kt` — запускаются командой
  `./gradlew :core:test` без сборки и запуска CLI.
- `:cli` — точка входа, аргументы, вывод и код завершения (`cli/src/main/kotlin/analyzer/Main.kt`).
  Тесты CLI: `cli/src/test/kotlin/analyzer/MainTest.kt`.

`:cli` зависит от `:core`, а не наоборот: CLI использует `analyze()` и модели результата,
а логике анализа консоль не нужна. Зависимость `implementation`, потому что `:cli` —
конечное приложение и не отдаёт типы `:core` наружу.

Версии плагина Kotlin и JUnit берутся из каталога `gradle/libs.versions.toml`.

Проверка, что перенос сохранил результат: файлы тестов перенесены без изменений
(`RepoAnalyzerTest` → `:core`, `MainTest` → `:cli`), все 14 тестов проходят, а `MainTest`
сравнивает полный текст вывода на примере из четырёх файлов с эталоном. Вывод `run` на
`examples/sample` совпадает с выводом монолита.

Пример из четырёх файлов лежит в `examples/sample`.

## Запуск

Linux / macOS:

```bash
./gradlew build --console=plain
./gradlew :cli:run --args="examples/sample" --console=plain
```

Windows (PowerShell):

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat :cli:run --args="examples/sample" --console=plain
```

Вывод на примере из четырёх файлов:

```text
By extension:
kt: 2 files, 4 lines
md: 1 file, 0 lines

Longest files:
src/A.kt (2)
src/B.kt (2)
README.md (0)
```

## Ссылки

- Монолит: https://github.com/y-a-r-i-k/KFD_lab4_repo_analyzer/commit/ef98045
- Модули: https://github.com/y-a-r-i-k/KFD_lab4_repo_analyzer/commit/62648df
- Успешный прогон CI для ветки `hw/repo-analyzer`: https://github.com/y-a-r-i-k/KFD_lab4_repo_analyzer/actions/runs/45877642490

## Эксперимент со сборкой

Условия: Windows 11, JDK 21, Gradle Wrapper 9.6.0, Kotlin plugin 2.4.20;
в `gradle.properties` выключены configuration cache, build cache и параллельность.

Порядок:

1. На состоянии «Модули» выполнена `./gradlew build --console=plain` — всё собрано.
2. Правка внутри `:core` без изменения публичных сигнатур и результата: в `countLines`
   (`core/src/main/kotlin/analyzer/RepoAnalyzer.kt`) цикл `while` по итератору заменён на
   `file.useLines { lines -> lines.count() }`. Функция `internal`, её сигнатура не менялась.
3. Повторена та же команда без `clean`: `./gradlew build --console=plain`.

### Прогноз (записан до правки)

| Задача | Ожидание | Почему |
|---|---|---|
| `:core:compileKotlin` | выполнится (без пометки `UP-TO-DATE`) | изменился исходный файл модуля |
| `:cli:compileKotlin` | `UP-TO-DATE` | исходники `:cli` не менялись, а у `:core` не изменился ABI — публичные сигнатуры прежние; Kotlin-плагин сравнивает снимок ABI зависимостей, а не байты тел функций |
| `:core:test`, `:cli:test` | выполнятся | у обоих изменился runtime classpath (новые классы `:core`) |

### Фактический результат (из лога после правки)

```text
> Task :core:compileKotlin
> Task :cli:compileKotlin UP-TO-DATE
```

| Задача | Статус |
|---|---|
| `:core:compileKotlin` | выполнилась (без пометки) |
| `:cli:compileKotlin` | `UP-TO-DATE` |

### Вывод

Прогноз совпал. `:core` перекомпилировалась, потому что поменялся её исходник, а `:cli:compileKotlin`
осталась `UP-TO-DATE`: снаружи `:core` выглядит так же, правка была только внутри тела `internal`-функции,
а Kotlin-плагин смотрит на ABI зависимости, а не на байты её классов. Тесты обоих модулей при этом
перезапустились, потому что у них поменялся runtime classpath — в нём лежат новые классы `:core`.
