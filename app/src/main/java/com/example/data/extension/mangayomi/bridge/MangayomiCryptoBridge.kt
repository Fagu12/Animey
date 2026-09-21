package com.example.data.extension.mangayomi.bridge

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * JS helper for Base64, AES cipher, and cryptographic primitives.
 */
class MangayomiCryptoBridge {

    fun atob(encoded: String): String {
        return try {
            val bytes = java.util.Base64.getDecoder().decode(encoded)
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            try {
                val bytes = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
                String(bytes, StandardCharsets.UTF_8)
            } catch (e2: Exception) {
                ""
            }
        }
    }

    fun btoa(plain: String): String {
        return try {
            val bytes = plain.toByteArray(StandardCharsets.UTF_8)
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (e: Exception) {
            try {
                val bytes = plain.toByteArray(StandardCharsets.UTF_8)
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } catch (e2: Exception) {
                ""
            }
        }
    }

    fun md5(input: String): String {
        return hashString("MD5", input)
    }

    fun sha256(input: String): String {
        return hashString("SHA-256", input)
    }

    fun hmacSha256(message: String, key: String): String {
        return try {
            val secretKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKey)
            val bytes = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Decrypts AES ciphertext (Base64 or Hex encoded) with given key and IV.
     */
    fun aesDecrypt(
        ciphertext: String,
        key: String,
        iv: String = "",
        mode: String = "CBC",
        padding: String = "PKCS5Padding"
    ): String {
        return try {
            val normalizedMode = if (mode.equals("ECB", ignoreCase = true)) "ECB" else "CBC"
            val normalizedPadding = if (padding.contains("no", ignoreCase = true)) "NoPadding" else "PKCS5Padding"
            val transformation = "AES/$normalizedMode/$normalizedPadding"

            val keyBytes = resolveBytes(key)
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance(transformation)
            if (normalizedMode == "ECB" || iv.isBlank()) {
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
            } else {
                val ivBytes = resolveBytes(iv)
                val ivSpec = IvParameterSpec(ivBytes)
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
            }

            val cipherBytes = resolveBytes(ciphertext)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Encrypts AES plaintext with given key and IV, returning Base64 string.
     */
    fun aesEncrypt(
        plaintext: String,
        key: String,
        iv: String = "",
        mode: String = "CBC",
        padding: String = "PKCS5Padding"
    ): String {
        return try {
            val normalizedMode = if (mode.equals("ECB", ignoreCase = true)) "ECB" else "CBC"
            val normalizedPadding = if (padding.contains("no", ignoreCase = true)) "NoPadding" else "PKCS5Padding"
            val transformation = "AES/$normalizedMode/$normalizedPadding"

            val keyBytes = resolveBytes(key)
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance(transformation)
            if (normalizedMode == "ECB" || iv.isBlank()) {
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            } else {
                val ivBytes = resolveBytes(iv)
                val ivSpec = IvParameterSpec(ivBytes)
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
            }

            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            java.util.Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: Exception) {
            ""
        }
    }

    fun stringToHex(input: String): String {
        return input.toByteArray(StandardCharsets.UTF_8).joinToString("") { "%02x".format(it) }
    }

    fun hexToString(hex: String): String {
        return try {
            val cleaned = hex.replace(" ", "").replace("0x", "")
            val bytes = ByteArray(cleaned.length / 2)
            for (i in bytes.indices) {
                val index = i * 2
                bytes[i] = cleaned.substring(index, index + 2).toInt(16).toByte()
            }
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private fun resolveBytes(input: String): ByteArray {
        // Try Base64 decode first
        if (input.matches(Regex("^[A-Za-z0-9+/=]+$")) && input.length % 4 == 0 && input.length >= 16) {
            try {
                return java.util.Base64.getDecoder().decode(input)
            } catch (ignored: Exception) {}
        }
        // Try Hex decode
        if (input.matches(Regex("^[0-9a-fA-F]+$")) && input.length % 2 == 0 && (input.length == 32 || input.length == 64)) {
            try {
                val bytes = ByteArray(input.length / 2)
                for (i in bytes.indices) {
                    bytes[i] = input.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                }
                return bytes
            } catch (ignored: Exception) {}
        }
        // Fallback UTF-8 bytes
        return input.toByteArray(StandardCharsets.UTF_8)
    }

    private fun hashString(algorithm: String, input: String): String {
        return try {
            val digest = MessageDigest.getInstance(algorithm)
            val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}

