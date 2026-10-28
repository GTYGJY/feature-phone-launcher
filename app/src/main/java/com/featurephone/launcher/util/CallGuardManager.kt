package com.featurephone.launcher.util

/*
 * 【修改】1.2.2 起「防护模式」改为纯文字「防护提示」页（ProtectionSettingsActivity），
 * 软件本身不再接管来电铃声：非默认拨号应用在 Android 9+ 通常拿不到来电号码，
 * 原「非通讯录来电静音」在多数机型上不会生效，反而会和系统电话应用抢铃声。
 *
 * 因此 CallGuardManager 的实现已整体移除（本文件仅为占位说明，不再被任何代码引用）。
 * 如后续确实需要恢复该功能，请从版本历史（1.2.1）取回原实现。
 */
