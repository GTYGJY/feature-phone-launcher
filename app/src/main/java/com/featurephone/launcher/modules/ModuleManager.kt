package com.featurephone.launcher.modules

import android.content.Context
import android.content.SharedPreferences

/**
 * 模块管理器
 * 负责注册、切换、获取当前信息模块
 * 支持两个独立模块位置（模块1和模块2），不能选择相同模块
 * 以后添加新模块只需在allModules列表中注册
 */
object ModuleManager {

    private const val PREFS_NAME = "module_settings"
    private const val KEY_MODULE_1 = "module_1_id"
    private const val KEY_MODULE_2 = "module_2_id"

    /**
     * 所有已注册的模块
     * 添加新模块时在此处注册即可
     */
    val allModules: List<InfoModule> = listOf(
        BatteryModule(),
        WeatherModule(),
        EmptyModule()
        // 以后添加新模块在此处注册，例如：
        // StepCounterModule(),
        // MusicModule(),
        // NotificationModule()
    )

    private lateinit var prefs: SharedPreferences
    private var module1: InfoModule? = null
    private var module2: InfoModule? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** 获取模块1，默认为电量模块 */
    fun getModule1(): InfoModule {
        if (module1 == null) {
            if (!::prefs.isInitialized) return allModules.first()
            val id = prefs.getString(KEY_MODULE_1, BatteryModule().getId())
            module1 = allModules.find { it.getId() == id } ?: allModules.first()
        }
        return module1!!
    }

    /** 获取模块2，默认为空白模块 */
    fun getModule2(): InfoModule {
        if (module2 == null) {
            if (!::prefs.isInitialized) return allModules.last()
            val id = prefs.getString(KEY_MODULE_2, WeatherModule().getId())
            module2 = allModules.find { it.getId() == id } ?: allModules.last()
        }
        return module2!!
    }

    /** 设置模块1，不能与模块2相同（空白模块除外） */
    fun setModule1(moduleId: String): Boolean {
        if (!::prefs.isInitialized) return false
        val other = getModule2().getId()
        if (moduleId == other && moduleId != EmptyModule().getId()) {
            return false // 重复，不允许
        }
        prefs.edit().putString(KEY_MODULE_1, moduleId).apply()
        module1 = allModules.find { it.getId() == moduleId } ?: allModules.first()
        return true
    }

    /** 设置模块2，不能与模块1相同（空白模块除外） */
    fun setModule2(moduleId: String): Boolean {
        if (!::prefs.isInitialized) return false
        val other = getModule1().getId()
        if (moduleId == other && moduleId != EmptyModule().getId()) {
            return false // 重复，不允许
        }
        prefs.edit().putString(KEY_MODULE_2, moduleId).apply()
        module2 = allModules.find { it.getId() == moduleId } ?: allModules.last()
        return true
    }

    /** 获取模块1的索引 */
    fun getModule1Index(): Int {
        return allModules.indexOfFirst { it.getId() == getModule1().getId() }.coerceAtLeast(0)
    }

    /** 获取模块2的索引 */
    fun getModule2Index(): Int {
        return allModules.indexOfFirst { it.getId() == getModule2().getId() }.coerceAtLeast(0)
    }

    // 兼容旧代码
    fun getCurrentModule(): InfoModule = getModule1()
    fun setCurrentModule(moduleId: String) { setModule1(moduleId) }
    fun getCurrentModuleIndex(): Int = getModule1Index()
    fun getModuleNames(): List<String> = allModules.map { it.getDisplayName() }
}
