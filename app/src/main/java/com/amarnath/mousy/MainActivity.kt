package com.amarnath.mousy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amarnath.mousy.ui.theme.MousyTheme
import com.mutualmobile.composesensors.rememberGyroscopeSensorState
import kotlinx.coroutines.delay
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URI
import java.util.Collections

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MousyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val enabled = remember { mutableStateOf(false) }
                    var client: WebSocketClient? by remember { mutableStateOf(null) }
                    val isConnected = remember { mutableStateOf(false) }
                    val accelerometerState = rememberGyroscopeSensorState()

                    val accelerometerValues = remember { mutableStateOf(Triple(0f, 0f, 0f)) }

                    LaunchedEffect(enabled.value, client, isConnected.value) {
                        while (enabled.value && client != null && isConnected.value) {
                            client?.send(
                                "${accelerometerValues.value.first},${accelerometerValues.value.second},${accelerometerValues.value.third}"
                            )
                            delay(100)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .padding(top = 128.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            12.dp
                        ),
                    ) {
                        Text(
                            text = "Hello, Mousy! 🐭",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        listOf(
                            "X: ${accelerometerState.xRotation}",
                            "Y: ${accelerometerState.yRotation}",
                            "Z: ${accelerometerState.zRotation}"
                        ).forEach {
                            accelerometerValues.value = Triple(
                                accelerometerState.xRotation,
                                accelerometerState.yRotation,
                                accelerometerState.zRotation
                            )
                            Text(
                                text = it,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Button(
                            onClick = {
                                enabled.value = !enabled.value
                                if (client == null) {
                                    client = newClient(isConnected)
                                } else if (!isConnected.value) {
                                    client = newClient(isConnected)
                                }
                            },
                            shape = RoundedCornerShape(20),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (enabled.value) Color(0xFF70D770) else Color(
                                    0xFFDE7979
                                )
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(
                                    8.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (enabled.value) Icons.Default.Done else Icons.Default.Close,
                                    contentDescription = null
                                )
                                Text(if (enabled.value) "Disable" else "Enable")
                            }
                        }

                        if (enabled.value) {
                            Text(
                                text = "Mousy is enabled!",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF70D770)
                                )
                            )
                        }

                        DisplayIPAddress()
                    }
                }
            }
        }
    }
}

fun newClient(isConnected: MutableState<Boolean>): WebSocketClient {
    val uri = URI("wss://bccc-136-232-57-110.ngrok-free.app/ws")
    val client = object : WebSocketClient(uri) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            println("Connected to ${uri.host}")
            isConnected.value = true
            println("setting up UpdateLoop ->X, Y, Z")
        }

        override fun onMessage(received: String?) {
            println("Received NewMessage: $received")
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            println("Disconnected from ${uri.host}")
            isConnected.value = false
            println("closing UpdateLoop ->X, Y, Z")
        }

        override fun onError(ex: Exception?) {
            ex?.printStackTrace()
        }
    }
    client.connect()
    return client
}

fun getIPAddress(useIPv4: Boolean): String {
    try {
        val interfaces: List<NetworkInterface> =
            Collections.list(NetworkInterface.getNetworkInterfaces())
        for (networkInterface in interfaces) {
            val addresses: List<InetAddress> = Collections.list(networkInterface.inetAddresses)
            for (address in addresses) {
                if (!address.isLoopbackAddress) {
                    val hostAddress: String = address.hostAddress ?: continue
                    val isIPv4 = hostAddress.indexOf(':') < 0
                    if (useIPv4) {
                        if (isIPv4) return hostAddress
                    } else {
                        if (!isIPv4) {
                            val ipv6Address = hostAddress.split('%')[0]
                            return ipv6Address
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "IP address not found"
}

@Composable
fun DisplayIPAddress() {
    val ipAddress = remember { getIPAddress(useIPv4 = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Your IP Address is:", style = TextStyle(fontSize = 18.sp),
            color = Color(0xFF70D770),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = ipAddress, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            color = Color(0xFFD9E5E3),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}
