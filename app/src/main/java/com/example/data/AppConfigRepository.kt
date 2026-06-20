package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppConfigRepository(private val dao: AppConfigDao) {
    val configFlow: Flow<AppConfig> = dao.getConfigFlow().map { it ?: AppConfig() }

    suspend fun getConfig(): AppConfig {
        return dao.getConfig() ?: AppConfig()
    }

    suspend fun updateConfig(config: AppConfig) {
        dao.saveConfig(config)
    }
}
