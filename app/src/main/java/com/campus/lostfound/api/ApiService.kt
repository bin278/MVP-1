package com.campus.lostfound.api

import com.campus.lostfound.model.MatchRequest
import com.campus.lostfound.model.MatchResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * AI 智能匹配 API 接口
 * 调用后端 Flask 服务的 /api/chat/match 端点
 */
interface ApiService {
    @POST("api/chat/match")
    fun matchItems(@Body request: MatchRequest): Call<MatchResponse>
}
