package com.example.pdvmaquineta.data.sync

import com.example.pdvmaquineta.data.sync.dto.ActivateRequest
import com.example.pdvmaquineta.data.sync.dto.ActivateResponse
import com.example.pdvmaquineta.data.sync.dto.PosTransactionInput
import com.example.pdvmaquineta.data.sync.dto.ConfigAckRequest
import com.example.pdvmaquineta.data.sync.dto.CustomerDto
import com.example.pdvmaquineta.data.sync.dto.OperatorDto
import com.example.pdvmaquineta.data.sync.dto.PosHeartbeatInput
import com.example.pdvmaquineta.data.sync.dto.PosConfigResponse
import com.example.pdvmaquineta.data.sync.dto.PosHeartbeatResult
import com.example.pdvmaquineta.data.sync.dto.PosTransactionResponse
import com.example.pdvmaquineta.data.sync.dto.ProductDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PosApiService {
    @POST("v1/pos/activate")
    suspend fun activate(@Body body: ActivateRequest): ActivateResponse

    @GET("v1/pos/catalog/products")
    suspend fun getProducts(@Query("since") since: String?): List<ProductDto>

    @GET("v1/pos/catalog/operators")
    suspend fun getOperators(@Query("since") since: String?): List<OperatorDto>

    @GET("v1/pos/catalog/customers")
    suspend fun getCustomers(@Query("since") since: String?): List<CustomerDto>

    @POST("v1/pos/transactions")
    suspend fun postTransaction(@Body body: PosTransactionInput): PosTransactionResponse

    @POST("v1/pos/heartbeat")
    suspend fun heartbeat(@Body body: PosHeartbeatInput): PosHeartbeatResult

    @GET("v1/pos/config")
    suspend fun getConfig(): PosConfigResponse

    @POST("v1/pos/config/ack")
    suspend fun ackConfig(@Body body: ConfigAckRequest): retrofit2.Response<Unit>
}
