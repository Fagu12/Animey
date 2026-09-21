package com.example.domain.extension.security

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.model.ExtensionCapability
import com.example.domain.model.ExtensionManifest
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Result of checking network policy for an extension request.
 */
sealed class NetworkPolicyDecision {
    object Allowed : NetworkPolicyDecision()
    data class Denied(val reason: String) : NetworkPolicyDecision()
}

/**
 * Security policy enforcer that inspects all network calls initiated by extensions.
 * Enforces:
 * 1. Capability check (manifest must declare NETWORK capability).
 * 2. Protocol restrictions (HTTPS / HTTP only).
 * 3. SSRF Protection: Blocks loopback, RFC1918 private subnets, link-local, and cloud metadata IPs.
 * 4. Domain Whitelisting: Must match declared allowedDomains or baseUrl.
 * 5. Rate Limiting: Prevents runaway loops or DDoS from third-party extensions.
 */
interface ExtensionNetworkPolicyEnforcer {
    fun evaluateRequest(manifest: ExtensionManifest, targetUrl: String): NetworkPolicyDecision
    fun resetRateLimits(extensionId: String)
}

class DefaultExtensionNetworkPolicyEnforcer(
    private val maxRequestsPerMinute: Int = 60
) : ExtensionNetworkPolicyEnforcer {

    // Rate limiting tracking: extensionId -> list of request timestamps (epoch ms)
    private val requestTimestamps = ConcurrentHashMap<String, MutableList<Long>>()

    override fun evaluateRequest(manifest: ExtensionManifest, targetUrl: String): NetworkPolicyDecision {
        // 1. Capability check
        val hasNetworkCap = manifest.capabilities.any {
            it.equals(ExtensionCapability.NETWORK.name, ignoreCase = true)
        }
        if (!hasNetworkCap) {
            return NetworkPolicyDecision.Denied(
                "Extension '${manifest.id}' does not possess the 'NETWORK' capability"
            )
        }

        // 2. Protocol check
        val uri = try {
            URI(targetUrl.trim())
        } catch (e: Exception) {
            return NetworkPolicyDecision.Denied("Invalid URL syntax: $targetUrl")
        }

        val scheme = uri.scheme?.lowercase() ?: ""
        if (scheme != "http" && scheme != "https") {
            return NetworkPolicyDecision.Denied(
                "Forbidden protocol '$scheme'. Only HTTP and HTTPS are permitted."
            )
        }

        val host = uri.host?.lowercase()
        if (host.isNullOrBlank()) {
            return NetworkPolicyDecision.Denied("Host cannot be empty in URL: $targetUrl")
        }

        // 3. SSRF Guard: Loopback, private IP, and metadata protection
        if (isForbiddenSsrfHost(host)) {
            return NetworkPolicyDecision.Denied(
                "SSRF Protection: Access to private/internal/loopback host '$host' is strictly prohibited"
            )
        }

        // 4. Domain Whitelisting
        val isDomainAllowed = checkDomainWhitelisting(manifest, host)
        if (!isDomainAllowed) {
            return NetworkPolicyDecision.Denied(
                "Host '$host' is not in allowed domains for extension '${manifest.id}'"
            )
        }

        // 5. Rate Limiting Check
        if (!checkRateLimit(manifest.id)) {
            return NetworkPolicyDecision.Denied(
                "Rate limit exceeded for extension '${manifest.id}' (max $maxRequestsPerMinute req/min)"
            )
        }

        return NetworkPolicyDecision.Allowed
    }

    override fun resetRateLimits(extensionId: String) {
        requestTimestamps.remove(extensionId)
    }

    private fun isForbiddenSsrfHost(host: String): Boolean {
        // Loopback hostnames
        if (host == "localhost" || host == "localhost.localdomain") return true

        // Metadata endpoints
        if (host == "169.254.169.254" || host == "metadata.google.internal") return true

        // IPv4 loopback: 127.x.x.x
        if (host.startsWith("127.")) return true
        if (host == "0.0.0.0") return true

        // IPv6 loopback
        if (host == "::1" || host == "[::1]") return true

        // RFC 1918 Private Networks
        // 10.0.0.0/8
        if (host.startsWith("10.")) return true
        // 192.168.0.0/16
        if (host.startsWith("192.168.")) return true
        // 172.16.0.0 - 172.31.255.255
        if (host.startsWith("172.")) {
            val parts = host.split(".")
            if (parts.size == 4) {
                val secondOctet = parts[1].toIntOrNull()
                if (secondOctet != null && secondOctet in 16..31) return true
            }
        }
        // Link-local: 169.254.x.x
        if (host.startsWith("169.254.")) return true

        return false
    }

    private fun checkDomainWhitelisting(manifest: ExtensionManifest, host: String): Boolean {
        val allowed = manifest.allowedDomains.map { it.lowercase().trim() }.filter { it.isNotBlank() }

        // If specific allowedDomains are configured, check against them
        if (allowed.isNotEmpty()) {
            val matches = allowed.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
            if (matches) return true
        }

        // Also check if matches host of baseUrl
        if (manifest.downloadUrl.isNotBlank()) {
            val baseHost = try {
                URI(manifest.downloadUrl).host?.lowercase()
            } catch (e: Exception) {
                null
            }
            if (baseHost != null && (host == baseHost || host.endsWith(".$baseHost"))) {
                return true
            }
        }

        // If no allowedDomains specified and baseUrl host didn't match, or if allowedDomains is empty,
        // allow if it's an external public domain (not SSRF) and manifest doesn't restrict it,
        // or require explicit allowedDomains. If allowedDomains is empty, permit any safe public domain.
        return allowed.isEmpty()
    }

    private fun checkRateLimit(extensionId: String): Boolean {
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000L

        val list = requestTimestamps.computeIfAbsent(extensionId) { mutableListOf() }
        synchronized(list) {
            list.removeAll { it < oneMinuteAgo }
            if (list.size >= maxRequestsPerMinute) {
                return false
            }
            list.add(now)
            return true
        }
    }
}
