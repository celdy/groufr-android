package com.celdy.groufr.ui.expensecreate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.events.EventsRepository
import com.celdy.groufr.data.expenses.CreateExpenseRequest
import com.celdy.groufr.data.expenses.ExpenseDetailDto
import com.celdy.groufr.data.expenses.ExpenseSplitInput
import com.celdy.groufr.data.expenses.ExpensesRepository
import com.celdy.groufr.data.expenses.UpdateExpenseRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParticipantItem(
    val id: Long,
    val name: String,
    var selected: Boolean = true
)

sealed class ExpenseFormState {
    data object Idle : ExpenseFormState()
    data object Submitting : ExpenseFormState()
    data object Success : ExpenseFormState()
    data class Error(val message: String) : ExpenseFormState()
}

@HiltViewModel
class ExpenseCreateViewModel @Inject constructor(
    private val expensesRepository: ExpensesRepository,
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _participants = MutableLiveData<List<ParticipantItem>>()
    val participants: LiveData<List<ParticipantItem>> = _participants

    private val _state = MutableLiveData<ExpenseFormState>(ExpenseFormState.Idle)
    val state: LiveData<ExpenseFormState> = _state

    private val _editData = MutableLiveData<ExpenseDetailDto?>()
    val editData: LiveData<ExpenseDetailDto?> = _editData

    var currentSplitType: String = "equal"
        private set

    var currentDueType: String = "immediately"
        private set

    fun setSplitType(type: String) {
        currentSplitType = type
    }

    fun setDueType(type: String) {
        currentDueType = type
    }

    fun loadParticipants(eventId: Long) {
        viewModelScope.launch {
            try {
                val event = eventsRepository.loadEventDetail(eventId)
                val items = event.participantsList
                    .filter { it.status in setOf("joined", "maybe") }
                    .map { ParticipantItem(id = it.user.id, name = it.user.name) }
                _participants.value = items
            } catch (_: Exception) {
                _participants.value = emptyList()
            }
        }
    }

    fun toggleParticipant(userId: Long, selected: Boolean) {
        _participants.value = _participants.value?.map {
            if (it.id == userId) it.copy(selected = selected) else it
        }
    }

    fun loadExpenseForEdit(expenseId: Long) {
        viewModelScope.launch {
            try {
                val detail = expensesRepository.getExpenseDetail(expenseId)
                _editData.value = detail
            } catch (_: Exception) {
                _editData.value = null
            }
        }
    }

    fun createExpense(
        eventId: Long,
        label: String,
        amountCents: Long,
        currency: String,
        participantValues: List<Pair<Long, Double?>>,
        note: String?,
        dueDate: String?
    ) {
        _state.value = ExpenseFormState.Submitting
        viewModelScope.launch {
            try {
                val splits = participantValues.map { (userId, value) ->
                    ExpenseSplitInput(userId = userId, value = value)
                }
                val request = CreateExpenseRequest(
                    label = label,
                    amountCents = amountCents,
                    currency = currency,
                    payerId = null,
                    splitType = currentSplitType,
                    splits = splits,
                    dueType = currentDueType,
                    dueDate = dueDate,
                    note = note
                )
                expensesRepository.createExpense(eventId, request)
                _state.value = ExpenseFormState.Success
            } catch (exception: Exception) {
                _state.value = ExpenseFormState.Error(exception.message ?: "Failed to create expense.")
            }
        }
    }

    fun updateExpense(
        expenseId: Long,
        label: String,
        amountCents: Long,
        currency: String,
        participantValues: List<Pair<Long, Double?>>,
        note: String?,
        dueDate: String?
    ) {
        _state.value = ExpenseFormState.Submitting
        viewModelScope.launch {
            try {
                val splits = participantValues.map { (userId, value) ->
                    ExpenseSplitInput(userId = userId, value = value)
                }
                val request = UpdateExpenseRequest(
                    label = label,
                    amountCents = amountCents,
                    currency = currency,
                    splitType = currentSplitType,
                    splits = splits,
                    dueType = currentDueType,
                    dueDate = dueDate,
                    note = note
                )
                expensesRepository.updateExpense(expenseId, request)
                _state.value = ExpenseFormState.Success
            } catch (exception: Exception) {
                _state.value = ExpenseFormState.Error(exception.message ?: "Failed to update expense.")
            }
        }
    }
}
