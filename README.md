# Практика: модули Gradle

Стартовый Kotlin/JVM-проект разбирает текстовый отчёт формата `путь|число строк`, проверяет вход и выводит общую статистику. Пример входа задан в Main.kt.

Начните с [полного гайда](PRACTICE.md#start). Он содержит исходники и все шаги: от первого запуска до проверки зависимостей, изменения тела функции, общего плагина и кэша конфигурации. Наблюдения записывайте в [MEASUREMENT.md](MEASUREMENT.md).

Исходная версия доступна в [репозитории](https://github.com/nikitosion/kfd-monolit-lab) и как [ZIP](https://github.com/nikitosion/kfd-monolit-lab/archive/refs/heads/main.zip). Распакуйте архив и откройте папку с settings.gradle.kts как Gradle-проект.

## Запуск

Используются JDK 21, Gradle Wrapper 9.6.0 и Kotlin plugin 2.4.20. Выполняйте команды из корня проекта. Первый запуск скачивает отсутствующие Gradle и зависимости.

PowerShell:

~~~powershell
.\gradlew.bat build :run --no-configuration-cache --no-build-cache --console=plain
~~~

Linux и macOS:

~~~bash
bash ./gradlew build :run --no-configuration-cache --no-build-cache --console=plain
~~~

Ожидаемый вывод: `files=3, lines=25`. В исходном проекте семь тестов: два для подсчёта, пять для разбора и ошибок входного формата. Отчёт после сборки находится в `build/reports/tests/test/index.html`.

## Исходные файлы

- [Stats.kt](src/main/kotlin/analyzer/Stats.kt) — модель и подсчёт итогов.
- [ReportParser.kt](src/main/kotlin/analyzer/ReportParser.kt) — разбор и проверка строк.
- [Main.kt](src/main/kotlin/analyzer/Main.kt) — консольный вывод.
- [StatsTest.kt](src/test/kotlin/analyzer/StatsTest.kt) и [ReportParserTest.kt](src/test/kotlin/analyzer/ReportParserTest.kt) — тесты.
- [MEASUREMENT.md](MEASUREMENT.md) — шаблон наблюдений.

Изначально все исходники находятся в одном модуле и пакете analyzer. При выделении модулей package можно сохранить.

## Ход практики

1. [Проверить исходный проект и сохранить первое наблюдение](PRACTICE.md#start).
2. [Предложить границы](PRACTICE.md#boundaries).
3. [Перенести код и проверить зависимости](PRACTICE.md#modules).
4. [Повторить ту же правку и сравнить задачи](PRACTICE.md#change).
5. [Вынести версии и общие настройки](PRACTICE.md#configuration).
6. [Проверить configuration cache](PRACTICE.md#cache).
7. [Объяснить результат](PRACTICE.md#conclusion).

Сохраняйте логи и копии профилей в measurements, вне build: clean удаляет результаты сборки. Шаблон отчёта не содержит готовых выводов — заполните его своими наблюдениями.
