package com.finance.expense_copilot

// AUTH MODELS
data class AuthRequest(val email: String)
data class OtpVerifyRequest(val email: String, val otp: String)
data class AuthResponse(val success: Boolean, val message: String, val token: String? = null)

// TRANSACTION MODELS
data class ExpenseRequest(
    val description: String,
    val amount: Double,
    val type: String,      // "INCOME" or "EXPENSE"
    val date: String,      // "yyyy-MM-dd"
    val category: String,
    val accountType: String? = null // "Cash" or "Bank"
)

data class ExpenseResponse(
    val id: String? = null,
    val description: String? = "",
    val amount: Double? = 0.0,
    val category: String? = "Others",
    val type: String? = "EXPENSE",
    val date: String? = "",
    val accountType: String? = null
)

// GOAL MODELS
data class GoalResponse(
    val id: String? = null,
    val goalName: String? = null,
    val targetAmount: Double? = null,
    val currentSaved: Double? = null,
    val deadline: String? = null
)

data class GoalRequest(
    val goalName: String,
    val targetAmount: Double,
    val deadline: String
)

data class SuggestCutsResponse(
    val targetToSave: Double?,
    val totalFlexibleSpend: Double?,
    val message: String?,
    val status: String?,
    val action: String?,
    val cuts: Map<String, Double>?
)

// REMINDER MODELS
data class ReminderResponse(
    val id: String,
    val title: String,
    val amount: Double,
    val dueDate: String,
    val type: String
)

// VOUCHER MODELS
data class Voucher(
    val id: String? = null,
    val platform: String,
    val code: String,
    val value: Double,
    val expiryDate: String
)

// SPLIT EXPENSE MODELS
data class SplitDetail(
    val person: String,
    val amountOwed: Double,
    val settled: Boolean = false
)

data class SplitExpense(
    val id: String? = null,
    val description: String,
    val totalAmount: Double,
    val paidBy: String,
    val splitDetails: List<SplitDetail>
)

// BUDGET ENGINE MODEL
data class BudgetStatusResponse(
    val totalBudget: Double,
    val totalSpent: Double,
    val remaining: Double,
    val exceeds80Percent: Boolean
)

// USER SETTINGS MODEL
data class User(
    val email: String,
    val name: String? = null,
    var notificationsEnabled: Boolean = true,
    var dailyLimit: Double = 500.0,
    var appLockEnabled: Boolean = false,
    var smsTrackingEnabled: Boolean = false,
    var cashBalance: Double = 0.0,
    var bankBalance: Double = 0.0,
    var categoryLimits: Map<String, Double> = emptyMap()
)