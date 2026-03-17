package com.finance.expense_copilot

import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ExpenseApi {
    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body request: AuthRequest): AuthResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): AuthResponse

    // Users
    @GET("api/users/{email}")
    suspend fun getUserSettings(@Path("email") email: String): User

    @PUT("api/users/{email}/settings")
    suspend fun updateUserSettings(@Path("email") email: String, @Body settings: User): User

    // Expenses
    @GET("api/expenses/all")
    suspend fun getAllExpenses(@Header("userEmail") email: String): List<ExpenseResponse>

    @GET("api/expenses/user/balance/{email}")
    suspend fun getUserBalance(@Path("email") email: String): Double

    @GET("api/expenses/user/{email}/daily-aggregated")
    suspend fun getDailyAggregated(@Path("email") email: String): Map<String, Double>

    @POST("api/expenses/add")
    suspend fun addExpense(@Header("userEmail") email: String, @Body expense: ExpenseRequest): ExpenseResponse

    @PUT("api/expenses/{id}")
    suspend fun updateExpense(@Header("userEmail") email: String, @Path("id") id: String, @Body expense: ExpenseRequest): ExpenseResponse

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Header("userEmail") email: String, @Path("id") id: String): retrofit2.Response<Unit>
    
    @DELETE("api/expenses/reset")
    suspend fun resetAllExpenses(@Header("userEmail") email: String): retrofit2.Response<Unit>

    @Multipart
    @POST("api/expenses/scan-bill")
    suspend fun scanBillOCR(@Part file: MultipartBody.Part): ExpenseResponse

    // Goals (Updated for Fintastics-Pro)
    @GET("api/goals/all")
    suspend fun getAllGoals(@Header("userEmail") email: String): List<GoalResponse>

    @POST("api/goals/add")
    suspend fun addGoal(@Header("userEmail") email: String, @Body goal: GoalRequest): GoalResponse

    @PUT("api/goals/{id}/contribute")
    suspend fun contributeToGoal(@Header("userEmail") email: String, @Path("id") id: String, @Query("amount") amount: Double): GoalResponse
    
    @GET("api/goals/suggest-cuts")
    suspend fun suggestCuts(@Header("userEmail") email: String, @Query("targetSavings") targetSavings: Double): SuggestCutsResponse

    // Budget Engine
    @GET("api/budget/status")
    suspend fun getBudgetStatus(@Header("userEmail") email: String, @Query("totalBudget") totalBudget: Double): BudgetStatusResponse

    // Reminders
    @GET("api/reminders/all")
    suspend fun getUpcomingReminders(@Header("userEmail") email: String): List<ReminderResponse>
    
    // Vouchers
    @GET("api/vouchers/all")
    suspend fun getAllVouchers(@Header("userEmail") email: String): List<Voucher>
    
    @POST("api/vouchers/add")
    suspend fun addVoucher(@Header("userEmail") email: String, @Body voucher: Voucher): Voucher
    
    // Split Expense
    @GET("api/split/all")
    suspend fun getAllSplitExpenses(@Header("userEmail") email: String): List<SplitExpense>
    
    @POST("api/split/add")
    suspend fun addSplitExpense(@Header("userEmail") email: String, @Body splitExpense: SplitExpense): SplitExpense
    
    @PUT("api/split/{id}/settle/{person}")
    suspend fun settleSplitPerson(@Header("userEmail") email: String, @Path("id") id: String, @Path("person") person: String): SplitExpense
}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"
    val api: ExpenseApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExpenseApi::class.java)
    }
}