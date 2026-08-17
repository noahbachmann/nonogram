package com.trainpaths.nonogram.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object MenuRoute

@Serializable
object GenListRoute

@Serializable
data class GenConfRoute(val editing: Boolean = false)

@Serializable
object GeneratorRoute

@Serializable
object SettingsRoute

@Serializable
data class GameRoute(val nonogramId: Long)

@Serializable
data class PlayDialogRoute(val nonogramId: Long)

@Serializable
object WinDialogRoute