package com.hyper.music.music

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class SystemDirectoryPicker : ActivityResultContract<Unit, Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent {
        val xiaomiPackages = listOf(
            "com.android.fileexplorer",
            "com.mi.android.globalFileexplorer",
        )
        for (pkg in xiaomiPackages) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                setPackage(pkg)
            }
            if (intent.resolveActivity(context.packageManager) != null) return intent
        }
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addCategory(Intent.CATEGORY_DEFAULT)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

fun displayPathOfTreeUri(uri: Uri): String? {
    val docId = runCatching { android.provider.DocumentsContract.getTreeDocumentId(uri) }
        .getOrNull() ?: return null
    if (!docId.startsWith("primary:")) return null
    val relative = docId.removePrefix("primary:")
    return "/storage/emulated/0/" + relative
}
