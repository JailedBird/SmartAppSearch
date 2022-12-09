package cn.jailedbird.smartappsearch.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import cn.jailedbird.smartappsearch.App
import com.github.promeg.pinyinhelper.Pinyin

internal fun toast(s: String?) {
    if (!s.isNullOrEmpty()) {
        Toast.makeText(App.applicationContext, s, Toast.LENGTH_SHORT).show()
    }
}

internal fun Any?.log() {
    val s = this?.toString() ?: return
    if (s.isNotEmpty()) {
        Log.d(App.appName, s)
    }
}

private fun String?.isChinese(): Boolean {
    val s = this
    if (s.isNullOrEmpty()) {
        return false
    } else {
        s.forEach {
            if (Pinyin.isChinese(it)) {
                return true
            }
        }
    }
    return false
}

internal fun String?.toPinyin(): String? {
    val s = this
    return try {
        if (s.isChinese()) {
            Pinyin.toPinyin(s, "")
        } else {
            s
        }
    } catch (e: Exception) {
        s
    }
}

internal fun Context.finishProcess() {
    android.os.Process.killProcess(android.os.Process.myPid())
}
