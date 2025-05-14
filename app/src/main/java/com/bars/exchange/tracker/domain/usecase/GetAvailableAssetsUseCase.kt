package com.bars.exchange.tracker.domain.usecase

import com.bars.exchange.tracker.domain.model.Asset
import com.bars.exchange.tracker.domain.repository.IAssetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting the list of available trading assets.
 */
class GetAvailableAssetsUseCase @Inject constructor(
    private val assetRepository: IAssetRepository
) {
    /**
     * Executes the use case to fetch available assets.
     *
     * @return A Flow emitting a Result containing a list of Assets on success, or an error on failure.
     *         We use Flow<Result<List<Asset>>> to handle loading/success/error states reactively.
     */
    operator fun invoke(): Flow<Result<List<Asset>>> {
        return assetRepository.getAvailableAssets()
    }
}
