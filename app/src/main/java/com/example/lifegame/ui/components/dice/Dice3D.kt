package com.example.lifegame.ui.components.dice

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.example.lifegame.ui.components.awaitFrameMillis
import com.example.lifegame.ui.components.rememberReduceMotion
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val DiceStageColor = Color(0xFF233B36)
/** Test evidence for a completed GL draw; this custom key adds no spoken UI copy. */
internal val DiceRenderedResult = SemanticsPropertyKey<String>("DiceRenderedResult")

/** A single actual GLES2 scene. The game supplies both results before any animation starts. */
@Composable
fun Dice3DStage(
    first: Int,
    second: Int,
    rollNonce: Int,
    modifier: Modifier = Modifier,
    label: String = "운동능력",
    /** Allows deterministic fallback verification and a future device quality policy. */
    allow3D: Boolean = true,
    /**
     * Presentation hook: called once per roll when that roll's final faces are on screen — the GL scene presented the
     * settled pose of the current pair, or the static pair is final (no GL, or GL failed). Rendering, the lifecycle and
     * the watchdog do not depend on it.
     */
    onSettled: ((rollNonce: Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    val reduceMotion = rememberReduceMotion()
    val supported = remember(context, allow3D) {
        allow3D && runCatching {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).deviceConfigurationInfo.reqGlEsVersion >= 0x20000
        }.getOrDefault(false)
    }
    var failed by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var renderedResult by remember { mutableStateOf("") }
    val surface = remember(context, supported) {
        if (supported) runCatching {
            DiceSurface(context, onPresented = { result -> ready = true; renderedResult = result }, onFailure = { failed = true })
        }.getOrNull() else null
    }
    DisposableEffect(surface, lifecycle, failed) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> surface?.setActive(!failed)
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> surface?.setActive(false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        surface?.setActive(!failed && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { lifecycle.removeObserver(observer); surface?.setActive(false) }
    }
    // Watch each requested result, not only initial EGL creation. A renderer that stalls
    // after a reroll must not leave an earlier result on screen indefinitely.
    // Background time is excluded: paused surfaces are not expected to submit frames.
    LaunchedEffect(surface, first, second, rollNonce, lifecycleState) {
        if (surface != null && lifecycleState == Lifecycle.State.RESUMED) {
            delay(2_000)
            if (!ready || renderedResult != "$first,$second:settled") failed = true
        }
    }
    if (onSettled != null) {
        val report by rememberUpdatedState(onSettled)
        // An animated roll lasts DiceGeometry.ROLL_DURATION_MS. Until the GL thread presents its first rolling frame, an
        // identical pair still carries the previous roll's settled marker, so a settle counts for this roll only after
        // that floor (frame clock, like the profile's rolling lock). The floor is lazy: the roll's start frame is read
        // once, and only when a settled marker shows is the rest of the floor waited. Nothing pumps frames before the GL
        // has presented, which keeps tests' virtual time (and the 2 s watchdog's real-time margin) untouched.
        val animated = rollNonce > 0 && !reduceMotion
        val rollStartMs = remember(rollNonce) { longArrayOf(-1L) }
        LaunchedEffect(rollNonce) {
            if (animated) rollStartMs[0] = withFrameMillis { it }
        }
        val markerSettled = surface == null || failed || ready && renderedResult == "$first,$second:settled"
        val reported = remember { intArrayOf(Int.MIN_VALUE) }
        LaunchedEffect(rollNonce, markerSettled) {
            if (!markerSettled || reported[0] == rollNonce) return@LaunchedEffect
            if (animated) {
                val start = rollStartMs[0]
                val elapsed = if (start < 0L) 0L else withFrameMillis { it } - start
                // If the marker turns to this roll's rolling frames meanwhile, this effect restarts (its key changes) and
                // reports at the real settle.
                awaitFrameMillis((DiceGeometry.ROLL_DURATION_MS - elapsed).coerceAtLeast(0L))
            }
            reported[0] = rollNonce
            report(rollNonce)
        }
    }
    Box(
        modifier.fillMaxWidth().height(156.dp).clip(RoundedCornerShape(12.dp))
            .background(DiceStageColor)
            .testTag("dice_3d_stage")
            .semantics {
                contentDescription = "$label: 주사위 $first, 주사위 $second, 합계 ${first + second}"
                this[DiceRenderedResult] = renderedResult
            },
    ) {
        if (surface != null && !failed) {
            AndroidView(
                factory = { surface },
                modifier = Modifier.fillMaxSize().testTag("dice_gl_surface"),
                update = { it.show(first, second, rollNonce, !reduceMotion) },
            )
        }
        if (surface == null || failed || !ready) {
            StaticDicePair(first, second, Modifier.fillMaxSize().testTag("dice_static_fallback"))
        }
    }
}

@Composable
private fun StaticDicePair(first: Int, second: Int, modifier: Modifier) {
    Canvas(modifier.background(DiceStageColor)) {
        val edge = size.height * .49f
        listOf(first, second).forEachIndexed { index, value ->
            val x = size.width * if (index == 0) .31f else .69f
            val y = size.height * .47f
            drawOval(Color.Black.copy(alpha = .22f), Offset(x - edge * .62f, y + edge * .53f), Size(edge * 1.24f, edge * .18f))
            drawRoundRect(Color(0xFFE9CD94), Offset(x - edge / 2f, y - edge / 2f), Size(edge, edge), CornerRadius(edge * .13f))
            val points = pipPoints(value)
            points.forEach { (px, py) ->
                drawCircle(Color(0xFF233B36), edge * .066f, Offset(x + px * edge * .25f, y + py * edge * .25f))
            }
        }
    }
}

internal fun pipPoints(value: Int): List<Pair<Float, Float>> = buildList {
    if (value % 2 == 1) add(0f to 0f)
    if (value >= 2) { add(-1f to -1f); add(1f to 1f) }
    if (value >= 4) { add(-1f to 1f); add(1f to -1f) }
    if (value == 6) { add(-1f to 0f); add(1f to 0f) }
}

private data class DiceFrame(val first: Int = 1, val second: Int = 1, val nonce: Int = 0, val animate: Boolean = false)

private class DiceSurface(context: Context, onPresented: (String) -> Unit, onFailure: () -> Unit) : GLSurfaceView(context) {
    constructor(context: Context) : this(context, onPresented = {}, onFailure = {})

    @Volatile private var active = false
    private var lastNonce = -1
    private var lastFrame = DiceFrame()
    private val nextFrame = Runnable { if (active) requestRender() }
    private val presentSettledFrame = Runnable {
        if (active) {
            // Compose may remove the initial static overlay after the GL draw callback.
            // Refresh the View's surface hole and submit the settled scene once more
            // after that UI transaction, without leaving an idle render loop running.
            invalidate()
            requestRender()
        }
    }
    private val scene = DiceRenderer(
        onPresented = { result -> post {
            onPresented(result)
            invalidate()
            if (result.endsWith(":settled") && active) {
                removeCallbacks(presentSettledFrame)
                postOnAnimation(presentSettledFrame)
            }
        } },
        onFailure = { post { onFailure() } },
        nextFrame = { if (active) postDelayed(nextFrame, 16L) },
    )

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        setRenderer(scene)
        renderMode = RENDERMODE_WHEN_DIRTY
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun show(first: Int, second: Int, nonce: Int, motion: Boolean) {
        val changedRoll = nonce != lastNonce
        val sameValues = first == lastFrame.first && second == lastFrame.second
        val newFrame = DiceFrame(first.coerceIn(1, 6), second.coerceIn(1, 6), nonce, motion && nonce > 0 && (changedRoll || sameValues && lastFrame.animate))
        lastNonce = nonce
        if (newFrame != lastFrame) { lastFrame = newFrame; scene.frame = newFrame; requestRender() }
    }

    fun setActive(value: Boolean) {
        if (!value) {
            removeCallbacks(nextFrame)
            removeCallbacks(presentSettledFrame)
            lastFrame = lastFrame.copy(animate = false)
            scene.frame = lastFrame
        }
        if (active == value) return
        active = value
        if (value) { onResume(); requestRender() } else {
            onPause()
        }
    }
}

private class DiceRenderer(
    private val onPresented: (String) -> Unit,
    private val onFailure: () -> Unit,
    private val nextFrame: () -> Unit,
) : GLSurfaceView.Renderer {
    @Volatile var frame = DiceFrame()
    private var seenNonce = Int.MIN_VALUE
    private var startedAt = 0L
    private var presentedResult = ""
    private var failed = false
    private var program = 0
    private var createdOnce = false
    private val mesh = createDiceMesh()
    private val shadow = createShadowMesh()
    private val projection = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private var aspect = 2f
    private var positionLocation = 0
    private var normalLocation = 0
    private var uvLocation = 0
    private var faceLocation = 0
    private var mvpLocation = 0
    private var modelLocation = 0
    private var shadowLocation = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) = safely {
        program = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        normalLocation = GLES20.glGetAttribLocation(program, "aNormal")
        uvLocation = GLES20.glGetAttribLocation(program, "aUv")
        faceLocation = GLES20.glGetAttribLocation(program, "aFace")
        mvpLocation = GLES20.glGetUniformLocation(program, "uMvp")
        modelLocation = GLES20.glGetUniformLocation(program, "uModel")
        shadowLocation = GLES20.glGetUniformLocation(program, "uShadow")
        GLES20.glClearColor(35f / 255, 59f / 255, 54f / 255, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        // A recreated context starts from the authoritative settled values.
        if (createdOnce) {
            seenNonce = frame.nonce
            startedAt = SystemClock.uptimeMillis() - DiceGeometry.ROLL_DURATION_MS
        }
        createdOnce = true
        presentedResult = ""
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) = safely {
        GLES20.glViewport(0, 0, width, height)
        aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.frustumM(projection, 0, -aspect * .42f, aspect * .42f, -.42f, .42f, 1.5f, 10f)
    }

    override fun onDrawFrame(gl: GL10?) = safely {
        val now = SystemClock.uptimeMillis()
        val input = frame
        if (seenNonce != input.nonce) { seenNonce = input.nonce; startedAt = now }
        val elapsed = now - startedAt
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        for (index in 0..1) {
            val value = if (index == 0) input.first else input.second
            val pose = DiceGeometry.pose(value, elapsed, input.animate, index == 1)
            val x = (if (index == 0) -1f else 1f) * (aspect * .4f).coerceAtMost(.85f)
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, x, -.65f, -5f)
            Matrix.scaleM(model, 0, 1f - pose.lift * .28f, 1f, 1f)
            draw(shadow, true)
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, x, .02f + pose.lift, -5f)
            Matrix.rotateM(model, 0, -18f, 1f, 0f, 0f)
            Matrix.rotateM(model, 0, -24f, 0f, 1f, 0f)
            Matrix.rotateM(model, 0, pose.rotation.y, 0f, 1f, 0f)
            Matrix.rotateM(model, 0, pose.rotation.x, 1f, 0f, 0f)
            Matrix.scaleM(model, 0, pose.scale, pose.scale, pose.scale)
            draw(mesh, false)
        }
        check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "Dice rendering failed" }
        val result = "${input.first},${input.second}:" + if (input.animate && elapsed < DiceGeometry.ROLL_DURATION_MS) "rolling" else "settled"
        if (result != presentedResult) { presentedResult = result; onPresented(result) }
        if (input.animate && elapsed < DiceGeometry.ROLL_DURATION_MS) nextFrame()
    }

    private fun draw(buffer: FloatBuffer, isShadow: Boolean) {
        Matrix.multiplyMM(mvp, 0, projection, 0, model, 0)
        GLES20.glUniformMatrix4fv(mvpLocation, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(modelLocation, 1, false, model, 0)
        GLES20.glUniform1f(shadowLocation, if (isShadow) 1f else 0f)
        fun attribute(location: Int, offset: Int, count: Int) {
            buffer.position(offset)
            GLES20.glEnableVertexAttribArray(location)
            GLES20.glVertexAttribPointer(location, count, GLES20.GL_FLOAT, false, 9 * 4, buffer)
        }
        attribute(positionLocation, 0, 3)
        attribute(normalLocation, 3, 3)
        attribute(uvLocation, 6, 2)
        attribute(faceLocation, 8, 1)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, buffer.capacity() / 9)
        GLES20.glDisableVertexAttribArray(positionLocation)
        GLES20.glDisableVertexAttribArray(normalLocation)
        GLES20.glDisableVertexAttribArray(uvLocation)
        GLES20.glDisableVertexAttribArray(faceLocation)
    }

    private inline fun safely(action: () -> Unit) {
        if (failed) return
        try { action() } catch (error: Exception) { failed = true; Log.w("LifeGameDice", "3D unavailable; static dice remain visible", error); onFailure() }
    }
}

private fun createDiceMesh(): FloatBuffer {
    val data = mutableListOf<Float>()
    fun vertex(p: FloatArray, normal: FloatArray, u: Float, v: Float, face: Float) {
        data.addAll(p.toList()); data.addAll(normal.toList()); data.add(u); data.add(v); data.add(face)
    }
    fun quad(points: List<FloatArray>, normal: FloatArray, face: Float = 0f) {
        val uv = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)
        listOf(0, 1, 2, 0, 2, 3).forEach { index -> vertex(points[index], normal, uv[index].first, uv[index].second, face) }
    }
    val inset = .42f
    for (axis in 0..2) for (sign in listOf(-1f, 1f)) {
        val a = (axis + 1) % 3
        val b = (axis + 2) % 3
        val normal = FloatArray(3).also { it[axis] = sign }
        val points = listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f).map { (u, v) ->
            FloatArray(3).also { it[axis] = sign * .5f; it[a] = u * inset; it[b] = v * inset }
        }
        val face = when (axis) { 0 -> if (sign > 0) 3f else 4f; 1 -> if (sign > 0) 2f else 5f; else -> if (sign > 0) 1f else 6f }
        quad(points, normal, face)
    }
    // Twelve chamfered edges and eight triangular corners provide real geometry highlights.
    for (axis in 0..2) {
        val a = (axis + 1) % 3
        val b = (axis + 2) % 3
        for (sa in listOf(-1f, 1f)) for (sb in listOf(-1f, 1f)) {
            fun edge(t: Float, swap: Boolean) = FloatArray(3).also {
                it[axis] = t * inset; it[a] = sa * if (swap) inset else .5f; it[b] = sb * if (swap) .5f else inset
            }
            val normal = FloatArray(3).also { it[a] = sa / sqrt(2f); it[b] = sb / sqrt(2f) }
            quad(listOf(edge(-1f, false), edge(1f, false), edge(1f, true), edge(-1f, true)), normal)
        }
    }
    for (x in listOf(-1f, 1f)) for (y in listOf(-1f, 1f)) for (z in listOf(-1f, 1f)) {
        val signs = floatArrayOf(x, y, z)
        val normal = FloatArray(3) { signs[it] / sqrt(3f) }
        for (axis in 0..2) vertex(FloatArray(3) { signs[it] * if (it == axis) .5f else inset }, normal, 0f, 0f, 0f)
    }
    return data.toBuffer()
}

private fun createShadowMesh(): FloatBuffer {
    val data = mutableListOf<Float>()
    fun vertex(x: Float, y: Float) { data.addAll(listOf(x, y, 0f, 0f, 0f, 1f, x, y, 0f)) }
    repeat(32) { segment ->
        vertex(0f, 0f)
        for (i in segment..segment + 1) {
            val angle = i * 2.0 * PI / 32
            vertex((cos(angle) * .65).toFloat(), (sin(angle) * .13).toFloat())
        }
    }
    return data.toBuffer()
}

private fun List<Float>.toBuffer(): FloatBuffer = ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(toFloatArray()); position(0) }

private fun linkProgram(vertex: String, fragment: String): Int {
    fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source); GLES20.glCompileShader(shader)
        val status = IntArray(1); GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] != 0) { "Dice shader unavailable" }
        return shader
    }
    val vs = compile(GLES20.GL_VERTEX_SHADER, vertex)
    val fs = compile(GLES20.GL_FRAGMENT_SHADER, fragment)
    val program = GLES20.glCreateProgram()
    GLES20.glAttachShader(program, vs); GLES20.glAttachShader(program, fs); GLES20.glLinkProgram(program)
    GLES20.glDeleteShader(vs); GLES20.glDeleteShader(fs)
    val status = IntArray(1); GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
    check(status[0] != 0) { "Dice program unavailable" }
    return program
}

private const val VERTEX_SHADER = """
attribute vec3 aPosition;
attribute vec3 aNormal;
attribute vec2 aUv;
attribute float aFace;
uniform mat4 uMvp;
uniform mat4 uModel;
varying vec3 vNormal;
varying vec2 vUv;
varying float vFace;
void main() {
    gl_Position = uMvp * vec4(aPosition, 1.0);
    vNormal = normalize(mat3(uModel) * aNormal);
    vUv = aUv;
    vFace = aFace;
}
"""

private const val FRAGMENT_SHADER = """
precision mediump float;
varying vec3 vNormal;
varying vec2 vUv;
varying float vFace;
uniform float uShadow;
float pip(vec2 center) { return 1.0 - smoothstep(0.068, 0.080, length(vUv - center)); }
void main() {
    if (uShadow > 0.5) { gl_FragColor = vec4(0.02, 0.04, 0.03, 0.32); return; }
    float ink = 0.0;
    if (vFace > 0.5) {
        if (abs(vFace - 1.0) < 0.2 || abs(vFace - 3.0) < 0.2 || abs(vFace - 5.0) < 0.2) ink += pip(vec2(0.5));
        if (vFace > 1.5) ink += pip(vec2(0.25, 0.25)) + pip(vec2(0.75, 0.75));
        if (vFace > 3.5) ink += pip(vec2(0.25, 0.75)) + pip(vec2(0.75, 0.25));
        if (vFace > 5.5) ink += pip(vec2(0.25, 0.5)) + pip(vec2(0.75, 0.5));
    }
    vec3 n = normalize(vNormal);
    vec3 light = normalize(vec3(-0.5, 0.8, 1.2));
    float diffuse = 0.58 + 0.42 * max(dot(n, light), 0.0);
    float shine = pow(max(dot(n, normalize(light + vec3(0.0, 0.0, 1.0))), 0.0), 28.0) * 0.22;
    vec3 ivory = vec3(0.94, 0.83, 0.61) * diffuse + shine;
    gl_FragColor = vec4(mix(ivory, vec3(0.10, 0.19, 0.16), clamp(ink, 0.0, 1.0)), 1.0);
}
"""
