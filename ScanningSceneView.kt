package ie.fastway.scansort.scanning.view

import android.support.design.widget.FloatingActionButton
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import ie.fastway.scansort.R
import ie.fastway.scansort.scene.BaseSceneView


/**
 * ViewHolder for showing the scannign scene.
 */
class ScanningSceneView(sceneRoot: ViewGroup) : BaseSceneView(sceneRoot) {

    lateinit var deviceNameTx: TextView
        private set

    lateinit var deviceConfigButton: Button
        private set

    lateinit var printerNameTx: TextView
       private set

    lateinit var primaryTx: EditText
        private set

    lateinit var secondaryTx: EditText
        private set

    lateinit var qrSwitch: Switch
        private set

    lateinit var fontSwitch: Switch
        private set

    //    private set

    lateinit var mockScanButton: FloatingActionButton
        private set

    override fun bindViews() {

        with(sceneRoot) {
            deviceNameTx = findViewById(R.id.scanningScene_connectedDevice_deviceId)
            deviceConfigButton = findViewById(R.id.scanningScene_connectedDevice_configureButton)
            printerNameTx = findViewById(R.id.scanningScene_printerName)

            primaryTx = findViewById(R.id.scanningScene_primary_tx)
            secondaryTx = findViewById(R.id.scanningScene_secondary_tx)
          //  deliveryAddress = findViewById(R.id.scanningScene_address)
         //   errorMessageTx = findViewById(R.id.scanningScene_errorMessage)
            mockScanButton = findViewById(R.id.scanningScene_manualScanButton)
            qrSwitch = findViewById(R.id.qrSwitch)

            fontSwitch = findViewById(R.id.fontSwitch)
        }

        showError(null)
    }


    fun isQRMode() : Boolean
    {

        return qrSwitch.isChecked

    }

    fun isGreekMode() : Boolean
    {

        return fontSwitch.isChecked

    }



    fun showError(errorContent: String?) {
        beginDelayedTransition()


    }


    fun clearLastScan() {
        //deviceNameTx.text = ""
       // lastScanTx.text = ""
       // errorMessageTx.text = ""
       // deliveryAddress.text = ""
       // cf.text = ""
        //rf.text = ""

    }

    override fun getLayoutId(): Int = R.layout.scene_scanning

}