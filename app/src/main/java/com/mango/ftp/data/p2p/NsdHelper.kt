package com.mango.ftp.data.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.mango.ftp.data.model.PeerDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

class NsdHelper(private val context: Context) {

    companion object {
        const val TAG = "MangoNSD"
        const val SERVICE_TYPE = "_mangochat._tcp."
        const val SERVICE_PREFIX = "MangoChat_"
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registeredServiceName: String? = null

    private val _discoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<PeerDevice>> = _discoveredPeers.asStateFlow()

    private val peersMap = mutableMapOf<String, PeerDevice>()

    fun startAdvertising(displayName: String, deviceModel: String, port: Int) {
        acquireMulticastLock()

        // Clean name pattern
        val rawServiceName = "${SERVICE_PREFIX}${displayName}__device_${deviceModel}"
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = rawServiceName
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(service: NsdServiceInfo) {
                registeredServiceName = service.serviceName
                Log.d(TAG, "NSD Service registered successfully: ${service.serviceName} on port $port")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "NSD Registration failed: errorCode=$errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d(TAG, "NSD Service unregistered: ${arg0.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "NSD Unregistration failed: errorCode=$errorCode")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering NSD service", e)
        }
    }

    fun startDiscovery() {
        acquireMulticastLock()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "NSD Service discovery started for: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "NSD Service found: ${service.serviceName}")
                // Filter only our MangoChat services and skip self
                if (service.serviceType.contains("mangochat") &&
                    service.serviceName.startsWith(SERVICE_PREFIX) &&
                    service.serviceName != registeredServiceName
                ) {
                    resolveService(service)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "NSD Service lost: ${service.serviceName}")
                val removed = peersMap.remove(service.serviceName)
                if (removed != null) {
                    _discoveredPeers.value = peersMap.values.toList()
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "NSD Discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "NSD Discovery start failed: errorCode=$errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "NSD Discovery stop failed: errorCode=$errorCode")
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering NSD services", e)
        }
    }

    private fun resolveService(service: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: code $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Resolve Succeeded: ${serviceInfo.serviceName} -> Host: ${serviceInfo.host}, Port: ${serviceInfo.port}")
                val host = serviceInfo.host?.hostAddress ?: return
                if (serviceInfo.serviceName == registeredServiceName) return

                val rawName = serviceInfo.serviceName.removePrefix(SERVICE_PREFIX)
                val parts = rawName.split("__device_")
                val displayName = parts.getOrNull(0)?.replace("_", " ") ?: "جهاز عائلي"
                val deviceModel = parts.getOrNull(1)?.replace("_", " ") ?: "هاتف محلي"

                val peer = PeerDevice(
                    serviceName = serviceInfo.serviceName,
                    displayName = displayName,
                    deviceModel = deviceModel,
                    hostAddress = host,
                    port = serviceInfo.port
                )

                peersMap[serviceInfo.serviceName] = peer
                _discoveredPeers.value = peersMap.values.toList()
            }
        }

        try {
            nsdManager.resolveService(service, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving service ${service.serviceName}", e)
        }
    }

    fun stopAdvertising() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering service", e)
            }
            registrationListener = null
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery", e)
            }
            discoveryListener = null
        }
        peersMap.clear()
        _discoveredPeers.value = emptyList()
        releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        if (multicastLock == null) {
            multicastLock = wifiManager.createMulticastLock("MangoChatMulticast").apply {
                setReferenceCounted(true)
                acquire()
            }
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }
}
