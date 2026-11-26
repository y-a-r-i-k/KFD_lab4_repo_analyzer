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
