package com.woohyman.nes.di

import com.woohyman.keyboard.emulator.NesEmulator
import com.woohyman.nes.NesEmulatorImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EmulatorComponent {

//    @Binds
//    abstract fun bindNesEmulator(
//        nesEmulatorImpl: NesEmulatorImpl,
//    ): NesEmulator


    @Provides
    @Singleton
    fun provideNesEmulator(): NesEmulator {
        return NesEmulatorImpl()
    }
}