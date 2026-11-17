package com.example.scenicguide.data.repository

import com.example.scenicguide.core.network.ApiResult
import com.example.scenicguide.core.network.apiCall
import com.example.scenicguide.data.model.PoiDetail
import com.example.scenicguide.data.model.PoiListItem
import com.example.scenicguide.data.remote.PoiApi

class PoiRepository(private val api: PoiApi) {

    suspend fun listPois(category: String? = null): ApiResult<List<PoiListItem>> =
        apiCall { api.listPois(category = category) }

    suspend fun getPoiDetail(id: Int): ApiResult<PoiDetail> =
        apiCall { api.getPoiDetail(id) }
}
