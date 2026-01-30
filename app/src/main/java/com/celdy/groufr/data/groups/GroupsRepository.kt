package com.celdy.groufr.data.groups

import com.celdy.groufr.data.local.GroupDao
import com.celdy.groufr.data.local.toDto
import com.celdy.groufr.data.local.toEntity
import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class GroupsRepository @Inject constructor(
    private val apiService: ApiService,
    private val groupDao: GroupDao
) {
    suspend fun loadGroups(): List<GroupDto> {
        return try {
            val response = apiService.sync()
            val groups = response.updates?.groups.orEmpty()
            groupDao.replaceAll(groups.map { it.toEntity() })
            groupDao.getAll().map { it.toDto() }
        } catch (exception: Exception) {
            val cached = groupDao.getAll()
            if (cached.isNotEmpty()) {
                cached.map { it.toDto() }
            } else {
                throw exception
            }
        }
    }
}
