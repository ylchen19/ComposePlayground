package com.example.composeplayground.data.repository

import com.example.composeplayground.data.model.PicsumPhoto
import com.example.composeplayground.data.model.PicsumPhotoDto
import com.example.composeplayground.data.model.toDomain
import com.example.composeplayground.network.NetworkResult
import com.example.composeplayground.network.api.ApiService
import com.example.composeplayground.network.api.get

/** 回傳 domain model；錯誤以例外丟出，由 PagingSource 包成 LoadResult.Error。 */
interface PicsumRepository {
    suspend fun fetchPhotos(page: Int, limit: Int): List<PicsumPhoto>
}

class PicsumRepositoryImpl(
    private val api: ApiService,
) : PicsumRepository {

    override suspend fun fetchPhotos(page: Int, limit: Int): List<PicsumPhoto> {
        val result = api.get<List<PicsumPhotoDto>>(
            endpoint = "v2/list",
            queryParams = mapOf("page" to page.toString(), "limit" to limit.toString()),
        )
        return when (result) {
            is NetworkResult.Success -> result.data.map { it.toDomain() }
            is NetworkResult.Error -> throw result.throwable
                ?: RuntimeException(result.message ?: "Picsum request failed")
            NetworkResult.Loading -> emptyList()
        }
    }
}
