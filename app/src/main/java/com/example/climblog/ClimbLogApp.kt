package com.example.climblog

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.climblog.data.seed.ChsRocksSeeder
import com.example.climblog.data.seed.WallSeeder

@HiltAndroidApp
class ClimbLogApp : Application() {

    @Inject lateinit var wallSeeder: WallSeeder
    @Inject lateinit var chsRocksSeeder: ChsRocksSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            wallSeeder.seedIfEmpty()
            chsRocksSeeder.seedIfNeeded()
        }
    }
}
