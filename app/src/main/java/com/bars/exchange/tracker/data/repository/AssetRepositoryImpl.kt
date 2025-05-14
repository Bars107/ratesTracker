package com.bars.exchange.tracker.data.repository

import com.bars.exchange.tracker.data.datasource.IDataSource
import com.bars.exchange.tracker.data.datasource.TickerData
import com.bars.exchange.tracker.di.LocalDataSourceAnnotation
import com.bars.exchange.tracker.di.RemoteDataSourceAnnotation
import com.bars.exchange.tracker.domain.model.Asset
import com.bars.exchange.tracker.domain.repository.IAssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Implementation of the IAssetRepository interface.
 * It uses a RemoteDataSource to fetch fresh data and a LocalDataSource as a cache and single source of truth.
 */
class AssetRepositoryImpl @Inject constructor(
    @RemoteDataSourceAnnotation private val remoteDataSource: IDataSource,
    @LocalDataSourceAnnotation private val localDataSource: IDataSource
) : IAssetRepository {

    override fun getAvailableAssets(): Flow<Result<List<Asset>>> = flow {
        // Attempt to fetch from remote and update local cache.
        // This runs when the flow is first collected.
        try {
            println("AssetRepository: Attempting to fetch from remote data source...")
            remoteDataSource.getAvailableAssets().firstOrNull()?.let { remoteResult -> // Collect first emission
                remoteResult.fold(
                    onSuccess = { assetInfoList ->
                        println("AssetRepository: Successfully fetched from remote. Updating local cache...")
                        localDataSource.clearAllAssets()
                        localDataSource.saveAssets(assetInfoList)
                        println("AssetRepository: Local cache updated.")
                    },
                    onFailure = { exception ->
                        println("AssetRepository: Failed to fetch from remote: ${exception.message}. Will serve from local cache.")
                        // Optionally, re-throw or handle if local cache is empty and this is critical
                    }
                )
            }
        } catch (e: Exception) {
            println("AssetRepository: Exception during remote fetch or local save: ${e.message}")
            // Local data will still be served by the flow below
        }

        // Emit the flow from the local data source (single source of truth)
        println("AssetRepository: Collecting from local data source...")
        localDataSource.getAvailableAssets().collect { localResult ->
            localResult.fold(
                onSuccess = { assetInfoList ->
                    val domainAssets = assetInfoList.map {
                        Asset(
                            symbol = it.symbol,
                            baseAsset = it.baseAsset,
                            quoteAsset = it.quoteAsset,
                            status = it.status
                        )
                    }
                    // println("AssetRepository: Emitting ${domainAssets.size} assets from local data source.")
                    emit(Result.success(domainAssets))
                },
                onFailure = { exception ->
                    println("AssetRepository: Error fetching from local data source: ${exception.message}")
                    emit(Result.failure(exception))
                }
            )
        }
    }

    override fun getTickerUpdates(symbols: List<String>): Flow<TickerData> {
        // For now, ticker updates are directly from the remote source.
        // Future enhancements could include caching or offline handling for ticker data if feasible.
        return remoteDataSource.getTickerUpdates(symbols)
    }

    override fun stopTickerUpdates(symbols: List<String>) {
        remoteDataSource.stopTickerUpdates(symbols)
    }

    override fun stopAllTickerUpdates() {
        remoteDataSource.stopAllTickerUpdates()
    }
}
