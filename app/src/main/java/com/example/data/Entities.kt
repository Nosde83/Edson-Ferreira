package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val role: String = "Publicador", // "Publicador", "Pioneiro Auxiliar", "Pioneiro Regular"
    val monthlyGoalHours: Int = 15,
    val reportDueDay: Int = 28 // Default recommendation for JW reports is near end of month
)

@Entity(tableName = "service_reports")
data class ServiceReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val month: Int, // 1-12
    val day: Int,
    val hours: Double = 0.0,
    val magazines: Int = 0,
    val videos: Int = 0,
    val returnVisits: Int = 0,
    val notes: String = ""
)

@Entity(tableName = "daily_plans")
data class DailyPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val month: Int,
    val day: Int,
    val plannedActivity: String = "",
    val companion: String = "",
    val notes: String = "",
    val isCompleted: Boolean = false
)

@Entity(tableName = "magazine_placements")
data class MagazinePlacement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val personName: String = "",
    val address: String = "",
    val publications: String = "",
    val notes: String = ""
)

@Entity(tableName = "bible_students")
data class BibleStudent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val bookOrTopic: String = "Seja Feliz Para Sempre!",
    val currentLesson: String = "Lição 1",
    val studySchedule: String = "",
    val notes: String = "",
    val isActive: Boolean = true
)
