package com.woohyman.xml.di

import com.woohyman.xml.emulator.EmulatorMediator
import com.woohyman.xml.emulator.IEmulatorMediator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelPlugin {

    @Binds
    @ViewModelScoped
    abstract fun bindEmulatorMediator(
        emulatorMediator: EmulatorMediator,
    ): IEmulatorMediator

}