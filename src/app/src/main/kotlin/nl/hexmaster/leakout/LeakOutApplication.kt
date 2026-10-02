package nl.hexmaster.leakout

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Makes the [AppContainer] and starts what runs once per process. */
class LeakOutApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, CoroutineScope(SupervisorJob() + Dispatchers.Default), getString(R.string.app_id))
        // Starts the Play Games SDK only when games-ids.xml has a real app id; the sync waits for a sign-in.
        container.playGames.start()
        // Players who finished levels before achievements existed, or before an update added
        // some, get what their history has earned.
        container.tracker.launchReconcile()
    }
}
