package com.example.data

import kotlinx.coroutines.flow.Flow

class MinistryRepository(private val db: AppDatabase) {

    private val userProfileDao = db.userProfileDao()
    private val serviceReportDao = db.serviceReportDao()
    private val dailyPlanDao = db.dailyPlanDao()
    private val magazinePlacementDao = db.magazinePlacementDao()
    private val bibleStudentDao = db.bibleStudentDao()

    // Profile
    val userProfile: Flow<UserProfile?> = userProfileDao.getProfile()
    suspend fun saveProfile(profile: UserProfile) {
        userProfileDao.insertProfile(profile)
    }

    // Reports
    val allReports: Flow<List<ServiceReport>> = serviceReportDao.getAllReports()
    fun getReportsForMonth(year: Int, month: Int): Flow<List<ServiceReport>> {
        return serviceReportDao.getReportsByMonth(year, month)
    }
    suspend fun insertReport(report: ServiceReport) {
        serviceReportDao.insertReport(report)
    }
    suspend fun deleteReportById(id: Int) {
        serviceReportDao.deleteReportById(id)
    }

    // Daily Plans
    val allPlans: Flow<List<DailyPlan>> = dailyPlanDao.getAllPlans()
    fun getPlansForDate(year: Int, month: Int, day: Int): Flow<List<DailyPlan>> {
        return dailyPlanDao.getPlansByDate(year, month, day)
    }
    suspend fun insertPlan(plan: DailyPlan) {
        dailyPlanDao.insertPlan(plan)
    }
    suspend fun deletePlan(plan: DailyPlan) {
        dailyPlanDao.deletePlan(plan)
    }

    // Placements
    val allPlacements: Flow<List<MagazinePlacement>> = magazinePlacementDao.getAllPlacements()
    suspend fun insertPlacement(placement: MagazinePlacement) {
        magazinePlacementDao.insertPlacement(placement)
    }
    suspend fun deletePlacement(placement: MagazinePlacement) {
        magazinePlacementDao.deletePlacement(placement)
    }

    // Students
    val allStudents: Flow<List<BibleStudent>> = bibleStudentDao.getAllStudents()
    suspend fun insertStudent(student: BibleStudent) {
        bibleStudentDao.insertStudent(student)
    }
    suspend fun deleteStudent(student: BibleStudent) {
        bibleStudentDao.deleteStudent(student)
    }
}
