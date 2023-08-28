package com.woohyman.keyboard.rom

import com.woohyman.keyboard.data.database.GameDescription
import kotlinx.coroutines.flow.SharedFlow

interface IRomLauncher {

    val isDBEmpty: Boolean

    suspend fun LauncherRom(game: GameDescription)

    val romLauncherState: SharedFlow<Result<GameDescription>>
}