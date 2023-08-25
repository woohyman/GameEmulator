package com.woohyman.keyboard.di

import com.woohyman.keyboard.rom.IRomLauncher
import com.woohyman.keyboard.rom.RomLauncher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SingleComponent {

    @Binds
    @Singleton
    abstract fun bindNesEmulator(
        romLauncher: RomLauncher,
    ): IRomLauncher
}