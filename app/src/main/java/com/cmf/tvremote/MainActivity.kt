package com.cmf.tvremote

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cmf.tvremote.data.ir.IrHardwareManager
import com.cmf.tvremote.data.network.TvSocketManager
import com.cmf.tvremote.domain.model.RemoteCommand
import com.cmf.tvremote.domain.model.TvConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val settingsEngine = SettingsEngine(context)
    private val irHardwareManager = IrHardwareManager(context)

    val isIrSupported: Boolean = irHardwareManager.isIrSupported

    private val _connectionState = MutableStateFlow(
        TvConnectionState(
            isConnected = true,
            connectedIp = "192.168.1.114",
            pingLatencyMs = 12,
            statusText = "Now Playing: Interstellar"
        )
    )
    val connectionState = _connectionState.asStateFlow()

    val settings = settingsEngine.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RemoteUiSettings()
    )

    fun sendCommand(cmd: RemoteCommand) {
        viewModelScope.launch {
            TvSocketManager.sendCommand(cmd)
        }
    }

    fun updateFontScale(scale: Float) = viewModelScope.launch {
        settingsEngine.updateFontScale(scale)
    }

    fun updateFontWeight(index: Int) = viewModelScope.launch {
        settingsEngine.updateFontWeight(index)
    }

    fun updateSquircleRadius(radiusDp: Float) = viewModelScope.launch {
        settingsEngine.updateSquircleRadius(radiusDp)
    }

    fun updateWallpaper(theme: WallpaperTheme) = viewModelScope.launch {
        settingsEngine.updateWallpaperTheme(theme)
    }

    fun connectTv(ip: String) {
        viewModelScope.launch {
            _connectionState.value = _connectionState.value.copy(statusText = "Connecting...")
            val result = TvSocketManager.connect(context, ip.trim())
            if (result.isSuccess) {
                _connectionState.value = TvConnectionState(
                    isConnected = true,
                    connectedIp = ip.trim(),
                    pingLatencyMs = 12,
                    statusText = "Now Playing: Interstellar"
                )
            } else {
                _connectionState.value = _connectionState.value.copy(
                    isConnected = false,
                    statusText = result.exceptionOrNull()?.message ?: "Check Connection"
                )
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: RemoteViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsState()
            val connection by viewModel.connectionState.collectAsState()
            var showSettingsSheet by remember { mutableStateOf(false) }
            var showIpDialog by remember { mutableStateOf(false) }
            var ipInput by remember { mutableStateOf("") }

            // Wallpaper Background Matrix
            val backgroundModifier = when (settings.wallpaperTheme) {
                WallpaperTheme.WARM_AURORA -> Modifier.background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE07A28), // Vibrant warm amber
                            Color(0xFF285E6E), // Deep muted cyan
                            Color(0xFF0F1B2A), // Dark navy slate
                            Color(0xFF070B12)  // Deep midnight base
                        ),
                        radius = 1600f
                    )
                )
                WallpaperTheme.DEEP_SLATE -> Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2E3440),
                            Color(0xFF1E222A),
                            Color(0xFF111317)
                        )
                    )
                )
                WallpaperTheme.OLED_BLACK -> Modifier.background(Color.Black)
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .then(backgroundModifier),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    // Top iOS-Style App Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "09:41",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = (15 * settings.fontScale).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Main Frosted Card Container (Matches attached preview)
                    val cardShape = SquircleShape(cornerRadius = settings.cornerRadius)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 44.dp)
                            .iosGlassmorphicSurface(
                                shape = cardShape,
                                tintAlpha = 0.11f,
                                borderAlpha = 0.28f,
                                elevation = 24.dp,
                                blurRadius = 35.dp
                            )
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Header Card Telemetry
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showIpDialog = true },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Xiaomi Google TV",
                                color = Color.White,
                                fontSize = (26 * settings.fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "IP: ${connection.connectedIp} • ${connection.pingLatencyMs}ms",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = (12 * settings.fontScale).sp,
                                fontWeight = settings.fontWeight
                            )
                            Spacer(Modifier.height(10.dp))

                            // Glowing Amber Status Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0xFFFFA000))
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFA000))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = connection.statusText,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = (13 * settings.fontScale).sp,
                                    fontWeight = settings.fontWeight
                                )
                            }
                        }

                        // Power Button (Positioned gracefully on left side)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .iosGlassmorphicSurface(
                                        shape = SquircleShape(cornerRadius = 14.dp),
                                        tintAlpha = 0.16f,
                                        borderAlpha = 0.25f,
                                        elevation = 8.dp
                                    )
                                    .clickable { viewModel.sendCommand(RemoteCommand.POWER) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = "Power",
                                    tint = Color(0xFFFF453A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Center Squircle D-Pad Dial
                        SquircleDPad(
                            onCommand = { cmd -> viewModel.sendCommand(cmd) },
                            radius = settings.cornerRadius
                        )

                        // Navigation Row: Back, Home, Mic
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.sendCommand(RemoteCommand.BACK) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.sendCommand(RemoteCommand.HOME) }) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.sendCommand(RemoteCommand.ASSISTANT) }) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Assistant",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Bottom Control Capsules (Volume & Channel)
                        Row(
                            modifier = Modifier.fillMaxWidth(0.82f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassCapsuleRocker(
                                onPlus = { viewModel.sendCommand(RemoteCommand.VOLUME_UP) },
                                onMinus = { viewModel.sendCommand(RemoteCommand.VOLUME_DOWN) }
                            )
                            GlassCapsuleRocker(
                                onPlus = { viewModel.sendCommand(RemoteCommand.DPAD_UP) },
                                onMinus = { viewModel.sendCommand(RemoteCommand.DPAD_DOWN) }
                            )
                        }

                        // Footer Status Text
                        Text(
                            text = if (viewModel.isIrSupported) "IR Blaster: Active" else "IR Blaster: Not Supported (Using Wi-Fi)",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = (11 * settings.fontScale).sp,
                            fontWeight = settings.fontWeight
                        )
                    }

                    // Settings Bottom Sheet
                    if (showSettingsSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showSettingsSheet = false },
                            containerColor = Color(0xFF14171D),
                            scrimColor = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                Text(
                                    text = "Interface Customization",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Squircle Radius Slider
                                Column {
                                    Text(
                                        text = "Squircle Corner Radius: ${settings.squircleRadiusDp.toInt()} dp",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                    Slider(
                                        value = settings.squircleRadiusDp,
                                        onValueChange = { viewModel.updateSquircleRadius(it) },
                                        valueRange = 8f..34f
                                    )
                                }

                                // Font Scaling Slider
                                Column {
                                    Text(
                                        text = "Typography Scale: ${String.format("%.2f", settings.fontScale)}x",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                    Slider(
                                        value = settings.fontScale,
                                        onValueChange = { viewModel.updateFontScale(it) },
                                        valueRange = 0.8f..1.3f
                                    )
                                }

                                // Font Weight Selector
                                Column {
                                    Text(
                                        text = "Secondary Text Weight",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Thin", "Light", "Normal", "Medium", "Bold").forEachIndexed { index, label ->
                                            FilterChip(
                                                selected = settings.fontWeightIndex == index,
                                                onClick = { viewModel.updateFontWeight(index) },
                                                label = { Text(label, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White.copy(alpha = 0.25f),
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                // Wallpaper Theme Selector
                                Column {
                                    Text(
                                        text = "Wallpaper Atmosphere",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    WallpaperTheme.values().forEach { theme ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.updateWallpaper(theme) }
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = settings.wallpaperTheme == theme,
                                                onClick = { viewModel.updateWallpaper(theme) }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(theme.displayName, color = Color.White, fontSize = 14.sp)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    // IP Config Dialog
                    if (showIpDialog) {
                        AlertDialog(
                            onDismissRequest = { showIpDialog = false },
                            title = { Text("Connect Xiaomi TV") },
                            text = {
                                Column {
                                    Text("Enter your Xiaomi TV local IP address:", fontSize = 13.sp)
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = ipInput,
                                        onValueChange = { ipInput = it },
                                        placeholder = { Text("192.168.1.114") },
                                        singleLine = true
                                    )
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    if (ipInput.isNotBlank()) viewModel.connectTv(ipInput)
                                    showIpDialog = false
                                }) { Text("Connect") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showIpDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    }
}
