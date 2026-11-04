package app.just.companion

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var pairingInput: EditText

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        showResult("Photos selected: ${uris.size}. Upload is not enabled yet.")
    }
    private val contactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showContactCount() else showResult("Contacts access was not granted.")
    }
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true || result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        showResult(if (granted) "Location permission enabled. Background tracking is not active." else "Location access was not granted.")
    }
    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        showResult("Files selected: ${uris.size}. Upload is not enabled yet.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(TextView(this).apply { text = "JUST Companion"; textSize = 28f })
        root.addView(TextView(this).apply {
            text = "Pair this phone to your JUST account. The account owner must generate the code."
            textSize = 15f
            setPadding(0, 12, 0, 20)
        })
        pairingInput = EditText(this).apply {
            hint = "8-digit pairing code"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(8))
            textSize = 18f
        }
        root.addView(pairingInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addButton(root, "Pair this phone") { pairDevice() }
        status = TextView(this).apply {
            text = savedDeviceMessage()
            textSize = 14f
            setPadding(0, 18, 0, 20)
        }
        root.addView(status)
        addButton(root, "Select photos and videos") {
            photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
        addButton(root, "Select documents and files") { filePicker.launch(arrayOf("*/*")) }
        addButton(root, "Share contacts") {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) showContactCount()
            else contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
        addButton(root, "Enable location permission") {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
        addButton(root, "View basic device information") { showDeviceInfo() }
        addButton(root, "Disconnect this phone") { disconnectDevice() }
        setContentView(root)
    }

    private fun pairDevice() {
        val code = pairingInput.text.toString().trim()
        if (!code.matches(Regex("\\d{8}"))) {
            showResult("Enter the 8-digit code shown in your JUST dashboard.")
            return
        }
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
            showResult("Pairing is not configured. Add JUST_SUPABASE_URL and JUST_SUPABASE_ANON_KEY to android/local.properties.")
            return
        }
        showResult("Connecting securely…")
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/pair-device"
                connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 15000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                }
                val payload = JSONObject().apply {
                    put("code", code)
                    put("displayName", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                    put("platform", "android")
                    put("osVersion", Build.VERSION.RELEASE)
                }
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
                val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                if (connection.responseCode !in 200..299) {
                    val error = JSONObject(response).optString("error", "Pairing failed.")
                    runOnUiThread { showResult(error) }
                } else {
                    val json = JSONObject(response)
                    val token = json.getString("deviceToken")
                    val deviceId = json.getString("deviceId")
                    securePreferences().edit().putString("device_token", token).putString("device_id", deviceId).apply()
                    runOnUiThread {
                        pairingInput.text.clear()
                        showResult("Phone paired successfully. Device ID: $deviceId. The private device token is stored encrypted on this phone.")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { showResult("Could not pair: ${e.message ?: "network error"}") }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun securePreferences() = EncryptedSharedPreferences.create(
        this,
        "just_secure_device",
        MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun savedDeviceMessage(): String {
        val id = securePreferences().getString("device_id", null)
        return if (id == null) "This phone is not paired. No data has been uploaded." else "Paired device ID: $id. Sharing selections are local in this starter."
    }

    private fun disconnectDevice() {
        securePreferences().edit().remove("device_token").remove("device_id").apply()
        showResult("Local pairing token removed. Ask the account owner to revoke the device in the dashboard when that control is available.")
    }

    private fun addButton(parent: LinearLayout, label: String, action: () -> Unit) {
        val button = Button(this).apply { text = label; setOnClickListener { action() } }
        parent.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 7 })
    }

    private fun showContactCount() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            showResult("Contacts permission is not enabled.")
            return
        }
        val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, arrayOf(ContactsContract.Contacts._ID), null, null, null)
        val count = cursor?.count ?: 0
        cursor?.close()
        showResult("Contacts available on this phone: $count. Contact details stay on this device in this starter.")
    }

    private fun showDeviceInfo() {
        val manager = getSystemService(Activity.BATTERY_SERVICE) as BatteryManager
        val battery = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        showResult("Device: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\nBattery: ${if (battery >= 0) "$battery%" else "Unavailable"}")
    }

    private fun showResult(message: String) { status.text = message }
}
