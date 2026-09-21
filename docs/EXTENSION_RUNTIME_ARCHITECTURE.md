# Production Extension Runtime Architecture

## 1. Overview & Core Principles

The Animey production extension runtime is designed as a **hardened, sandboxed, out-of-process isolation architecture**. Its primary goal is to safely execute third-party extensions without risking the stability, security, or performance of the primary application process.

### Fundamental Invariants
1. **No Arbitrary Untrusted Code in Main Process**: Untrusted extension code (whether remote scripts, web-based extractors, or dynamic modules) is executed inside an isolated sandbox boundary with strictly controlled capabilities.
2. **Immutable Domain Contract**: The core application continues to consume only normalized domain models:
   ```
   Extension (Isolated Sandbox)
     └── IPC / Isolation Bridge
          └── Normalized Domain Models (Anime, Episode, VideoSource)
               └── Domain Repositories & UseCases
                    └── Player / UI / Tracking / Room Database
   ```
   No UI screen (Anime UI, Episode UI, Player), background tracker (AniList), or persistence layer (Room DB) ever depends on extension internals or runtime implementations.
3. **Fail-Safe & Self-Healing**: A malfunctioning or crashing extension can never crash the Android application. Repeated crashes trip a circuit breaker, and memory/coroutines are deterministically collected.

---

## 2. Architectural Layers

```
+-------------------------------------------------------------------------+
|                              Application Core                            |
|  [Anime UI]   [Episode UI]   [Player]   [AniList Sync]   [Room Database]|
+-------------------------------------------------------------------------+
                                    ▲
                                    │ (Normalized Domain Models)
                                    ▼
+-------------------------------------------------------------------------+
|                  Extension Domain Facade (ExtensionManager)             |
+-------------------------------------------------------------------------+
                                    │
                                    ▼
+-------------------------------------------------------------------------+
|                       ExtensionRuntimeManager                           |
|  - Lifecycle Coordination       - Runtime Registry                      |
|  - Update Orchestrator          - Global Preference Routing             |
+-------------------------------------------------------------------------+
          │                                           │
          ▼                                           ▼
+---------------------------------------+  +------------------------------+
|       BuiltInExtensionRuntime         |  |   IsolatedProcessRuntime     |
|   (Bundled, verified Kotlin sources)  |  | (IPC / Sandboxed Sandbox)    |
+---------------------------------------+  +------------------------------+
          │                                           │
          ▼                                           ▼
+-------------------------------------------------------------------------+
|                 Sandboxed Execution Wrapper (IsolatedAnimeExtension)    |
|  - Operation Timeout Enforcer (withTimeout)                             |
|  - Circuit Breaker (CLOSED -> OPEN -> HALF_OPEN)                        |
|  - Exception Containment & Typed Error Normalization                    |
|  - Structured Logging & Metrics Collection                              |
+-------------------------------------------------------------------------+
                                    │
                                    ▼
+-------------------------------------------------------------------------+
|               Security Policy & Controlled Networking                   |
|  - ExtensionValidator (Schema, Version, Checksums)                      |
|  - ExtensionCapability & Permission System                              |
|  - ExtensionNetworkPolicyEnforcer (Domain Whitelisting, SSRF Guard)     |
|  - ControlledHttpClient (Rate Limiting, Header Sanitization)            |
+-------------------------------------------------------------------------+
```

---

## 3. Extension API Versioning & Compatibility

### API Specifications
- **Current Host Extension API Version**: `1` (semver equivalent `1.0.0`).
- Every extension manifest declares:
  - `apiVersion`: The API level against which the extension was built.
  - `minApiVersion`: Minimum host API version supported.
  - `targetApiVersion`: Recommended host API version.
  - `minAppVersion`: Minimum app build version (e.g., `1.0.0`).

### Compatibility Matrix
- If `manifest.apiVersion > HOST_CURRENT_API_VERSION`, loading is rejected with `ExtensionValidationResult.Incompatible("Extension requires newer API version")`.
- If `manifest.minAppVersion > HOST_APP_VERSION`, loading is rejected.
- Graceful backward compatibility: Older API version extensions run in compatibility mode.

---

## 4. Manifest Validation & Security Enforcement

### Validation Rules
1. **Identifier Safety**: Regex `^[a-zA-Z0-9_.-]{3,64}$` to prevent path traversal or injection.
2. **Endpoint Validation**: Only `https://` and `http://` protocols. Protocols such as `file:`, `javascript:`, `content:`, and `data:` are blocked.
3. **Integrity & Checksum**: SHA-256 payload checksum validation prior to dynamic loading.

### Capability & Permission Model
Extensions must explicitly declare capabilities in their manifest. Undeclared operations are blocked:
- `NETWORK`: Permission to execute HTTP/HTTPS requests.
- `PREFERENCES`: Permission to persist isolated key-value settings.
- `DOM_PARSING`: Permission to parse HTML/XML structures.
- `COOKIES`: Permission to store and send session cookies.

---

## 5. Controlled Networking & SSRF Prevention

All networking originating from an extension is routed through `ExtensionNetworkPolicyEnforcer`:
1. **Domain Whitelisting**: An extension may only query domains declared in its `allowedDomains` list or matching its `baseUrl`.
2. **SSRF Guard**: Prohibits access to:
   - Loopback addresses (`127.0.0.1`, `localhost`, `::1`)
   - RFC 1918 private subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`)
   - Link-local and cloud metadata endpoints (`169.254.169.254`)
3. **Rate Limiting**: Configurable token bucket per extension (e.g., maximum 30 requests/minute) to prevent server hammering.
4. **Header Sanitization**: Prevents tampering with sensitive app-level headers and injects standard `User-Agent`.

---

## 6. Extension Lifecycle & Load/Unload

### Lifecycle States
```
UNLOADED ──> VALIDATING ──> LOADING ──> ACTIVE ──> UNLOADING ──> TERMINATED
                │              │           │
                ▼              ▼           ▼
              ERROR          ERROR       PAUSED (Circuit Breaker OPEN)
```

- **Load**:
  1. Validate manifest and permissions.
  2. Instantiate isolated sandbox / container.
  3. Wire controlled networking and preferences.
  4. Wrap in `IsolatedAnimeExtension`.
  5. Register into runtime registry.
- **Unload**:
  1. Cancel all active coroutines and child jobs.
  2. Close network connections and clear ephemeral caches.
  3. Release references for garbage collection.
- **Atomic Reload / Update**:
  1. Validate new extension payload and initialize new instance in background.
  2. Migrate existing user preferences.
  3. Atomically replace active reference in `instances` map.
  4. Unload old instance cleanly. If initialization fails, rollback to previous instance.

---

## 7. Crash Isolation & Timeout Handling

- **Thread & Coroutine Boundary**: Each extension call runs inside an isolated coroutine scope with independent timeout constraints (`withTimeout`):
  - Catalog browsing: 15 seconds
  - Search: 15 seconds
  - Video stream extraction: 25 seconds
- **Circuit Breaker**:
  - `CLOSED`: Normal operation.
  - `OPEN`: If consecutive failures reach `5`, requests fast-fail immediately for `30` seconds without touching external servers.
  - `HALF_OPEN`: Allows a single probe request to test extension recovery.
- **Exception Normalization**: All errors (network, parsing, timeout, crash) are transformed into typed `AppResult.Error(AppError)` so caller code never encounters unhandled exceptions.

---

## 8. Logging & Observability

- In-memory structured ring buffer per extension (capped at e.g. 50 log entries).
- Telemetry metrics:
  - Total requests, successful requests, failed requests
  - Average latency (ms)
  - Consecutive failures & current circuit breaker state
  - Last failure timestamp and cause
- Sensitive token scrubbing in URL and body logs.
