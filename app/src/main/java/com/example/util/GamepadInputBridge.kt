package com.example.util

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

@Immutable
data class GamepadState(
  val connected: Boolean = false,
  val deviceName: String = "",
  val leftX: Float = 0f,
  val leftY: Float = 0f,
  val rightX: Float = 0f,
  val rightY: Float = 0f,
  val l2Trigger: Float = 0f,
  val r2Trigger: Float = 0f,
  val lastActionButtonCode: Int = 0,
  val actionSeq: Long = 0L,
)

/**
 * Hardware Dual-Stick + Trigger bridge matching classic console/twin-stick shooter ergonomics
 * (PlayStation DualSense, Xbox Controller, Switch Pro, Backbone / MFi).
 */
object GamepadInputBridge {
  private val _state = MutableStateFlow(GamepadState())
  val state: StateFlow<GamepadState> = _state.asStateFlow()

  private const val DEADZONE = 0.14f

  private fun applyDeadzone(v: Float): Float =
    if (abs(v) < DEADZONE) 0f else ((abs(v) - DEADZONE) / (1f - DEADZONE)) * if (v < 0f) -1f else 1f

  fun onGenericMotionEvent(event: MotionEvent): Boolean {
    val src = event.source
    val isPad = (src and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
      (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    if (!isPad || event.action != MotionEvent.ACTION_MOVE) return false

    val lx = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_X))
    val ly = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Y))
    val hatX = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_HAT_X))
    val hatY = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_HAT_Y))

    val effLx = if (lx != 0f || ly != 0f) lx else hatX
    val effLy = if (lx != 0f || ly != 0f) ly else hatY

    val rz = event.getAxisValue(MotionEvent.AXIS_Z)
    val rrz = event.getAxisValue(MotionEvent.AXIS_RZ)
    val rrx = event.getAxisValue(MotionEvent.AXIS_RX)
    val rry = event.getAxisValue(MotionEvent.AXIS_RY)

    val rawRx = if (abs(rz) >= abs(rrx)) rz else rrx
    val rawRy = if (abs(rrz) >= abs(rry)) rrz else rry

    val l2 = maxOf(
      event.getAxisValue(MotionEvent.AXIS_LTRIGGER),
      event.getAxisValue(MotionEvent.AXIS_BRAKE),
    ).coerceIn(0f, 1f)
    val r2 = maxOf(
      event.getAxisValue(MotionEvent.AXIS_RTRIGGER),
      event.getAxisValue(MotionEvent.AXIS_GAS),
    ).coerceIn(0f, 1f)

    _state.value = _state.value.copy(
      connected = true,
      deviceName = event.device?.name ?: "Controller",
      leftX = effLx.coerceIn(-1f, 1f),
      leftY = effLy.coerceIn(-1f, 1f),
      rightX = applyDeadzone(rawRx).coerceIn(-1f, 1f),
      rightY = applyDeadzone(rawRy).coerceIn(-1f, 1f),
      l2Trigger = l2,
      r2Trigger = r2,
    )
    return true
  }

  fun onKeyEvent(event: KeyEvent): Boolean {
    val src = event.source
    val isPad = (src and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
      (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    if (!isPad || event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return false
    val code = event.keyCode
    return when (code) {
      KeyEvent.KEYCODE_BUTTON_A,
      KeyEvent.KEYCODE_BUTTON_B,
      KeyEvent.KEYCODE_BUTTON_X,
      KeyEvent.KEYCODE_BUTTON_Y,
      KeyEvent.KEYCODE_BUTTON_L1,
      KeyEvent.KEYCODE_BUTTON_R1,
      KeyEvent.KEYCODE_BUTTON_L2,
      KeyEvent.KEYCODE_BUTTON_R2,
      KeyEvent.KEYCODE_BUTTON_THUMBR,
      -> {
        _state.value = _state.value.copy(
          connected = true,
          deviceName = event.device?.name ?: _state.value.deviceName.ifBlank { "Controller" },
          lastActionButtonCode = code,
          actionSeq = _state.value.actionSeq + 1L,
        )
        true
      }
      else -> false
    }
  }
}
