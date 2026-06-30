package com.trainpaths.nonogram

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

// TODO: Web requires a DatabaseFactory implementation (jsMain/wasmJsMain) before it can run.
//  Once a web DatabaseFactory exists, construct AppSDK and ViewModels here directly
//  (no Koin on web), e.g.:
//
//  val sdk = AppSDK(WebDatabaseFactory())
//  App(
//      menuViewModel = remember { MenuViewModel(sdk) },
//      gameViewModelFactory = { id ->
//          val vm = remember(id) { GameViewModel(sdk) }
//          LaunchedEffect(id) { vm.loadNonogram(id) }
//          vm
//      }
//  )

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        // Web build is a work-in-progress — DatabaseFactory for web is not yet implemented.
    }
}
