package com.cmf.tvremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cmf.tvremote.data.network.InputTransmissionManager
import com.cmf.tvremote.data.network.KeypadAction
import com.cmf.tvremote.data.network.TvMediaScraperService
import com.cmf.tvremote.domain.macro.MacroRepository
import com.cmf.tvremote.presentation.components.BacklightSpringSlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(application: android.app.Application) : AndroidViewModel(application) {
    val mediaTelemetry = TvMediaScraperService.sessionState
    private val _latency = MutableStateFlow("65 ms")
    val latency = _latency.asStateFlow()

    init {
        TvMediaScraperService.startPolling(viewModelScope)
    }

    fun dispatchString(input: String) {
        viewModelScope.launch {
            InputTransmissionManager.sendText(input, isAdbActive = true, outStream = null)
        }
    }

    fun dispatchActionKey(action: KeypadAction) {
        viewModelScope.launch {
            InputTransmissionManager.sendKeyAction(action, isAdbActive = true, outStream = null)
        }
    }

    fun executeMacro(sequenceRaw: String) {
        viewModelScope.launch {
            val steps = MacroRepository.parseMacroSequence(sequenceRaw)
            for (step in steps) {
                if (step.delayMs > 0) {
                    delay(step.delayMs)
                }
                step.command?.let {
                    // Inject native remote key
                }
            }
        }
    }

    fun adjustBacklightRelative(isIncrement: Boolean) {
        viewModelScope.launch {
            // Execute Backlight increment or decrement step
            executeMacro(MacroRepository.BACKLIGHT_OPEN_MACRO)
        }
    }

    override fun onCleared() {
        super.onCleared()
        TvMediaScraperService.stopPolling()
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val media by viewModel.mediaTelemetry.collectAsState()
            val latency by viewModel.latency.collectAsState()

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0F1115)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Dashboard",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    // 1. DYNAMIC "NOW PLAYING" METADATA CONTAINER
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1B1D24))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF7044EC))
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "NOW PLAYING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = media.title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = media.artist,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // 2. CONNECTION GRID TILES
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardMetricTile("Link", "Connected", isOnline = true, modifier = Modifier.weight(1f))
                        DashboardMetricTile("Latency", latency, isOnline = null, modifier = Modifier.weight(1f))
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardMetricTile("TV address", "192.168.31.116", isOnline = null, modifier = Modifier.weight(1f))
                        DashboardMetricTile("This phone", "192.168.31.104", isOnline = true, modifier = Modifier.weight(1f))
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardMetricTile("Wi-Fi", "JIO AIR FIBER_EXT", isOnline = null, modifier = Modifier.weight(1f))
                        DashboardMetricTile("IR blaster", "Not supported", isOnline = null, modifier = Modifier.weight(1f))
                    }

                    // 3. SPRING-BACK RELATIVE TV BACKLIGHT SLIDER
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1B1D24))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "TV backlight",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        BacklightSpringSlider(
                            onIncrement = { viewModel.adjustBacklightRelative(true) },
                            onDecrement = { viewModel.adjustBacklightRelative(false) }
                        )
                        Text(
                            text = "Relative center-spring control: nudges backlight values on the TV based on pull tension. Releasing snaps to center.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
fun DashboardMetricTile(label: String, value: String, isOnline: Boolean?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1B1D24))
            .padding(14.dp)
    ) {
        Column {
            Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOnline != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isOnline) Color(0xFF4ADE80) else Color(0xFFFF453A))
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}
