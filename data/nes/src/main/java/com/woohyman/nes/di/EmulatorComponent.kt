package com.woohyman.nes.di

import com.woohyman.keyboard.emulator.NesEmulator
import com.woohyman.nes.NesEmulatorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EmulatorComponent {

    @Binds
    @Singleton
    abstract fun bindNesEmulator(
        nesEmulatorImpl: NesEmulatorImpl,
    ): NesEmulator
}