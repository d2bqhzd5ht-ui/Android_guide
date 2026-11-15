package com.example.scenicguide.data.repository

import com.example.scenicguide.core.network.ApiResult
import com.example.scenicguide.core.network.apiCall
import com.example.scenicguide.data.model.HomeContent
import com.example.scenicguide.data.remote.HomeApi

class HomeRepository(private val api: HomeApi) {

    suspend fun loadHome(): ApiResult<HomeContent> =
        apiCall { api.home() }
}
