package com.jolttime.game.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink=Color(0xFF090B0F); val Card=Color(0xFF141820); val Gold=Color(0xFFD5AE62); val Text=Color(0xFFF3F4F6); val Muted=Color(0xFF9399A5)
private val colors=darkColorScheme(primary=Gold,onPrimary=Ink,background=Ink,onBackground=Text,surface=Card,onSurface=Text,outline=Color(0xFF2A303B),secondary=Color(0xFF8AAEC6))
@Composable fun JoltTheme(content:@Composable()->Unit)=MaterialTheme(colorScheme=colors,typography=Typography(),content=content)
