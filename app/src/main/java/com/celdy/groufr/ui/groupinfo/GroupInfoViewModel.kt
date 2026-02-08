package com.celdy.groufr.ui.groupinfo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.groups.GroupDetailDto
import com.celdy.groufr.data.groups.GroupMemberDto
import com.celdy.groufr.data.groups.GroupsRepository
import com.celdy.groufr.data.storage.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val groupsRepository: GroupsRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _state = MutableLiveData<GroupInfoState>(GroupInfoState.Loading)
    val state: LiveData<GroupInfoState> = _state

    private val _membersState = MutableLiveData<MembersState>(MembersState.Loading)
    val membersState: LiveData<MembersState> = _membersState

    private val _actionState = MutableLiveData<GroupActionState>(GroupActionState.Idle)
    val actionState: LiveData<GroupActionState> = _actionState

    private var members: List<GroupMemberDto> = emptyList()

    fun loadGroupInfo(slug: String?, groupId: Long) {
        _state.value = GroupInfoState.Loading
        _membersState.value = MembersState.Loading
        viewModelScope.launch {
            try {
                val membersDeferred = async { groupsRepository.getGroupMembers(groupId) }
                val detailDeferred = if (!slug.isNullOrBlank()) {
                    async { groupsRepository.getGroupDetail(slug) }
                } else null

                val membersResponse = membersDeferred.await()
                members = membersResponse.members
                _membersState.value = MembersState.Content(members)

                val detail = detailDeferred?.await()
                if (detail != null) {
                    _state.value = GroupInfoState.Content(detail, isSoleOwner(members))
                } else {
                    _state.value = GroupInfoState.NoDetail(isSoleOwner(members))
                }
            } catch (exception: Exception) {
                _state.value = GroupInfoState.Error
                _membersState.value = MembersState.Error
            }
        }
    }

    fun leaveGroup(groupId: Long) {
        _actionState.value = GroupActionState.Loading
        viewModelScope.launch {
            try {
                groupsRepository.leaveGroup(groupId)
                _actionState.value = GroupActionState.LeaveSuccess
            } catch (exception: Exception) {
                _actionState.value = GroupActionState.Error
            }
        }
    }

    fun deleteGroup(groupId: Long) {
        _actionState.value = GroupActionState.Loading
        viewModelScope.launch {
            try {
                groupsRepository.deleteGroup(groupId)
                _actionState.value = GroupActionState.DeleteSuccess
            } catch (exception: Exception) {
                _actionState.value = GroupActionState.Error
            }
        }
    }

    private fun isSoleOwner(members: List<GroupMemberDto>): Boolean {
        val userId = tokenStore.getUserId()
        val owners = members.filter { it.role == "owner" }
        return owners.size == 1 && owners[0].user.id == userId
    }
}

sealed class GroupInfoState {
    data object Loading : GroupInfoState()
    data class Content(val detail: GroupDetailDto, val isSoleOwner: Boolean) : GroupInfoState()
    data class NoDetail(val isSoleOwner: Boolean) : GroupInfoState()
    data object Error : GroupInfoState()
}

sealed class MembersState {
    data object Loading : MembersState()
    data class Content(val members: List<GroupMemberDto>) : MembersState()
    data object Error : MembersState()
}

sealed class GroupActionState {
    data object Idle : GroupActionState()
    data object Loading : GroupActionState()
    data object LeaveSuccess : GroupActionState()
    data object DeleteSuccess : GroupActionState()
    data object Error : GroupActionState()
}
