package com.aistudio.classroomlms

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aistudio.classroomlms.data.database.LmsSettings
import com.aistudio.classroomlms.fake.FakePreferencesManager
import com.aistudio.classroomlms.testutil.MainDispatcherRule
import com.aistudio.classroomlms.ui.viewmodel.SessionLockViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionLockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakePreferencesManager: FakePreferencesManager
    private lateinit var viewModel: SessionLockViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fakePreferencesManager = FakePreferencesManager(context)
    }

    @Test
    fun `initial state is unlocked when biometric setting is disabled`() = runTest {
        fakePreferencesManager.saveSettings(LmsSettings(isBiometricLockEnabled = false))
        viewModel = SessionLockViewModel(fakePreferencesManager)
        advanceUntilIdle()

        assertFalse(viewModel.isLocked.value)
    }

    @Test
    fun `initial state is locked when biometric setting is enabled`() = runTest {
        fakePreferencesManager.saveSettings(LmsSettings(isBiometricLockEnabled = true))
        viewModel = SessionLockViewModel(fakePreferencesManager)
        advanceUntilIdle()

        assertTrue(viewModel.isLocked.value)
    }

    @Test
    fun `unlock transitions isLocked from true to false`() = runTest {
        fakePreferencesManager.saveSettings(LmsSettings(isBiometricLockEnabled = true))
        viewModel = SessionLockViewModel(fakePreferencesManager)
        advanceUntilIdle()
        assertTrue(viewModel.isLocked.value)

        viewModel.unlock()
        assertFalse(viewModel.isLocked.value)
    }

    @Test
    fun `lock transitions isLocked from false to true`() = runTest {
        fakePreferencesManager.saveSettings(LmsSettings(isBiometricLockEnabled = false))
        viewModel = SessionLockViewModel(fakePreferencesManager)
        advanceUntilIdle()
        assertFalse(viewModel.isLocked.value)

        viewModel.lock()
        assertTrue(viewModel.isLocked.value)
    }

    @Test
    fun `disabling biometric in settings automatically unlocks the session`() = runTest {
        fakePreferencesManager.saveSettings(LmsSettings(isBiometricLockEnabled = true))
        viewModel = SessionLockViewModel(fakePreferencesManager)
        advanceUntilIdle()
        assertTrue(viewModel.isLocked.value)

        fakePreferencesManager.saveSettings(LmsSettings(isBiometricLockEnabled = false))
        advanceUntilIdle()
        assertFalse(viewModel.isLocked.value)
    }
}
