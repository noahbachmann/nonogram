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
val scan: ImageVector
    get() {
        if (_scan != null) {
            return _scan!!
        }
        _scan =
            ImageVector.Builder(
                name = "scan",
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
                        // The mountain is a second subpath knocked out of the frame.
                        pathFillType = PathFillType.EvenOdd,
                    ) {
                        moveTo(21f, 19f)
                        verticalLineTo(5f)
                        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                        horizontalLineTo(5f)
                        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                        verticalLineToRelative(14f)
                        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                        horizontalLineToRelative(14f)
                        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                        close()
                        moveTo(8.5f, 13.5f)
                        lineToRelative(2.5f, 3.01f)
                        lineTo(14.5f, 12f)
                        lineToRelative(4.5f, 6f)
                        horizontalLineTo(5f)
                        close()
                    }
                }
                .build()
        return _scan!!
    }

private var _scan: ImageVector? = null
