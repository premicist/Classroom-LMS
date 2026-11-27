package com.aistudio.classroomlms.fake

import com.aistudio.classroomlms.data.dao.AcademicEventDao
import com.aistudio.classroomlms.data.entity.AcademicEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeAcademicEventDao : AcademicEventDao {
    private val eventsFlow = MutableStateFlow<List<AcademicEventEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllEvents(): Flow<List<AcademicEventEntity>> = eventsFlow

    override fun getAllEventsOnce(): List<AcademicEventEntity> = eventsFlow.value

    override suspend fun insertEvent(event: AcademicEventEntity): Long {
        val inserted = event.copy(id = if (event.id == 0L) nextId++ else event.id)
        eventsFlow.update { it + inserted }
        return inserted.id
    }

    override fun insertEventsSync(events: List<AcademicEventEntity>) {
        eventsFlow.update { it + events }
    }

    override suspend fun updateEvent(event: AcademicEventEntity) {
        eventsFlow.update { list -> list.map { if (it.id == event.id) event else it } }
    }

    override suspend fun deleteEvent(event: AcademicEventEntity) {
        eventsFlow.update { list -> list.filter { it.id != event.id } }
    }
}
