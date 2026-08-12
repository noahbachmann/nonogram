package com.trainpaths.nonogram.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val lockClosed: ImageVector
    get() {
        if (_lockClosed != null) return _lockClosed!!
        _lockClosed = toolIcon("lock_closed") {
            moveTo(18f, 8f)
            horizontalLineTo(17f)
            verticalLineTo(6f)
            cubicTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
            reflectiveCubicTo(7f, 3.24f, 7f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(6f)
            cubicTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
            verticalLineTo(20f)
            cubicTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            horizontalLineTo(18f)
            cubicTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(10f)
            cubicTo(20f, 8.9f, 19.1f, 8f, 18f, 8f)
            close()
            moveTo(9f, 6f)
            cubicTo(9f, 4.34f, 10.34f, 3f, 12f, 3f)
            reflectiveCubicTo(15f, 4.34f, 15f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(9f)
            close()
        }
        return _lockClosed!!
    }

@Suppress("CheckReturnValue")
val lockOpen: ImageVector
    get() {
        if (_lockOpen != null) return _lockOpen!!
        _lockOpen = toolIcon("lock_open") {
            moveTo(18f, 8f)
            horizontalLineTo(10f)
            verticalLineTo(6f)
            cubicTo(10f, 4.9f, 9.1f, 4f, 8f, 4f)
            reflectiveCubicTo(6f, 4.9f, 6f, 6f)
            horizontalLineTo(4f)
            cubicTo(4f, 3.79f, 5.79f, 2f, 8f, 2f)
            reflectiveCubicTo(12f, 3.79f, 12f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(18f)
            cubicTo(19.1f, 8f, 20f, 8.9f, 20f, 10f)
            verticalLineTo(20f)
            cubicTo(20f, 21.1f, 19.1f, 22f, 18f, 22f)
            horizontalLineTo(6f)
            cubicTo(4.9f, 22f, 4f, 21.1f, 4f, 20f)
            verticalLineTo(10f)
            cubicTo(4f, 8.9f, 4.9f, 8f, 6f, 8f)
            close()
        }
        return _lockOpen!!
    }

@Suppress("CheckReturnValue")
val save: ImageVector
    get() {
        if (_save != null) return _save!!
        _save = toolIcon("save") {
            moveTo(17f, 3f)
            horizontalLineTo(5f)
            cubicTo(3.89f, 3f, 3f, 3.9f, 3f, 5f)
            verticalLineTo(19f)
            cubicTo(3f, 20.1f, 3.89f, 21f, 5f, 21f)
            horizontalLineTo(19f)
            cubicTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
            verticalLineTo(7f)
            close()
            moveTo(12f, 19f)
            cubicTo(10.34f, 19f, 9f, 17.66f, 9f, 16f)
            reflectiveCubicTo(10.34f, 13f, 12f, 13f)
            reflectiveCubicTo(15f, 14.34f, 15f, 16f)
            reflectiveCubicTo(13.66f, 19f, 12f, 19f)
            close()
            moveTo(6f, 5f)
            horizontalLineTo(15f)
            verticalLineTo(9f)
            horizontalLineTo(6f)
            close()
        }
        return _save!!
    }

@Suppress("CheckReturnValue")
val moreHorizontal: ImageVector
    get() {
        if (_moreHorizontal != null) return _moreHorizontal!!
        _moreHorizontal = toolIcon("more_horizontal") {
            moveTo(6f, 10f)
            cubicTo(4.9f, 10f, 4f, 10.9f, 4f, 12f)
            reflectiveCubicTo(4.9f, 14f, 6f, 14f)
            reflectiveCubicTo(8f, 13.1f, 8f, 12f)
            reflectiveCubicTo(7.1f, 10f, 6f, 10f)
            close()
            moveTo(12f, 10f)
            cubicTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
            reflectiveCubicTo(10.9f, 14f, 12f, 14f)
            reflectiveCubicTo(14f, 13.1f, 14f, 12f)
            reflectiveCubicTo(13.1f, 10f, 12f, 10f)
            close()
            moveTo(18f, 10f)
            cubicTo(16.9f, 10f, 16f, 10.9f, 16f, 12f)
            reflectiveCubicTo(16.9f, 14f, 18f, 14f)
            reflectiveCubicTo(20f, 13.1f, 20f, 12f)
            reflectiveCubicTo(19.1f, 10f, 18f, 10f)
            close()
        }
        return _moreHorizontal!!
    }

private inline fun toolIcon(
    name: String,
    crossinline pathData: PathBuilder.() -> Unit,
): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
        ) { pathData() }
    }.build()

private var _lockClosed: ImageVector? = null
private var _lockOpen: ImageVector? = null
private var _save: ImageVector? = null
private var _moreHorizontal: ImageVector? = null

private fun PathBuilder.cubicTo(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    x3: Float,
    y3: Float,
) = curveTo(x1, y1, x2, y2, x3, y3)

private fun PathBuilder.reflectiveCubicTo(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
) = reflectiveCurveTo(x1, y1, x2, y2)
