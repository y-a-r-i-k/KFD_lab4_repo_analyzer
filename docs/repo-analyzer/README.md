# Анализатор репозитория

Утилита на Kotlin принимает путь к каталогу и выводит два отчёта:

1. **По расширениям** — для `.kt`, `.kts`, `.md`: число файлов и суммарное число строк, расширения по алфавиту.
2. **Три самых длинных файла** — относительный путь и число строк; больше строк — выше, при равенстве — по пути по алфавиту.

Каталоги `.git`, `.gradle`, `build` и символические ссылки пропускаются.
Если путь не существует или указывает на файл, программа печатает ошибку в stderr и завершается с кодом 1
(без аргументов — код 2).

## Состояние: монолит

Весь код в одном Gradle-модуле:

- `src/main/kotlin/analyzer/RepoAnalyzer.kt` — обход файлов, подсчёт строк, сортировка;
- `src/main/kotlin/analyzer/Main.kt` — точка входа, аргументы, вывод, код завершения;
- `src/test/kotlin/analyzer/RepoAnalyzerTest.kt` — тесты расчётов (оба отчёта, пустой каталог, ошибка пути, пропуск каталогов и ссылок);
- `src/test/kotlin/analyzer/MainTest.kt` — тесты CLI (текст вывода, коды завершения).

Пример из четырёх файлов лежит в `examples/sample`.

## Запуск

Linux / macOS:

```bash
./gradlew build --console=plain
./gradlew run --args="examples/sample" --console=plain
```

Windows (PowerShell):

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat run --args="examples/sample" --console=plain
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
