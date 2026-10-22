package com.example.rcarpremium

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.max

/** Gráficos nativos desenhados em Canvas para não exigir uma dependência externa. */
class RcarAnalyticsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Mode { REVENUE, FUNNEL }

    private var mode = Mode.REVENUE
    private var labels: List<String> = emptyList()
    private var revenue: List<Double> = emptyList()
    private var carts: List<Int> = emptyList()
    private var purchases: List<Int> = emptyList()
    private var abandoned: List<Int> = emptyList()

    private val orange = Color.rgb(255, 112, 0)
    private val green = Color.rgb(38, 195, 135)
    private val red = Color.rgb(255, 82, 82)
    private val gridColor = Color.rgb(52, 52, 56)
    private val textColor = Color.rgb(175, 175, 182)
    private val mutedColor = Color.rgb(110, 110, 118)

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gridColor
        strokeWidth = dp(1).toFloat()
        style = Paint.Style.STROKE
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = sp(10f)
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = orange; style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val noDataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mutedColor
        textSize = sp(12f)
        textAlign = Paint.Align.CENTER
    }

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        contentDescription = "Gráfico de análises RCAR Premium"
    }

    fun setRevenueData(newLabels: List<String>, values: List<Double>) {
        mode = Mode.REVENUE
        labels = newLabels
        revenue = values
        invalidate()
    }

    fun setFunnelData(newLabels: List<String>, newCarts: List<Int>, newPurchases: List<Int>, newAbandoned: List<Int>) {
        mode = Mode.FUNNEL
        labels = newLabels
        carts = newCarts
        purchases = newPurchases
        abandoned = newAbandoned
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        when (mode) {
            Mode.REVENUE -> drawRevenue(canvas)
            Mode.FUNNEL -> drawFunnel(canvas)
        }
    }

    private fun drawRevenue(canvas: Canvas) {
        val values = revenue
        val count = minOf(labels.size, values.size)
        if (count == 0) {
            canvas.drawText("Aguardando dados de vendas", width / 2f, height / 2f, noDataPaint)
            return
        }
        val left = dp(48).toFloat()
        val right = width - dp(12).toFloat()
        val top = dp(18).toFloat()
        val bottom = height - dp(36).toFloat()
        if (right <= left || bottom <= top) return
        val maxVal = max(1.0, values.take(count).maxOrNull() ?: 0.0) * 1.18
        for (step in 0..4) {
            val y = bottom - (bottom - top) * step / 4f
            canvas.drawLine(left, y, right, y, gridPaint)
            val axisVal = maxVal * step / 4.0
            canvas.drawText(shortCurrency(axisVal), left - dp(7f), y + dp(3f), axisPaint.apply { textAlign = Paint.Align.RIGHT })
        }
        val slot = (right - left) / count
        val barWidth = minOf(dp(28f), slot * 0.52f)
        values.take(count).forEachIndexed { index, value ->
            val safeValue = value.coerceAtLeast(0.0)
            val barHeight = ((bottom - top) * safeValue / maxVal).toFloat()
            val xCenter = left + slot * (index + 0.5f)
            val rect = RectF(xCenter - barWidth / 2f, bottom - barHeight, xCenter + barWidth / 2f, bottom)
            canvas.drawRoundRect(rect, dp(4f), dp(4f), barPaint)
            if (count <= 7 || index % 2 == 0) {
                axisPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(labels[index], xCenter, height - dp(12f), axisPaint)
            }
        }
        if (values.take(count).all { it <= 0.0 }) {
            canvas.drawText("Nenhuma venda registrada no período", (left + right) / 2f, (top + bottom) / 2f, noDataPaint)
        }
    }

    private fun drawFunnel(canvas: Canvas) {
        val count = minOf(labels.size, carts.size, purchases.size, abandoned.size)
        if (count == 0) {
            canvas.drawText("Aguardando atividade de compra", width / 2f, height / 2f, noDataPaint)
            return
        }
        val left = dp(34).toFloat()
        val right = width - dp(10).toFloat()
        val top = dp(16).toFloat()
        val bottom = height - dp(32).toFloat()
        if (right <= left || bottom <= top) return
        val all = carts.take(count) + purchases.take(count) + abandoned.take(count)
        val maxVal = max(1, all.maxOrNull() ?: 0).toFloat()
        for (step in 0..4) {
            val y = bottom - (bottom - top) * step / 4f
            canvas.drawLine(left, y, right, y, gridPaint)
            axisPaint.textAlign = Paint.Align.RIGHT
            val label = (maxVal * step / 4f).toInt().toString()
            canvas.drawText(label, left - dp(7f), y + dp(3f), axisPaint)
        }
        val slot = if (count > 1) (right - left) / (count - 1) else right - left
        drawSeries(canvas, carts.take(count), count, maxVal, left, top, bottom, slot, orange)
        drawSeries(canvas, purchases.take(count), count, maxVal, left, top, bottom, slot, green)
        drawSeries(canvas, abandoned.take(count), count, maxVal, left, top, bottom, slot, red)
        labels.take(count).forEachIndexed { index, label ->
            if (index % 4 == 0 || index == count - 1) {
                val x = left + slot * index
                axisPaint.textAlign = when (index) { 0 -> Paint.Align.LEFT; count - 1 -> Paint.Align.RIGHT; else -> Paint.Align.CENTER }
                canvas.drawText(label, x, height - dp(10f), axisPaint)
            }
        }
        if (all.all { it == 0 }) {
            canvas.drawText("Os eventos aparecerão conforme os clientes comprarem", (left + right) / 2f, (top + bottom) / 2f, noDataPaint)
        }
    }

    private fun drawSeries(
        canvas: Canvas,
        values: List<Int>,
        count: Int,
        maxValue: Float,
        left: Float,
        top: Float,
        bottom: Float,
        slot: Float,
        color: Int
    ) {
        if (count == 0) return
        val path = Path()
        values.take(count).forEachIndexed { index, value ->
            val x = left + slot * index
            val y = bottom - (bottom - top) * value.coerceAtLeast(0) / maxValue
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        linePaint.color = color
        canvas.drawPath(path, linePaint)
        dotPaint.color = color
        values.take(count).forEachIndexed { index, value ->
            if (value > 0 && (count < 12 || index % 2 == 0)) {
                val x = left + slot * index
                val y = bottom - (bottom - top) * value.coerceAtLeast(0) / maxValue
                canvas.drawCircle(x, y, dp(3f), dotPaint)
            }
        }
    }

    private fun shortCurrency(value: Double): String = when {
        value >= 1000 -> String.format(Locale("pt", "BR"), "R$%.1fk", value / 1000.0)
        else -> String.format(Locale("pt", "BR"), "R$%.0f", value)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
