package com.splitmate.app.ui.export

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class GroupExportFormat(
    val extension: String,
    val mimeType: String,
    val title: String
) {
    EXCEL_XLSX(
        extension = "xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        title = "Excel Workbook (.xlsx)"
    ),
    CSV(
        extension = "csv",
        mimeType = "text/csv",
        title = "CSV Spreadsheet (.csv)"
    )
}

/**
 * Writes a per-group `.xlsx` or `.csv` ledger export into `cacheDir/shared_exports/` and opens
 * the Android system share sheet via [FileProvider] (`${applicationId}.fileprovider`).
 */
object GroupLedgerExportHelper {

    const val SHARED_EXPORTS_DIR = "shared_exports"
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun authority(context: Context): String = context.packageName + AUTHORITY_SUFFIX

    fun buildSafeFileName(groupName: String, format: GroupExportFormat): String {
        val clean = groupName.trim()
            .replace(Regex("[^A-Za-z0-9_\\-]+"), "_")
            .trim('_')
            .take(40)
            .ifEmpty { "SplitMate_Group" }
        return "${clean}_SplitMate_Ledger.${format.extension}"
    }

    fun writeExportFile(
        context: Context,
        model: GroupLedgerExportModel,
        format: GroupExportFormat
    ): File {
        val dir = File(context.cacheDir, SHARED_EXPORTS_DIR)
        if (dir.exists()) {
            dir.listFiles()?.forEach { runCatching { it.delete() } }
        } else if (!dir.mkdirs() && !dir.exists()) {
            throw java.io.IOException("Cannot create ${dir.absolutePath}")
        }
        val file = File(dir, buildSafeFileName(model.groupName, format))
        val bytes = when (format) {
            GroupExportFormat.EXCEL_XLSX -> GroupLedgerXlsxWriter.writeToBytes(model)
            GroupExportFormat.CSV -> GroupLedgerCsvWriter.writeToBytes(model)
        }
        FileOutputStream(file).use { out ->
            out.write(bytes)
            out.flush()
        }
        return file
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, authority(context), file)

    fun shareExportFile(
        context: Context,
        file: File,
        format: GroupExportFormat,
        model: GroupLedgerExportModel
    ) {
        val uri = uriFor(context, file)
        val summary = buildShareCaption(model)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${model.groupName} — SplitMate Ledger")
            putExtra(Intent.EXTRA_TEXT, summary)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share ${model.groupName} ledger (${format.extension.uppercase()})").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    suspend fun exportAndShare(
        context: Context,
        model: GroupLedgerExportModel,
        format: GroupExportFormat
    ): Result<Unit> = runCatching {
        val appContext = context.applicationContext
        val file = withContext(Dispatchers.IO) {
            writeExportFile(appContext, model, format)
        }
        shareExportFile(context, file, format, model)
    }

    fun buildShareCaption(model: GroupLedgerExportModel): String {
        val total = "${model.currencySymbol}${GroupLedgerExportBuilder.formatCentsDecimal(model.totalSpendCents)}"
        val transfers = if (model.pendingTransfers.isEmpty()) {
            "all settled"
        } else {
            val count = model.pendingTransfers.size
            "$count minimum ${if (count == 1) "transfer" else "transfers"}"
        }
        return "${model.groupName} ledger: ${model.expenses.size} expenses ($total), $transfers. Exported from SplitMate."
    }
}
