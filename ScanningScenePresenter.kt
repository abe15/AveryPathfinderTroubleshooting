package ie.fastway.scansort.scanning.view

import android.view.View
import avd.api.core.BarcodeType
import com.squareup.otto.Subscribe
import ie.fastway.scansort.device.DeviceManager
import ie.fastway.scansort.device.mocking.UiButtonMockScanner
import ie.fastway.scansort.device.scanner.ScanEvent
import ie.fastway.scansort.device.scanner.ScanResult
import ie.fastway.scansort.config.AppConfig
import ie.fastway.scansort.config.AppConfig.USE_MOCK_BLUETOOTH
import ie.fastway.scansort.device.averydennison.LabelType
import ie.fastway.scansort.device.averydennison.PathfinderResourceHelper
import ie.fastway.scansort.device.mocking.MockScanner
import ie.fastway.scansort.device.printer.LabelMessage
import ie.fastway.scansort.lifecycle.EventPublisher
import ie.fastway.scansort.logging.LogConfig
import ie.fastway.scansort.scene.BaseScenePresenter
import ie.fastway.scansort.scene.MainSceneCoordinator
import ie.logistio.equinox.Equinox
import org.threeten.bp.Instant
import timber.log.Timber
import ie.fastway.scansort.lifecycle.AppSessionProvider

/**
 *
 */
class ScanningScenePresenter(
        override val sceneView: ScanningSceneView,
        private val deviceManager: DeviceManager
) : BaseScenePresenter(sceneView) {

    var onLaunchDeviceConfigListener: (() -> Unit)? = null

    override fun onEnterScene() {
        if (LogConfig.SCENES) {
            Timber.d("ScanningScenePresenter; onEnterScene")
        }

        sceneView.clearLastScan()

        sceneView.deviceConfigButton.setOnClickListener {
            onLaunchDeviceConfigListener?.invoke()
        }


        sceneView.mockScanButton.setOnClickListener { this.triggerRandomScan() }

        sceneView.mockScanButton.visibility = View.VISIBLE
        sceneView.deviceNameTx.text = deviceManager.getConnectedScannerName()
        sceneView.printerNameTx.text = ""
        sceneView.primaryTx.setText("")
        sceneView.secondaryTx.setText("")
    }

    val qrMode = false
    fun triggerRandomScan() {

        var primaryVal = sceneView.primaryTx.text.toString()
        var secondaryVal = sceneView.secondaryTx.text.toString()

       // var ternaryVal =  sceneView.secondaryTx.text.toString() //change edittext name
        if(!sceneView.isQRMode()) {
            sceneView.printerNameTx.text = deviceManager.connectedPrinter?.print(LabelMessage(
                    timestamp = Instant.now(),
                    primaryText = primaryVal,
                    secondaryText = secondaryVal,
                    labelType = LabelType.SHIPMENT
            ), sceneView.isGreekMode()
            )
       // sceneView.primaryTx.setText("")
        }
        else
        {
            //change in order to print to qr code lnt
            sceneView.printerNameTx.text = deviceManager.connectedPrinter?.print(primaryVal)

        }

   //     var m = MockScanner()
      //  var scanEv = ScanEvent("hello",BarcodeType.Code39,m, Instant.now())
      //  AppSessionProvider.eventPublisher.postOnUiThread(scanEv)

    }

    @Subscribe
    fun onScanEvent(scanEvent: ScanEvent) {

         sceneView.primaryTx.setText(scanEvent.scannedValue)
    /*
        sceneView.printerNameTx.text = deviceManager.connectedPrinter?.print(LabelMessage(
                timestamp = Instant.now(),
                primaryText = scanEvent.scannedValue,
                secondaryText = scanEvent.scannedValue,
                labelType = LabelType.SHIPMENT
        )
        )*/
        //change in order to print to qr code lnt
                sceneView.printerNameTx.text = deviceManager.connectedPrinter?.print(scanEvent.scannedValue)



    }


    override fun getName() = MainSceneCoordinator.SceneKey.SCANNING

}