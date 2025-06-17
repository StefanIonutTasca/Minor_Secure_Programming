package com.example.minor_secure_programming.utils

object Constants {
    // Those credentials are safe to be pushed and ment for the public due to the fact that we have Row Level Security
    // enabled and we are using anonymous authentication. 
    // Meaning that if you want to do anything to the database, you need to be authenticated.
    const val SUPABASE_URL = "https://nevvbfvsrqertmwgvhlw.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5ldnZiZnZzcnFlcnRtd2d2aGx3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDkwNjA3MzcsImV4cCI6MjA2NDYzNjczN30.hpay2xx8HBDrOM21SCJj96ZUGKUYOOTQJiuLo4T4fTQ"
}