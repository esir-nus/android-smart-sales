package com.smartsales.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Transcription Request Models for Qwen Tingwu
 */
data class TranscriptionRequest(
    val input: TranscriptionInput,
    val parameters: TranscriptionParameters
)

data class TranscriptionInput(
    @SerializedName("source_language")
    val sourceLanguage: String,    // "zh" | "en"
    @SerializedName("task_key")
    val taskKey: String,           // Audio file URL or OSS path
    val format: String = "mp3"     // "mp3" | "wav" | "m4a"
)

data class TranscriptionParameters(
    val transcription: TranscriptionConfig,
    val translation: TranslationConfig? = null,
    val summarization: SummarizationConfig? = null,
    val diarization: DiarizationConfig? = null
)

data class TranscriptionConfig(
    @SerializedName("output_level")
    val outputLevel: String = "sentence",  // "word" | "sentence"
    @SerializedName("enable_punctuation")
    val enablePunctuation: Boolean = true,
    @SerializedName("enable_inverse_text_normalization")
    val enableInverseTextNormalization: Boolean = true
)

data class TranslationConfig(
    @SerializedName("target_language")
    val targetLanguage: String = "en"
)

data class SummarizationConfig(
    val types: List<String> = listOf("paragraph", "conversational")
)

data class DiarizationConfig(
    val enable: Boolean = true,
    @SerializedName("speaker_count")
    val speakerCount: Int? = null  // null = auto-detect
)

/**
 * Transcription Response Models
 */
data class TaskResponse(
    @SerializedName("task_id")
    val taskId: String,
    val status: String,
    @SerializedName("request_id")
    val requestId: String
)

data class TaskStatusResponse(
    @SerializedName("task_id")
    val taskId: String,
    val status: String,  // "QUEUING" | "RUNNING" | "SUCCEEDED" | "FAILED"
    val progress: Int,   // 0-100
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("request_id")
    val requestId: String
)

data class TranscriptionResult(
    @SerializedName("task_id")
    val taskId: String,
    val status: String,
    val transcription: TranscriptionData,
    val diarization: DiarizationData?,
    val summarization: SummarizationData?,
    @SerializedName("request_id")
    val requestId: String
)

data class TranscriptionData(
    val sentences: List<Sentence>
)

data class Sentence(
    val text: String,
    @SerializedName("begin_time")
    val beginTime: Long,      // milliseconds
    @SerializedName("end_time")
    val endTime: Long,        // milliseconds
    @SerializedName("speaker_id")
    val speakerId: String?,   // Speaker ID
    val words: List<Word>?
)

data class Word(
    val text: String,
    @SerializedName("begin_time")
    val beginTime: Long,
    @SerializedName("end_time")
    val endTime: Long,
    val confidence: Float     // 0.0-1.0
)

data class DiarizationData(
    val speakers: List<Speaker>
)

data class Speaker(
    @SerializedName("speaker_id")
    val speakerId: String,
    val duration: Long,       // Total speaking time (milliseconds)
    val segments: List<Segment>
)

data class Segment(
    @SerializedName("begin_time")
    val beginTime: Long,
    @SerializedName("end_time")
    val endTime: Long,
    val text: String
)

data class SummarizationData(
    val paragraph: String?,
    val conversational: String?
)
