package com.cmf.tvremote.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.cmf.tvremote.domain.model.RemoteCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class BleFallbackController(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private var gatt: BluetoothGatt? = null
    private var hidWriteCharacteristic: BluetoothGattCharacteristic? = null

    // Standard HID over GATT UUIDs
    private val HOGP_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb")
    private val REPORT_CHAR_UUID = UUID.fromString("00002a4d-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    fun connectDevice(deviceAddress: String, onConnected: (Boolean) -> Unit) {
        val device = adapter?.getRemoteDevice(deviceAddress) ?: return
        gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    onConnected(false)
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt?, status: Int) {
                val service = gatt?.getService(HOGP_SERVICE_UUID)
                hidWriteCharacteristic = service?.getCharacteristic(REPORT_CHAR_UUID)
                onConnected(hidWriteCharacteristic != null)
            }
        })
    }

    @SuppressLint("MissingPermission")
    suspend fun sendBleCommand(command: RemoteCommand): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val characteristic = hidWriteCharacteristic ?: throw IllegalStateException("BLE HID unavailable")
            val hidReport = mapToConsumerReport(command)
            
            // Press report
            characteristic.value = hidReport
            gatt?.writeCharacteristic(characteristic)
            
            // Clear / Key release report
            characteristic.value = byteArrayOf(0, 0)
            gatt?.writeCharacteristic(characteristic)
        }
    }

    private fun mapToConsumerReport(command: RemoteCommand): ByteArray {
        // Consumer Control HID Key codes
        return when (command) {
            RemoteCommand.POWER -> byteArrayOf(0x30, 0x00) // Power
            RemoteCommand.VOLUME_UP -> byteArrayOf(0xE9.toByte(), 0x00) // Vol Up
            RemoteCommand.VOLUME_DOWN -> byteArrayOf(0xEA.toByte(), 0x00) // Vol Down
            RemoteCommand.HOME -> byteArrayOf(0x23, 0x02) // Android Home
            RemoteCommand.BACK -> byteArrayOf(0x24, 0x02) // Back
            RemoteCommand.SELECT -> byteArrayOf(0x41, 0x00) // Menu Pick
            RemoteCommand.DPAD_UP -> byteArrayOf(0x42, 0x00)
            RemoteCommand.DPAD_DOWN -> byteArrayOf(0x43, 0x00)
            RemoteCommand.DPAD_LEFT -> byteArrayOf(0x44, 0x00)
            RemoteCommand.DPAD_RIGHT -> byteArrayOf(0x45, 0x00)
            else -> byteArrayOf(0x00, 0x00)
        }
    }
}
