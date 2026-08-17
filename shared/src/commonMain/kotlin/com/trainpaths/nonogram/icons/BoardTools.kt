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
val tileFill: ImageVector
    get() {
        if (_tileFill != null) return _tileFill!!
        _tileFill = toolIcon("tile_fill") {
            moveTo(3f, 3f)
            horizontalLineTo(21f)
            verticalLineTo(21f)
            horizontalLineTo(3f)
            close()
        }
        return _tileFill!!
    }

@Suppress("CheckReturnValue")
val tileCross: ImageVector
    get() {
        if (_tileCross != null) return _tileCross!!
        _tileCross = toolIcon("tile_cross", fillType = PathFillType.EvenOdd) {
            squareOutline()
            // One traced X outline, not two crossed bars: under even-odd two overlapping bars would
            // cancel where they meet and punch a hole through the middle of the cross. Sits inside
            // the ring's hole, so even-odd never subtracts it either.
            moveTo(17f, 8f)
            lineTo(16f, 7f)
            lineTo(12f, 11f)
            lineTo(8f, 7f)
            lineTo(7f, 8f)
            lineTo(11f, 12f)
            lineTo(7f, 16f)
            lineTo(8f, 17f)
            lineTo(12f, 13f)
            lineTo(16f, 17f)
            lineTo(17f, 16f)
            lineTo(13f, 12f)
            close()
        }
        return _tileCross!!
    }

@Suppress("CheckReturnValue")
val tileErase: ImageVector
    get() {
        if (_tileErase != null) return _tileErase!!
        _tileErase = toolIcon("tile_erase", fillType = PathFillType.EvenOdd) {
            squareOutline()
        }
        return _tileErase!!
    }

@Suppress("CheckReturnValue")
public val expand_content: ImageVector
    get() {
        if (_expand_content != null) {
            return _expand_content!!
        }
        _expand_content =
            ImageVector.Builder(
                name = "expand_content",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(5f, 19f)
                        verticalLineTo(13f)
                        horizontalLineTo(7f)
                        verticalLineToRelative(4f)
                        horizontalLineToRelative(4f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(5f)
                        close()
                        moveTo(17f, 11f)
                        verticalLineTo(7f)
                        horizontalLineTo(13f)
                        verticalLineTo(5f)
                        horizontalLineToRelative(6f)
                        verticalLineToRelative(6f)
                        horizontalLineTo(17f)
                        close()
                    }
                }
                .build()
        return _expand_content!!
    }

@Suppress("CheckReturnValue")
public val stylus: ImageVector
    get() {
        if (_stylus != null) {
            return _stylus!!
        }
        _stylus =
            ImageVector.Builder(
                name = "stylus",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(4.18f, 21f)
                        quadTo(3.65f, 21.13f, 3.26f, 20.74f)
                        reflectiveQuadTo(3f, 19.83f)
                        lineTo(4f, 15.05f)
                        lineTo(8.95f, 20f)
                        lineTo(4.18f, 21f)
                        close()
                        moveTo(8.95f, 20f)
                        lineTo(4f, 15.05f)
                        lineTo(15.45f, 3.6f)
                        quadTo(16.03f, 3.02f, 16.88f, 3.02f)
                        quadToRelative(0.85f, 0f, 1.43f, 0.57f)
                        lineToRelative(2.1f, 2.1f)
                        quadToRelative(0.57f, 0.57f, 0.57f, 1.43f)
                        quadToRelative(0f, 0.85f, -0.57f, 1.42f)
                        lineTo(8.95f, 20f)
                        close()
                        moveTo(16.88f, 5f)
                        lineTo(6.53f, 15.35f)
                        lineToRelative(2.13f, 2.13f)
                        lineTo(19f, 7.13f)
                        lineTo(16.88f, 5f)
                        close()
                    }
                }
                .build()
        return _stylus!!
    }

private inline fun toolIcon(
    name: String,
    fillType: PathFillType = PathFillType.NonZero,
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
            pathFillType = fillType,
        ) { pathData() }
    }.build()

@Suppress("CheckReturnValue")
private fun PathBuilder.squareOutline(inset: Float = 3f, thickness: Float = 2f) {
    val outer = 24f - inset
    val innerStart = inset + thickness
    val innerEnd = outer - thickness

    moveTo(inset, inset)
    horizontalLineTo(outer)
    verticalLineTo(outer)
    horizontalLineTo(inset)
    close()

    moveTo(innerStart, innerStart)
    horizontalLineTo(innerEnd)
    verticalLineTo(innerEnd)
    horizontalLineTo(innerStart)
    close()
}

private var _lockClosed: ImageVector? = null
private var _lockOpen: ImageVector? = null
private var _save: ImageVector? = null
private var _stylus: ImageVector? = null
private var _tileFill: ImageVector? = null
private var _tileCross: ImageVector? = null
private var _tileErase: ImageVector? = null
private var _expand_content: ImageVector? = null

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
