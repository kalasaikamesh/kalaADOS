package com.example.kalaados

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var dataList: ArrayList<String>
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var listView: ListView
    private lateinit var ipAddressEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var tcpCheckBox: CheckBox
    private lateinit var udpCheckBox: CheckBox

    // Flag to control packet sending
    @Volatile
    private var isRunning = false

    // Hold references to the running threads
    private var tcpThread: Thread? = null
    private var udpThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        listView = findViewById(R.id.listView)
        ipAddressEditText = findViewById(R.id.ipAddress)
        portEditText = findViewById(R.id.portNumber)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.StopButton)
        tcpCheckBox = findViewById(R.id.tcpCheckBox)
        udpCheckBox = findViewById(R.id.udpCheckBox)

        // Initialize the list and adapter
        dataList = ArrayList()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataList)
        listView.adapter = adapter

        // Start button click listener
        startButton.setOnClickListener {
            val targetIp = ipAddressEditText.text.toString()
            val portStr = portEditText.text.toString()

            // Validate IP
            if (targetIp.isNotEmpty()) {
                if (portStr.isNotEmpty()) {
                    val port = portStr.toInt()
                    isRunning = true // Set the flag to true

                    // Start sending packets based on selected protocol
                    if (tcpCheckBox.isChecked) {
                        startTcpSending(targetIp, port)
                    }

                    if (udpCheckBox.isChecked) {
                        startUdpSending(targetIp, port)
                    }
                } else {
                    // If port is null, perform a ping
                    pingIp(targetIp)
                }
            } else {
                addDataToListView("Please enter a target IP.")
            }
        }

        // Stop button click listener
        stopButton.setOnClickListener {
            isRunning = false // Set the flag to false to stop the threads
            clearListView() // Clear the ListView
            addDataToListView("Stopped sending packets.")

            // Interrupt running threads if they exist
            tcpThread?.let {
                if (it.isAlive) {
                    it.interrupt()
                }
            }
            udpThread?.let {
                if (it.isAlive) {
                    it.interrupt()
                }
            }
        }
    }

    // Start sending TCP packets
    private fun startTcpSending(targetIp: String, port: Int) {
        tcpThread = thread {
            try {
                while (isRunning) {
                    val spoofedIp = generateRandomIp()
                    sendTcpPacket(targetIp, port, "Hello from TCP! Spoofed IP: $spoofedIp")
                    Thread.sleep(1000) // Delay between packets
                }
            } catch (e: InterruptedException) {
                // Thread was interrupted
                addDataToListView("TCP thread stopped.")
            } catch (e: Exception) {
                e.printStackTrace()
                addDataToListView("TCP Error: ${e.message}")
            }
        }
    }

    // Start sending UDP packets
    private fun startUdpSending(targetIp: String, port: Int) {
        udpThread = thread {
            try {
                while (isRunning) {
                    val spoofedIp = generateRandomIp()
                    sendUdpPacket(targetIp, spoofedIp, port, "Hello from UDP! Spoofed IP: $spoofedIp")
                    Thread.sleep(1000) // Delay between packets
                }
            } catch (e: InterruptedException) {
                // Thread was interrupted
                addDataToListView("UDP thread stopped.")
            } catch (e: Exception) {
                e.printStackTrace()
                addDataToListView("UDP Error: ${e.message}")
            }
        }
    }

    // Method to add data to ListView
    private fun addDataToListView(data: String) {
        runOnUiThread {
            dataList.add(data)
            adapter.notifyDataSetChanged() // Notify the adapter to update the ListView
        }
    }

    // Clear the ListView
    private fun clearListView() {
        runOnUiThread {
            dataList.clear()
            adapter.notifyDataSetChanged() // Notify the adapter to update the ListView
        }
    }

    // Generate a random IP address
    private fun generateRandomIp(): String {
        val random = Random
        return "${random.nextInt(1, 255)}.${random.nextInt(0, 256)}.${random.nextInt(0, 256)}.${random.nextInt(0, 256)}"
    }

    // Send TCP packet
    private fun sendTcpPacket(targetIp: String, port: Int, message: String) {
        thread {
            try {
                val socket = Socket(targetIp, port)
                val outputStream = socket.getOutputStream()

                outputStream.write(message.toByteArray())
                outputStream.flush()
                addDataToListView("TCP Packet sent to $targetIp on port $port: $message")

                // Close connection
                outputStream.close()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                addDataToListView("TCP Error: ${e.message}")
            }
        }
    }

    // Send UDP packet
    private fun sendUdpPacket(targetIp: String, spoofedIp: String, port: Int, message: String) {
        thread {
            try {
                val socket = DatagramSocket()
                val address = InetAddress.getByName(targetIp)
                val buffer = message.toByteArray()

                // Construct a datagram packet
                val packet = DatagramPacket(buffer, buffer.size, address, port)
                socket.send(packet)

                addDataToListView("UDP Packet sent to $targetIp from $spoofedIp on port $port: $message")

                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                addDataToListView("UDP Error: ${e.message}")
            }
        }
    }

    // Ping IP Address
    private fun pingIp(targetIp: String) {
        thread {
            try {
                val address = InetAddress.getByName(targetIp)
                val reachable = address.isReachable(2000) // Timeout in milliseconds
                addDataToListView(if (reachable) "Ping to $targetIp successful!" else "Ping to $targetIp failed!")
            } catch (e: Exception) {
                e.printStackTrace()
                addDataToListView("Ping Error: ${e.message}")
            }
        }
    }
}
