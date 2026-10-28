package com.featurephone.launcher.model

import android.content.Intent

data class AppItem(
    val id: String,
    val name: String,
    val icon: String? = null,
    val intent: Intent? = null,
    val action: (() -> Unit)? = null
) {
    override fun toString(): String = name
}
