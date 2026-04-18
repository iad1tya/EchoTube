package com.echotube.iad1tya.di

import android.content.Context
import com.echotube.iad1tya.data.local.PlayerPreferences
import com.echotube.iad1tya.data.repository.YouTubeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideYouTubeRepository(playerPreferences: PlayerPreferences): YouTubeRepository {
        return YouTubeRepository.getInstance(playerPreferences)
    }

    @Provides
    @Singleton
    fun provideSubscriptionRepository(@ApplicationContext context: Context): com.echotube.iad1tya.data.local.SubscriptionRepository {
        return com.echotube.iad1tya.data.local.SubscriptionRepository.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideLikedVideosRepository(@ApplicationContext context: Context): com.echotube.iad1tya.data.local.LikedVideosRepository {
        return com.echotube.iad1tya.data.local.LikedVideosRepository.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideViewHistory(@ApplicationContext context: Context): com.echotube.iad1tya.data.local.ViewHistory {
        return com.echotube.iad1tya.data.local.ViewHistory.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideInterestProfile(@ApplicationContext context: Context): com.echotube.iad1tya.data.recommendation.InterestProfile {
        return com.echotube.iad1tya.data.recommendation.InterestProfile.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideMusicPlaylistRepository(@ApplicationContext context: Context): com.echotube.iad1tya.data.music.PlaylistRepository {
        return com.echotube.iad1tya.data.music.PlaylistRepository(context)
    }


    // VideoDownloadManager is now @Singleton @Inject — Hilt provides it automatically
    @Provides
    @Singleton
    fun providePlayerPreferences(@ApplicationContext context: Context): com.echotube.iad1tya.data.local.PlayerPreferences {
        return com.echotube.iad1tya.data.local.PlayerPreferences(context)
    }

    @Provides
    @Singleton
    fun provideShortsRepository(@ApplicationContext context: Context): com.echotube.iad1tya.data.shorts.ShortsRepository {
        return com.echotube.iad1tya.data.shorts.ShortsRepository.getInstance(context)
    }
}
