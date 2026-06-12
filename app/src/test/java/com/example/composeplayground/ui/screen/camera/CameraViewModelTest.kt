package com.example.composeplayground.ui.screen.camera

import com.example.composeplayground.ui.screen.camera.model.CameraControlGroup
import com.example.composeplayground.ui.screen.camera.model.CameraMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraViewModelTest {

    @Test
    fun `toggleGrid switches grid visibility`() {
        val viewModel = CameraViewModel()

        viewModel.toggleGrid()
        assertTrue(viewModel.uiState.value.showGrid)

        viewModel.toggleGrid()
        assertFalse(viewModel.uiState.value.showGrid)
    }

    @Test
    fun `normal mode collapses dashboard and selects filters`() {
        val viewModel = CameraViewModel()
        viewModel.setMode(CameraMode.Advanced)

        viewModel.setMode(CameraMode.Normal)

        val state = viewModel.uiState.value
        assertEquals(CameraMode.Normal, state.mode)
        assertFalse(state.isDashboardExpanded)
        assertEquals(CameraControlGroup.Filters, state.activeControlGroup)
    }

    @Test
    fun `advanced mode expands dashboard and selects exposure`() {
        val viewModel = CameraViewModel()

        viewModel.setMode(CameraMode.Advanced)

        val state = viewModel.uiState.value
        assertEquals(CameraMode.Advanced, state.mode)
        assertTrue(state.isDashboardExpanded)
        assertEquals(CameraControlGroup.Exposure, state.activeControlGroup)
    }

    @Test
    fun `toggleDashboard switches dashboard expansion`() {
        val viewModel = CameraViewModel()

        viewModel.toggleDashboard()
        assertTrue(viewModel.uiState.value.isDashboardExpanded)

        viewModel.toggleDashboard()
        assertFalse(viewModel.uiState.value.isDashboardExpanded)
    }

    @Test
    fun `setActiveControlGroup expands dashboard with selected group`() {
        val viewModel = CameraViewModel()

        viewModel.setActiveControlGroup(CameraControlGroup.Focus)

        val state = viewModel.uiState.value
        assertTrue(state.isDashboardExpanded)
        assertEquals(CameraControlGroup.Focus, state.activeControlGroup)
    }
}
