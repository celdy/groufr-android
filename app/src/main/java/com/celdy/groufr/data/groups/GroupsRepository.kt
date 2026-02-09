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
            val groups = apiService.getGroups().groups
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

    suspend fun getGroupDetail(slug: String): GroupDetailDto {
        return apiService.getGroupDetail(slug)
    }

    suspend fun getGroupMembers(groupId: Long): GroupMembersResponse {
        return apiService.getGroupMembers(groupId)
    }

    suspend fun leaveGroup(groupId: Long): GroupActionResponse {
        return apiService.leaveGroup(groupId)
    }

    suspend fun deleteGroup(groupId: Long): GroupActionResponse {
        return apiService.deleteGroup(groupId)
    }
}
