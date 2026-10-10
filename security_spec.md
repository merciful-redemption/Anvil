# Anvil v5.0 — Firestore Security Specification (Phase 0 TDD & Red Team Audit)

## 1. Data Invariants
1. **Strict Multi-Tenant User Isolation**: All user data lives under `/users/{userId}` and its subcollections (`concept_states`, `snippets`, `mistakes`, `reports`). A user may only read, create, update, or delete documents where `request.auth != null` and `request.auth.uid == userId`, and the document's `userId` field matches `request.auth.uid`.
2. **Path Variable Hardening**: Every document ID (`userId`, `conceptId`, `snippetId`, `mistakeId`, `reportId`) must satisfy `isValidId(id)` (`id is string && id.size() >= 1 && id.size() <= 128 && id.matches('^[a-zA-Z0-9_\\-]+$')`) on single-document operations (`get`, `create`, `update`, `delete`).
3. **Immutable Ownership & Creation Metadata**: On `update`, `incoming().userId == existing().userId` and `incoming().createdAt == existing().createdAt` must hold for every entity.
4. **Temporal Integrity**: On `create` and `update`, timestamp fields (`createdAt`, `updatedAt`) must be server timestamps validated via `is timestamp && <= request.time`.
5. **Bounded String & Numeric Enforcements**: Every string field has an explicit `.size() <= MAX` bound, and every numeric field has range bounds to prevent resource exhaustion and value poisoning.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read (`users/{userId}`)**: Attempt `get` with `request.auth == null` -> Rejected (`isSignedIn()`).
2. **Cross-User Profile Read (`users/bob_456` by `alice_123`)**: Attempt `get` where `request.auth.uid != userId` -> Rejected (`request.auth.uid == userId`).
3. **Identity Spoofing on Create (`users/alice_123` with `userId: "bob_456"`)**: Attempt `create` where `incoming().userId != request.auth.uid` -> Rejected.
4. **Shadow Update / Ghost Field Injection (`isVerified: true`)**: Attempt `update` with an undeclared field -> Rejected by `incoming().keys().hasOnly(...)` and `incoming().diff(existing()).affectedKeys().hasOnly(...)`.
5. **Ownership Mutation on Update (`userId` changed after creation)**: Attempt `update` modifying `userId` -> Rejected by `incoming().userId == existing().userId` and `affectedKeys().hasOnly(...)`.
6. **Creation Timestamp Tampering (`createdAt` modified on update)**: Attempt `update` changing `createdAt` -> Rejected by `incoming().createdAt == existing().createdAt`.
7. **Future Timestamp Injection (`createdAt` in the future)**: Attempt `create` with client timestamp ahead of `request.time` -> Rejected by `incoming().createdAt <= request.time`.
8. **Value Poisoning on Numeric Field (`xp: "one_million"` or `xp: -50`)**: Attempt `create` or `update` with wrong type or negative XP -> Rejected by `isValidUserProfile(incoming())`.
9. **Denial-of-Wallet Oversized String (`code` > 50,000 chars or `displayName` > 100 chars)**: Attempt `create` with oversized string -> Rejected by `.size() <= MAX` in `isValid*`.
10. **Cross-User Subcollection Scraping (`users/bob_456/snippets` list by `alice_123`)**: Attempt `list` on another user's subcollection -> Rejected by `request.auth.uid == userId && resource.data.userId == request.auth.uid`.
11. **Missing Required Field via Partial Update Delete (`FieldValue.delete()` on `title`)**: Attempt `update` deleting required `title` -> Rejected because `isValidCodeSnippet(incoming())` asserts `data.title is string`.
12. **Unprotected Wildcard Collection Access (`/unknown_collection/doc1`)**: Attempt read/write to undeclared collection -> Rejected by global default-deny `match /{document=**} { allow read, write: if false; }`.

## 3. Red Team Conflict Report
- **Identity Spoofing**: Blocked on all 5 collections via `request.auth.uid == userId && incoming().userId == request.auth.uid` (create) and `incoming().userId == existing().userId` (update).
- **Shadow Updates**: Blocked via strict `incoming().keys().hasOnly(...)` on create and `incoming().diff(existing()).affectedKeys().hasOnly(...)` on update.
- **Value Poisoning**: Blocked because `isValid[Entity](incoming())` wraps every `create` and `update` rule.
- **Result**: 0 vulnerabilities remaining.
