package com.example.domain.model

enum class ExtensionType {
    ANIME,
    MANGA,
    NOVEL
}

enum class ExtensionInstallState {
    NOT_INSTALLED,
    INSTALLING,
    INSTALLED,
    UPDATE_AVAILABLE,
    ERROR
}

enum class ExtensionCapability {
    NETWORK,
    PREFERENCES,
    DOM_PARSING,
    COOKIES,
    HEADLESS_BROWSER;

    companion object {
        fun fromString(value: String): ExtensionCapability? {
            return try {
                valueOf(value.uppercase().trim())
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val language: String,
    val type: ExtensionType = ExtensionType.ANIME,
    val iconUrl: String = "",
    val description: String = "",
    val downloadUrl: String = "",
    val minAppVersion: String = "1.0.0",
    val author: String = "Community",
    val isInstalled: Boolean = false,
    val isEnabled: Boolean = true,
    val hasUpdate: Boolean = false,
    val order: Int = 0,
    val isBuiltIn: Boolean = false,
    val apiVersion: Int = CURRENT_HOST_EXTENSION_API_VERSION,
    val minApiVersion: Int = 1,
    val targetApiVersion: Int = CURRENT_HOST_EXTENSION_API_VERSION,
    val capabilities: List<String> = listOf("NETWORK", "PREFERENCES"),
    val allowedDomains: List<String> = emptyList(),
    val checksum: String? = null,
    val nsfw: Boolean = false,
    val sourceHash: String = "",
    val sourceCodeUrl: String = downloadUrl,
    val baseUrl: String = "",
    val versionCode: Int = 1
) {
    companion object {
        const val CURRENT_HOST_EXTENSION_API_VERSION = 1
    }
}

data class ExtensionInfo(
    val manifest: ExtensionManifest,
    val isLoaded: Boolean = false,
    val installState: ExtensionInstallState = if (manifest.isInstalled) ExtensionInstallState.INSTALLED else ExtensionInstallState.NOT_INSTALLED,
    val errorMessage: String? = null
)

enum class PreferenceType {
    BOOLEAN,
    STRING,
    INTEGER,
    NUMBER,
    SELECT,
    MULTI_SELECT;

    companion object {
        fun fromString(value: String): PreferenceType {
            return when (value.uppercase().trim()) {
                "BOOLEAN", "BOOL", "SWITCH" -> BOOLEAN
                "STRING", "TEXT" -> STRING
                "INTEGER", "INT" -> INTEGER
                "NUMBER", "FLOAT", "DOUBLE" -> NUMBER
                "SELECT", "LIST", "SINGLE_SELECT" -> SELECT
                "MULTI_SELECT", "MULTICHOICE", "MULTISELECT" -> MULTI_SELECT
                else -> STRING
            }
        }
    }
}

sealed class ExtensionPreference(
    open val key: String,
    open val title: String,
    open val summary: String = ""
) {
    abstract val preferenceType: PreferenceType
    open val options: List<String> = emptyList()
    abstract val preferenceDefault: Any?
    abstract val preferenceValue: Any?

    fun asBoolean(): Boolean = (preferenceValue as? Boolean)
        ?: (preferenceDefault as? Boolean)
        ?: preferenceValue?.toString()?.toBooleanStrictOrNull()
        ?: false

    fun asString(): String = preferenceValue?.toString()
        ?: preferenceDefault?.toString()
        ?: ""

    fun asInt(): Int = (preferenceValue as? Number)?.toInt()
        ?: preferenceValue?.toString()?.toIntOrNull()
        ?: (preferenceDefault as? Number)?.toInt()
        ?: 0

    fun asDouble(): Double = (preferenceValue as? Number)?.toDouble()
        ?: preferenceValue?.toString()?.toDoubleOrNull()
        ?: (preferenceDefault as? Number)?.toDouble()
        ?: 0.0

    @Suppress("UNCHECKED_CAST")
    fun asMultiSelect(): List<String> = when (val v = preferenceValue) {
        is List<*> -> v.mapNotNull { it?.toString() }
        is Set<*> -> v.mapNotNull { it?.toString() }
        else -> when (val d = preferenceDefault) {
            is List<*> -> d.mapNotNull { it?.toString() }
            is Set<*> -> d.mapNotNull { it?.toString() }
            else -> emptyList()
        }
    }

    abstract fun copyWithValue(newValue: Any): ExtensionPreference

    data class BooleanPreference(
        override val key: String,
        override val title: String,
        override val summary: String = "",
        val defaultValue: Boolean = false,
        val value: Boolean = defaultValue
    ) : ExtensionPreference(key, title, summary) {
        override val preferenceType: PreferenceType = PreferenceType.BOOLEAN
        override val preferenceDefault: Any = defaultValue
        override val preferenceValue: Any = value
        override fun copyWithValue(newValue: Any): ExtensionPreference {
            val boolVal = (newValue as? Boolean) ?: newValue.toString().toBooleanStrictOrNull() ?: defaultValue
            return copy(value = boolVal)
        }
    }

    data class StringPreference(
        override val key: String,
        override val title: String,
        override val summary: String = "",
        val defaultValue: String = "",
        val value: String = defaultValue
    ) : ExtensionPreference(key, title, summary) {
        override val preferenceType: PreferenceType = PreferenceType.STRING
        override val preferenceDefault: Any = defaultValue
        override val preferenceValue: Any = value
        override fun copyWithValue(newValue: Any): ExtensionPreference = copy(value = newValue.toString())
    }

    data class IntegerPreference(
        override val key: String,
        override val title: String,
        override val summary: String = "",
        val defaultValue: Int = 0,
        val value: Int = defaultValue
    ) : ExtensionPreference(key, title, summary) {
        override val preferenceType: PreferenceType = PreferenceType.INTEGER
        override val preferenceDefault: Any = defaultValue
        override val preferenceValue: Any = value
        override fun copyWithValue(newValue: Any): ExtensionPreference {
            val intVal = (newValue as? Number)?.toInt() ?: newValue.toString().toIntOrNull() ?: defaultValue
            return copy(value = intVal)
        }
    }

    data class NumberPreference(
        override val key: String,
        override val title: String,
        override val summary: String = "",
        val defaultValue: Double = 0.0,
        val value: Double = defaultValue
    ) : ExtensionPreference(key, title, summary) {
        override val preferenceType: PreferenceType = PreferenceType.NUMBER
        override val preferenceDefault: Any = defaultValue
        override val preferenceValue: Any = value
        override fun copyWithValue(newValue: Any): ExtensionPreference {
            val numVal = (newValue as? Number)?.toDouble() ?: newValue.toString().toDoubleOrNull() ?: defaultValue
            return copy(value = numVal)
        }
    }

    data class SelectPreference(
        override val key: String,
        override val title: String,
        override val summary: String = "",
        override val options: List<String>,
        val defaultValue: String = options.firstOrNull() ?: "",
        val value: String = defaultValue
    ) : ExtensionPreference(key, title, summary) {
        override val preferenceType: PreferenceType = PreferenceType.SELECT
        override val preferenceDefault: Any = defaultValue
        override val preferenceValue: Any = value
        override fun copyWithValue(newValue: Any): ExtensionPreference = copy(value = newValue.toString())
    }

    data class MultiSelectPreference(
        override val key: String,
        override val title: String,
        override val summary: String = "",
        override val options: List<String>,
        val defaultValue: List<String> = emptyList(),
        val value: List<String> = defaultValue
    ) : ExtensionPreference(key, title, summary) {
        override val preferenceType: PreferenceType = PreferenceType.MULTI_SELECT
        override val preferenceDefault: Any = defaultValue
        override val preferenceValue: Any = value
        override fun copyWithValue(newValue: Any): ExtensionPreference {
            val list = when (newValue) {
                is List<*> -> newValue.mapNotNull { it?.toString() }
                is Set<*> -> newValue.mapNotNull { it?.toString() }
                else -> newValue.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
            return copy(value = list)
        }
    }

    data class DynamicPreference(
        override val key: String,
        override val title: String,
        override val summary: String = "",
        override val preferenceType: PreferenceType,
        override val options: List<String> = emptyList(),
        override val preferenceDefault: Any? = null,
        override val preferenceValue: Any? = preferenceDefault
    ) : ExtensionPreference(key, title, summary) {
        override fun copyWithValue(newValue: Any): ExtensionPreference = copy(preferenceValue = newValue)
    }

    // Backwards compatibility with existing code
    data class SwitchPreference(
        override val key: String,
        override val title: String,
        override val summary: String = "",
        val defaultValue: Boolean = false,
        val value: Boolean = defaultValue
    ) : ExtensionPreference(key, title, summary) {
        override val preferenceType: PreferenceType = PreferenceType.BOOLEAN
        override val preferenceDefault: Any = defaultValue
        override val preferenceValue: Any = value
        override fun copyWithValue(newValue: Any): ExtensionPreference {
            val b = (newValue as? Boolean) ?: newValue.toString().toBooleanStrictOrNull() ?: defaultValue
            return copy(value = b)
        }
    }

    data class ListPreference(
        override val key: String,
        override val title: String,
        override val summary: String = "",
        val entries: List<String>,
        val entryValues: List<String>,
        val defaultValue: String,
        val value: String = defaultValue
    ) : ExtensionPreference(key, title, summary) {
        override val preferenceType: PreferenceType = PreferenceType.SELECT
        override val options: List<String> = entries
        override val preferenceDefault: Any = defaultValue
        override val preferenceValue: Any = value
        override fun copyWithValue(newValue: Any): ExtensionPreference = copy(value = newValue.toString())
    }

    data class TextPreference(
        override val key: String,
        override val title: String,
        override val summary: String = "",
        val defaultValue: String = "",
        val value: String = defaultValue
    ) : ExtensionPreference(key, title, summary) {
        override val preferenceType: PreferenceType = PreferenceType.STRING
        override val preferenceDefault: Any = defaultValue
        override val preferenceValue: Any = value
        override fun copyWithValue(newValue: Any): ExtensionPreference = copy(value = newValue.toString())
    }

    companion object {
        fun fromJsonObject(json: org.json.JSONObject): ExtensionPreference {
            val key = json.getString("key")
            val typeStr = json.optString("type", "STRING")
            val type = PreferenceType.fromString(typeStr)
            val title = json.optString("title", key)
            val summary = json.optString("summary", json.optString("description", ""))

            val optionsList = mutableListOf<String>()
            val optionsJson = json.optJSONArray("options")
            if (optionsJson != null) {
                for (i in 0 until optionsJson.length()) {
                    optionsList.add(optionsJson.getString(i))
                }
            }

            return when (type) {
                PreferenceType.BOOLEAN -> {
                    val defaultVal = json.optBoolean("default", false)
                    val currentVal = if (json.has("value")) json.optBoolean("value", defaultVal) else defaultVal
                    BooleanPreference(key, title, summary, defaultVal, currentVal)
                }
                PreferenceType.STRING -> {
                    val defaultVal = json.optString("default", "")
                    val currentVal = if (json.has("value")) json.optString("value", defaultVal) else defaultVal
                    StringPreference(key, title, summary, defaultVal, currentVal)
                }
                PreferenceType.INTEGER -> {
                    val defaultVal = json.optInt("default", 0)
                    val currentVal = if (json.has("value")) json.optInt("value", defaultVal) else defaultVal
                    IntegerPreference(key, title, summary, defaultVal, currentVal)
                }
                PreferenceType.NUMBER -> {
                    val defaultVal = json.optDouble("default", 0.0)
                    val currentVal = if (json.has("value")) json.optDouble("value", defaultVal) else defaultVal
                    NumberPreference(key, title, summary, defaultVal, currentVal)
                }
                PreferenceType.SELECT -> {
                    val defaultVal = json.optString("default", optionsList.firstOrNull() ?: "")
                    val currentVal = if (json.has("value")) json.optString("value", defaultVal) else defaultVal
                    SelectPreference(key, title, summary, optionsList, defaultVal, currentVal)
                }
                PreferenceType.MULTI_SELECT -> {
                    val defaultList = mutableListOf<String>()
                    val defArray = json.optJSONArray("default")
                    if (defArray != null) {
                        for (i in 0 until defArray.length()) {
                            defaultList.add(defArray.getString(i))
                        }
                    }
                    val currentList = if (json.has("value")) {
                        val valArray = json.optJSONArray("value")
                        if (valArray != null) {
                            val list = mutableListOf<String>()
                            for (i in 0 until valArray.length()) list.add(valArray.getString(i))
                            list
                        } else defaultList
                    } else defaultList
                    MultiSelectPreference(key, title, summary, optionsList, defaultList, currentList)
                }
            }
        }

        fun fromJson(jsonStr: String): ExtensionPreference {
            return fromJsonObject(org.json.JSONObject(jsonStr))
        }

        fun fromJsonArray(jsonStr: String): List<ExtensionPreference> {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<ExtensionPreference>()
            for (i in 0 until array.length()) {
                list.add(fromJsonObject(array.getJSONObject(i)))
            }
            return list
        }
    }
}

data class ExtensionRepository(
    val id: String,
    val name: String,
    val url: String,
    val version: Int = 1,
    val isEnabled: Boolean = true,
    val extensionCount: Int = 0
)

data class RepositoryIndex(
    val name: String,
    val version: Int,
    val extensions: List<ExtensionManifest>
)
