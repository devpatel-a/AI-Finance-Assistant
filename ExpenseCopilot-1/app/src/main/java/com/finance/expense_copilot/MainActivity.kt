package com.finance.expense_copilot

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.finance.expense_copilot.ui.theme.ExpenseCopilotTheme
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

// 1. THIS MUST BE AT THE TOP LEVEL (Outside the class)
enum class AppState { LOGIN, OTP_VERIFY, DASHBOARD }

// Extends FragmentActivity for BiometricPrompt compatibility
class MainActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseCopilotTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ExpenseApp(this)
                }
            }
        }
    }
}

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseApp(activity: FragmentActivity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) Toast.makeText(context, "SMS reading initialized", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECEIVE_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(android.Manifest.permission.RECEIVE_SMS)
        }
    }

    var currentScreen by remember { mutableStateOf<AppState>(AppState.LOGIN) }
    var expenseList by remember { mutableStateOf<List<ExpenseResponse>>(listOf()) }
    var savingsGoal by remember { mutableStateOf(50000.0) } 
    var savedAmount by remember { mutableStateOf(0.0) }
    var searchQuery by remember { mutableStateOf("") }

    var email by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var amtTxt by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") }
    // DEFAULT to 1st mandated category
    var selectedCategory by remember { mutableStateOf("Food") }

    var budgetStatus by remember { mutableStateOf<BudgetStatusResponse?>(null) }
    var remindersList by remember { mutableStateOf<List<ReminderResponse>>(listOf()) }
    var aiSuggestions by remember { mutableStateOf<SuggestCutsResponse?>(null) }
    var userSettings by remember { mutableStateOf<User?>(null) }
    var dailyAgg by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var goalsList by remember { mutableStateOf<List<GoalResponse>>(emptyList()) }
    
    // CRUD States
    var editingExpenseId by remember { mutableStateOf<String?>(null) }
    var expenseToManage by remember { mutableStateOf<ExpenseResponse?>(null) }
    // SharedPreferences for isolating User Email globally
    val sharedPrefs = context.getSharedPreferences("ExpenseAppPrefs", Context.MODE_PRIVATE)

    fun refresh() {
        // Read from shared prefs whenever refreshing data
        val currentUserEmail = sharedPrefs.getString("userEmail", email) ?: email
        if (currentUserEmail.isBlank()) return // Safety check
        
        scope.launch {
            try {
                expenseList = RetrofitClient.api.getAllExpenses(currentUserEmail).filter { it.date != null && it.type != null }
                budgetStatus = RetrofitClient.api.getBudgetStatus(currentUserEmail, savingsGoal) // Example using goal as total budget
                remindersList = RetrofitClient.api.getUpcomingReminders(currentUserEmail)
                userSettings = RetrofitClient.api.getUserSettings(currentUserEmail)
                dailyAgg = RetrofitClient.api.getDailyAggregated(currentUserEmail)
                goalsList = RetrofitClient.api.getAllGoals(currentUserEmail)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    when (currentScreen) {
        AppState.LOGIN -> LoginScreen(
            email = email,
            onEmailChange = { email = it },
            onSendOtp = {
                if (email.isNotBlank()) {
                    scope.launch {
                        try {
                            val res = RetrofitClient.api.sendOtp(AuthRequest(email))
                            Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                            currentScreen = AppState.OTP_VERIFY
                        } catch (e: Exception) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                    }
                } else {
                    Toast.makeText(context, "Enter email", Toast.LENGTH_SHORT).show()
                }
            },
            onBiometricAuth = {
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        refresh()
                        currentScreen = AppState.DASHBOARD
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(context, "Biometric Error: $errString", Toast.LENGTH_SHORT).show()
                    }
                })
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric login for Expense Copilot")
                    .setSubtitle("Log in using your biometric or device PIN/pattern")
                    .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                try {
                    biometricPrompt.authenticate(promptInfo)
                } catch(e: Exception) {
                    Toast.makeText(context, "Biometrics not supported on this device.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        AppState.OTP_VERIFY -> OtpScreen(otpInput, { otpInput = it }) {
            scope.launch {
                try {
                    val res = RetrofitClient.api.verifyOtp(OtpVerifyRequest(email, otpInput))
                    if (res.success) { 
                        // SAVE USER EMAIL ON SUCCESSFUL LOGIN so SMS and Dashboard can use it
                        sharedPrefs.edit().putString("userEmail", email).apply()
                        refresh() 
                        currentScreen = AppState.DASHBOARD 
                    }
                } catch (e: Exception) { Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show() }
            }
        }
        AppState.DASHBOARD -> {
            val currentUserEmail = sharedPrefs.getString("userEmail", email) ?: email
            var selectedTab by remember { mutableStateOf(0) }
            var selectedDateFilter by remember { mutableStateOf<String?>(null) }
            var timeFilter by remember { mutableStateOf("All Time") } // "Week", "Month", "Year", "All Time"

            @SuppressLint("NewApi")
            val filteredByTimeList = expenseList.filter {
                if (it.date == null) false
                else {
                    val dateObj = try { LocalDate.parse(it.date) } catch(e: Exception) { null }
                    if (dateObj == null) true
                    else {
                        val now = LocalDate.now()
                        when (timeFilter) {
                            "Week" -> dateObj.isAfter(now.minusDays(7)) || dateObj.isEqual(now.minusDays(7))
                            "Month" -> dateObj.month == now.month && dateObj.year == now.year
                            "Year" -> dateObj.year == now.year
                            else -> true
                        }
                    }
                }
            }

            val expense = filteredByTimeList.filter { it.type?.uppercase() == "EXPENSE" }.sumOf { it.amount ?: 0.0 }
            val filteredHistory = filteredByTimeList.filter {
                (selectedDateFilter == null || it.date == selectedDateFilter) &&
                (searchQuery.isEmpty() || it.description?.contains(searchQuery, ignoreCase = true) == true ||
                 it.category?.contains(searchQuery, ignoreCase = true) == true)
            }.reversed()

            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Records", Modifier.padding(12.dp)) }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Analysis", Modifier.padding(12.dp)) }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text("Budgets", Modifier.padding(12.dp)) }
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) { Text("Accounts", Modifier.padding(12.dp)) }
                    Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }) { Text("Goals", Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                }

                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    // TAB 0: Records (History)
                    if (selectedTab == 0) {
                        val groupedTransactions = filteredHistory.filter { it.date != null }.groupBy { it.date!! }
                        
                        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Records", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                }
                                Text("Transaction History", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                Spacer(Modifier.height(16.dp))
                                
                                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), placeholder = { Text("Search by category or description...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = CircleShape)
                            }
                            
                            if (groupedTransactions.isEmpty()) {
                                item { 
                                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("No records found.", color = Color.Gray)
                                    }
                                }
                            } else {
                                groupedTransactions.forEach { (dateStr, transList) ->
                                    item {
                                        val dayIncome = transList.filter { it.type == "INCOME" }.sumOf { it.amount ?: 0.0 }
                                        val dayExpense = transList.filter { it.type == "EXPENSE" }.sumOf { it.amount ?: 0.0 }
                                        
                                        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                            Text(dateStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Row {
                                                if (dayIncome > 0) Text("+$dayIncome ", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelMedium)
                                                if (dayExpense > 0) Text("-$dayExpense", color = Color(0xFFE53935), style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    items(transList) { TransactionItem(it) { expenseToManage = it } }
                                }
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                        
                        // FAB to trigger the Add Transaction Bottom Sheet / Dialog could go here, 
                        // but for now we'll keep the inline form simple for MyMoney parity.
                        // For Fintastics-Pro, Let's place the TransactionForm as a sticky bottom sheet or inline at the top of Records.
                        
                        // We will inline it here at the bottom of the column for immediate access
                        Box(contentAlignment = Alignment.BottomCenter) {
                             Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Add Record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    var selectedGoalId by remember { mutableStateOf<String?>(null) }
                                    var transactionDate by remember { mutableStateOf(LocalDate.now().toString()) }
                                    var transactionAccount by remember { mutableStateOf("Bank") }
                                    
                                    TransactionForm(selectedType, { selectedType = it }, selectedCategory, { selectedCategory = it }, desc, { desc = it }, amtTxt, { amtTxt = it }, transactionDate, { transactionDate = it }, transactionAccount, { transactionAccount = it },
                                        goalsList = goalsList,
                                        selectedGoalId = selectedGoalId,
                                        onGoalSelected = { selectedGoalId = it },
                                        isEditMode = editingExpenseId != null,
                                        onAdd = {
                                            val a = amtTxt.toDoubleOrNull()
                                            if (desc.isNotBlank() && a != null) {
                                                scope.launch {
                                                    if (editingExpenseId != null) {
                                                        RetrofitClient.api.updateExpense(currentUserEmail, editingExpenseId!!, ExpenseRequest(desc, a, selectedType, transactionDate, selectedCategory, transactionAccount))
                                                        editingExpenseId = null
                                                    } else {
                                                        RetrofitClient.api.addExpense(currentUserEmail, ExpenseRequest(desc, a, selectedType, transactionDate, selectedCategory, transactionAccount))
                                                        // Allocation Logic (If Income or GOAL_DEPOSIT and a Goal is selected)
                                                        if ((selectedType == "INCOME" || selectedType == "GOAL_DEPOSIT") && selectedGoalId != null) {
                                                            try {
                                                                RetrofitClient.api.contributeToGoal(currentUserEmail, selectedGoalId!!, a)
                                                                Toast.makeText(context, "₹${a.toInt()} allocated to Goal!", Toast.LENGTH_SHORT).show()
                                                            } catch (e: Exception) { e.printStackTrace() }
                                                        }
                                                    }
                                                    desc = ""; amtTxt = ""; selectedGoalId = null; transactionDate = LocalDate.now().toString(); transactionAccount = "Bank"; refresh()
                                                }
                                            }
                                        },
                                        onScanBill = {
                                            scope.launch {
                                                try {
                                                    val mockFile = "mock_receipt_data".toRequestBody("text/plain".toMediaTypeOrNull())
                                                    val body = MultipartBody.Part.createFormData("file", "receipt.jpg", mockFile)
                                                    val scanResult = RetrofitClient.api.scanBillOCR(body)
                                                    desc = scanResult.description ?: ""
                                                    amtTxt = scanResult.amount?.toString() ?: ""
                                                    selectedCategory = scanResult.category ?: "Food"
                                                    Toast.makeText(context, "Bill Scanned!", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) { Toast.makeText(context, "OCR Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Edit/Delete Dialog remains for recent transactions
                        if (expenseToManage != null) {
                            AlertDialog(
                                onDismissRequest = { expenseToManage = null },
                                title = { Text("Manage Transaction") },
                                text = { Text("What would you like to do with ${expenseToManage?.description}?") },
                                confirmButton = {
                                    Button(onClick = {
                                        editingExpenseId = expenseToManage?.id
                                        desc = expenseToManage?.description ?: ""
                                        amtTxt = expenseToManage?.amount?.toString() ?: ""
                                        selectedType = expenseToManage?.type ?: "EXPENSE"
                                        selectedCategory = expenseToManage?.category ?: "Food"
                                        // We can't actually prepopulate the bottom form's local state 
                                        // from this dialog's onClick because of Compose scope boundaries
                                        // without lifting the state higher. I will lift state higher.
                                        expenseToManage = null
                                    }) { Text("Edit") }
                                },
                                dismissButton = {
                                    Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), onClick = {
                                        scope.launch {
                                            expenseToManage?.id?.let { RetrofitClient.api.deleteExpense(currentUserEmail, it) }
                                            expenseToManage = null
                                            refresh()
                                        }
                                    }) { Text("Delete") }
                                }
                            )
                        }
                    } 
                    // TAB 1: Analysis (Donut Chart & Trends)
                    else if (selectedTab == 1) {
                        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                            item {
                                Text("Analytics Dashboard", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(16.dp))
                                
                                // Time Tracker Selection
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    listOf("Week", "Month", "Year", "All Time").forEach { filterOption ->
                                        FilterChip(
                                            selected = timeFilter == filterOption,
                                            onClick = { timeFilter = filterOption },
                                            label = { Text(filterOption) }
                                        )
                                    }
                                }
                                
                                // Fintastics-Pro Analytics Reset
                                if (selectedDateFilter != null) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Viewing: $selectedDateFilter", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        TextButton(onClick = { selectedDateFilter = null }) { Text("Reset View") }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }

                                // Interactive Donut Chart tied to selectedDateFilter
                                val donutData = if (selectedDateFilter != null) {
                                    filteredByTimeList.filter { it.date == selectedDateFilter }
                                } else {
                                    filteredByTimeList
                                }
                                
                                CategoryDonutChart(donutData)
                                SpendingCalendar(expenseList, selectedDateFilter, onDateSelect = { selectedDateFilter = if (selectedDateFilter == it) null else it })
                                Spacer(Modifier.height(80.dp))
                            }
                        }
                    } 
                    // TAB 2: Budgets & AI Plan
                    else if (selectedTab == 2) {
                        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                            item {
                                Text("Monthly Budgets", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                Text("Control your spending limits", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                Spacer(Modifier.height(16.dp))
                                
                                // Overall Budget overview
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Global Limit", fontWeight = FontWeight.Bold)
                                            Text("₹${budgetStatus?.totalSpent?.toInt() ?: 0} / ₹${savingsGoal.toInt()}")
                                        }
                                        LinearProgressIndicator(
                                            progress = { if(savingsGoal>0) (expense / savingsGoal).toFloat().coerceIn(0f,1f) else 0f },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp),
                                            color = if (budgetStatus?.exceeds80Percent == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                Text("Category Limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                
                                // Category specific budgets
                                // We use userSettings?.categoryLimits if available, otherwise default fallback values
                                val defaultLimits = mapOf(
                                    "Food" to 5000.0, "Shopping" to 3000.0, "Travel" to 2000.0,
                                    "Entertainment" to 1500.0, "Bills" to 4000.0
                                )
                                val categoryBudgets = userSettings?.categoryLimits?.takeIf { it.isNotEmpty() } ?: defaultLimits
                                
                                var editingCategoryLimit by remember { mutableStateOf<String?>(null) }
                                var newLimitValue by remember { mutableStateOf("") }
                                
                                if (editingCategoryLimit != null) {
                                    AlertDialog(
                                        onDismissRequest = { editingCategoryLimit = null },
                                        title = { Text("Update Limit for $editingCategoryLimit") },
                                        text = {
                                            OutlinedTextField(
                                                value = newLimitValue,
                                                onValueChange = { newLimitValue = it },
                                                label = { Text("Amount (₹)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        confirmButton = {
                                            Button(onClick = {
                                                val amt = newLimitValue.toDoubleOrNull()
                                                if (amt != null && userSettings != null) {
                                                    scope.launch {
                                                        val limits = userSettings!!.categoryLimits?.toMutableMap() ?: mutableMapOf()
                                                        limits[editingCategoryLimit!!] = amt
                                                        userSettings!!.categoryLimits = limits
                                                        RetrofitClient.api.updateUserSettings(currentUserEmail, userSettings!!)
                                                        refresh()
                                                        editingCategoryLimit = null
                                                    }
                                                }
                                            }) { Text("Save") }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { editingCategoryLimit = null }) { Text("Cancel") }
                                        }
                                    )
                                }
                                
                                categoryBudgets.forEach { (cat, limit) ->
                                    val catSpent = filteredByTimeList.filter { it.type == "EXPENSE" && it.category == cat }.sumOf { it.amount ?: 0.0 }
                                    val catProg = if (limit > 0) (catSpent / limit).toFloat().coerceIn(0f, 1f) else 0f
                                    val isOver = catSpent > limit
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                            editingCategoryLimit = cat
                                            newLimitValue = limit.toString()
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("$cat ✎", fontWeight = FontWeight.SemiBold)
                                                if (isOver) {
                                                    Text("₹${catSpent.toInt()} / ₹${limit.toInt()} (Overspent)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                } else {
                                                    Text("₹${catSpent.toInt()} / ₹${limit.toInt()}", color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                            LinearProgressIndicator(
                                                progress = { catProg }, 
                                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp), 
                                                color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.height(24.dp))
                                Text("AI Copilot Suggestion Engine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = {
                                    scope.launch {
                                        try { aiSuggestions = RetrofitClient.api.suggestCuts(currentUserEmail, savingsGoal) }
                                        catch (e: Exception) { e.printStackTrace() }
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text("Ask Copilot for Specific Saving Tips") }
                                
                                aiSuggestions?.let { cuts ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text(cuts.action ?: "AI Recommendations:", fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(8.dp))
                                            
                                            // New Dynamic AI Tips (Strings)
                                            val aiTips = cuts.cuts?.keys?.toList() ?: emptyList()
                                            
                                            if (cuts.action?.contains("No flexible spending", ignoreCase = true) == true) {
                                                Text(cuts.action)
                                            } else {
                                                aiTips.forEachIndexed { index, cat ->
                                                    val amount = cuts.cuts?.get(cat) ?: 0.0
                                                    // Map the cut data into Fintastics Strings if the backend doesn't output the raw string array directly in this legacy model
                                                    Text("💡 Cut ₹${amount.toInt()} from ${cat} to stay on track")
                                                    Spacer(Modifier.height(4.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                                Text("Split Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                var splitDesc by remember { mutableStateOf("") }
                                var splitAmt by remember { mutableStateOf("") }
                                var numFriends by remember { mutableStateOf("2") }
                                OutlinedTextField(value = splitDesc, onValueChange = { splitDesc = it }, label = { Text("Dinner, movie...") }, modifier = Modifier.fillMaxWidth())
                                Row {
                                    OutlinedTextField(value = splitAmt, onValueChange = { splitAmt = it }, label = { Text("Total Amt") }, modifier = Modifier.weight(1f))
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedTextField(value = numFriends, onValueChange = { numFriends = it }, label = { Text("Friends Count") }, modifier = Modifier.weight(1f))
                                }
                                Button(onClick = {
                                    scope.launch {
                                        val amt = splitAmt.toDoubleOrNull() ?: return@launch
                                        val count = numFriends.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        val splitPerPerson = amt / count
                                        val details = (1..count).map { SplitDetail("Friend $it", splitPerPerson) }
                                        RetrofitClient.api.addSplitExpense(currentUserEmail, SplitExpense(description = splitDesc, totalAmount = amt, paidBy = "Me", splitDetails = details))
                                        splitDesc = ""; splitAmt = ""; Toast.makeText(context, "Split Created for $count friends", Toast.LENGTH_SHORT).show()
                                    }
                                }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Log Split with Friends") }
                            }
                        }
                    }
                    // TAB 3: Accounts & Settings (Passbook)
                    else if (selectedTab == 3) {
                        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                            item {
                                Text("Account Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                
                                val emailDisplay = userSettings?.email ?: currentUserEmail
                                Text("Email: $emailDisplay", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(16.dp))

                                // Passbook
                                Text("Virtual Passbook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                
                                var showBalanceDialog by remember { mutableStateOf(false) }
                                var editingBalanceType by remember { mutableStateOf("") }
                                var newBalanceValue by remember { mutableStateOf("") }

                                if (showBalanceDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showBalanceDialog = false },
                                        title = { Text("Update $editingBalanceType Balance") },
                                        text = {
                                            OutlinedTextField(
                                                value = newBalanceValue,
                                                onValueChange = { newBalanceValue = it },
                                                label = { Text("Amount (₹)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        confirmButton = {
                                            Button(onClick = {
                                                val amt = newBalanceValue.toDoubleOrNull()
                                                if (amt != null && userSettings != null) {
                                                    scope.launch {
                                                        if (editingBalanceType == "Cash") userSettings!!.cashBalance = amt
                                                        else userSettings!!.bankBalance = amt
                                                        RetrofitClient.api.updateUserSettings(currentUserEmail, userSettings!!)
                                                        refresh()
                                                        showBalanceDialog = false
                                                    }
                                                }
                                            }) { Text("Save") }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showBalanceDialog = false }) { Text("Cancel") }
                                        }
                                    )
                                }

                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Card(
                                        Modifier.weight(1f).padding(end = 4.dp).clickable { 
                                            editingBalanceType = "Cash"
                                            newBalanceValue = userSettings?.cashBalance?.toString() ?: "0.0"
                                            showBalanceDialog = true
                                        }, 
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text("Cash on Hand ✎", style = MaterialTheme.typography.labelMedium)
                                            Text("₹${userSettings?.cashBalance ?: 0.0}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Card(
                                        Modifier.weight(1f).padding(start = 4.dp).clickable { 
                                            editingBalanceType = "Bank"
                                            newBalanceValue = userSettings?.bankBalance?.toString() ?: "0.0"
                                            showBalanceDialog = true
                                        }, 
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text("Bank Accounts ✎", style = MaterialTheme.typography.labelMedium)
                                            Text("₹${userSettings?.bankBalance ?: 0.0}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.height(24.dp))
                                Text("General Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("App Lock (Biometric)")
                                            Switch(
                                                checked = userSettings?.appLockEnabled ?: false,
                                                onCheckedChange = { isChecked ->
                                                    scope.launch {
                                                        userSettings?.let {
                                                            it.appLockEnabled = isChecked
                                                            RetrofitClient.api.updateUserSettings(currentUserEmail, it)
                                                            refresh()
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        androidx.compose.material3.Divider(Modifier.padding(vertical = 8.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("SMS Tracking Toggle")
                                            Switch(
                                                checked = userSettings?.smsTrackingEnabled ?: false,
                                                onCheckedChange = { isChecked ->
                                                    scope.launch {
                                                        userSettings?.let {
                                                            it.smsTrackingEnabled = isChecked
                                                            RetrofitClient.api.updateUserSettings(currentUserEmail, it)
                                                            refresh()
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        RetrofitClient.api.resetAllExpenses(currentUserEmail)
                                        refresh()
                                        Toast.makeText(context, "All Data Cleared!", Toast.LENGTH_SHORT).show()
                                    }
                                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { 
                                    Text("Reset & Clear All Expenses Database") 
                                }

                                Spacer(Modifier.height(16.dp))
                                Button(onClick = {
                                    sharedPrefs.edit().clear().apply()
                                    currentScreen = AppState.LOGIN
                                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                    Text("Logout")
                                }
                            }
                        }
                    }

                    // TAB 4: Goals Dashboard (New)
                    else if (selectedTab == 4) {
                        var showGoalDialog by remember { mutableStateOf(false) }
                        
                        Box(Modifier.fillMaxSize()) {
                            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                                item {
                                    Text("My Goals", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                    Text("Track what matters most", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                    Spacer(Modifier.height(16.dp))
                                }
                                
                                if (goalsList.isEmpty()) {
                                    item {
                                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                            Spacer(Modifier.height(16.dp))
                                            Text("No goals set yet.", color = Color.Gray)
                                            Text("Tap + to start saving!", color = Color.Gray)
                                        }
                                    }
                                } else {
                                    items(goalsList) { g ->
                                        GoalCard(g) { showGoalDialog = true }
                                    }
                                }
                                item { Spacer(Modifier.height(80.dp)) } // Space for FAB
                            }
                            
                            FloatingActionButton(
                                onClick = { showGoalDialog = true },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "New Goal")
                            }
                        }
                        
                        if (showGoalDialog) {
                            var gName by remember { mutableStateOf("") }
                            var gAmount by remember { mutableStateOf("") }
                            var gDeadline by remember { mutableStateOf("") }
                            
                            AlertDialog(
                                onDismissRequest = { showGoalDialog = false },
                                title = { Text("Create New Goal") },
                                text = { 
                                    Column {
                                        OutlinedTextField(value = gName, onValueChange = { gName = it }, label = { Text("Goal Name (e.g. Vacation)") }, modifier = Modifier.fillMaxWidth())
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(value = gAmount, onValueChange = { gAmount = it }, label = { Text("Target Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(value = gDeadline, onValueChange = { gDeadline = it }, label = { Text("Deadline (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        val amt = gAmount.toDoubleOrNull()
                                        if (gName.isNotBlank() && amt != null) {
                                            scope.launch {
                                                RetrofitClient.api.addGoal(currentUserEmail, GoalRequest(gName, amt, gDeadline))
                                                showGoalDialog = false
                                                refresh()
                                                Toast.makeText(context, "Goal Created!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }) { Text("Save Goal") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }

                    // Confetti Effect
                    if (savedAmount >= savingsGoal && savingsGoal > 0) {
                        KonfettiView(
                            modifier = Modifier.fillMaxSize(),
                            parties = listOf(Party(speed = 0f, maxSpeed = 30f, damping = 0.9f, spread = 360, colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def), position = Position.Relative(0.5, 0.3), emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)))
                        )
                    }
                }
            }
        }
    }
}

// 3. REPAIRED: SpendingCalendar fixed Arrangement/Alignment mismatch and made Interactive
@Composable
fun SpendingCalendar(expenses: List<ExpenseResponse>, selectedDate: String?, onDateSelect: (String) -> Unit) {
    @SuppressLint("NewApi")
    val daily = expenses.filter { it.date != null }.groupBy { it.date!! }.mapValues { it.value.sumOf { e -> if (e.type == "EXPENSE") e.amount ?: 0.0 else 0.0 } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        items(7) { i ->
            @SuppressLint("NewApi")
            val d = LocalDate.now().minusDays(i.toLong()).toString()
            val isSelected = d == selectedDate
            Card(
                modifier = Modifier.width(75.dp).clickable { onDateSelect(d) },
                colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(d.takeLast(2), fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified)
                    Text("₹${(daily[d] ?: 0.0).toInt()}", style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified)
                }
            }
        }
    }
}

// --- REMAINING UI COMPONENTS ---

@Composable
fun LoginScreen(email: String, onEmailChange: (String) -> Unit, onSendOtp: () -> Unit, onBiometricAuth: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center) {
        OutlinedTextField(value = email, onValueChange = onEmailChange, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onSendOtp, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Login with OTP") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBiometricAuth, modifier = Modifier.fillMaxWidth()) { Text("Use Biometrics") }
    }
}

@Composable
fun OtpScreen(otp: String, onOtpChange: (String) -> Unit, onVerify: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center) {
        OutlinedTextField(value = otp, onValueChange = onOtpChange, label = { Text("OTP") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onVerify, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Verify") }
    }
}

@Composable
fun BalanceCard(inc: Double, exp: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total Balance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            Text("₹${inc - exp}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Income", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Income", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Text("₹$inc", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
                androidx.compose.material3.Divider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.Gray.copy(alpha = 0.3f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Expense", tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Expense", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Text("₹$exp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                }
            }
        }
    }
}

@Composable
fun GoalCard(goal: GoalResponse, onClickEdit: () -> Unit) {
    val saved = goal.currentSaved ?: 0.0
    val target = goal.targetAmount ?: 1.0
    val prog = (saved / target).toFloat().coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(goal.goalName ?: "Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("₹${(target - saved).toInt()} left to save", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                
                // Circular Progress Percentage
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { prog },
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        strokeWidth = 4.dp
                    )
                    Text("${(prog * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Saved: ₹${saved.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("Target: ₹${target.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().height(8.dp), strokeCap = StrokeCap.Round, color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            
            if (!goal.deadline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Deadline: ${goal.deadline}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}


@Composable
fun CategoryDonutChart(expenses: List<ExpenseResponse>) {
    val data = expenses.filter { it.type == "EXPENSE" }.groupBy { it.category ?: "Other" }.mapValues { it.value.sumOf { it.amount ?: 0.0 } }
    val total = data.values.sum()
    
    // Constant vibrant colors mapping for strict consistency
    val consistentColors = mapOf(
        "Food" to Color(0xFFEF5350),      // Red
        "Travel" to Color(0xFF42A5F5),    // Blue
        "Shopping" to Color(0xFFAB47BC),  // Purple
        "Academic" to Color(0xFFFFA726),  // Orange
        "Health" to Color(0xFF66BB6A),    // Green
        "Entertainment" to Color(0xFFEC407A), // Pink
        "Investment" to Color(0xFF26A69A), // Teal
        "Rent" to Color(0xFF8D6E63),      // Brown
        "Recharges" to Color(0xFF29B6F6), // Light Blue
        "Bills" to Color(0xFFFFCA28)      // Amber
    )
    val colorFallback = listOf(Color.Cyan, Color.Magenta, Color.Yellow)
    
    val colorMap = data.keys.withIndex().associate { (index, cat) -> 
        cat to (consistentColors[cat] ?: colorFallback[index % colorFallback.size]) 
    }

    val categoryEmoji = mapOf(
        "Food" to "🍔", "Travel" to "🚗", "Shopping" to "🛍️", "Academic" to "📚", 
        "Health" to "🏥", "Entertainment" to "🎬", "Investment" to "📈", "Rent" to "🏠", 
        "Recharges" to "📱", "Bills" to "🧾", "Other" to "📌"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Monthly Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.onSurface)
            
            if (data.isEmpty()) {
                Text("No expenses logged for this period.", color = Color.Gray, modifier = Modifier.padding(32.dp))
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(140.dp)) {
                            var startAngle = -90f // Start from top
                            val strokeWidth = 45f // Donut width
                            for ((category, amount) in data) {
                                val sweepAngle = (amount.toFloat() / total.toFloat()) * 360f
                                drawArc(color = colorMap[category] ?: Color.Gray, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth))
                                startAngle += sweepAngle
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total", style = MaterialTheme.typography.labelMedium)
                            Text("₹${total.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        data.forEach { (category, amount) ->
                            val percent = if (total > 0) ((amount / total) * 100).toInt() else 0
                            val emoji = categoryEmoji[category] ?: "📌"
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Box(modifier = Modifier.size(12.dp).background(colorMap[category] ?: Color.Gray, CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text("$emoji $category: $percent%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionForm(type: String, onT: (String) -> Unit, cat: String, onC: (String) -> Unit, d: String, onD: (String) -> Unit, a: String, onA: (String) -> Unit, date: String, onDateChange: (String) -> Unit, accountType: String, onAccountChange: (String) -> Unit, goalsList: List<GoalResponse> = emptyList(), selectedGoalId: String? = null, onGoalSelected: (String?) -> Unit = {}, isEditMode: Boolean, onAdd: () -> Unit, onScanBill: () -> Unit) {
    // 10 Requested Categories Implementation
    val categories = if (type == "INCOME") {
        listOf("Salary", "Investment", "Pocket Money", "Other")
    } else if (type == "GOAL_DEPOSIT") {
        listOf("Savings", "Goal Transfer", "Other")
    } else {
        listOf("Food", "Travel", "Shopping", "Academic", "Health", "Entertainment", "Investment", "Rent", "Recharges", "Bills", "Other")
    }

    LaunchedEffect(type) {
        if (cat !in categories) {
            onC(categories.first())
        }
        if (type == "EXPENSE") onGoalSelected(null)
    }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = type == "EXPENSE", onClick = { onT("EXPENSE") }, label = { Text("Expense") })
                FilterChip(selected = type == "INCOME", onClick = { onT("INCOME") }, label = { Text("Income") })
                FilterChip(selected = type == "GOAL_DEPOSIT", onClick = { onT("GOAL_DEPOSIT") }, label = { Text("Save for Goal") })
            }
            IconButton(onClick = onScanBill) { Icon(Icons.Default.CameraAlt, "Scan OCR") }
        }
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            items(categories) { categoryChip ->
                FilterChip(selected = cat == categoryChip, onClick = { onC(categoryChip) }, label = { Text(categoryChip) })
            }
        }
        
        OutlinedTextField(value = d, onValueChange = onD, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(value = a, onValueChange = onA, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            
            // Fintastics-Pro Date Picker
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
            
            OutlinedTextField(
                value = date,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, null) } },
                modifier = Modifier.weight(1f).clickable { showDatePicker = true }
            )
            
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { 
                            showDatePicker = false
                            datePickerState.selectedDateMillis?.let { millis ->
                                // Very simple formatting for MyMoney parity
                                val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                formatter.timeZone = java.util.TimeZone.getTimeZone("UTC") // DatePicker requires UTC assumption
                                onDateChange(formatter.format(java.util.Date(millis)))
                            }
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
        
        // Fintastics-Pro Account Picker
        var accountExpanded by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = accountType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Account") },
                trailingIcon = { IconButton(onClick = { accountExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                modifier = Modifier.fillMaxWidth().clickable { accountExpanded = true }
            )
            DropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                DropdownMenuItem(text = { Text("Cash") }, onClick = { onAccountChange("Cash"); accountExpanded = false })
                DropdownMenuItem(text = { Text("Bank") }, onClick = { onAccountChange("Bank"); accountExpanded = false })
            }
        }
        
        // Fintastics-Pro Feature: Allocate Income or Goal Saving to a Goal
        if ((type == "INCOME" || type == "GOAL_DEPOSIT") && goalsList.isNotEmpty() && !isEditMode) {
            Spacer(Modifier.height(8.dp))
            var expanded by remember { mutableStateOf(false) }
            val selectedGoalName = goalsList.find { it.id == selectedGoalId }?.goalName ?: "Don't Allocate"
            
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedGoalName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Allocate to Goal (Optional)") },
                    trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                    modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Don't Allocate") }, onClick = { onGoalSelected(null); expanded = false })
                    goalsList.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g.goalName ?: "") },
                            onClick = { onGoalSelected(g.id); expanded = false }
                        )
                    }
                }
            }
        }
        
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(if (isEditMode) "Update Transaction" else "Add Transaction") }
    }
}

@Composable
fun TransactionItem(item: ExpenseResponse, onClick: () -> Unit) {
    val categoryEmoji = mapOf(
        "Food" to "🍔", "Travel" to "🚗", "Shopping" to "🛍️", "Academic" to "📚", 
        "Health" to "🏥", "Entertainment" to "🎬", "Investment" to "📈", "Rent" to "🏠", 
        "Recharges" to "📱", "Bills" to "🧾", "Salary" to "💰", "Pocket Money" to "💵",
        "Savings" to "🏦", "Goal Transfer" to "🎯", "Other" to "📌"
    )
    val emoji = categoryEmoji[item.category] ?: "📌"

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(emoji, style = MaterialTheme.typography.titleLarge)
                }
            },
            headlineContent = { Text(item.description ?: "", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) }, 
            supportingContent = { Text(item.category ?: "", color = Color.Gray) },
            trailingContent = { 
                Text(
                    "₹${item.amount}", 
                    color = if (item.type == "INCOME") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                ) 
            }
        )
    }
}