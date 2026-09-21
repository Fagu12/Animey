package com.example.domain.util

/**
 * SemVer and multi-part version comparison utility.
 */
object VersionComparator {

    /**
     * Compares two semantic version strings (e.g., "1.2.0" vs "1.2.1", "1.0" vs "1.0.0").
     * Returns:
     * - negative if [v1] < [v2] (update available)
     * - 0 if [v1] == [v2]
     * - positive if [v1] > [v2]
     */
    fun compare(v1: String, v2: String): Int {
        val clean1 = cleanVersion(v1)
        val clean2 = cleanVersion(v2)

        val parts1 = clean1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = clean2.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }
        return 0
    }

    /**
     * Checks if [remoteVersion] is strictly newer than [installedVersion].
     */
    fun isUpdateAvailable(installedVersion: String, remoteVersion: String): Boolean {
        return compare(installedVersion, remoteVersion) < 0
    }

    private fun cleanVersion(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V").substringBefore("-")
    }
}
