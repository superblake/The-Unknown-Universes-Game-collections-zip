package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Lightweight inline Material 24dp vector icons for The Unknown Universes.
 * Keeps APK size compact by avoiding the multi-megabyte material-icons-extended artifact.
 */
object AppIcons {
  private fun vec(name: String, pathData: String): ImageVector {
    val nodes = PathParser().parsePathString(pathData).toNodes()
    return ImageVector.Builder(
      name = name,
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).addPath(
      pathData = nodes,
      fill = SolidColor(Color.Black),
    ).build()
  }

  val Gamepad: ImageVector by lazy {
    vec(
      "Gamepad",
      "M21,6H3C1.9,6 1,6.9 1,8v8c0,1.1 0.9,2 2,2h18c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zM11,13H8v3H6v-3H3v-2h3V8h2v3h3v2zM15.5,15c-0.83,0 -1.5,-0.67 -1.5,-1.5s0.67,-1.5 1.5,-1.5 1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5zM19.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S18.67,9 19.5,9s1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5z",
    )
  }

  val Animation: ImageVector by lazy {
    vec(
      "Animation",
      "M15,2c-2.71,0 -5.05,1.54 -6.22,3.78 -1.61,0.5 -3.01,1.62 -3.9,3.09C2.57,10.03 1,12.33 1,15c0,3.87 3.13,7 7,7 2.71,0 5.05,-1.54 6.22,-3.78 1.61,-0.5 3.01,-1.62 3.9,-3.09C20.43,13.97 22,11.67 22,9c0,-3.87 -3.13,-7 -7,-7zM8,20c-2.76,0 -5,-2.24 -5,-5 0,-1.78 0.94,-3.34 2.35,-4.22 -0.22,0.7 -0.35,1.44 -0.35,2.22 0,3.87 3.13,7 7,7 0.78,0 1.52,-0.13 2.22,-0.35C11.34,19.06 9.78,20 8,20zM15,14c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z",
    )
  }

  val Devices: ImageVector by lazy {
    vec(
      "Devices",
      "M4,6h18V4H4C2.9,4 2,4.9 2,6v11H0v3h14v-3H4V6zM23,8h-6c-0.55,0 -1,0.45 -1,1v10c0,0.55 0.45,1 1,1h6c0.55,0 1,-0.45 1,-1V9C24,8.45 23.55,8 23,8zM22,17h-4v-7h4V17z",
    )
  }

  val Shield: ImageVector by lazy {
    vec(
      "Shield",
      "M12,1L3,5v6c0,5.55 3.84,10.74 9,12 5.16,-1.26 9,-6.45 9,-12V5L12,1z",
    )
  }

  val DesktopWindows: ImageVector by lazy {
    vec(
      "DesktopWindows",
      "M20,3H4C2.9,3 2,3.9 2,5v11c0,1.1 0.9,2 2,2h6l-2,3v1h8v-1l-2,-3h6c1.1,0 2,-0.9 2,-2V5C22,3.9 21.1,3 20,3zM20,16H4V5h16V16z",
    )
  }

  val Download: ImageVector by lazy {
    vec(
      "Download",
      "M5,20h14v-2H5V20zM19,9h-4V3H9v6H5l7,7L19,9z",
    )
  }

  val Palette: ImageVector by lazy {
    vec(
      "Palette",
      "M12,3c-4.97,0 -9,4.03 -9,9 0,4.97 4.03,9 9,9 0.83,0 1.5,-0.67 1.5,-1.5 0,-0.39 -0.15,-0.74 -0.39,-1.01 -0.23,-0.26 -0.38,-0.61 -0.38,-0.99 0,-0.83 0.67,-1.5 1.5,-1.5H16c2.76,0 5,-2.24 5,-5C21,6.58 16.97,3 12,3zM6.5,12C5.67,12 5,11.33 5,10.5S5.67,9 6.5,9 8,9.67 8,10.5 7.33,12 6.5,12zM9.5,8C8.67,8 8,7.33 8,6.5S8.67,5 9.5,5 11,5.67 11,6.5 10.33,8 9.5,8zM14.5,8C13.67,8 13,7.33 13,6.5S13.67,5 14.5,5 16,5.67 16,6.5 15.33,8 14.5,8zM17.5,12C16.67,12 16,11.33 16,10.5S16.67,9 17.5,9 19,9.67 19,10.5 18.33,12 17.5,12z",
    )
  }

  val Logout: ImageVector by lazy {
    vec(
      "Logout",
      "M17,7l-1.41,1.41L18.17,11H8v2h10.17l-2.58,2.58L17,17l5,-5L17,7zM4,5h8V3H4C2.9,3 2,3.9 2,5v14c0,1.1 0.9,2 2,2h8v-2H4V5z",
    )
  }

  val Bolt: ImageVector by lazy {
    vec(
      "Bolt",
      "M11,21h-1l1,-7H7.5c-0.58,0 -0.57,-0.32 -0.38,-0.66 0.19,-0.34 0.05,-0.08 0.07,-0.12C8.48,10.94 10.42,7.54 13,3h1l-1,7h3.5c0.49,0 0.56,0.33 0.47,0.51l-0.07,0.15C12.96,17.55 11,21 11,21z",
    )
  }

  val Terminal: ImageVector by lazy {
    vec(
      "Terminal",
      "M20,4H4C2.89,4 2,4.9 2,6v12c0,1.1 0.89,2 2,2h16c1.1,0 2,-0.9 2,-2V6C22,4.9 21.1,4 20,4zM20,18H4V8h16V18zM18,17h-6v-2h6V17zM7.5,17l-1.41,-1.41L8.67,13 6.09,10.41 7.5,9l4,4L7.5,17z",
    )
  }

  val AutoAwesome: ImageVector by lazy {
    vec(
      "AutoAwesome",
      "M19,9l1.25,-2.75L23,5l-2.75,-1.25L19,1l-1.25,2.75L15,5l2.75,1.25L19,9zM11.5,9.5L9,4 6.5,9.5 1,12l5.5,2.5L9,20l2.5,-5.5L17,12L11.5,9.5zM19,15l-1.25,2.75L15,19l2.75,1.25L19,23l1.25,-2.75L23,19l-2.75,-1.25L19,15z",
    )
  }

  val ArrowUpward: ImageVector by lazy {
    vec(
      "ArrowUpward",
      "M4,12l1.41,1.41L11,7.83V20h2V7.83l5.58,5.59L20,12l-8,-8L4,12z",
    )
  }

  val RadioButtonUnchecked: ImageVector by lazy {
    vec(
      "RadioButtonUnchecked",
      "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z",
    )
  }

  val Code: ImageVector by lazy {
    vec(
      "Code",
      "M9.4,16.6L4.8,12l4.6,-4.6L8,6l-6,6 6,6L9.4,16.6zM14.6,16.6l4.6,-4.6 -4.6,-4.6L16,6l6,6 -6,6L14.6,16.6z",
    )
  }

  val Web: ImageVector by lazy {
    vec(
      "Web",
      "M20,4H4C2.9,4 2.01,4.9 2.01,6L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6C22,4.9 21.1,4 20,4zM15,18H4v-4h11V18zM15,13H4V9h11V13zM20,18h-4V9h4V18z",
    )
  }

  val BookmarkAdd: ImageVector by lazy {
    vec(
      "BookmarkAdd",
      "M21,7h-2v2h-2V7h-2V5h2V3h2v2h2V7zM19,21l-7,-3 -7,3V5c0,-1.1 0.9,-2 2,-2h7c-0.63,0.84 -1,1.87 -1,3 0,2.76 2.24,5 5,5 0.34,0 0.68,-0.03 1,-0.1V21z",
    )
  }

  val Pause: ImageVector by lazy {
    vec(
      "Pause",
      "M6,19h4V5H6V19zM14,5v14h4V5H14z",
    )
  }

  val RestartAlt: ImageVector by lazy {
    vec(
      "RestartAlt",
      "M12,5V2L8,6l4,4V7c3.31,0 6,2.69 6,6 0,2.97 -2.17,5.43 -5,5.91v2.02c3.95,-0.49 7,-3.85 7,-7.93C20,8.58 16.42,5 12,5zM6,13c0,-1.65 0.67,-3.15 1.76,-4.24L6.34,7.34C4.9,8.79 4,10.79 4,13c0,4.08 3.05,7.44 7,7.93v-2.02C8.17,18.43 6,15.97 6,13z",
    )
  }

  val FlashOn: ImageVector by lazy {
    vec(
      "FlashOn",
      "M7,2v11h3v9l7,-12h-4l4,-8H7z",
    )
  }

  val MedicalServices: ImageVector by lazy {
    vec(
      "MedicalServices",
      "M20,6h-4V4c0,-1.1 -0.9,-2 -2,-2h-4C8.9,2 8,2.9 8,4v2H4C2.9,6 2,6.9 2,8v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8C22,6.9 21.1,6 20,6zM10,4h4v2h-4V4zM16,15h-3v3h-2v-3H8v-2h3v-3h2v3h3V15z",
    )
  }

  val MyLocation: ImageVector by lazy {
    vec(
      "MyLocation",
      "M12,8c-2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4 -1.79,-4 -4,-4zM20.94,11C20.48,6.83 17.17,3.52 13,3.06V1h-2v2.06C6.83,3.52 3.52,6.83 3.06,11H1v2h2.06c0.46,4.17 3.77,7.48 7.94,7.94V23h2v-2.06c4.17,-0.46 7.48,-3.77 7.94,-7.94H23v-2H20.94zM12,19c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z",
    )
  }

  val PhotoLibrary: ImageVector by lazy {
    vec(
      "PhotoLibrary",
      "M22,16V4c0,-1.1 -0.9,-2 -2,-2H8C6.9,2 6,2.9 6,4v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2zM11,12l2.03,2.71L16,11l4,5H8l3,-4zM2,6v14c0,1.1 0.9,2 2,2h14v-2H4V6H2z",
    )
  }

  val VideoLibrary: ImageVector by lazy {
    vec(
      "VideoLibrary",
      "M4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6zM20,2H8C6.9,2 6,2.9 6,4v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM12,14.5v-9l6,4.5 -6,4.5z",
    )
  }

  val RotateRight: ImageVector by lazy {
    vec(
      "RotateRight",
      "M15.55,5.55L11,1v3.07C7.06,4.56 4,7.92 4,12s3.05,7.44 7,7.93v-2.02c-2.84,-0.48 -5,-2.94 -5,-5.91s2.16,-5.43 5,-5.91V10l4.55,-4.45zM19.93,11c-0.17,-1.39 -0.72,-2.73 -1.62,-3.89l-1.42,1.42c0.54,0.75 0.88,1.6 1.02,2.47h2.02zM13,17.9v2.02c1.39,-0.17 2.74,-0.71 3.9,-1.61l-1.44,-1.44c-0.75,0.54 -1.59,0.89 -2.46,1.03zM16.89,15.48l1.42,1.41c0.9,-1.16 1.45,-2.5 1.62,-3.89h-2.02c-0.14,0.87 -0.48,1.72 -1.02,2.48z",
    )
  }

  val Wallpaper: ImageVector by lazy {
    vec(
      "Wallpaper",
      "M4,4h7V2H4C2.9,2 2,2.9 2,4v7h2V4zM10,13l-4,5h12l-3,-4 -2.03,2.71L10,13zM17,8.5c0,-0.83 -0.67,-1.5 -1.5,-1.5S14,7.67 14,8.5s0.67,1.5 1.5,1.5S17,9.33 17,8.5zM20,2h-7v2h7v7h2V4c0,-1.1 -0.9,-2 -2,-2zM20,20h-7v2h7c1.1,0 2,-0.9 2,-2v-7h-2v7zM4,13H2v7c0,1.1 0.9,2 2,2h7v-2H4v-7z",
    )
  }

  val Close: ImageVector by lazy {
    vec(
      "Close",
      "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
    )
  }

  val SkipPrevious: ImageVector by lazy {
    vec(
      "SkipPrevious",
      "M6,6h2v12H6zM9.5,12l8.5,6V6z",
    )
  }

  val SkipNext: ImageVector by lazy {
    vec(
      "SkipNext",
      "M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z",
    )
  }

  val CloudDone: ImageVector by lazy {
    vec(
      "CloudDone",
      "M19.35,10.04C18.67,6.59 15.64,4 12,4 9.11,4 6.6,5.64 5.35,8.04 2.34,8.36 0,10.91 0,14c0,3.31 2.69,6 6,6h13c2.76,0 5,-2.24 5,-5 0,-2.64 -2.05,-4.78 -4.65,-4.96zM10,17l-3.5,-3.5 1.41,-1.41L10,14.17 15.18,9l1.41,1.41L10,17z",
    )
  }

  val UploadFile: ImageVector by lazy {
    vec(
      "UploadFile",
      "M14,2H6c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8l-6,-6zm4,18H6V4h7v5h5v11zM8,15.01l1.41,1.41L11,14.84V19h2v-4.16l1.59,1.59L16,15.01 12.01,11z",
    )
  }
}

