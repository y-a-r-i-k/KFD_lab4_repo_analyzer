// :cli — точка входа: аргументы, вывод отчётов и код завершения.
// Вся логика расчётов берётся из :core.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // implementation, а не api: типы :core нужны cli внутри,
    // но наружу cli никому как библиотека не отдаёт.
    implementation(project(":core"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

application {
    mainClass.set("analyzer.MainKt")
}

// По умолчанию run запускается из каталога cli/.
// Запускаем из корня, чтобы команда с путём examples/sample осталась прежней.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
