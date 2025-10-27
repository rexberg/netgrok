package com.example.netgrok

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.*
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var infoTextView: TextView
    private lateinit var refreshButton: Button
    private lateinit var copyButton: Button
    private lateinit var connectivityManager: ConnectivityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        infoTextView = findViewById(R.id.infoTextView)
        refreshButton = findViewById(R.id.refreshButton)
        copyButton = findViewById(R.id.copyButton)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        infoTextView.movementMethod = ScrollingMovementMethod()
        infoTextView.setTextIsSelectable(true)

        refreshButton.setOnClickListener { displayNetworkInfo() }
        copyButton.setOnClickListener { copyToClipboard() }

        displayNetworkInfo()
    }

    private fun displayNetworkInfo() {
        val sb = StringBuilder()
        sb.append("Network Information:\n\n")

        val activeNetwork = connectivityManager.activeNetwork
        val networks = mutableSetOf<Network>()
        if (activeNetwork != null) {
            networks.add(activeNetwork)
        }

        val request = NetworkRequest.Builder().build()
        val tempCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networks.add(network)
            }
        }

        connectivityManager.registerNetworkCallback(request, tempCallback)
        Thread.sleep(200)
        connectivityManager.unregisterNetworkCallback(tempCallback)

        if (networks.isEmpty()) {
            sb.append("No active network connections found.\n")
        } else {
            for (network in networks) {
                val nc = connectivityManager.getNetworkCapabilities(network)
                val lp = connectivityManager.getLinkProperties(network)
                if (nc != null && lp != null) {
                    sb.append("Network: $network\n")

                    when {
                        nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> sb.append("Type: Wi-Fi\n")
                        nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> sb.append("Type: Ethernet\n")
                        nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> sb.append("Type: Cellular\n")
                        else -> sb.append("Type: Other\n")
                    }

                    sb.append("Interface: ${lp.interfaceName}\n")

                    val addresses = lp.linkAddresses.joinToString { it.address.hostAddress ?: "" }
                    sb.append("IP Addresses: $addresses\n")

                    val gateways = lp.routes.filter { it.isDefaultRoute }
                        .joinToString { it.gateway?.hostAddress ?: "" }
                    sb.append("Gateway: $gateways\n")

                    val dnsServers = lp.dnsServers.joinToString { it.hostAddress ?: "" }
                    sb.append("DNS: $dnsServers\n\n")
                }
            }
        }

        infoTextView.text = sb.toString()
        infoTextView.setTextIsSelectable(true)
    }

    private fun copyToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Network Info", infoTextView.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Network info copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
