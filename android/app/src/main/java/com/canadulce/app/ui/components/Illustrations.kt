package com.canadulce.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.canadulce.app.ui.theme.Forest
import com.canadulce.app.ui.theme.Gold
import com.canadulce.app.ui.theme.Ink
import com.canadulce.app.ui.theme.Leaf
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun LeafMark(modifier: Modifier) {
    Canvas(modifier) {
        drawOval(
            Leaf,
            Offset(size.width * .1f, size.height * .1f),
            Size(size.width * .75f, size.height * .6f),
        )
        drawLine(
            Forest,
            Offset(size.width * .2f, size.height * .85f),
            Offset(size.width * .7f, size.height * .25f),
            strokeWidth = 3f,
        )
    }
}

/** Ilustración nativa de un libro y una planta que crece con las historias. */
@Composable
internal fun StoryArt(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "semilla")
    val tilt by
        transition.animateFloat(
            -2f,
            2f,
            infiniteRepeatable(tween(2000), RepeatMode.Reverse),
            label = "balanceo",
        )
    Canvas(
        modifier.rotate(tilt).semantics {
            contentDescription = "Un libro abierto del que crece una planta sonriente"
        },
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        drawCircle(Color(0xFFFFE8AD), h * .33f, Offset(cx, h * .4f))
        drawCircle(Color(0xFFE0EACB), h * .14f, Offset(cx - h * .4f, h * .62f))
        drawCircle(Color(0xFFD4E8C4), h * .1f, Offset(cx + h * .38f, h * .35f))
        val book =
            Path().apply {
                moveTo(cx, h * .68f)
                quadraticTo(cx - h * .2f, h * .52f, cx - h * .4f, h * .58f)
                lineTo(cx - h * .4f, h * .84f)
                quadraticTo(cx - h * .2f, h * .8f, cx, h * .93f)
                quadraticTo(cx + h * .2f, h * .8f, cx + h * .4f, h * .84f)
                lineTo(cx + h * .4f, h * .58f)
                quadraticTo(cx + h * .2f, h * .52f, cx, h * .68f)
                close()
            }
        drawPath(book, Forest)
        drawPath(book, Color.White, style = Stroke(h * .017f))
        drawLine(Color(0xFFB8D5A5), Offset(cx, h * .68f), Offset(cx, h * .91f), h * .013f)
        repeat(3) { i ->
            drawLine(
                Color(0xFF9DBA96),
                Offset(cx - h * .32f, h * (.65f + i * .055f)),
                Offset(cx - h * .08f, h * (.71f + i * .055f)),
                h * .009f,
            )
            drawLine(
                Color(0xFF9DBA96),
                Offset(cx + h * .08f, h * (.71f + i * .055f)),
                Offset(cx + h * .32f, h * (.65f + i * .055f)),
                h * .009f,
            )
        }
        drawLine(Leaf, Offset(cx, h * .66f), Offset(cx, h * .28f), h * .038f, cap = StrokeCap.Round)
        drawOval(Leaf, Offset(cx - h * .24f, h * .22f), Size(h * .26f, h * .16f))
        drawOval(Color(0xFF7CA75C), Offset(cx, h * .13f), Size(h * .26f, h * .19f))
        drawCircle(Color(0xFFA9C97E), h * .105f, Offset(cx, h * .48f))
        drawCircle(Ink, h * .012f, Offset(cx - h * .038f, h * .46f))
        drawCircle(Ink, h * .012f, Offset(cx + h * .038f, h * .46f))
        drawArc(
            Ink,
            10f,
            160f,
            false,
            Offset(cx - h * .035f, h * .48f),
            Size(h * .07f, h * .045f),
            style = Stroke(h * .01f),
        )
    }
}

@Composable
internal fun BadgeArt(obtained: Boolean, modifier: Modifier) {
    Canvas(
        modifier.semantics {
            contentDescription = if (obtained) "Insignia obtenida" else "Insignia por descubrir"
        },
    ) {
        drawCircle(if (obtained) Color(0xFFF6C762) else Color(0xFFE4E7DE), size.minDimension / 2)
        drawCircle(
            if (obtained) Gold else Color.Gray,
            size.minDimension * .4f,
            style = Stroke(2.dp.toPx()),
        )
        val center = Offset(size.width / 2, size.height / 2)
        val star = Path()
        repeat(10) { i ->
            val radius = size.minDimension * if (i % 2 == 0) .27f else .12f
            val angle = (-90 + i * 36) * Math.PI / 180
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, if (obtained) Forest else Color.Gray)
    }
}
