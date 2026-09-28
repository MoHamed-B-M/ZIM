package com.zimapp.zim.ui.settings.lock

import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import kotlin.math.sqrt

// Ported from EasyNotes PatternLock; setup vs verify via expected pattern,
// navigation replaced with callbacks.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PatternLock(
    expected: String?,
    onPatternEntered: (String) -> Unit,
    onUnlock: () -> Unit,
    onCancel: () -> Unit,
    lockVm: LockViewModel = koinViewModel(),
) {
    val context = LocalContext.current

    BackHandler {
        if (expected != null) (context as? ComponentActivity)?.finish() else onCancel()
    }

    val rowCount = 3
    val columnCount = 3
    val dotColor = MaterialTheme.colorScheme.primary
    val pathColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.padding(vertical = 64.dp).fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TitleText(text = if (expected.isNullOrBlank()) "Draw a pattern" else "Draw pattern to unlock")

        androidx.compose.foundation.Canvas(
            modifier = Modifier.width(300.dp).height(300.dp)
                .background(MaterialTheme.colorScheme.background)
                .onSizeChanged { size ->
                    lockVm.canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                }
                .pointerInteropFilter {
                    when (it.action) {
                        MotionEvent.ACTION_DOWN -> {
                            lockVm.canvasSize.takeIf { s -> s != Size.Zero }?.let { size ->
                                val cell = getNearestCell(Offset(it.x, it.y), size.width, size.height)
                                if (cell != null) {
                                    lockVm.clearPattern()
                                    lockVm.selectedCellsIndexList.add(cell.index)
                                    lockVm.selectedCellCenterList.add(cell.center)
                                    lockVm.lastCellCenter = cell.center
                                    lockVm.path = Path().apply { moveTo(cell.center.x, cell.center.y) }
                                }
                            }
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val touchOffset = Offset(it.x, it.y)
                            lockVm.currentTouchOffset = touchOffset
                            lockVm.canvasSize.takeIf { s -> s != Size.Zero }?.let { size ->
                                val cell = getNearestCell(touchOffset, size.width, size.height)
                                if (cell != null && cell.index !in lockVm.selectedCellsIndexList) {
                                    lockVm.selectedCellsIndexList.add(cell.index)
                                    lockVm.selectedCellCenterList.add(cell.center)
                                    lockVm.updatePath(cell.center)
                                } else if (cell == null) {
                                    lockVm.currentTouchOffset = null
                                }
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            val drawn = lockVm.selectedCellsIndexList.joinToString("")
                            if (expected == null) {
                                if (drawn.length >= 4) onPatternEntered(drawn)
                            } else if (drawn == expected) {
                                onUnlock()
                            }
                            lockVm.clearPattern()
                        }
                    }
                    true
                },
        ) {
            val width = size.width
            val height = size.height
            val boxSizeInX = width / columnCount
            val boxCenterInX = boxSizeInX / 2
            val boxSizeInY = height / rowCount
            val boxCenterInY = boxSizeInY / 2
            for (row in 0 until rowCount) {
                for (column in 0 until columnCount) {
                    drawCircle(
                        color = dotColor,
                        radius = 25f,
                        center = Offset(
                            (boxCenterInX + boxSizeInX * column),
                            (boxCenterInY + boxSizeInY * row),
                        ),
                    )
                }
            }
            drawPath(path = lockVm.path, color = pathColor, style = Stroke(width = 20f, cap = StrokeCap.Round))
            lockVm.currentTouchOffset?.let { offset ->
                lockVm.lastCellCenter?.let { lastCenter ->
                    val connectingPath = Path().apply {
                        moveTo(lastCenter.x, lastCenter.y)
                        lineTo(offset.x, offset.y)
                    }
                    drawPath(path = connectingPath, color = pathColor, style = Stroke(width = 20f, cap = StrokeCap.Round))
                }
            }
        }
    }
}

data class CellModel(val index: Int, val center: Offset)

private fun getNearestCell(offset: Offset, width: Float, height: Float): CellModel? {
    val rowCount = 3
    val columnCount = 3
    val boxSizeInX = width / columnCount
    val boxCenterInX = boxSizeInX / 2
    val boxSizeInY = height / rowCount
    val boxCenterInY = boxSizeInY / 2
    val circleRadius = width / 8
    for (row in 0 until rowCount) {
        for (column in 0 until columnCount) {
            val cellCenter = Offset(
                (boxCenterInX + boxSizeInX * column),
                (boxCenterInY + boxSizeInY * row),
            )
            val distanceFromCenter = sqrt(
                (offset.x - cellCenter.x) * (offset.x - cellCenter.x) +
                    (offset.y - cellCenter.y) * (offset.y - cellCenter.y),
            )
            if (distanceFromCenter < circleRadius) {
                val index = column + 1 + row * columnCount
                return CellModel(index, cellCenter)
            }
        }
    }
    return null
}
