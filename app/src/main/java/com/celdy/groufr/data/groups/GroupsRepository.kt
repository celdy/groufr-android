package com.celdy.groufr.data.groups

import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class GroupsRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun loadGroups(): List<GroupDto> {
        val response = apiService.sync()
        return response.updates?.groups.orEmpty()
    }
}
