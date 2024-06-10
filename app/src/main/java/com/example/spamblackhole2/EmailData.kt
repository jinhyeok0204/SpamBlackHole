package com.example.spamblackhole2

data class EmailData(val id: String, val subject: String, val snippet: String, var isExpanded: Boolean = false) {
}