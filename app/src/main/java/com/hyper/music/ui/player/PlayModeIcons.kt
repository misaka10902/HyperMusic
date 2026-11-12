package com.hyper.music.ui.player
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.addPathNodes

private const val SHUFFLE_PATH =
    "M17.5,2.749V4.225C14.37,4.942 11.8,7.308 10.89,10.494L10.187,12.956C9.334,15.942 " +
        "6.605,18 3.5,18C2.948,18 2.5,18.448 2.5,19C2.5,19.552 2.948,20 3.5,20C7.498,20 " +
        "11.012,17.35 12.11,13.506L12.813,11.044C13.476,8.723 15.274,6.962 17.5,6.293V7.251C17.5,7.665 " +
        "17.854,8 18.291,8C18.453,8 18.611,7.953 18.744,7.866L22.161,5.614C22.52,5.378 22.608,4.911 " +
        "22.358,4.572C22.304,4.499 22.238,4.436 22.161,4.386L18.744,2.134C18.385,1.898 17.892,1.982 " +
        "17.642,2.321C17.549,2.446 17.5,2.596 17.5,2.749ZM3.5,4C6.153,4 8.593,5.167 10.255,7.076C9.862,7.694 " +
        "9.528,8.357 9.262,9.059C7.994,7.184 5.855,6 3.5,6C2.948,6 2.5,5.552 2.5,5C2.5,4.448 2.948,4 " +
        "3.5,4ZM17.5,19.774C15.64,19.349 13.978,18.341 12.745,16.924C13.138,16.306 13.472,15.642 " +
        "13.738,14.941C14.634,16.266 15.965,17.246 17.5,17.707V16.749C17.5,16.596 17.549,16.447 " +
        "17.642,16.321C17.892,15.982 18.385,15.898 18.744,16.134L22.161,18.386C22.238,18.436 22.304,18.499 " +
        "22.358,18.572C22.608,18.912 22.52,19.378 22.161,19.614L18.744,21.866C18.611,21.953 18.453,22 " +
        "18.291,22C17.854,22 17.5,21.665 17.5,21.251V19.774Z"

private const val REPEAT_ONE_STROKE_TOP = "M2.5,9C2.5,6.791 4.291,5 6.5,5H18.5"
private const val REPEAT_ONE_ARROW_TOP =
    "M16.5,2.749V7.251C16.5,7.665 16.854,8 17.291,8C17.453,8 17.611,7.953 17.744,7.866L21.161,5.614C21.52,5.378 " +
        "21.608,4.911 21.358,4.572C21.304,4.499 21.238,4.436 21.161,4.386L17.744,2.134C17.385,1.898 16.892,1.982 " +
        "16.642,2.321C16.549,2.446 16.5,2.596 16.5,2.749Z"
private const val REPEAT_ONE_ARROW_BOTTOM =
    "M7.5,16.749V21.251C7.5,21.665 7.146,22 6.709,22C6.547,22 6.389,21.953 6.256,21.866L2.839,19.614C2.48,19.378 " +
        "2.392,18.912 2.642,18.572C2.696,18.499 2.762,18.436 2.839,18.386L6.256,16.134C6.615,15.898 7.108,15.982 " +
        "7.358,16.321C7.45,16.447 7.5,16.596 7.5,16.749Z"
private const val REPEAT_ONE_STROKE_BOTTOM = "M21.5,15C21.5,17.209 19.709,19 17.5,19H5.5"
private const val REPEAT_ONE_DIGIT =
    "M11.875,14.533C11.875,14.978 12.236,15.338 12.681,15.338C13.126,15.338 13.486,14.978 13.486,14.533V10C13.486,9.448 " +
        "13.038,9 12.486,9H12.236C12.081,9 11.928,9.036 11.789,9.106L11,9.5L10.616,9.692C10.254,9.873 10.106,10.382 " +
        "10.237,10.765C10.397,11.23 10.875,11.356 11.333,11.18L11.788,11.005C11.796,11.002 11.803,11.001 11.811,11.001C11.847,11.001 " +
        "11.875,11.03 11.875,11.065V14.533Z"

val ShuffleIcon: ImageVector by lazy {
    ImageVector.Builder("Shuffle", 24.dp, 24.dp, 24f, 24f)
        .addPath(
            pathData = addPathNodes(SHUFFLE_PATH),
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd,
        )
        .build()
}

val RepeatOneIcon: ImageVector by lazy {
    ImageVector.Builder("RepeatOne", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = addPathNodes(REPEAT_ONE_STROKE_TOP),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        )
        addPath(
            pathData = addPathNodes(REPEAT_ONE_ARROW_TOP),
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd,
        )
        addPath(
            pathData = addPathNodes(REPEAT_ONE_ARROW_BOTTOM),
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd,
        )
        addPath(
            pathData = addPathNodes(REPEAT_ONE_STROKE_BOTTOM),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        )
        addPath(
            pathData = addPathNodes(REPEAT_ONE_DIGIT),
            fill = SolidColor(Color.Black),
        )
    }.build()
}

val RepeatAllIcon: ImageVector by lazy {
    ImageVector.Builder("RepeatAll", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = addPathNodes(REPEAT_ONE_STROKE_TOP),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        )
        addPath(
            pathData = addPathNodes(REPEAT_ONE_ARROW_TOP),
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd,
        )
        addPath(
            pathData = addPathNodes(REPEAT_ONE_ARROW_BOTTOM),
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd,
        )
        addPath(
            pathData = addPathNodes(REPEAT_ONE_STROKE_BOTTOM),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        )
    }.build()
}
