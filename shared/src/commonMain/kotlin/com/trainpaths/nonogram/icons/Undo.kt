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
public val undo: ImageVector
    get() {
        if (_undo != null) {
            return _undo!!
        }
        _undo =
            ImageVector.Builder(
                name = "undo",
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
                        moveTo(12.5f, 8f)
                        curveToRelative(-2.65f, 0f, -5.05f, 1f, -6.9f, 2.6f)
                        lineTo(2f, 7f)
                        verticalLineToRelative(9f)
                        horizontalLineToRelative(9f)
                        lineToRelative(-3.62f, -3.62f)
                        curveToRelative(1.39f, -1.16f, 3.16f, -1.88f, 5.12f, -1.88f)
                        curveToRelative(3.54f, 0f, 6.55f, 2.31f, 7.6f, 5.5f)
                        lineToRelative(2.37f, -0.78f)
                        curveTo(21.08f, 11.03f, 17.15f, 8f, 12.5f, 8f)
                        close()
                    }
                }
                .build()
        return _undo!!
    }

private var _undo: ImageVector? = null
