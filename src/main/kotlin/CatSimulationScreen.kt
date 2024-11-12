import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random
import androidx.compose.ui.draw.drawBehind

const val X_MAP_SIZE = 500f
const val Y_MAP_SIZE = 500f
const val R_0 = 70f
const val r_0 = 30f
const val NUM_CATS = 50
const val TAU = 500L
const val CAT_SIZE = X_MAP_SIZE/NUM_CATS

enum class CatState { CALM, HISSING, FIGHTING }

data class Cat(var x: MutableState<Float>, var y: MutableState<Float>, var state: MutableState<CatState> = mutableStateOf(CatState.CALM))

fun calculateDistance(cat1: Cat, cat2: Cat): Float {
    return sqrt((cat1.x.value - cat2.x.value).pow(2) + (cat1.y.value - cat2.y.value).pow(2))
}

fun updateCatStates(cats: List<Cat>) {
    for (i in cats.indices) {
        val cat1 = cats[i]
        cat1.state.value = CatState.CALM
        for (j in i + 1 until cats.size) {
            val cat2 = cats[j]
            val distance = calculateDistance(cat1, cat2)
            when {
                distance <= r_0 -> {
                    cat1.state.value = CatState.FIGHTING
                    cat2.state.value = CatState.FIGHTING
                }
                distance <= R_0 -> {
                    if (Random.nextFloat() < 1 / distance.pow(2)) {
                        cat1.state.value = CatState.HISSING
                        cat2.state.value = CatState.HISSING
                    }
                }
            }
        }
    }
}

fun moveCatsRandomly(cats: List<Cat>) {
    for (cat in cats) {
        var newX: Float
        var newY: Float
        var isOverlapping: Boolean

        do {
            newX = (cat.x.value + Random.nextFloat() * 20 - 10).coerceIn(CAT_SIZE, X_MAP_SIZE - CAT_SIZE)
            newY = (cat.y.value + Random.nextFloat() * 20 - 10).coerceIn(CAT_SIZE, Y_MAP_SIZE - CAT_SIZE)
            isOverlapping = cats.any { otherCat ->
                otherCat != cat && calculateDistance(Cat(mutableStateOf(newX), mutableStateOf(newY)), otherCat) < 2 * CAT_SIZE
            }
        } while (isOverlapping)

        cat.x.value = newX
        cat.y.value = newY
    }
}

@Composable
fun catSimulationScreen() {
    val cats = remember {
        mutableStateListOf<Cat>().apply {
            while (size < NUM_CATS) {
                val newX = Random.nextFloat() * (X_MAP_SIZE - 2 * CAT_SIZE) + CAT_SIZE
                val newY = Random.nextFloat() * (Y_MAP_SIZE - 2 * CAT_SIZE) + CAT_SIZE
                val newCat = Cat(mutableStateOf(newX), mutableStateOf(newY))

                // Ensure the new cat is not too close to any existing cats
                if (none { calculateDistance(it, newCat) < 2 * CAT_SIZE }) {
                    add(newCat)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            moveCatsRandomly(cats)
            updateCatStates(cats)
            delay(TAU)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(Color(0xFFae99b8)) }
    ) {
        Box(
            modifier = Modifier
                .size(X_MAP_SIZE.dp, Y_MAP_SIZE.dp)
                .align(Alignment.Center)
                .drawBehind { drawRect(Color(0xFFb5f096)) }
        ) {
            cats.forEach { cat ->
                drawCat(cat)
            }
        }
    }
}

@Composable
fun drawCat(cat: Cat) {
    var animatedX by remember { mutableStateOf(cat.x.value) }
    var animatedY by remember { mutableStateOf(cat.y.value) }
    var currentColor by remember { mutableStateOf(getColorForState(cat.state.value)) }

    LaunchedEffect(cat.x.value, cat.y.value, cat.state.value) {
        val steps = 20
        val targetColor = getColorForState(cat.state.value)
        for (i in 1..steps) {
            animatedX += (cat.x.value - animatedX) / steps
            animatedY += (cat.y.value - animatedY) / steps
            currentColor = interpolateColor(currentColor, targetColor, 5f / steps)
            delay(10)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val catOffset = androidx.compose.ui.geometry.Offset(
            animatedX.dp.toPx(),
            animatedY.dp.toPx()
        )
        drawCircle(color = currentColor, center = catOffset, radius = CAT_SIZE.dp.toPx())
    }
}

fun getColorForState(state: CatState): Color {
    return when (state) {
        CatState.CALM -> Color.White
        CatState.HISSING -> Color.Gray
        CatState.FIGHTING -> Color.Black
    }
}

fun interpolateColor(start: Color, end: Color, fraction: Float): Color {
    val r = start.red + (end.red - start.red) * fraction
    val g = start.green + (end.green - start.green) * fraction
    val b = start.blue + (end.blue - start.blue) * fraction
    val a = start.alpha + (end.alpha - start.alpha) * fraction
    return Color(r, g, b, a)
}
