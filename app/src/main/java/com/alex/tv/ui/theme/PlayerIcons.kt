package com.alex.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object PlayerIcons {
    val Play: ImageVector
        get() = ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(6f, 4f)
            verticalLineToRelative(16f)
            curveToRelative(0f, 0.552f, 0.448f, 1f, 1f, 1f)
            curveToRelative(0.184f, 0f, 0.364f, -0.051f, 0.524f, -0.148f)
            lineToRelative(13f, -8f)
            curveToRelative(0.468f, -0.288f, 0.614f, -0.902f, 0.326f, -1.37f)
            curveToRelative(-0.082f, -0.133f, -0.193f, -0.244f, -0.326f, -0.326f)
            lineToRelative(-13f, -8f)
            curveToRelative(-0.468f, -0.288f, -1.082f, -0.142f, -1.37f, 0.326f)
            curveToRelative(-0.097f, 0.16f, -0.148f, 0.34f, -0.148f, 0.524f)
            close()
        }.build()

    val Pause: ImageVector
        get() = ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(9f, 4f)
            horizontalLineToRelative(-2f)
            curveToRelative(-1.105f, 0f, -2f, 0.895f, -2f, 2f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.105f, 0.895f, 2f, 2f, 2f)
            horizontalLineToRelative(2f)
            curveToRelative(1.105f, 0f, 2f, -0.895f, 2f, -2f)
            verticalLineToRelative(-12f)
            curveToRelative(0f, -1.105f, -0.895f, -2f, -2f, -2f)
            close()
            moveTo(17f, 4f)
            horizontalLineToRelative(-2f)
            curveToRelative(-1.105f, 0f, -2f, 0.895f, -2f, 2f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.105f, 0.895f, 2f, 2f, 2f)
            horizontalLineToRelative(2f)
            curveToRelative(1.105f, 0f, 2f, -0.895f, 2f, -2f)
            verticalLineToRelative(-12f)
            curveToRelative(0f, -1.105f, -0.895f, -2f, -2f, -2f)
            close()
        }.build()

    val Rewind: ImageVector
        get() = ImageVector.Builder(
            name = "Rewind",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(20.341f, 4.247f)
            lineToRelative(-8f, 7f)
            curveToRelative(-0.457f, 0.4f, -0.457f, 1.106f, 0f, 1.506f)
            lineToRelative(8f, 7f)
            curveToRelative(0.647f, 0.565f, 1.659f, 0.106f, 1.659f, -0.753f)
            verticalLineToRelative(-14f)
            curveToRelative(0f, -0.86f, -1.012f, -1.318f, -1.659f, -0.753f)
            close()
            moveTo(9.341f, 4.247f)
            lineToRelative(-8f, 7f)
            curveToRelative(-0.457f, 0.4f, -0.457f, 1.106f, 0f, 1.506f)
            lineToRelative(8f, 7f)
            curveToRelative(0.647f, 0.565f, 1.659f, 0.106f, 1.659f, -0.753f)
            verticalLineToRelative(-14f)
            curveToRelative(0f, -0.86f, -1.012f, -1.318f, -1.659f, -0.753f)
            close()
        }.build()

    val Forward: ImageVector
        get() = ImageVector.Builder(
            name = "Forward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(2f, 5f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 0.86f, 1.012f, 1.318f, 1.659f, 0.753f)
            lineToRelative(8f, -7f)
            curveToRelative(0.457f, -0.4f, 0.457f, -1.106f, 0f, -1.506f)
            lineToRelative(-8f, -7f)
            curveToRelative(-0.647f, -0.565f, -1.659f, -0.106f, -1.659f, 0.753f)
            close()
            moveTo(13f, 5f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 0.86f, 1.012f, 1.318f, 1.659f, 0.753f)
            lineToRelative(8f, -7f)
            curveToRelative(0.457f, -0.4f, 0.457f, -1.106f, 0f, -1.506f)
            lineToRelative(-8f, -7f)
            curveToRelative(-0.647f, -0.565f, -1.659f, -0.106f, -1.659f, 0.753f)
            close()
        }.build()

    val Settings: ImageVector
        get() = ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(14.647f, 4.081f)
            arcToRelative(0.724f, 0.724f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1.08f, 0.448f)
            curveToRelative(2.439f, -1.485f, 5.23f, 1.305f, 3.745f, 3.744f)
            arcToRelative(0.724f, 0.724f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.447f, 1.08f)
            curveToRelative(2.775f, 0.673f, 2.775f, 4.62f, 0f, 5.294f)
            arcToRelative(0.724f, 0.724f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.448f, 1.08f)
            curveToRelative(1.485f, 2.439f, -1.305f, 5.23f, -3.744f, 3.745f)
            arcToRelative(0.724f, 0.724f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.08f, 0.447f)
            curveToRelative(-0.673f, 2.775f, -4.62f, 2.775f, -5.294f, 0f)
            arcToRelative(0.724f, 0.724f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.08f, -0.448f)
            curveToRelative(-2.439f, 1.485f, -5.23f, -1.305f, -3.745f, -3.744f)
            arcToRelative(0.724f, 0.724f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.447f, -1.08f)
            curveToRelative(-2.775f, -0.673f, -2.775f, -4.62f, 0f, -5.294f)
            arcToRelative(0.724f, 0.724f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.448f, -1.08f)
            curveToRelative(-1.485f, -2.439f, 1.305f, -5.23f, 3.744f, -3.745f)
            arcToRelative(0.722f, 0.722f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1.08f, -0.447f)
            curveToRelative(0.673f, -2.775f, 4.62f, -2.775f, 5.294f, 0f)
            close()
            moveTo(12f, 9f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 0f, 6f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -6f)
            close()
        }.build()

    val Home: ImageVector
        get() = ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(12.707f, 2.293f)
            lineToRelative(9f, 9f)
            curveToRelative(0.63f, 0.63f, 0.184f, 1.707f, -0.707f, 1.707f)
            horizontalLineToRelative(-1f)
            verticalLineToRelative(6f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3f, 3f)
            horizontalLineToRelative(-1f)
            verticalLineToRelative(-7f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2.824f, -2.995f)
            lineToRelative(-0.176f, -0.005f)
            horizontalLineToRelative(-2f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, -3f, 3f)
            verticalLineToRelative(7f)
            horizontalLineToRelative(-1f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3f, -3f)
            verticalLineToRelative(-6f)
            horizontalLineToRelative(-1f)
            curveToRelative(-0.89f, 0f, -1.337f, -1.077f, -0.707f, -1.707f)
            lineToRelative(9f, -9f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1.414f, 0f)
            moveToRelative(0.293f, 11.707f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, 1f)
            verticalLineToRelative(7f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(-7f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.883f, -0.993f)
            lineToRelative(0.117f, -0.007f)
            close()
        }.build()

    val Update: ImageVector
        get() = ImageVector.Builder(
            name = "Update",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(17f, 2f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 5f)
            verticalLineToRelative(10f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, -5f, 5f)
            horizontalLineToRelative(-10f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, -5f, -5f)
            verticalLineToRelative(-10f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, -5f)
            close()
            moveTo(16f, 14f)
            horizontalLineToRelative(-3f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 2f)
            horizontalLineToRelative(3f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -2f)
            moveToRelative(-7.293f, -5.707f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.414f, 0f)
            lineToRelative(-0.083f, 0.094f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.083f, 1.32f)
            lineToRelative(2.292f, 2.293f)
            lineToRelative(-2.292f, 2.293f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1.414f, 1.414f)
            lineToRelative(3f, -3f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -1.414f)
            close()
        }.build()

    val Tv: ImageVector
        get() = ImageVector.Builder(
            name = "Tv",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(8.707f, 2.293f)
            lineToRelative(3.293f, 5.585f) // wait, correcting based on path
            moveTo(8.707f, 2.293f)
            lineToRelative(3.293f, 3.292f)
            lineToRelative(3.293f, -3.292f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1.32f, -0.083f)
            lineToRelative(0.094f, 0.083f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, 1.414f)
            lineToRelative(-2.293f, 2.293f)
            horizontalLineToRelative(4.586f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 3f)
            verticalLineToRelative(9f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3f, 3f)
            horizontalLineToRelative(-14f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3f, -3f)
            verticalLineToRelative(-9f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, -3f)
            horizontalLineToRelative(4.585f)
            lineToRelative(-2.292f, -2.293f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1.414f, -1.414f)
            close()
            moveTo(19f, 8f)
            horizontalLineToRelative(-2f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1f, 1f)
            verticalLineToRelative(9f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1f, 1f)
            horizontalLineToRelative(2f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1f, -1f)
            verticalLineToRelative(-9f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1f, -1f)
        }.path(fill = SolidColor(Color.White)) {
            moveTo(18f, 14f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.993f, 0.883f)
            lineToRelative(0.007f, 0.127f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.993f, 0.117f)
            lineToRelative(-0.007f, -0.127f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, -1f)
        }.path(fill = SolidColor(Color.White)) {
            moveTo(18f, 11f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.993f, 0.883f)
            lineToRelative(0.007f, 0.127f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.993f, 0.117f)
            lineToRelative(-0.007f, -0.127f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, -1f)
        }.build()

    val Movies: ImageVector
        get() = ImageVector.Builder(
            name = "Movies",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(19f, 4f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 3f)
            verticalLineToRelative(10f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3f, 3f)
            horizontalLineToRelative(-14f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3f, -3f)
            verticalLineToRelative(-10f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, -3f)
            close()
            moveTo(15f, 8f)
            horizontalLineToRelative(-1f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1f, 1f)
            verticalLineToRelative(6f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1f, 1f)
            horizontalLineToRelative(1f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3f, -3f)
            verticalLineToRelative(-2f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, -3f, -3f)
            moveToRelative(-5f, 0f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1f, 1f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(-1f)
            verticalLineToRelative(-2f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.883f, -0.993f)
            lineToRelative(-0.117f, -0.007f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1f, 1f)
            verticalLineToRelative(6f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, 0f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(1f)
            verticalLineToRelative(2f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.883f, 0.993f)
            lineToRelative(0.117f, 0.007f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1f, -1f)
            verticalLineToRelative(-6f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1f, -1f)
            moveToRelative(5f, 2f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, 1f)
            verticalLineToRelative(2f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.883f, 0.993f)
            lineToRelative(-0.117f, 0.007f)
            close()
        }.build()

    val Bolt: ImageVector
        get() = ImageVector.Builder(
            name = "Bolt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(13f, 2f)
            lineToRelative(0.018f, 0.001f)
            lineToRelative(0.016f, 0.001f)
            lineToRelative(0.083f, 0.005f)
            lineToRelative(0.011f, 0.002f)
            horizontalLineToRelative(0.011f)
            lineToRelative(0.038f, 0.009f)
            lineToRelative(0.052f, 0.008f)
            lineToRelative(0.016f, 0.006f)
            lineToRelative(0.011f, 0.001f)
            lineToRelative(0.029f, 0.011f)
            lineToRelative(0.052f, 0.014f)
            lineToRelative(0.019f, 0.009f)
            lineToRelative(0.015f, 0.004f)
            lineToRelative(0.028f, 0.014f)
            lineToRelative(0.04f, 0.017f)
            lineToRelative(0.021f, 0.012f)
            lineToRelative(0.022f, 0.01f)
            lineToRelative(0.023f, 0.015f)
            lineToRelative(0.031f, 0.017f)
            lineToRelative(0.034f, 0.024f)
            lineToRelative(0.018f, 0.011f)
            lineToRelative(0.013f, 0.012f)
            lineToRelative(0.024f, 0.017f)
            lineToRelative(0.038f, 0.034f)
            lineToRelative(0.022f, 0.017f)
            lineToRelative(0.008f, 0.01f)
            lineToRelative(0.014f, 0.012f)
            lineToRelative(0.036f, 0.041f)
            lineToRelative(0.026f, 0.027f)
            lineToRelative(0.006f, 0.009f)
            curveToRelative(0.12f, 0.147f, 0.196f, 0.322f, 0.218f, 0.513f)
            lineToRelative(0.001f, 0.012f)
            lineToRelative(0.002f, 0.041f)
            lineToRelative(0.004f, 0.064f)
            verticalLineToRelative(6f)
            horizontalLineToRelative(5f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.868f, 1.497f)
            lineToRelative(-0.06f, 0.091f)
            lineToRelative(-8f, 11f)
            curveToRelative(-0.568f, 0.783f, -1.808f, 0.38f, -1.808f, -0.588f)
            verticalLineToRelative(-6f)
            horizontalLineToRelative(-5f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.868f, -1.497f)
            lineToRelative(0.06f, -0.091f)
            lineToRelative(8f, -11f)
            lineToRelative(0.01f, -0.013f)
            lineToRelative(0.018f, -0.024f)
            lineToRelative(0.033f, -0.038f)
            lineToRelative(0.018f, -0.022f)
            lineToRelative(0.009f, -0.008f)
            lineToRelative(0.013f, -0.014f)
            lineToRelative(0.04f, -0.036f)
            lineToRelative(0.028f, -0.026f)
            lineToRelative(0.008f, -0.006f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.402f, -0.199f)
            lineToRelative(0.011f, -0.001f)
            lineToRelative(0.027f, -0.005f)
            lineToRelative(0.074f, -0.013f)
            lineToRelative(0.011f, -0.001f)
            lineToRelative(0.041f, -0.002f)
            close()
        }.build()

}
