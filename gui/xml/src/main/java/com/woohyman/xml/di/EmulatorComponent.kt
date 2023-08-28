package com.woohyman.xml.di

import com.woohyman.xml.gamegallery.IPermissionManager
import com.woohyman.xml.gamegallery.PermissionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ActivityComponent::class)
abstract class EmulatorComponent {

    @Binds
    @ActivityScoped
    abstract fun bindPermissionManager(
        permissionManager: PermissionManager,
    ): IPermissionManager

}