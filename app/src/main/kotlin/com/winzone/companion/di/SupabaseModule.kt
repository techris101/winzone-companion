package com.winzone.companion.di

import com.winzone.companion.BuildConfig
import com.winzone.companion.agreement.AgreementGate
import com.winzone.companion.agreement.LocalAgreementGate
import com.winzone.companion.data.auth.AuthRepository
import com.winzone.companion.data.auth.AuthRepositoryImpl
import com.winzone.companion.data.match.MatchRepository
import com.winzone.companion.data.match.MatchRepositoryImpl
import com.winzone.companion.data.remote.SupabaseRpcClient
import com.winzone.companion.data.remote.SupabaseRpcClientImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SupabaseBindingsModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMatchRepository(impl: MatchRepositoryImpl): MatchRepository

    @Binds
    @Singleton
    abstract fun bindSupabaseRpcClient(impl: SupabaseRpcClientImpl): SupabaseRpcClient

    @Binds
    @Singleton
    abstract fun bindAgreementGate(impl: LocalAgreementGate): AgreementGate
}

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}
