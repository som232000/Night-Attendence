package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.NightShiftEntryEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    /**
     * Exports list of NightShiftEntryEntity to a CSV file compatible with Microsoft Excel,
     * including UTF-8 Byte Order Mark (BOM) to correctly preserve Bengali and international text.
     */
    fun exportToExcel(context: Context, entries: List<NightShiftEntryEntity>, language: AppLanguage) {
        if (entries.isEmpty()) {
            Toast.makeText(
                context,
                if (language == AppLanguage.BENGALI) "এক্সপোর্ট করার জন্য কোনো রেকর্ড নেই" else "No records to export",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Night_Shift_Report_$timeStamp.csv"
            val exportDir = File(context.cacheDir, "reports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, fileName)
            val outputStream = FileOutputStream(file)

            // UTF-8 BOM for Microsoft Excel compatibility with non-ASCII characters
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = "ID,Date,Mistri Name,Labour Name,Work Place / Site,Shift Details,Remarks\n"
            outputStream.write(header.toByteArray(Charsets.UTF_8))

            entries.forEachIndexed { index, item ->
                val line = buildString {
                    append(index + 1).append(",")
                    append(escapeCsv(item.workDate)).append(",")
                    append(escapeCsv(item.mistriName)).append(",")
                    append(escapeCsv(item.labourName)).append(",")
                    append(escapeCsv(item.workPlace)).append(",")
                    append(escapeCsv(item.shiftDetails)).append(",")
                    append(escapeCsv(item.remarks)).append("\n")
                }
                outputStream.write(line.toByteArray(Charsets.UTF_8))
            }
            outputStream.flush()
            outputStream.close()

            // Share / Open Intent
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Night Shift Records Export ($timeStamp)")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    if (language == AppLanguage.BENGALI) "রিপোর্ট শেয়ার অথবা সেভ করুন" else "Share or Save Excel/CSV Report"
                )
            )

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Prints or saves as PDF a beautifully formatted HTML report using Android's PrintManager.
     */
    fun printReport(context: Context, entries: List<NightShiftEntryEntity>, title: String, language: AppLanguage) {
        if (entries.isEmpty()) {
            Toast.makeText(
                context,
                if (language == AppLanguage.BENGALI) "প্রিন্ট করার জন্য কোনো রেকর্ড নেই" else "No records to print",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Print service not available", Toast.LENGTH_SHORT).show()
            return
        }

        val printDate = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
        val isBn = language == AppLanguage.BENGALI

        val htmlContent = buildString {
            append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            append("<style>")
            append("body { font-family: 'Segoe UI', Tahoma, sans-serif; margin: 20px; color: #1e293b; }")
            append("h1 { color: #0f172a; margin-bottom: 4px; font-size: 22px; }")
            append(".meta { color: #64748b; font-size: 13px; margin-bottom: 16px; }")
            append("table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 13px; }")
            append("th { background-color: #0d47a1; color: #ffffff; text-align: left; padding: 10px 8px; }")
            append("td { padding: 9px 8px; border-bottom: 1px solid #e2e8f0; }")
            append("tr:nth-child(even) { background-color: #f8fafc; }")
            append(".badge { display: inline-block; padding: 2px 8px; border-radius: 4px; background: #e0f2fe; color: #0369a1; font-weight: bold; font-size: 11px; }")
            append(".footer { margin-top: 24px; text-align: right; font-size: 12px; color: #94a3b8; }")
            append("</style></head><body>")
            append("<h1>${if (isBn) "নাইট শিফট কাজের প্রতিবেদন" else "Night Shift Work Report"}</h1>")
            append("<div class='meta'>${if (isBn) "তারিখ ও সময়: " else "Generated on: "}$printDate | ${if (isBn) "মোট এন্ট্রি: " else "Total Entries: "}${entries.size}</div>")

            append("<table><thead><tr>")
            append("<th>#</th>")
            append("<th>${if (isBn) "তারিখ" else "Date"}</th>")
            append("<th>${if (isBn) "মিস্ত্রি" else "Mistri"}</th>")
            append("<th>${if (isBn) "লেবার" else "Labour"}</th>")
            append("<th>${if (isBn) "কাজের স্থান" else "Work Place / Site"}</th>")
            append("<th>${if (isBn) "শিফট বিবরণ" else "Shift Details"}</th>")
            append("<th>${if (isBn) "মন্তব্য" else "Remarks"}</th>")
            append("</tr></thead><tbody>")

            entries.forEachIndexed { i, e ->
                append("<tr>")
                append("<td>${i + 1}</td>")
                append("<td><strong>${e.workDate}</strong></td>")
                append("<td>${e.mistriName}</td>")
                append("<td><span class='badge'>${e.labourName}</span></td>")
                append("<td>${e.workPlace}</td>")
                append("<td>${e.shiftDetails}</td>")
                append("<td>${e.remarks.ifBlank { "-" }}</td>")
                append("</tr>")
            }

            append("</tbody></table>")
            append("<div class='footer'>${if (isBn) "নাইট শিফট ম্যানেজমেন্ট সিস্টেম দ্বারা প্রস্তুত" else "Prepared by Night Shift Management System"}</div>")
            append("</body></html>")
        }

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val printAdapter = webView.createPrintDocumentAdapter("Night_Shift_Report_${System.currentTimeMillis()}")
                val jobName = "${title}_${System.currentTimeMillis()}"
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\"", "\"\"")
        if (str.contains(",") || str.contains("\n") || str.contains("\"")) {
            str = "\"$str\""
        }
        return str
    }
}
