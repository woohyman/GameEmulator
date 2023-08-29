package com.woohyman.xml.di

import com.woohyman.xml.emulator.NesGameDataProvider
import com.woohyman.keyboard.rom.INesGameDataProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EmulatorComponent {

    @Binds
    @Singleton
    abstract fun bindFetchProxy(
        nesGameDataProvider: NesGameDataProvider,
    ): INesGameDataProvider

}