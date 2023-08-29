package com.woohyman.xml.di

import com.woohyman.keyboard.emulator.NesEmulator
import com.woohyman.xml.emulator.business.FetchProxy
import com.woohyman.xml.emulator.business.IFetchProxy
import com.woohyman.xml.gamegallery.IPermissionManager
import com.woohyman.xml.gamegallery.PermissionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EmulatorComponent {

    @Binds
    @Singleton
    abstract fun bindFetchProxy(
        fetchProxy: FetchProxy,
    ): IFetchProxy

}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FetchProxyEntryPoint {
    fun getFetchProxy(): IFetchProxy
}