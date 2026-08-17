package com.trainpaths.nonogram.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val redo: ImageVector
    get() {
        if (_redo != null) {
            return _redo!!
        }
        _redo =
            ImageVector.Builder(
                name = "redo",
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
                        moveTo(18.4f, 10.6f)
                        curveTo(16.55f, 9f, 14.15f, 8f, 11.5f, 8f)
                        curveToRelative(-4.65f, 0f, -8.58f, 3.03f, -9.96f, 7.22f)
                        lineTo(3.9f, 16f)
                        curveToRelative(1.05f, -3.19f, 4.06f, -5.5f, 7.6f, -5.5f)
                        curveToRelative(1.95f, 0f, 3.73f, 0.72f, 5.12f, 1.88f)
                        lineTo(13f, 16f)
                        horizontalLineToRelative(9f)
                        verticalLineTo(7f)
                        lineToRelative(-3.6f, 3.6f)
                        close()
                    }
                }
                .build()
        return _redo!!
    }

private var _redo: ImageVector? = null
