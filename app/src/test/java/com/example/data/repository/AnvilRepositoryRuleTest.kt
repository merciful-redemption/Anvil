package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FsrsGrade
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AnvilRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_and_createSnippet_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = AnvilRepository(firestore, auth)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile("Alice Smith")
        }
        assertTrue("Expected profile creation to succeed: ${profileResult.exceptionOrNull()}", profileResult.isSuccess)

        val snippetResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createSnippet(
                title = "Binary Search",
                language = "Python",
                code = "def bsearch(arr, x): return 0",
                category = "Playground"
            )
        }
        assertTrue("Expected snippet creation to succeed: ${snippetResult.exceptionOrNull()}", snippetResult.isSuccess)
        val snippetId = snippetResult.getOrThrow()

        val emittedSnippets = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSnippets().first { list -> list.any { it.id == snippetId } }
        }
        assertTrue(emittedSnippets.any { it.id == snippetId })
    }

    @Test
    fun recordConceptReview_and_generateReport_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = AnvilRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) { repository.ensureUserProfile("Alice Smith") }

        val reviewResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordConceptReview(
                conceptId = "py_loops",
                courseId = "python-core",
                domain = "Control Flow",
                title = "For & While Loops",
                grade = FsrsGrade.GOOD,
                xpDelta = 20L
            )
        }
        assertTrue("Expected concept review to succeed: ${reviewResult.exceptionOrNull()}", reviewResult.isSuccess)
        val conceptState = reviewResult.getOrThrow()

        val reportResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.generateMonthlyReport(
                periodLabel = "October 2026",
                concepts = listOf(conceptState),
                profile = null
            )
        }
        assertTrue("Expected report generation to succeed: ${reportResult.exceptionOrNull()}", reportResult.isSuccess)
        assertNotNull(reportResult.getOrThrow().overallBand)
    }

    @Test
    fun getSnippetById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = AnvilRepository(firestore, auth)
        val snippetId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createSnippet("Secret Algo", "Python", "print(1)").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = AnvilRepository(firestore, auth)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getSnippetById(aliceUid, snippetId)
        }
        assertTrue("Expected cross-user read to fail", crossResult.isFailure)
        val exception = crossResult.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull("Expected FirebaseFirestoreException", exception)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
    }

    @Test
    fun observeSnippets_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).collection("snippets")
                    .whereEqualTo("userId", aliceUid)
                    .snapshots()
                    .first()
            }
            fail("Expected FirebaseFirestoreException.PERMISSION_DENIED when unauthenticated")
        } catch (t: Throwable) {
            var current: Throwable? = t
            var firestoreEx: FirebaseFirestoreException? = null
            while (current != null) {
                if (current is FirebaseFirestoreException) {
                    firestoreEx = current
                    break
                }
                current = current.cause
            }
            assertNotNull("Expected FirebaseFirestoreException in cause chain of $t", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@anvil.test"
        const val BOB_EMAIL = "bob@anvil.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
