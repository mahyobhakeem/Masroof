package com.masroofi.app.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.masroofi.app.R
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * محرك تصدير التقارير المالية الاحترافية بصيغة PDF لتطبيق مصروف
 * يعتمد على Android PdfDocument و Canvas الأصيلين لضمان اتصال وحيادية الحروف العربية 100%
 * مع دعم التنسيق الجدولي الراقي وتعدد الصفحات التلقائي والحفظ في مجلد التنزيلات والمشاركة الفورية
 */
object PdfReportExporter {

    private const val PAGE_WIDTH = 595 // A4 standard width at 72 dpi
    private const val PAGE_HEIGHT = 842 // A4 standard height at 72 dpi
    private const val MARGIN_RIGHT = 565f
    private const val MARGIN_LEFT = 30f
    private const val USABLE_WIDTH = MARGIN_RIGHT - MARGIN_LEFT // 535f

    /**
     * توليد ملف PDF كامل وتنسيقه وحفظه
     */
    fun generatePdfReport(
        context: Context,
        transactions: List<TransactionItem>,
        periodTitle: String,
        currencySymbol: String,
        reportTitle: String = "تقرير العمليات المالية المفصل",
        customOutputFile: File? = null
    ): File {
        val pdfDocument = PdfDocument()

        val tajawalBold = try {
            ResourcesCompat.getFont(context, R.font.tajawal_bold) ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } catch (_: Exception) {
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val tajawalRegular = try {
            ResourcesCompat.getFont(context, R.font.tajawal_regular) ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        } catch (_: Exception) {
            Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // الحسابات المالية للملخص
        val totalExpenses: Long = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalIncome: Long = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val netBalance: Long = totalIncome - totalExpenses

        // فرش الرسم المعتمدة
        val paintEmerald = Paint().apply {
            color = Color.rgb(15, 81, 50) // #0F5132 زمردي ملكي
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val paintTextWhite = Paint().apply {
            color = Color.WHITE
            typeface = tajawalBold
            textSize = 15f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val paintSubWhite = Paint().apply {
            color = Color.rgb(220, 240, 225)
            typeface = tajawalRegular
            textSize = 10f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val paintDateLeft = Paint().apply {
            color = Color.rgb(230, 245, 235)
            typeface = tajawalRegular
            textSize = 8.5f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val paintCellText = Paint().apply {
            color = Color.rgb(33, 33, 33)
            typeface = tajawalRegular
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val paintCellCenter = Paint().apply {
            color = Color.rgb(33, 33, 33)
            typeface = tajawalRegular
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintTableHeader = Paint().apply {
            color = Color.WHITE
            typeface = tajawalBold
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintBorder = Paint().apply {
            color = Color.rgb(220, 220, 220)
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
            isAntiAlias = true
        }

        val paintZebra = Paint().apply {
            color = Color.rgb(246, 249, 247)
            style = Paint.Style.FILL
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // 1. رسم الترويسة الرئيسية للصفحة الأولى
        drawHeader(canvas, paintEmerald, paintTextWhite, paintSubWhite, paintDateLeft, reportTitle, periodTitle, currencySymbol)

        // 2. رسم بطاقات الملخص المالي
        var currentY = 158f
        drawSummaryCards(canvas, tajawalBold, tajawalRegular, totalExpenses, totalIncome, netBalance, currencySymbol)

        // 3. جدول العمليات
        currentY = 172f
        drawTableHeader(canvas, currentY, paintEmerald, paintTableHeader, paintBorder)
        currentY += 24f

        val rowHeight = 22f
        val maxY = 790f

        // تقسيم الأعمدة (من اليمين إلى اليسار RTL):
        // م: 565 -> 535 (30)
        // التاريخ واليوم: 535 -> 440 (95)
        // النوع: 440 -> 390 (50)
        // التصنيف: 390 -> 310 (80)
        // المبلغ: 310 -> 220 (90)
        // الملاحظات: 220 -> 30 (190)

        for (i in transactions.indices) {
            val tx = transactions[i]

            // التحقق من الحاجة لإنشاء صفحة جديدة
            if (currentY + rowHeight > maxY) {
                drawFooter(canvas, pageNumber, tajawalRegular)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                // ترويسة مختصرة للصفحات المتتالية
                drawContinuationHeader(canvas, paintEmerald, paintTextWhite, paintSubWhite, reportTitle, periodTitle)
                currentY = 60f
                drawTableHeader(canvas, currentY, paintEmerald, paintTableHeader, paintBorder)
                currentY += 24f
            }

            // تظليل السطر بالتناوب
            if (i % 2 == 1) {
                canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + rowHeight, paintZebra)
            }

            // خطوط خلايا السطر
            canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + rowHeight, paintBorder)
            canvas.drawLine(535f, currentY, 535f, currentY + rowHeight, paintBorder)
            canvas.drawLine(440f, currentY, 440f, currentY + rowHeight, paintBorder)
            canvas.drawLine(390f, currentY, 390f, currentY + rowHeight, paintBorder)
            canvas.drawLine(310f, currentY, 310f, currentY + rowHeight, paintBorder)
            canvas.drawLine(220f, currentY, 220f, currentY + rowHeight, paintBorder)

            val textBaseline = currentY + 15f

            // م (التسلسل)
            canvas.drawText("${i + 1}", 550f, textBaseline, paintCellCenter)

            // التاريخ واليوم
            val formattedDate = try {
                FormatUtils.formatArabicDate(tx.toLocalDate(), includeDayOfWeek = true)
            } catch (_: Exception) {
                tx.transactionDate
            }
            canvas.drawText(formattedDate, 530f, textBaseline, paintCellText)

            // النوع
            val isExpense = tx.type == TransactionType.EXPENSE
            val typeText = if (isExpense) "مصروف" else "إيراد"
            val paintType = Paint(paintCellCenter).apply {
                color = if (isExpense) Color.rgb(198, 40, 40) else Color.rgb(46, 125, 50)
                typeface = tajawalBold
            }
            canvas.drawText(typeText, 415f, textBaseline, paintType)

            // التصنيف
            val catName = tx.categoryName ?: "عام"
            val categoryText = if (catName.length > 14) catName.take(13) + "…" else catName
            canvas.drawText(categoryText, 385f, textBaseline, paintCellText)

            // المبلغ
            val amountFormatted = "${if (isExpense) "-" else "+"} ${formatNumber(tx.amount)}"
            val paintAmount = Paint(paintCellText).apply {
                color = if (isExpense) Color.rgb(198, 40, 40) else Color.rgb(46, 125, 50)
                typeface = tajawalBold
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(amountFormatted, 265f, textBaseline, paintAmount)

            // الملاحظات
            val noteText = if (tx.note.isNullOrBlank()) "—" else {
                if (tx.note.length > 32) tx.note.take(31) + "…" else tx.note
            }
            canvas.drawText(noteText, 215f, textBaseline, paintCellText)

            currentY += rowHeight
        }

        // رسم تذييل الصفحة الأخيرة
        drawFooter(canvas, pageNumber, tajawalRegular)
        pdfDocument.finishPage(page)

        // حفظ الملف
        val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
        val fileName = "Masroof_Report_$timeStamp.pdf"

        val targetFile = customOutputFile ?: run {
            val localDir = File(context.getExternalFilesDir(null), "reports")
            if (!localDir.exists()) localDir.mkdirs()
            File(localDir, fileName)
        }

        FileOutputStream(targetFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        // حفظ نسخة إضافية تلقائياً في مجلد التنزيلات العام Downloads/Masroof
        savePdfToPublicDownloads(context, targetFile, fileName)

        return targetFile
    }

    private fun drawHeader(
        canvas: Canvas,
        paintBg: Paint,
        paintTitle: Paint,
        paintSub: Paint,
        paintDate: Paint,
        reportTitle: String,
        periodTitle: String,
        currencySymbol: String
    ) {
        // شريط الترويسة الزمردي
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 82f, paintBg)

        // اسم التطبيق والعنوان
        canvas.drawText("تطبيق مصروف | Masroof", MARGIN_RIGHT, 32f, paintTitle)
        canvas.drawText(reportTitle, MARGIN_RIGHT, 48f, paintSub)
        canvas.drawText("الفترة: $periodTitle • العملة المعتمدة: $currencySymbol", MARGIN_RIGHT, 65f, paintSub)

        // تاريخ وتوقيت التصدير باليسار
        val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        canvas.drawText("تاريخ التصدير:", MARGIN_LEFT, 45f, paintDate)
        canvas.drawText(nowStr, MARGIN_LEFT, 60f, paintDate)
    }

    private fun drawContinuationHeader(
        canvas: Canvas,
        paintBg: Paint,
        paintTitle: Paint,
        paintSub: Paint,
        reportTitle: String,
        periodTitle: String
    ) {
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 42f, paintBg)
        val continuationPaint = Paint(paintTitle).apply { textSize = 11f }
        canvas.drawText("$reportTitle (تابع) • $periodTitle", MARGIN_RIGHT, 26f, continuationPaint)
    }

    private fun drawSummaryCards(
        canvas: Canvas,
        bold: Typeface,
        regular: Typeface,
        expenses: Long,
        income: Long,
        net: Long,
        currency: String
    ) {
        val cardWidth = 171f
        val cardHeight = 52f
        val top = 94f

        // بطاقة 1: الإيرادات (باليمين)
        val r1Right = MARGIN_RIGHT
        val r1Left = r1Right - cardWidth
        drawSingleSummaryCard(
            canvas = canvas,
            left = r1Left,
            top = top,
            right = r1Right,
            bottom = top + cardHeight,
            bgColor = Color.rgb(232, 245, 233),
            borderColor = Color.rgb(129, 199, 132),
            textColor = Color.rgb(46, 125, 50),
            title = "إجمالي الإيرادات",
            value = "+ ${formatNumber(income)} $currency",
            bold = bold,
            regular = regular
        )

        // بطاقة 2: المصروفات (بالوسط)
        val r2Right = r1Left - 11f
        val r2Left = r2Right - cardWidth
        drawSingleSummaryCard(
            canvas = canvas,
            left = r2Left,
            top = top,
            right = r2Right,
            bottom = top + cardHeight,
            bgColor = Color.rgb(255, 235, 238),
            borderColor = Color.rgb(229, 115, 115),
            textColor = Color.rgb(198, 40, 40),
            title = "إجمالي المصروفات",
            value = "- ${formatNumber(expenses)} $currency",
            bold = bold,
            regular = regular
        )

        // بطاقة 3: الصافي (باليسار)
        val r3Right = r2Left - 11f
        val r3Left = MARGIN_LEFT
        val netColor = if (net >= 0) Color.rgb(0, 105, 92) else Color.rgb(198, 40, 40)
        val netBg = if (net >= 0) Color.rgb(224, 242, 241) else Color.rgb(255, 235, 238)
        val netBorder = if (net >= 0) Color.rgb(77, 182, 172) else Color.rgb(229, 115, 115)
        drawSingleSummaryCard(
            canvas = canvas,
            left = r3Left,
            top = top,
            right = r3Right,
            bottom = top + cardHeight,
            bgColor = netBg,
            borderColor = netBorder,
            textColor = netColor,
            title = "الرصيد الصافي للفترة",
            value = "${if (net >= 0) "+" else "-"} ${formatNumber(abs(net))} $currency",
            bold = bold,
            regular = regular
        )
    }

    private fun drawSingleSummaryCard(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        bgColor: Int,
        borderColor: Int,
        textColor: Int,
        title: String,
        value: String,
        bold: Typeface,
        regular: Typeface
    ) {
        val rect = RectF(left, top, right, bottom)
        val fillPaint = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val strokePaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
        canvas.drawRoundRect(rect, 8f, 8f, strokePaint)

        val titlePaint = Paint().apply {
            color = Color.rgb(80, 80, 80)
            typeface = regular
            textSize = 9f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = textColor
            typeface = bold
            textSize = 11.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val centerX = (left + right) / 2f
        canvas.drawText(title, centerX, top + 18f, titlePaint)
        canvas.drawText(value, centerX, top + 38f, valuePaint)
    }

    private fun drawTableHeader(
        canvas: Canvas,
        top: Float,
        paintBg: Paint,
        paintText: Paint,
        paintBorder: Paint
    ) {
        val height = 24f
        val bottom = top + height

        // خلفية ترويسة الجدول
        canvas.drawRect(MARGIN_LEFT, top, MARGIN_RIGHT, bottom, paintBg)
        canvas.drawRect(MARGIN_LEFT, top, MARGIN_RIGHT, bottom, paintBorder)

        // فواصل الأعمدة
        canvas.drawLine(535f, top, 535f, bottom, paintBorder)
        canvas.drawLine(440f, top, 440f, bottom, paintBorder)
        canvas.drawLine(390f, top, 390f, bottom, paintBorder)
        canvas.drawLine(310f, top, 310f, bottom, paintBorder)
        canvas.drawLine(220f, top, 220f, bottom, paintBorder)

        val textY = top + 16f
        canvas.drawText("م", 550f, textY, paintText)
        canvas.drawText("التاريخ واليوم", 487.5f, textY, paintText)
        canvas.drawText("النوع", 415f, textY, paintText)
        canvas.drawText("التصنيف", 350f, textY, paintText)
        canvas.drawText("المبلغ", 265f, textY, paintText)
        canvas.drawText("الملاحظات والوصف", 125f, textY, paintText)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, regular: Typeface) {
        val y = 815f
        val paintLine = Paint().apply {
            color = Color.rgb(210, 210, 210)
            strokeWidth = 0.6f
        }
        canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLine)

        val paintTextRight = Paint().apply {
            color = Color.rgb(120, 120, 120)
            typeface = regular
            textSize = 8f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val paintTextLeft = Paint().apply {
            color = Color.rgb(120, 120, 120)
            typeface = regular
            textSize = 8f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        canvas.drawText("تم التصدير عبر تطبيق مصروف (Masroof) • الحل المالي الذكي لإدارة المصروفات", MARGIN_RIGHT, y + 14f, paintTextRight)
        canvas.drawText("صفحة $pageNumber", MARGIN_LEFT, y + 14f, paintTextLeft)
    }

    /**
     * حفظ نسخة من ملف PDF في مجلد التنزيلات العام Downloads/Masroof
     */
    private fun savePdfToPublicDownloads(context: Context, sourceFile: File, fileName: String): String {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Masroof")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    return "Downloads/Masroof/$fileName"
                }
            }
        } catch (_: Exception) {}

        return try {
            val pubDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val masroofDir = File(pubDownloads, "Masroof")
            if (!masroofDir.exists()) masroofDir.mkdirs()
            val target = File(masroofDir, fileName)
            sourceFile.copyTo(target, overwrite = true)
            target.absolutePath
        } catch (_: Exception) {
            sourceFile.absolutePath
        }
    }

    /**
     * فتح ملف PDF مباشرة وتلقائياً عبر Intent.ACTION_VIEW
     */
    fun openPdf(context: Context, pdfFile: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "com.masroofi.app.fileprovider", pdfFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "عرض تقرير مصروف المالي")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * مشاركة ملف PDF مباشرة عبر واتساب، تيليجرام، أو البريد الإلكتروني
     */
    fun sharePdf(context: Context, pdfFile: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "com.masroofi.app.fileprovider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "تقرير مصروف المالي")
            putExtra(Intent.EXTRA_TEXT, "مرفق تقرير العمليات المالية الصادر من تطبيق مصروف.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "مشاركة تقرير PDF عبر")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
