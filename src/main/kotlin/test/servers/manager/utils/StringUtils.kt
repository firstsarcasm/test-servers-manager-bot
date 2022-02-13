package test.servers.manager.utils

object StringUtils {
    public fun String.containsOr(vararg strings: String): Boolean = strings.asSequence()
            .map { this.contains(it) }
            .reduce { acc: Boolean, b: Boolean -> acc || b }
}