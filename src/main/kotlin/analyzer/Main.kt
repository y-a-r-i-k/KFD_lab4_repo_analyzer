package analyzer

fun main() {
    val report = "src/App.kt|12\nsrc/Utils.kt|8\nREADME.md|5\n"
    val summary = analyzeReport(report)
    println("files=${summary.files}, lines=${summary.totalLines}")
}
