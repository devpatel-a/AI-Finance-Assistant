package com.finance.expense_copilot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val messageBody = sms.messageBody ?: continue
                Log.d("SmsReceiver", "Received SMS: $messageBody")
                
                // Typical banking regex for debits: "Rs. 1,000.00 debited" or "INR 500 spent"
                val match = Regex("(?i)(?:rs\\.?|inr)\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)").find(messageBody)
                
                if (match != null && messageBody.contains(Regex("(?i)debited|spent|paid"))) {
                    val amountStr = match.groupValues[1].replace(",", "")
                    val amount = amountStr.toDoubleOrNull()
                    
                    if (amount != null) {
                        Log.d("SmsReceiver", "Parsed amount: $amount")
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val req = ExpenseRequest(
                                    description = "Auto-logged from SMS: ${messageBody.take(20)}...",
                                    amount = amount,
                                    type = "EXPENSE",
                                    date = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) LocalDate.now().toString() else "2026-03-16",
                                    category = "Other"
                                )
                                // To properly isolate, we need the logged-in user's email. For background services, 
                                // we either need to fetch it from SharedPreferences or use a fallback.
                                val sharedPrefs = context.getSharedPreferences("ExpenseAppPrefs", Context.MODE_PRIVATE)
                                val fallbackEmail = sharedPrefs.getString("userEmail", "sms_fallback@example.com") ?: "sms_fallback@example.com"
                                RetrofitClient.api.addExpense(fallbackEmail, req)
                                Log.d("SmsReceiver", "Successfully posted SMS expense!")
                            } catch (e: Exception) {
                                Log.e("SmsReceiver", "Error posting SMS expense", e)
                            }
                        }
                    }
                }
            }
        }
    }
}
