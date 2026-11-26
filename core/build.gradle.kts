// :core — чистая логика анализа: обход файлов, подсчёт строк, сортировка.
// Ничего не знает о консоли, аргументах и кодах завершения.
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
