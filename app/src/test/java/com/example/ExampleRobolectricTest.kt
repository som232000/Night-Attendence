package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AdminEntity
import com.example.data.repository.NightShiftRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: NightShiftRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NightShiftRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Night Shift", appName)
    }

    @Test
    fun testAdminAuthentication() = runTest {
        val success = repository.authenticateAdmin("admin", "admin123")
        assertTrue("Admin login should succeed with default credentials", success)

        val wrongPass = repository.authenticateAdmin("admin", "wrongpass")
        assertFalse("Admin login should fail with invalid password", wrongPass)
    }

    @Test
    fun testMistriRegistrationAndLogin() = runTest {
        val regResult = repository.registerMistri("Test Mistri", "9876543210", "pass123")
        assertTrue("Registration should succeed", regResult.isSuccess)
        val mistri = regResult.getOrNull()
        assertNotNull(mistri)

        // Login with correct credentials
        val loginResult = repository.loginMistri("9876543210", "pass123")
        assertTrue("Login should succeed", loginResult.isSuccess)

        // Login with wrong password
        val wrongLogin = repository.loginMistri("9876543210", "badpass")
        assertFalse("Login with wrong password should fail", wrongLogin.isSuccess)

        // Duplicate registration check
        val dupResult = repository.registerMistri("Duplicate", "9876543210", "pass456")
        assertFalse("Duplicate mobile should fail", dupResult.isSuccess)
    }

    @Test
    fun testLabourAndNightShiftOperations() = runTest {
        val mistriResult = repository.registerMistri("Supervisor Mistri", "9988776655", "secret")
        val mistri = mistriResult.getOrThrow()

        // Add labour
        val labourId = repository.saveLabour(mistri.id, "Labour One", "9112233445")
        val labours = repository.getLaboursForMistri(mistri.id).first()
        assertEquals(1, labours.size)
        assertEquals("Labour One", labours[0].name)

        // Submit Night Shift Entry
        val shiftId = repository.saveNightShiftEntry(
            mistriId = mistri.id,
            mistriName = mistri.fullName,
            labourId = labourId,
            labourName = "Labour One",
            workPlace = "Site Alpha Sector 5",
            workDate = "2026-11-03",
            shiftDetails = "08:00 PM - 04:00 AM (8 hrs)",
            remarks = "Completed casting"
        )
        assertTrue(shiftId > 0)

        val entries = repository.getAllEntries().first()
        assertEquals(1, entries.size)
        assertEquals("Site Alpha Sector 5", entries[0].workPlace)

        // Deactivate Mistri Account
        repository.updateMistriStatus(mistri.id, false)
        val deactivatedLogin = repository.loginMistri("9988776655", "secret")
        assertFalse("Deactivated Mistri should not be able to log in", deactivatedLogin.isSuccess)
    }
}
