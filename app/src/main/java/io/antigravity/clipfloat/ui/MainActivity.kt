package io.antigravity.clipfloat.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.antigravity.clipfloat.data.ClipboardRepository
import io.antigravity.clipfloat.data.EngineConfig
import io.antigravity.clipfloat.data.PreferenceBridge
import io.antigravity.clipfloat.design.theme.ThemePreset
import io.antigravity.clipfloat.service.ClipFloatAccessibilityService
import io.antigravity.clipfloat.service.FloatingHUDService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefBridge: PreferenceBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefBridge = PreferenceBridge(this)

        setContent {
            val config by prefBridge.configStream.collectAsState(initial = EngineConfig())
            val slots by ClipboardRepository.slotsStream.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            var showDisclosureDialog by remember { mutableStateOf(false) }

            val dominantColor = Color(config.theme.dominantColor)
            val secondaryColor = Color(config.theme.secondaryColor)
            val borderColor = Color(config.theme.secondaryBorderColor)
            val textColor = Color(config.theme.textColor)
            val accentColor = Color(config.theme.accentColor)

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = dominantColor
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "ClipFloat Engine",
                            color = accentColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Floating Multi-Register Clipboard HUD",
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }

                    // Permissions Card
                    item {
                        PermissionStatusCard(
                            context = context,
                            secondaryColor = secondaryColor,
                            borderColor = borderColor,
                            textColor = textColor,
                            accentColor = accentColor,
                            onShowDisclosure = { showDisclosureDialog = true }
                        )
                    }

                    // Service Controls
                    item {
                        ServiceControlCard(
                            context = context,
                            secondaryColor = secondaryColor,
                            borderColor = borderColor,
                            textColor = textColor,
                            accentColor = accentColor
                        )
                    }

                    // Theme Picker
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = secondaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "HUD 60-30-10 Theme Preset",
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ThemePreset.values().forEach { preset ->
                                        val isSelected = config.theme == preset
                                        Button(
                                            onClick = {
                                                coroutineScope.launch { prefBridge.setTheme(preset) }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) accentColor else Color.DarkGray
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = preset.displayName.split(" ").first(),
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Slot Registers Live Preview
                    item {
                        Text(
                            text = "Active Registers (S1 - S${config.slotCount})",
                            color = accentColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    itemsIndexed(slots.take(config.slotCount)) { index, slot ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = secondaryColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "S${index + 1}",
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (slot.text.isEmpty()) "(Empty)" else slot.text,
                                    color = textColor,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }

                // Prominent Disclosure Dialog for Google Play Compliance
                if (showDisclosureDialog) {
                    AlertDialog(
                        onDismissRequest = { showDisclosureDialog = false },
                        title = { Text(text = "Accessibility & Privacy Disclosure") },
                        text = {
                            Text(
                                text = "ClipFloat uses the Accessibility API solely to insert multi-register clipboard payloads directly into active form fields without switching apps.\n\n" +
                                        "• Zero Network: ClipFloat has NO internet permission.\n" +
                                        "• Zero Keystroke Logging: No text or input is recorded or monitored.\n" +
                                        "• Volatile RAM: Clipboard entries reside solely in memory."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDisclosureDialog = false
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("I Understand & Enable")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDisclosureDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionStatusCard(
    context: Context,
    secondaryColor: Color,
    borderColor: Color,
    textColor: Color,
    accentColor: Color,
    onShowDisclosure: () -> Unit
) {
    val hasOverlay = Settings.canDrawOverlays(context)
    val hasAccessibility = ClipFloatAccessibilityService.isServiceActive

    Card(
        colors = CardDefaults.cardColors(containerColor = secondaryColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Required OS Permissions", color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

            // Overlay Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Display Overlay: ${if (hasOverlay) "GRANTED" else "MISSING"}", color = textColor, fontSize = 13.sp)
                if (!hasOverlay) {
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Grant", color = Color.Black, fontSize = 12.sp)
                    }
                }
            }

            // Accessibility Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Accessibility Engine: ${if (hasAccessibility) "ACTIVE" else "DISABLED"}", color = textColor, fontSize = 13.sp)
                if (!hasAccessibility) {
                    Button(
                        onClick = onShowDisclosure,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Enable", color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceControlCard(
    context: Context,
    secondaryColor: Color,
    borderColor: Color,
    textColor: Color,
    accentColor: Color
) {
    val hasOverlay = Settings.canDrawOverlays(context)

    Card(
        colors = CardDefaults.cardColors(containerColor = secondaryColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "HUD Service Controls", color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (hasOverlay) {
                            val intent = Intent(context, FloatingHUDService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        }
                    },
                    enabled = hasOverlay,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start HUD", color = Color.Black)
                }

                Button(
                    onClick = {
                        val intent = Intent(context, FloatingHUDService::class.java)
                        context.stopService(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop HUD", color = Color.White)
                }
            }
        }
    }
}
