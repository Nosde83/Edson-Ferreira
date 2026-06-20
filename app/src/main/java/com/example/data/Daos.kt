package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)
}

@Dao
interface ServiceReportDao {
    @Query("SELECT * FROM service_reports ORDER BY year DESC, month DESC, day DESC")
    fun getAllReports(): Flow<List<ServiceReport>>

    @Query("SELECT * FROM service_reports WHERE year = :year AND month = :month ORDER BY day DESC")
    fun getReportsByMonth(year: Int, month: Int): Flow<List<ServiceReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ServiceReport)

    @Delete
    suspend fun deleteReport(report: ServiceReport)

    @Query("DELETE FROM service_reports WHERE id = :id")
    suspend fun deleteReportById(id: Int)
}

@Dao
interface DailyPlanDao {
    @Query("SELECT * FROM daily_plans ORDER BY year DESC, month DESC, day DESC")
    fun getAllPlans(): Flow<List<DailyPlan>>

    @Query("SELECT * FROM daily_plans WHERE year = :year AND month = :month AND day = :day")
    fun getPlansByDate(year: Int, month: Int, day: Int): Flow<List<DailyPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: DailyPlan)

    @Delete
    suspend fun deletePlan(plan: DailyPlan)
}

@Dao
interface MagazinePlacementDao {
    @Query("SELECT * FROM magazine_placements ORDER BY date DESC")
    fun getAllPlacements(): Flow<List<MagazinePlacement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlacement(placement: MagazinePlacement)

    @Delete
    suspend fun deletePlacement(placement: MagazinePlacement)
}

@Dao
interface BibleStudentDao {
    @Query("SELECT * FROM bible_students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<BibleStudent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: BibleStudent)

    @Delete
    suspend fun deleteStudent(student: BibleStudent)
}
