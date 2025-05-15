package com.bars.exchange.tracker.di

import android.content.Context // Added
import com.bars.exchange.tracker.data.datasource.IDataSource
import com.bars.exchange.tracker.data.datasource.LocalDataSource // Added
import com.bars.exchange.tracker.data.datasource.RemoteDataSource
import com.bars.exchange.tracker.data.datasource.local.AppDatabase // Added
import com.bars.exchange.tracker.data.datasource.local.dao.AssetDao // Added
import com.bars.exchange.tracker.data.datasource.local.dao.SelectedAssetDao
import com.bars.exchange.tracker.data.datasource.local.dao.TickerUpdateDao
import com.bars.exchange.tracker.data.repository.AssetRepositoryImpl
import com.bars.exchange.tracker.domain.repository.IAssetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext // Added
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient // Added
import io.ktor.client.engine.okhttp.OkHttp // Added
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation // Added
import io.ktor.client.plugins.logging.LogLevel // Added
import io.ktor.client.plugins.logging.Logging // Added
import io.ktor.client.plugins.websocket.WebSockets // Added
import io.ktor.serialization.kotlinx.json.json // Added
import javax.inject.Singleton
import kotlinx.serialization.json.Json // Added

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Database Providers ---
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return AppDatabase.getDatabase(appContext)
    }

    @Provides
    @Singleton
    fun provideAssetDao(appDatabase: AppDatabase): AssetDao {
        return appDatabase.assetDao()
    }
    
    @Provides
    @Singleton
    fun provideSelectedAssetDao(appDatabase: AppDatabase): SelectedAssetDao {
        return appDatabase.selectedAssetDao()
    }
    
    @Provides
    @Singleton
    fun provideTickerUpdateDao(appDatabase: AppDatabase): TickerUpdateDao {
        return appDatabase.tickerUpdateDao()
    }

    // --- DataSource Providers (Qualified) ---
    @Provides
    @Singleton
    @LocalDataSourceAnnotation // Qualifier
    fun provideLocalDataSource(assetDao: AssetDao, selectedAssetDao: SelectedAssetDao, tickerUpdateDao: TickerUpdateDao): IDataSource { // Returns IDataSource
        return LocalDataSource(assetDao, selectedAssetDao, tickerUpdateDao)
    }

    @Provides
    @Singleton
    @RemoteDataSourceAnnotation // Qualifier
    fun provideRemoteDataSource(httpClient: HttpClient): IDataSource { // Returns IDataSource
        return RemoteDataSource(httpClient)
    }

    // --- Repository Provider ---
    @Provides
    @Singleton
    fun provideAssetRepository(
        @RemoteDataSourceAnnotation remoteDataSource: IDataSource, // Inject qualified RemoteDataSource
        @LocalDataSourceAnnotation localDataSource: IDataSource    // Inject qualified LocalDataSource
    ): IAssetRepository {
        return AssetRepositoryImpl(remoteDataSource, localDataSource)
    }

    // --- Network Providers (Ktor HttpClient) ---
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            // Engine-specific configuration (OkHttp)
            engine {
                // config {
                //    // OkHttp specific settings, e.g., timeouts
                //    connectTimeout(30, TimeUnit.SECONDS)
                //    readTimeout(30, TimeUnit.SECONDS)
                //    writeTimeout(30, TimeUnit.SECONDS)
                // }
            }

            // For JSON Serialization/Deserialization
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true // Important for Binance API responses
                })
            }

            // For Logging HTTP requests and responses (Optional, good for debugging)
            install(Logging) {
                level = LogLevel.ALL // Or LogLevel.INFO, LogLevel.BODY, etc.
                // logger = object : Logger {
                //     override fun log(message: String) {
                //         Log.v("KtorLogger", message)
                //     }
                // }
            }

            // For WebSockets (the same client instance can be used for HTTP and WebSockets)
            install(WebSockets) {
                // pingIntervalMillis = 15_000 // Optional: configure ping interval
            }
        }
    }

    // TODO: Add @Provides methods for WebSocketClient later
    // @Provides
    // @Singleton
    // fun provideWebSocketClient(): YourWebSocketClient { /* ... setup Ktor WebSocket client ... */ }
}
