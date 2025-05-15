package com.bars.exchange.tracker.data.datasource

import android.util.Log
import com.bars.exchange.tracker.data.datasource.dto.ExchangeInfoResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

/**
 * Remote data source implementation for fetching data from the Binance API.
 *
 * Uses Ktor HTTP client for API requests. WebSocket implementation for real-time
 * ticker updates will be added in the future.
 */
class RemoteDataSource(private val httpClient: HttpClient) : IDataSource { // Implements IDataSource
    companion object {
        private const val TAG = "RemoteDataSource"
    }

    // Base URL for Binance API - public endpoints
    private val binanceApiBaseUrl = "https://api.binance.com/api/v3/"

    // WebSocket URL for Binance streams - public streams
    private val binanceWebSocketBaseUrl = "wss://stream.binance.com:9443/ws"
    
    // Map to track active subscriptions
    private val activeSubscriptions = ConcurrentHashMap<String, Job>()
    
    // JSON parser
    private val json = Json { ignoreUnknownKeys = true }

    // --- Asset Information ---
    override suspend fun getAvailableAssets(): Result<List<AssetInfo>> = withContext(Dispatchers.IO) {
        try {
            // Make the GET request to the /exchangeInfo endpoint
            val response = httpClient.get(binanceApiBaseUrl + "exchangeInfo") {
                // Configure request parameters if needed (e.g., headers)
            }
            val exchangeInfo = response.body<ExchangeInfoResponse>()
            Result.success(exchangeInfo.symbols)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching available assets: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Caching/Local Storage Operations (No-op or Unsupported for RemoteDataSource) ---

    override suspend fun saveAssets(assets: List<AssetInfo>) {
        // This operation is not applicable to RemoteDataSource
        // Option 1: No-op
        // Option 2: throw UnsupportedOperationException("RemoteDataSource cannot save assets")
    }

    override suspend fun clearAllAssets() {
        // Not applicable
    }

    override suspend fun getAssetsCount(): Int {
        // Not applicable, could return 0 or throw exception
        return 0
    }

    override suspend fun getLastUpdateTime(): Long {
        // Not applicable, could return 0 or throw exception
        return 0L
    }

    // --- Real-time Ticker Operations ---
    override fun getTickerUpdates(symbols: List<String>): Flow<TickerData> {
        Log.d(TAG, "Initializing ticker updates for symbols: ${symbols.joinToString()}")
        
        // Create a shared flow that can be collected by multiple collectors
        val tickerFlow = MutableSharedFlow<TickerData>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        
        // For each symbol, create a WebSocket connection
        symbols.forEach { symbol ->
            // Skip if already subscribed
            if (activeSubscriptions.containsKey(symbol)) {
                return@forEach
            }
            
            val normalizedSymbol = symbol.lowercase()
            val streamName = "${normalizedSymbol}@ticker"
            
            // Create a coroutine job for the WebSocket connection
            val job = CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Connect to WebSocket and listen for updates
                    httpClient.webSocket("$binanceWebSocketBaseUrl/$streamName") {
                        Log.d(TAG,"RemoteDataSource: WebSocket connected for $symbol")
                        
                        // Keep the connection alive and process incoming frames
                        while (isActive) {
                            val frame = incoming.receive()
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                try {
                                    // Parse the JSON response
                                    val tickerData = parseTickerData(text, symbol)
                                    tickerFlow.emit(tickerData)
                                    Log.d(TAG,"Received ticker update for $symbol: ${tickerData.price}")
                                } catch (e: Exception) {
                                    Log.e(TAG,"Error parsing ticker data: ${e.message}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG,"WebSocket error for $symbol: ${e.message}")
                    // Try to reconnect after a delay
                    delay(5000)
                    // Remove the subscription so it can be recreated
                    activeSubscriptions.remove(symbol)
                }
            }
            
            // Store the job for later cancellation
            activeSubscriptions[symbol] = job
        }
        
        return tickerFlow
    }

    override fun stopTickerUpdates(symbols: List<String>) {
        symbols.forEach { symbol ->
            val job = activeSubscriptions.remove(symbol)
            job?.cancel()
            Log.d(TAG,"Stopped ticker updates for $symbol")
        }
    }

    override fun stopAllTickerUpdates() {
        activeSubscriptions.forEach { (symbol, job) ->
            job.cancel()
            Log.d(TAG,"Stopped ticker updates for $symbol")
        }
        activeSubscriptions.clear()
        Log.d(TAG,"Stopped all ticker updates")
    }
    
    /**
     * Parses a ticker WebSocket message into a TickerData object.
     * @param jsonString The JSON string from the WebSocket.
     * @param symbol The symbol this update is for.
     * @return A TickerData object with the parsed information.
     */
    private fun parseTickerData(jsonString: String, symbol: String): TickerData {
        try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            
            // Extract the relevant fields
            val price = jsonObject["c"]?.jsonPrimitive?.content ?: "0.0"
            val priceChangePercent = jsonObject["P"]?.jsonPrimitive?.content ?: "0.0"
            val volume = jsonObject["v"]?.jsonPrimitive?.content ?: "0.0"
            
            return TickerData(
                symbol = symbol,
                price = price,
                priceChangePercent = priceChangePercent,
                volume = volume,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG,"Error parsing ticker data: ${e.message}")
            throw e
        }
    }
}
