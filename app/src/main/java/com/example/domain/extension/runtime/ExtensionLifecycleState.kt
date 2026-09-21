package com.example.domain.extension.runtime

/**
 * Standard lifecycle states for extensions within the system.
 */
enum class ExtensionLifecycleState {
    UNINSTALLED,
    INSTALLING,
    INSTALLED,
    UNLOADED,
    VALIDATING,
    LOADING,
    INITIALIZING,
    ACTIVE,
    PAUSED,
    FAILED,
    DISABLED,
    UPDATING,
    UNLOADING,
    TERMINATED,
    DESTROYED,
    ERROR;

    val isUsable: Boolean
        get() = this == ACTIVE

    /**
     * Checks whether a lifecycle state transition is legally allowed.
     */
    fun canTransitionTo(target: ExtensionLifecycleState): Boolean {
        if (this == target) return true
        return when (this) {
            UNINSTALLED -> target in setOf(INSTALLING, ERROR)
            INSTALLING -> target in setOf(INSTALLED, FAILED, ERROR)
            INSTALLED -> target in setOf(VALIDATING, LOADING, DISABLED, UNINSTALLED, ERROR)
            VALIDATING -> target in setOf(LOADING, FAILED, ERROR, DISABLED)
            LOADING -> target in setOf(INITIALIZING, FAILED, ERROR, DISABLED)
            INITIALIZING -> target in setOf(ACTIVE, FAILED, ERROR)
            ACTIVE -> target in setOf(PAUSED, DISABLED, UPDATING, UNLOADING, ERROR)
            PAUSED -> target in setOf(ACTIVE, DISABLED, UNLOADING, ERROR)
            DISABLED -> target in setOf(VALIDATING, LOADING, UNINSTALLED, ERROR)
            FAILED -> target in setOf(UNINSTALLED, INSTALLING, LOADING, ERROR)
            ERROR -> target in setOf(UNINSTALLED, INSTALLING, LOADING, DESTROYED)
            UPDATING -> target in setOf(VALIDATING, LOADING, FAILED, ERROR)
            UNLOADING -> target in setOf(UNLOADED, TERMINATED, DESTROYED, ERROR)
            UNLOADED -> target in setOf(LOADING, UNINSTALLED, DESTROYED)
            TERMINATED -> target in setOf(UNINSTALLED, DESTROYED)
            DESTROYED -> false
        }
    }

    /**
     * Validates transition and returns true or throws IllegalStateException.
     */
    fun validateTransition(target: ExtensionLifecycleState): Boolean {
        if (!canTransitionTo(target)) {
            throw IllegalStateException("Invalid lifecycle state transition from $this to $target")
        }
        return true
    }
}

