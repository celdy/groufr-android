package com.celdy.groufr.data.settlements

import com.celdy.groufr.data.local.SettlementDao
import com.celdy.groufr.data.local.toDto
import com.celdy.groufr.data.local.toEntity
import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class SettlementsRepository @Inject constructor(
    private val apiService: ApiService,
    private val settlementDao: SettlementDao
) {
    suspend fun loadGroupSettlements(groupId: Long): List<SettlementDto> {
        return try {
            val response = apiService.getGroupSettlements(groupId)
            settlementDao.upsertAll(response.settlements.map { it.toEntity() })
            response.settlements
        } catch (exception: Exception) {
            val cached = settlementDao.getByGroup(groupId)
            if (cached.isNotEmpty()) {
                cached.map { it.toDto() }
            } else {
                throw exception
            }
        }
    }

    suspend fun createSettlement(groupId: Long, request: CreateSettlementRequest): SettlementDto {
        val settlement = apiService.createSettlement(groupId, request)
        settlementDao.upsert(settlement.toEntity())
        return settlement
    }

    suspend fun confirmSettlement(settlementId: Long): SettlementDto {
        val settlement = apiService.confirmSettlement(settlementId)
        settlementDao.upsert(settlement.toEntity())
        return settlement
    }

    suspend fun rejectSettlement(settlementId: Long, reason: String?): SettlementDto {
        val settlement = apiService.rejectSettlement(settlementId, RejectSettlementRequest(reason))
        settlementDao.upsert(settlement.toEntity())
        return settlement
    }

    suspend fun cancelSettlement(settlementId: Long): SettlementDto {
        val settlement = apiService.cancelSettlement(settlementId)
        settlementDao.upsert(settlement.toEntity())
        return settlement
    }
}
