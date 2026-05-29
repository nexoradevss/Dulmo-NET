package com.dolmus.netapp

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime

val supabase = createSupabaseClient(
    supabaseUrl = "https://ezmvwhpmvuokrrvsohuv.supabase.co",
    supabaseKey = "sb_publishable_tfN63FododffKy9jdqa8Tw_zD5GTnzY"
) {
    install(Postgrest)
    install(Auth) {
        alwaysAutoRefresh = true
        autoLoadFromStorage = true
    }
    install(Realtime)
}