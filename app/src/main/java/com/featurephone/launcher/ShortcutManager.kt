package com.featurephone.launcher

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 快捷应用管理器
 * 最多保存8个快捷应用，显示在主菜单中
 */
object ShortcutManager {

    private const val PREFS_NAME = "shortcut_settings"
    private const val KEY_SHORTCUTS = "shortcut_list"
    const val MAX_SHORTCUTS = 8

    data class ShortcutApp(
        val packageName: String,
        val appName: String
    )

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getShortcuts(): List<ShortcutApp> {
        val jsonStr = prefs.getString(KEY_SHORTCUTS, "[]") ?: "[]"
        val list = mutableListOf<ShortcutApp>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ShortcutApp(
                        obj.getString("packageName"),
                        obj.getString("appName")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun addShortcut(packageName: String, appName: String) {
        val list = getShortcuts().toMutableList()
        if (list.any { it.packageName == packageName }) return
        if (list.size >= MAX_SHORTCUTS) return
        list.add(ShortcutApp(packageName, appName))
        saveShortcuts(list)
    }

    fun removeShortcut(packageName: String) {
        val list = getShortcuts().filter { it.packageName != packageName }
        saveShortcuts(list)
    }

    fun clearShortcuts() {
        saveShortcuts(emptyList())
    }

    private fun saveShortcuts(list: List<ShortcutApp>) {
        val jsonArray = JSONArray()
        for (app in list) {
            val obj = JSONObject()
            obj.put("packageName", app.packageName)
            obj.put("appName", app.appName)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SHORTCUTS, jsonArray.toString()).apply()
    }
}
