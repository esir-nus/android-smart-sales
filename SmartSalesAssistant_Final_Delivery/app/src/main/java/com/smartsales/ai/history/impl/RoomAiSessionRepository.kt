package com.smartsales.ai.history.impl

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartsales.ai.data.local.AiSessionDao
import com.smartsales.ai.data.local.AiSessionEntity
import com.smartsales.ai.data.local.StoredAiMessage
import com.smartsales.ai.data.local.toDomain
import com.smartsales.ai.data.local.toStored
import com.smartsales.ai.history.AiSession
import com.smartsales.ai.history.AiSessionRepository
import com.smartsales.ai.history.AiSessionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAiSessionRepository
    @Inject
    constructor(
        private val dao: AiSessionDao,
        private val gson: Gson,
    ) : AiSessionRepository {
        private val typeToken = object : TypeToken<List<StoredAiMessage>>() {}.type

        override fun observeSummaries(): Flow<List<AiSessionSummary>> {
            return dao.observeSessions()
                .map { entities ->
                    entities.map { entity ->
                        AiSessionSummary(
                            id = entity.sessionId,
                            title = entity.title,
                            timestampMillis = entity.timestampMillis,
                            subtitle = entity.subtitle,
                        )
                    }
                }
        }

        override suspend fun readSession(sessionId: String): AiSession? {
            val entity = dao.findById(sessionId) ?: return null
            val messages: List<StoredAiMessage> = gson.fromJson(entity.messagesJson, typeToken)
            return AiSession(
                summary =
                    AiSessionSummary(
                        id = entity.sessionId,
                        title = entity.title,
                        timestampMillis = entity.timestampMillis,
                        subtitle = entity.subtitle,
                    ),
                messages = messages.map { it.toDomain() },
            )
        }

        override suspend fun upsertSession(session: AiSession) {
            val storedMessages = session.messages.map { it.toStored() }
            val entity =
                AiSessionEntity(
                    sessionId = session.summary.id,
                    title = session.summary.title,
                    subtitle = session.summary.subtitle,
                    timestampMillis = session.summary.timestampMillis,
                    messagesJson = gson.toJson(storedMessages, typeToken),
                )
            dao.upsert(entity)
        }

        override suspend fun deleteSession(sessionId: String) {
            dao.delete(sessionId)
        }
    }
