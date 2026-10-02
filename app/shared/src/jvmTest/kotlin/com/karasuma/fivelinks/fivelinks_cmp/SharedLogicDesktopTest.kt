package com.karasuma.fivelinks.fivelinks_cmp

import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertNotNull

class SharedLogicDesktopTest {

    @Test
    fun testMainDispatcherIsAvailableOnDesktop() {
        val mainDispatcher = Dispatchers.Main
        assertNotNull(mainDispatcher)
    }

    @Test
    fun testGameViewModelCanBeCreatedOnDesktop() {
        val vm = GameViewModel()
        assertNotNull(vm.uiState.value)
    }
}