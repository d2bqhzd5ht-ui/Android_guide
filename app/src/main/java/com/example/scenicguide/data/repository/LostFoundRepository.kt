package com.example.scenicguide.data.repository

import com.example.scenicguide.core.network.ApiResult
import com.example.scenicguide.core.network.apiCall
import com.example.scenicguide.data.model.LostFoundDraft
import com.example.scenicguide.data.model.LostFoundItem
import com.example.scenicguide.data.model.LostFoundPage
import com.example.scenicguide.data.model.LostFoundStatusRequest
import com.example.scenicguide.data.remote.LostFoundApi

class LostFoundRepository(private val api: LostFoundApi) {

    suspend fun list(
        type: String? = null,
        status: String? = null,
        keyword: String? = null,
        page: Int = 1,
        size: Int = 20,
    ): ApiResult<LostFoundPage> = apiCall {
        api.list(type = type, category = null, status = status, keyword = keyword, page = page, size = size)
    }

    suspend fun detail(id: Int): ApiResult<LostFoundItem> = apiCall { api.detail(id) }

    suspend fun create(draft: LostFoundDraft): ApiResult<LostFoundItem> = apiCall { api.create(draft) }

    suspend fun updateStatus(id: Int, status: String): ApiResult<LostFoundItem> = apiCall {
        api.updateStatus(id, LostFoundStatusRequest(status))
    }
}
