package com.team.expensetracker.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AutoLoggingController].
 * Uses simple in-memory fakes to test state transitions without Android dependencies.
 */
class AutoLoggingControllerTest {

    private class FakeSmsPermissionChecker(var granted: Boolean = false) : SmsPermissionChecker {
        override fun isGranted(): Boolean = granted
    }

    private class FakeReceiverSwitch(var isSwitchOn: Boolean = false) : ReceiverSwitch {
        override fun setEnabled(enabled: Boolean) {
            this.isSwitchOn = enabled
        }
    }

    private class FakeAutoLoggingStore(override var isEnabled: Boolean = false) : AutoLoggingStore

    private lateinit var permissionChecker: FakeSmsPermissionChecker
    private lateinit var receiverSwitch: FakeReceiverSwitch
    private lateinit var store: FakeAutoLoggingStore
    private lateinit var controller: AutoLoggingController

    @Before
    fun setUp() {
        permissionChecker = FakeSmsPermissionChecker(granted = false)
        receiverSwitch = FakeReceiverSwitch(isSwitchOn = false)
        store = FakeAutoLoggingStore(isEnabled = false)
        controller = AutoLoggingController(permissionChecker, receiverSwitch, store)
    }

    @Test
    fun `initial state is Off when not previously enabled`() {
        assertEquals(AutoLoggingState.Off, controller.state.value)
        assertFalse(store.isEnabled)
        assertFalse(receiverSwitch.isSwitchOn)
    }

    @Test
    fun `enable when permission already granted enables receiver immediately and transitions to On`() {
        permissionChecker.granted = true

        controller.onEnableRequested()

        assertEquals(AutoLoggingState.On, controller.state.value)
        assertTrue(receiverSwitch.isSwitchOn)
        assertTrue(store.isEnabled)
    }

    @Test
    fun `enable when permission not granted transitions to ShowingExplainer without enabling receiver`() {
        permissionChecker.granted = false

        controller.onEnableRequested()

        assertEquals(AutoLoggingState.ShowingExplainer, controller.state.value)
        assertFalse(receiverSwitch.isSwitchOn)
        assertFalse(store.isEnabled)
    }

    @Test
    fun `explainer accepted transitions to RequestingPermission`() {
        permissionChecker.granted = false
        controller.onEnableRequested()
        assertEquals(AutoLoggingState.ShowingExplainer, controller.state.value)

        controller.onExplainerAccepted()

        assertEquals(AutoLoggingState.RequestingPermission, controller.state.value)
        assertFalse(receiverSwitch.isSwitchOn)
        assertFalse(store.isEnabled)
    }

    @Test
    fun `permission granted transitions to On and enables receiver`() {
        permissionChecker.granted = false
        controller.onEnableRequested()
        controller.onExplainerAccepted()

        controller.onPermissionResult(granted = true)

        assertEquals(AutoLoggingState.On, controller.state.value)
        assertTrue(receiverSwitch.isSwitchOn)
        assertTrue(store.isEnabled)
    }

    @Test
    fun `permission denied transitions to PermissionDenied and receiver stays disabled`() {
        permissionChecker.granted = false
        controller.onEnableRequested()
        controller.onExplainerAccepted()

        controller.onPermissionResult(granted = false)

        assertEquals(AutoLoggingState.PermissionDenied, controller.state.value)
        assertFalse(receiverSwitch.isSwitchOn)
        assertFalse(store.isEnabled)
    }

    @Test
    fun `dismiss explainer transitions back to Off and receiver stays disabled`() {
        permissionChecker.granted = false
        controller.onEnableRequested()
        assertEquals(AutoLoggingState.ShowingExplainer, controller.state.value)

        controller.onExplainerDismissed()

        assertEquals(AutoLoggingState.Off, controller.state.value)
        assertFalse(receiverSwitch.isSwitchOn)
        assertFalse(store.isEnabled)
    }

    @Test
    fun `disable transitions to Off and disables receiver and clears store flag`() {
        // Start in On state
        permissionChecker.granted = true
        controller.onEnableRequested()
        assertEquals(AutoLoggingState.On, controller.state.value)
        assertTrue(receiverSwitch.isSwitchOn)
        assertTrue(store.isEnabled)

        // Now disable
        controller.disable()

        assertEquals(AutoLoggingState.Off, controller.state.value)
        assertFalse(receiverSwitch.isSwitchOn)
        assertFalse(store.isEnabled)
    }

    @Test
    fun `refresh with revoked permission disables receiver and transitions to Off`() {
        // Initially granted and enabled
        permissionChecker.granted = true
        controller.onEnableRequested()
        assertEquals(AutoLoggingState.On, controller.state.value)

        // User revokes permission outside the app
        permissionChecker.granted = false
        controller.refresh()

        assertEquals(AutoLoggingState.Off, controller.state.value)
        assertFalse(receiverSwitch.isSwitchOn)
        assertFalse(store.isEnabled)
    }

    @Test
    fun `initialization self-heals if store was true but permission was revoked`() {
        val testStore = FakeAutoLoggingStore(isEnabled = true)
        val testChecker = FakeSmsPermissionChecker(granted = false)
        val testSwitch = FakeReceiverSwitch(isSwitchOn = true)

        val newController = AutoLoggingController(testChecker, testSwitch, testStore)

        assertEquals(AutoLoggingState.Off, newController.state.value)
        assertFalse(testStore.isEnabled)
        assertFalse(testSwitch.isSwitchOn)
    }
}
