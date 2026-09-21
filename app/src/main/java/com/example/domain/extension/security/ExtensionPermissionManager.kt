package com.example.domain.extension.security

import com.example.domain.model.ExtensionCapability
import com.example.domain.model.ExtensionManifest
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages and enforces runtime capabilities and security permissions for extensions.
 */
interface ExtensionPermissionManager {
    /**
     * Registers declared capabilities from an extension manifest.
     */
    fun registerExtension(manifest: ExtensionManifest)

    /**
     * Checks whether an extension holds a given capability.
     */
    fun hasPermission(extensionId: String, capability: ExtensionCapability): Boolean

    /**
     * Returns all granted capabilities for the extension.
     */
    fun getGrantedPermissions(extensionId: String): Set<ExtensionCapability>

    /**
     * Unregisters an extension upon unload.
     */
    fun unregisterExtension(extensionId: String)
}

class DefaultExtensionPermissionManager : ExtensionPermissionManager {

    private val permissionsMap = ConcurrentHashMap<String, Set<ExtensionCapability>>()

    override fun registerExtension(manifest: ExtensionManifest) {
        val caps = manifest.capabilities.mapNotNull {
            ExtensionCapability.fromString(it)
        }.toSet()
        permissionsMap[manifest.id] = caps
    }

    override fun hasPermission(extensionId: String, capability: ExtensionCapability): Boolean {
        val granted = permissionsMap[extensionId] ?: return false
        return granted.contains(capability)
    }

    override fun getGrantedPermissions(extensionId: String): Set<ExtensionCapability> {
        return permissionsMap[extensionId] ?: emptySet()
    }

    override fun unregisterExtension(extensionId: String) {
        permissionsMap.remove(extensionId)
    }
}
