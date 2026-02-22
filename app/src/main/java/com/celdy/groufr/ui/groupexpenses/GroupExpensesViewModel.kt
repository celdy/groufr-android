package com.celdy.groufr.ui.groupexpenses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.expenses.ExpensesRepository
import com.celdy.groufr.data.expenses.GroupBalancesResponse
import com.celdy.groufr.data.settlements.CreateSettlementRequest
import com.celdy.groufr.data.settlements.SettlementDto
import com.celdy.groufr.data.settlements.SettlementsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GroupExpensesState {
    data object Loading : GroupExpensesState()
    data class Content(
        val balances: GroupBalancesResponse?,
        val settlements: List<SettlementDto>
    ) : GroupExpensesState()
    data object Error : GroupExpensesState()
}

sealed class SettlementActionState {
    data object Idle : SettlementActionState()
    data object Sending : SettlementActionState()
    data object Sent : SettlementActionState()
    data class Error(val message: String) : SettlementActionState()
}

@HiltViewModel
class GroupExpensesViewModel @Inject constructor(
    private val expensesRepository: ExpensesRepository,
    private val settlementsRepository: SettlementsRepository
) : ViewModel() {

    private val _state = MutableLiveData<GroupExpensesState>(GroupExpensesState.Loading)
    val state: LiveData<GroupExpensesState> = _state

    private val _actionState = MutableLiveData<SettlementActionState>(SettlementActionState.Idle)
    val actionState: LiveData<SettlementActionState> = _actionState

    private var currentGroupId: Long = -1

    fun load(groupId: Long) {
        currentGroupId = groupId
        _state.value = GroupExpensesState.Loading
        viewModelScope.launch {
            try {
                val balances = try { expensesRepository.getGroupBalances(groupId) } catch (_: Exception) { null }
                val settlements = try { settlementsRepository.loadGroupSettlements(groupId) } catch (_: Exception) { emptyList() }
                _state.value = GroupExpensesState.Content(balances, settlements)
            } catch (exception: Exception) {
                _state.value = GroupExpensesState.Error
            }
        }
    }

    fun createSettlement(groupId: Long, recipientId: Long, amountCents: Long, currency: String, note: String?) {
        _actionState.value = SettlementActionState.Sending
        viewModelScope.launch {
            try {
                settlementsRepository.createSettlement(
                    groupId,
                    CreateSettlementRequest(recipientId, amountCents, currency, note)
                )
                _actionState.value = SettlementActionState.Sent
                load(groupId)
            } catch (exception: Exception) {
                _actionState.value = SettlementActionState.Error("Failed to create payment.")
            }
        }
    }

    fun confirmSettlement(settlementId: Long) {
        _actionState.value = SettlementActionState.Sending
        viewModelScope.launch {
            try {
                settlementsRepository.confirmSettlement(settlementId)
                _actionState.value = SettlementActionState.Sent
                load(currentGroupId)
            } catch (exception: Exception) {
                _actionState.value = SettlementActionState.Error("Failed to confirm payment.")
            }
        }
    }

    fun rejectSettlement(settlementId: Long) {
        _actionState.value = SettlementActionState.Sending
        viewModelScope.launch {
            try {
                settlementsRepository.rejectSettlement(settlementId, null)
                _actionState.value = SettlementActionState.Sent
                load(currentGroupId)
            } catch (exception: Exception) {
                _actionState.value = SettlementActionState.Error("Failed to reject payment.")
            }
        }
    }

    fun cancelSettlement(settlementId: Long) {
        _actionState.value = SettlementActionState.Sending
        viewModelScope.launch {
            try {
                settlementsRepository.cancelSettlement(settlementId)
                _actionState.value = SettlementActionState.Sent
                load(currentGroupId)
            } catch (exception: Exception) {
                _actionState.value = SettlementActionState.Error("Failed to cancel payment.")
            }
        }
    }
}
