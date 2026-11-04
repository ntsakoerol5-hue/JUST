package app.just.companion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView

    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris -> showResult("Photos selected: ${uris.size}. Nothing has been uploaded.") }

    private val contactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showContactCount() else showResult("Contacts access was not granted.")
    }

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        showResult(if (granted) "Location permission enabled. Location is not being tracked in the background." else "Location access was not granted.")
    }

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        showResult("Files selected: ${uris.size}. Nothing has been uploaded.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(TextView(this).apply {
            text = "JUST Companion"
            textSize = 28f
        })
        root.addView(TextView(this).apply {
            text = "Your phone. Your choice. Select what you want to share."
            textSize = 16f
            setPadding(0, 12, 0, 28)
        })
        status = TextView(this).apply {
            text = "No information has been shared."
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        root.addView(status)
        addButton(root, "Select photos and videos") {
            photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
        addButton(root, "Select documents and files") {
            filePicker.launch(arrayOf("*/*"))
        }
        addButton(root, "Share contacts") {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                showContactCount()
            } else contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
        addButton(root, "Enable location permission") {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
        addButton(root, "View basic device information") { showDeviceInfo() }
        addButton(root, "Stop / close sharing app") { finish() }
        setContentView(root)
    }

    private fun addButton(parent: LinearLayout, label: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = label
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 8
        })
    }

    private fun showContactCount() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            showResult("Contacts permission is not enabled.")
            return
        }
        val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, arrayOf(ContactsContract.Contacts._ID), null, null, null)
        val count = cursor?.count ?: 0
        cursor?.close()
        showResult("Contacts available on this phone: $count. Contact details stay on this device in this starter build.")
    }

    private fun showDeviceInfo() {
        val manager = getSystemService(Activity.BATTERY_SERVICE) as BatteryManager
        val battery = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        showResult("Device: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\nBattery: ${if (battery >= 0) "$battery%" else "Unavailable"}")
    }

    private fun showResult(message: String) {
        status.text = message
    }
}
