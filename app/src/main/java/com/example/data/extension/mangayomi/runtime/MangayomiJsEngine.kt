package com.example.data.extension.mangayomi.runtime

import com.example.data.extension.mangayomi.bridge.MangayomiCryptoBridge
import com.example.data.extension.mangayomi.bridge.MangayomiDomBridge
import com.example.data.extension.mangayomi.bridge.MangayomiHttpBridge
import com.example.data.extension.mangayomi.bridge.MangayomiStorageBridge
import org.mozilla.javascript.ClassShutter
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/**
 * Sandboxed Rhino JS Engine factory.
 * Restricts JVM class exposure via ClassShutter.
 */
object MangayomiJsEngine {

    init {
        // Configure standard context factory with sandboxing
        if (!ContextFactory.hasExplicitGlobal()) {
            ContextFactory.initGlobal(SandboxedContextFactory())
        }
    }

    private class SandboxedContextFactory : ContextFactory() {
        override fun makeContext(): Context {
            val cx = super.makeContext()
            cx.languageVersion = Context.VERSION_ES6
            cx.optimizationLevel = -1 // Interpreted mode for Android compatibility
            cx.setClassShutter(SandboxedClassShutter())
            return cx
        }
    }

    private class SandboxedClassShutter : ClassShutter {
        private val forbiddenPrefixes = listOf(
            "java.lang.Runtime",
            "java.lang.Process",
            "java.lang.ProcessBuilder",
            "java.lang.System",
            "java.lang.reflect",
            "java.lang.ClassLoader",
            "java.lang.Thread",
            "java.lang.ThreadGroup",
            "java.io.",
            "java.nio.",
            "java.net.",
            "android.",
            "androidx.",
            "dalvik.",
            "javax."
        )

        private val allowedPrefixes = listOf(
            "java.lang.String",
            "java.lang.Number",
            "java.lang.Boolean",
            "java.lang.Double",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Short",
            "java.lang.Byte",
            "java.lang.Character",
            "java.lang.Object",
            "java.lang.CharSequence",
            "java.lang.Math",
            "java.util.",
            "kotlin.",
            "com.example.data.extension.mangayomi.bridge."
        )

        override fun visibleToScripts(fullClassName: String): Boolean {
            if (forbiddenPrefixes.any { fullClassName.startsWith(it) }) {
                return false
            }
            return allowedPrefixes.any { fullClassName.startsWith(it) }
        }
    }

    /**
     * Initializes a fresh sandboxed global scope with all standard Mangayomi bridges.
     */
    fun createSandboxedScope(
        context: Context,
        httpBridge: MangayomiHttpBridge = MangayomiHttpBridge(),
        domBridge: MangayomiDomBridge = MangayomiDomBridge(),
        cryptoBridge: MangayomiCryptoBridge = MangayomiCryptoBridge(),
        storageBridge: MangayomiStorageBridge = MangayomiStorageBridge("default")
    ): Scriptable {
        context.optimizationLevel = -1
        context.languageVersion = Context.VERSION_ES6
        val scope = context.initSafeStandardObjects()

        // Expose underlying native bridges as private variables
        ScriptableObject.putProperty(scope, "__httpBridge", Context.javaToJS(httpBridge, scope))
        ScriptableObject.putProperty(scope, "__domBridge", Context.javaToJS(domBridge, scope))
        ScriptableObject.putProperty(scope, "__cryptoBridge", Context.javaToJS(cryptoBridge, scope))
        ScriptableObject.putProperty(scope, "__storageBridge", Context.javaToJS(storageBridge, scope))

        // Inject standard Mangayomi globals, CryptoJS, and polyfills
        val polyfills = """
            function Client() {
                if (!(this instanceof Client)) {
                    return new Client();
                }
                this.get = function(url, headers) { return __httpBridge.get(url, headers || null); };
                this.post = function(url, headers, body) { return __httpBridge.post(url, headers || null, body || null); };
            }
            var http = new Client();

            function Document(html) {
                return __domBridge.createDocument(html || "");
            }
            function parseHtml(html) {
                return __domBridge.parseHtml(html || "");
            }
            function DOMParser() {
                this.parseFromString = function(str, mimeType) {
                    return __domBridge.createDocument(str || "");
                };
            }

            var console = {
                log: function() {},
                info: function() {},
                warn: function() {},
                error: function() {}
            };

            function atob(str) {
                return __cryptoBridge.atob(str || "");
            }
            function btoa(str) {
                return __cryptoBridge.btoa(str || "");
            }
            function md5(str) {
                return __cryptoBridge.md5(str || "");
            }
            function sha256(str) {
                return __cryptoBridge.sha256(str || "");
            }

            var localStorage = {
                getItem: function(k) { return __storageBridge.getItem(String(k)); },
                setItem: function(k, v) { __storageBridge.setItem(String(k), v); },
                removeItem: function(k) { __storageBridge.removeItem(String(k)); },
                clear: function() { __storageBridge.clear(); },
                key: function(i) { return __storageBridge.key(i); },
                get length() { return __storageBridge.getLength(); }
            };
            var sessionStorage = localStorage;

            var CryptoJS = (function() {
                var enc = {
                    Utf8: {
                        parse: function(str) { return { type: 'utf8', value: String(str || ""), toString: function() { return String(str || ""); } }; },
                        stringify: function(wordArray) { return wordArray ? (wordArray.value !== undefined ? wordArray.value : String(wordArray)) : ""; }
                    },
                    Hex: {
                        parse: function(hexStr) { return { type: 'hex', value: String(hexStr || ""), toString: function() { return String(hexStr || ""); } }; },
                        stringify: function(wordArray) {
                            if (!wordArray) return "";
                            if (wordArray.type === 'hex') return wordArray.value;
                            return __cryptoBridge.stringToHex(wordArray.value !== undefined ? wordArray.value : String(wordArray));
                        }
                    },
                    Base64: {
                        parse: function(b64Str) { return { type: 'base64', value: String(b64Str || ""), toString: function() { return String(b64Str || ""); } }; },
                        stringify: function(wordArray) {
                            if (!wordArray) return "";
                            if (wordArray.type === 'base64') return wordArray.value;
                            return __cryptoBridge.btoa(wordArray.value !== undefined ? wordArray.value : String(wordArray));
                        }
                    }
                };

                var AES = {
                    decrypt: function(ciphertext, key, cfg) {
                        var ct = (typeof ciphertext === 'object' && ciphertext.value !== undefined) ? ciphertext.value : String(ciphertext || "");
                        var k = (typeof key === 'object' && key.value !== undefined) ? key.value : String(key || "");
                        var iv = "";
                        if (cfg && cfg.iv) {
                            iv = (typeof cfg.iv === 'object' && cfg.iv.value !== undefined) ? cfg.iv.value : String(cfg.iv);
                        }
                        var mode = (cfg && cfg.mode && cfg.mode.name) ? cfg.mode.name : "CBC";
                        var pad = (cfg && cfg.padding && cfg.padding.name) ? cfg.padding.name : "PKCS5Padding";
                        var decrypted = __cryptoBridge.aesDecrypt(ct, k, iv, mode, pad);
                        return {
                            value: decrypted,
                            toString: function(encoder) {
                                if (encoder === enc.Utf8 || !encoder) return decrypted;
                                if (encoder === enc.Hex) return __cryptoBridge.stringToHex(decrypted);
                                if (encoder === enc.Base64) return __cryptoBridge.btoa(decrypted);
                                return decrypted;
                            }
                        };
                    },
                    encrypt: function(plaintext, key, cfg) {
                        var pt = (typeof plaintext === 'object' && plaintext.value !== undefined) ? plaintext.value : String(plaintext || "");
                        var k = (typeof key === 'object' && key.value !== undefined) ? key.value : String(key || "");
                        var iv = "";
                        if (cfg && cfg.iv) {
                            iv = (typeof cfg.iv === 'object' && cfg.iv.value !== undefined) ? cfg.iv.value : String(cfg.iv);
                        }
                        var mode = (cfg && cfg.mode && cfg.mode.name) ? cfg.mode.name : "CBC";
                        var pad = (cfg && cfg.padding && cfg.padding.name) ? cfg.padding.name : "PKCS5Padding";
                        var encrypted = __cryptoBridge.aesEncrypt(pt, k, iv, mode, pad);
                        return {
                            value: encrypted,
                            toString: function() { return encrypted; }
                        };
                    }
                };

                var mode = {
                    CBC: { name: "CBC" },
                    ECB: { name: "ECB" },
                    CTR: { name: "CTR" }
                };

                var pad = {
                    Pkcs7: { name: "PKCS5Padding" },
                    PKCS5Padding: { name: "PKCS5Padding" },
                    NoPadding: { name: "NoPadding" },
                    ZeroPadding: { name: "ZeroPadding" }
                };

                return {
                    enc: enc,
                    AES: AES,
                    mode: mode,
                    pad: pad,
                    MD5: function(str) {
                        var val = (typeof str === 'object' && str.value !== undefined) ? str.value : String(str || "");
                        var res = __cryptoBridge.md5(val);
                        return { value: res, toString: function() { return res; } };
                    },
                    SHA256: function(str) {
                        var val = (typeof str === 'object' && str.value !== undefined) ? str.value : String(str || "");
                        var res = __cryptoBridge.sha256(val);
                        return { value: res, toString: function() { return res; } };
                    },
                    HmacSHA256: function(message, key) {
                        var msg = (typeof message === 'object' && message.value !== undefined) ? message.value : String(message || "");
                        var k = (typeof key === 'object' && key.value !== undefined) ? key.value : String(key || "");
                        var res = __cryptoBridge.hmacSha256(msg, k);
                        return { value: res, toString: function() { return res; } };
                    }
                };
            })();
        """.trimIndent()

        context.evaluateString(scope, polyfills, "polyfills.js", 1, null)
        return scope
    }
}

