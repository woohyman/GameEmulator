package com.woohyman.xml.di

import com.woohyman.xml.emulator.EmulatorMediator
import com.woohyman.xml.emulator.IEmulatorMediator
import com.woohyman.xml.gamegallery.IPermissionManager
import com.woohyman.xml.gamegallery.PermissionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
abstract class ActivityPlugin {

    @Binds
    @ActivityScoped
    abstract fun bindPermissionManager(
        permissionManager: PermissionManager,
    ): IPermissionManager

    @Binds
    @ActivityScoped
    abstract fun bindEmulatorMediator(
        emulatorMediator: EmulatorMediator,
    ): IEmulatorMediator

}