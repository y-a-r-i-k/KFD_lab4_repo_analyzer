# Разбираем монолит на модули: зависимости и повторная сборка

На практике разделим работающий проект, проверим зависимости и посмотрим, какие задачи выполняются после одной и той же правки кода. Затем уберём повторение настроек и проверим configuration cache. Численное ускорение считаем установленным только тогда, когда его подтверждают сопоставимые измерения.

Готовый исходный проект: [репозиторий на GitHub](https://github.com/nikitosion/kfd-monolit-lab), [скачать ZIP](https://github.com/nikitosion/kfd-monolit-lab/archive/refs/heads/main.zip). Распакуйте архив и откройте папку, в которой лежит settings.gradle.kts, как Gradle-проект. Git для этого не требуется.

Полные исходники приведены и в этой статье. Если создаёте пример самостоятельно, создайте в IntelliJ IDEA Kotlin/JVM-проект с Gradle Wrapper и замените файлы приведёнными версиями.

**Содержание**

- [Исходный проект и первое наблюдение](#start)
- [Выбор границ](#boundaries)
- [Перенос и проверка зависимости](#modules)
- [Та же правка в модулях](#change)
- [Каталог и общий плагин](#configuration)
- [Configuration cache](#cache)
- [Вывод по наблюдениям](#conclusion)
- [Дополнительное упражнение](#extra)
- [Документация](#sources)

<a id="start"></a>

## 1. Запускаем готовый монолит и сохраняем первое наблюдение

Программа принимает текстовый отчёт: одна строка содержит путь и количество строк файла через `|`. В примере данные заданы прямо в Main.kt. Разбор проверяет формат, а подсчёт собирает итог. Мы не измеряем скорость самой программы — нас интересует работа сборки.

В стартовом проекте закреплены Gradle Wrapper 9.6.0, Kotlin plugin 2.4.20, JUnit 6.1.3 и JDK toolchain 21. Версии сохраняем во всех опытах. JVM, на которой работает Gradle, и JDK для задач компиляции могут задаваться отдельно; в подготовленной среде используем JDK 21. Wrapper включает gradlew, gradlew.bat, gradle/wrapper/gradle-wrapper.jar и gradle-wrapper.properties. URL дистрибутива в последнем файле — `https://services.gradle.org/distributions/gradle-9.6.0-bin.zip`.

Корневой `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kfd-monolith-lab"
```

Корневой `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.4.20"
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

application {
    mainClass.set("analyzer.MainKt")
}
```

В `gradle.properties` отключены оба кэша и параллельность. Эти режимы сохраняем при сравнении монолита с модулями:

```properties
org.gradle.configuration-cache=false
org.gradle.caching=false
org.gradle.parallel=false
```

Все исходники находятся в одном пакете `analyzer`. Пакет обозначает пространство имён; отдельного Gradle-модуля он не создаёт.

`src/main/kotlin/analyzer/Stats.kt` описывает статистику файла и итог для списка:

```kotlin
package analyzer

data class FileStat(val path: String, val lines: Int)

data class Summary(val files: Int, val totalLines: Int)

fun summarize(files: List<FileStat>): Summary =
    Summary(files.size, files.sumOf { it.lines })
```

`src/main/kotlin/analyzer/ReportParser.kt` разбирает непустые строки и проверяет разделитель, путь и число. `analyzeReport` возвращает итог, а `parseLine` остаётся внутренней операцией:

```kotlin
package analyzer

fun analyzeReport(report: String): Summary {
    val files = report.lineSequence()
        .filter { it.isNotBlank() }
        .map(::parseLine)
        .toList()
    return summarize(files)
}

internal fun parseLine(line: String): FileStat {
    val parts = line.split('|', limit = 2)
    require(parts.size == 2) { "Expected path|lines" }
    val path = parts[0].trim()
    require(path.isNotEmpty()) { "Path must not be blank" }
    val lines = requireNotNull(parts[1].trim().toIntOrNull()) {
        "Lines must be an integer"
    }
    require(lines >= 0) { "Lines must not be negative" }
    return FileStat(path, lines)
}
```

`src/main/kotlin/analyzer/Main.kt` задаёт вход и формат консольного вывода:

```kotlin
package analyzer

fun main() {
    val report = "src/App.kt|12\nsrc/Utils.kt|8\nREADME.md|5\n"
    val summary = analyzeReport(report)
    println("files=${summary.files}, lines=${summary.totalLines}")
}
```

`src/test/kotlin/analyzer/StatsTest.kt` проверяет подсчёт на пустом и непустом списках:

```kotlin
package analyzer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatsTest {
    @Test
    fun summarizesEmptyList() {
        assertEquals(Summary(0, 0), summarize(emptyList()))
    }

    @Test
    fun summarizesFiles() {
        val files = listOf(FileStat("a.kt", 12), FileStat("b.kt", 8))
        assertEquals(Summary(2, 20), summarize(files))
    }
}
```

`src/test/kotlin/analyzer/ReportParserTest.kt` проверяет разбор и отказы на неверном входе:

```kotlin
package analyzer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ReportParserTest {
    @Test
    fun analyzesReportAndSkipsBlankLines() {
        assertEquals(Summary(2, 20), analyzeReport("a.kt|12\n\nb.kt|8\n"))
    }

    @Test
    fun parsesLineAndTrimsPath() {
        assertEquals(FileStat("a.kt", 12), parseLine(" a.kt | 12 "))
    }

    @Test
    fun rejectsMissingSeparator() {
        assertThrows(IllegalArgumentException::class.java) { parseLine("a.kt") }
    }

    @Test
    fun rejectsNegativeLines() {
        assertThrows(IllegalArgumentException::class.java) { parseLine("a.kt|-1") }
    }

    @Test
    fun rejectsNonNumericLines() {
        assertThrows(IllegalArgumentException::class.java) { parseLine("a.kt|many") }
    }
}
```

Все команды выполняйте из корня текущей рабочей копии — папки с settings.gradle.kts и gradlew.bat. Для первого запуска нужен доступ к интернету, если Gradle и зависимости ещё не загружены. Откройте терминал в этой папке и выполните:

```powershell
.\gradlew.bat build :run --no-configuration-cache --no-build-cache --console=plain
```

В PowerShell `.\` запускает файл из текущей папки. В Linux и macOS можно использовать `bash ./gradlew` вместо `.\gradlew.bat`. `build` собирает проект и выполняет проверки, `:run` запускает приложение корня. `--console=plain` оставляет обычный текст, `--no-configuration-cache` и `--no-build-cache` явно отключают соответствующие кэши.

Приложение должно вывести `files=3, lines=25`. В отчёте `build/reports/tests/test/index.html` должны быть семь успешных тестов. Сохраните отдельную копию исходного проекта рядом с рабочей папкой. Это исходная версия для сравнения; для её сохранения Git не обязателен.

До разделения проведите первый опыт. Проект уже собран предыдущей командой. Замените только `summarize` в Stats.kt на эквивалентную реализацию, сохранив модели, пакет и сигнатуру:

```kotlin
fun summarize(files: List<FileStat>): Summary {
    var total = 0
    for (file in files) {
        total += file.lines
    }
    return Summary(files.size, total)
}
```

Сначала спрогнозируйте, какие задачи выполнятся. Затем запустите сборку без clean:

```powershell
New-Item -ItemType Directory -Path .\measurements\monolith -Force | Out-Null
.\gradlew.bat build --profile --no-configuration-cache --no-build-cache --console=plain 2>&1 | Tee-Object -FilePath .\measurements\monolith\build.log
```

New-Item создаёт папку для наблюдений; `-Force` позволяет повторить команду, если папка уже есть. Конструкция `2>&1 | Tee-Object` сохраняет обычный вывод и сообщения об ошибках в build.log, одновременно показывая их в терминале. Убедитесь, что сборка закончилась строкой BUILD SUCCESSFUL.

`--profile` создаёт локальный HTML-профиль; путь указан в конце вывода, обычно это `build/reports/profile/`. Скопируйте всю папку с ресурсами до следующего clean:

```powershell
New-Item -ItemType Directory -Path .\measurements\monolith\profile -Force | Out-Null
Copy-Item -Path .\build\reports\profile\* -Destination .\measurements\monolith\profile -Recurse -Force
```

Copy-Item копирует содержимое профиля; `-Recurse` включает подпапки. Откройте в measurements/monolith/profile тот HTML-файл, который указан в выводе Gradle. В MEASUREMENT.md запишите задачи компиляции и тестов и их статусы. Эта одна попытка даёт наблюдение, а не устойчивую оценку времени. В Linux и macOS можно сохранить вывод перенаправлением в файл или через tee, а папку профиля скопировать файловым менеджером.

`UP-TO-DATE` означает пропуск задачи по её входам и выходам. `FROM-CACHE` означает восстановление выходов из build cache, который в опыте выключен. `NO-SOURCE` означает отсутствие подходящих исходников. Выполненная задача Kotlin может скомпилировать лишь часть файлов; по одному факту её запуска нельзя заключать, что скомпилировано всё.

Замените только функцию summarize исходной версией, сохранив остальные объявления Stats.kt:

```kotlin
fun summarize(files: List<FileStat>): Summary =
    Summary(files.size, files.sumOf { it.lines })
```

Соберите восстановленную версию:

```powershell
.\gradlew.bat build --no-configuration-cache --no-build-cache --console=plain
```

К разделению переходим с исходным телом функции и проходящими тестами.

<a id="boundaries"></a>

## 2. Выбираем границы по работе кода

Перед переносом нарисуйте свой граф. Определите, какие части можно проверить без разбора входного текста, какие знают формат строки и какие отвечают за вывод.

1. Где должны остаться FileStat, Summary и summarize?
2. Где разместить analyzeReport и внутреннюю parseLine?
3. Кто должен зависеть от кого? Нужен ли подсчёту доступ к консольному приложению?
4. Какие тесты перенести вместе с каждой частью?

Сравните варианты из двух и трёх модулей. Два модуля могут быть достаточны для небольшого приложения. В дальнейшем опыте используем три, чтобы отдельно проверить границу формата входа и публичный результат операции разбора.

Зафиксируйте ответственность каждого модуля одним предложением. Затем сопоставьте свой вариант с выбранной для опыта раскладкой:

| Файл | Модуль |
|---|---|
| Stats.kt и StatsTest.kt | core |
| ReportParser.kt и ReportParserTest.kt | parser |
| Main.kt | cli |

Имена пакетов оставим прежними. Модуль создаётся настройками Gradle, а не переименованием package.

<a id="modules"></a>

## 3. Переносим код и проверяем ошибку зависимости

В конец settings.gradle.kts добавьте строку, сохранив существующие блоки:

```kotlin
include("core", "parser", "cli")
```

Перенесите исходники и тесты по следующим путям без изменения содержимого:

| Исходный путь | Новый путь |
|---|---|
| src/main/kotlin/analyzer/Stats.kt | core/src/main/kotlin/analyzer/Stats.kt |
| src/test/kotlin/analyzer/StatsTest.kt | core/src/test/kotlin/analyzer/StatsTest.kt |
| src/main/kotlin/analyzer/ReportParser.kt | parser/src/main/kotlin/analyzer/ReportParser.kt |
| src/test/kotlin/analyzer/ReportParserTest.kt | parser/src/test/kotlin/analyzer/ReportParserTest.kt |
| src/main/kotlin/analyzer/Main.kt | cli/src/main/kotlin/analyzer/Main.kt |

Создайте недостающие папки. В корневом src не должно остаться копий перенесённых файлов.

Корневой build.gradle.kts замените целиком:

```kotlin
plugins {
    kotlin("jvm") version "2.4.20" apply false
}
```

`apply false` объявляет Kotlin-плагин без применения к корню. Корень больше не запускает приложение; настройки в скрипты модулей автоматически не копируются. Создайте `core/build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

Создайте `parser/build.gradle.kts`. **В этом варианте намеренно заложена ошибка выбора зависимости.** Сначала воспроизведите её, затем найдите причину по публичным типам:

```kotlin
plugins {
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

Создайте `cli/build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":parser"))
}

application {
    mainClass.set("analyzer.MainKt")
}
```

Обновите Gradle-проект в IDE и выполните:

```powershell
.\gradlew.bat projects --no-configuration-cache --console=plain
.\gradlew.bat :cli:compileKotlin --no-configuration-cache --no-build-cache --console=plain
```

`projects` должен показать core, parser и cli. Вторая команда обращается к задаче компиляции CLI. Для этого шага ожидается ошибка компиляции. Найдите в диагностике первый недоступный тип и выясните, в каком модуле он объявлен.

Найдите тип результата analyzeReport и ответьте:

- В каком модуле объявлен этот тип?
- Почему исходники parser могут собраться, а исходники CLI — нет?
- Какую связь нужно изменить, если этот публичный контракт сохраняется?

`implementation` даёт зависимость самому модулю и включает её в runtime потребителей. `api` дополнительно передаёт её потребителям при компиляции. Плагин java-library предоставляет библиотечную модель с api. Исправьте одну связь в parser, сохранив граф и публичный контракт, затем проверьте:

```powershell
.\gradlew.bat clean build --no-configuration-cache --no-build-cache --console=plain
.\gradlew.bat :cli:run --no-configuration-cache --console=plain
```

`clean` удаляет выходы основной сборки, а build выполняет сборку и проверки подпроектов. Команда run теперь адресована CLI; mainClass остаётся analyzer.MainKt, потому что пакет и имя Main.kt не изменялись.

Ожидаем прежние `files=3, lines=25`, два успешных теста в `core/build/reports/tests/test/index.html` и пять в `parser/build/reports/tests/test/index.html`. В CLI тестовых исходников нет: NO-SOURCE для его тестовой задачи ожидаем.

ReportParserTest должен остаться в parser: он использует внутреннюю parseLine. Одинаковый package не открывает internal из другого Gradle-модуля; api тоже не меняет видимость Kotlin. Стандартные тесты parser получают доступ к его main через настройку Kotlin Gradle plugin.

<a id="change"></a>

## 4. Повторяем ту же правку в модульном проекте

Сейчас исходная summarize собрана в core. До изменения запишите прогноз по задачам core, parser и cli. Замените только её тело тем же циклом из раздела 1 и выполните ту же команду:

```powershell
New-Item -ItemType Directory -Path .\measurements\modules -Force | Out-Null
.\gradlew.bat build --profile --no-configuration-cache --no-build-cache --console=plain 2>&1 | Tee-Object -FilePath .\measurements\modules\build.log
```

Clean здесь не используется: иначе мы удалили бы выходы перед опытом с изменённым исходником. Проверьте BUILD SUCCESSFUL, затем сохраните профиль:

```powershell
New-Item -ItemType Directory -Path .\measurements\modules\profile -Force | Out-Null
Copy-Item -Path .\build\reports\profile\* -Destination .\measurements\modules\profile -Recurse -Force
```

Лог находится в measurements/modules/build.log. Сопоставьте задачи с первым наблюдением:

| Состояние после одной и той же правки | Компиляция | Тесты | Что можно заключить |
|---|---|---|---|
| Один модуль | Свои задачи и статусы | Свои задачи и статусы | По выводу и профилю |
| Три модуля | Свои задачи и статусы | Свои задачи и статусы | По выводу и профилю |

Укажите, где работа была выполнена, где пропущена и что осталось неизвестным по одному логу. Сохранение сигнатуры обычной функции даёт возможность избежать лишней компиляции потребителей, но не гарантирует одинакового статуса каждой задачи при всех версиях и настройках.

Для обязательного опыта достаточно прогноза, двух сохранённых наблюдений и объяснения. Если хотите сделать численный вывод об ускорении, выполните по три измеряемые правки в исходной и модульной версиях. Перед каждой правкой возвращайте исходную функцию и выполняйте подготовительный build; затем снова вносите ту же правку и измеряйте. Сравните медианы и диапазоны. Повторный build без нового изменения в такую серию не входит.

После опыта в core/src/main/kotlin/analyzer/Stats.kt верните исходную функцию:

```kotlin
fun summarize(files: List<FileStat>): Summary =
    Summary(files.size, files.sumOf { it.lines })
```

Повторите build с выключенными кэшами:

```powershell
.\gradlew.bat build --no-configuration-cache --no-build-cache --console=plain
```

Далее работаем с модулями и больше не возвращаемся к монолиту.

<a id="configuration"></a>

## 5. Убираем повторение настроек

Теперь проект собирается и его зависимости проверены. Версии Kotlin-плагина и тестовых библиотек пока указаны прямо в build-скриптах. Сначала вынесем их в каталог. Создайте `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.4.20"
junit = "6.1.3"

[libraries]
kotlin-gradle-plugin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher", version.ref = "junit" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

Каталог libs из стандартного файла подключается автоматически. Он хранит записи, но не добавляет зависимости к модулю. Корневой build.gradle.kts:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}
```

Полный `core/build.gradle.kts` после переноса версий:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

Полный `parser/build.gradle.kts` после переноса версий:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

Полный `cli/build.gradle.kts` после переноса версий:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":parser"))
}

application {
    mainClass.set("analyzer.MainKt")
}
```

Повторите clean build и :cli:run командами раздела 3. Зависимости, число тестов и вывод должны сохраниться. На этом этапе решена задача хранения версий; отдельную серию замеров для каталога не проводим.

Теперь вынесем повторяющиеся настройки Kotlin, JDK и запуска тестов в convention plugin. Он позволяет применить их по одному идентификатору в каждом модуле. Выполните следующие шаги после успешной сборки с каталогом версий.

Создайте `build-logic/settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
```

Build-logic — самостоятельная сборка. Она явно объявляет репозитории и импортирует каталог из основной сборки. Создайте `build-logic/build.gradle.kts`:

```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
}
```

Kotlin-dsl компилирует скрипты в плагины. Kotlin Gradle plugin подключён как зависимость этой сборки. В `build-logic/src/main/kotlin/kfd.kotlin-module.gradle.kts` запишите общие настройки:

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

Имя скрипта без package даёт ID kfd.kotlin-module. В нём не нужна версия Kotlin-плагина: реализация и версия определены зависимостью самой build-logic.

Подключите сборку плагина в pluginManagement. Полный корневой settings.gradle.kts:

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kfd-monolith-lab"
include("core", "parser", "cli")
```

Include добавляет подпроекты, includeBuild подключает другую сборку. **Корневой build.gradle.kts теперь оставьте пустым:** прежнее объявление Kotlin с apply false больше не требуется. Запись kotlin-jvm в каталоге после этого перехода больше не используется модулями.

Итоговый `core/build.gradle.kts`:

```kotlin
plugins {
    id("kfd.kotlin-module")
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
```

Итоговый `parser/build.gradle.kts`:

```kotlin
plugins {
    id("kfd.kotlin-module")
    `java-library`
}

dependencies {
    api(project(":core"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
```

Итоговый `cli/build.gradle.kts`:

```kotlin
plugins {
    id("kfd.kotlin-module")
    application
}

dependencies {
    implementation(project(":parser"))
}

application {
    mainClass.set("analyzer.MainKt")
}
```

Снова проверьте clean build и :cli:run. Плагин задаёт запуск через JUnit Platform, а библиотеки тестов по-прежнему объявлены в core и parser. Поведение приложения и семь тестов должны сохраниться.

<a id="cache"></a>

## 6. Проверяем configuration cache двумя запусками

В gradle.properties измените только первую строку. Итог:

```properties
org.gradle.configuration-cache=true
org.gradle.caching=false
org.gradle.parallel=false
```

Дважды выполните одну и ту же команду без изменений между запусками:

```powershell
.\gradlew.bat build --configuration-cache --no-build-cache --console=plain
```

Первое обращение подготавливает и сохраняет конфигурацию, второе должно подтвердить `Reusing configuration cache.` и `Configuration cache entry reused.`. Если запись пересчитана, найдите причину в выводе и повторите проверку после подготовки.

Configuration cache сохраняет подготовленное состояние для выполнения задач. Build cache хранит выходы поддерживающих его задач. UP-TO-DATE относится к проверке входов и существующих выходов отдельной задачи. Поэтому переиспользование конфигурации и пропуск компиляции подтверждаются разными сообщениями.

Для этого блока достаточно двух запусков и объяснения вывода. Если дополнительно сравниваете время с выключенным и включённым configuration cache, используйте одну версию проекта, одну команду build и одинаковые условия. Подготовьте каждый режим, затем сделайте по три запуска без правок. Не смешивайте создание записи с её переиспользованием и не приписывайте выигрыш от кэша разделению на модули.

<a id="conclusion"></a>

## 7. Объясняем результат

В MEASUREMENT.md сохраните граф, исходные наблюдения и короткий вывод. Достаточно ответить на вопросы:

1. Почему модули связаны именно так? Какая публичная сигнатура объясняет api?
2. Сохранились ли семь тестов и вывод приложения?
3. Что произошло с задачами после одной и той же правки в монолите и в модулях?
4. Какие повторения убрали каталог и плагин?
5. Чем подтверждено повторное использование configuration cache?

Укажите ОС, процессор, память, Wrapper, Kotlin plugin, JVM Gradle, JDK toolchain, настройки workers и параллельности, команды и подготовку. Сведения о Gradle и его JVM можно получить командой:

```powershell
.\gradlew.bat --version --console=plain
```

Поле Kotlin в этом выводе относится к встроенному Kotlin Gradle; версию плагина приложения мы задавали отдельно.

Если сравнивали только статусы задач, вывод должен описывать выполненную и пропущенную работу. Для численного вывода нужны свои серии времени, медианы и разброс. Если различие сопоставимо с разбросом или модульный пример стал медленнее, так и запишите. Архитектурную пользу границ и время сборки рассматриваем отдельно.

Сохраните исходники, настройки, полный Wrapper, MEASUREMENT.md и скопированные логи с профилями. Папка measurements должна переживать clean. Если запускали сборку на CI, её время не смешивайте с локальными наблюдениями.

<a id="extra"></a>

## Если осталось время: диагностируем задачу

После основных опытов добавьте в cli/build.gradle.kts задачу для проверки работы с configuration cache:

```kotlin
tasks.register("stats") {
    doLast { println("модуль = " + project.name) }
}
```

Выполните:

```powershell
.\gradlew.bat :cli:stats --configuration-cache --console=plain
```

При ошибке откройте отчёт по пути из консоли. Найдите запрещённое обращение и определите, какое значение нужно действию и когда его можно получить. Исправьте задачу и выполните эту же команду дважды. Нужны строка `модуль = cli` и подтверждение переиспользования конфигурации во втором запуске. Объясните, почему строка выводится снова. После проверки оставьте исправленный вариант либо удалите экспериментальную регистрацию stats.

<a id="sources"></a>

## Источники

- [Многомодульные сборки](https://docs.gradle.org/current/userguide/multi_project_builds.html).
- [Api и implementation](https://docs.gradle.org/current/userguide/java_library_plugin.html).
- [Каталоги версий](https://docs.gradle.org/current/userguide/version_catalogs.html).
- [Precompiled script plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_precompiled.html).
- [Configuration cache и требования к задачам](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html).
- [Инкрементальная компиляция Kotlin](https://kotlinlang.org/docs/gradle-compilation-and-caches.html).
