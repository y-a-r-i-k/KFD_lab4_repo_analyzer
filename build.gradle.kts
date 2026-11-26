// Корневой проект сам ничего не компилирует.
// Плагин Kotlin объявлен здесь с apply false, чтобы его версия (из каталога libs)
// загружалась один раз и была общей для :core и :cli.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}
