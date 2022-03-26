package ie.fastway.scansort.device.averydennison

import avd.api.core.exceptions.ApiConfigurationException
import avd.api.core.exceptions.ApiException
import avd.api.core.exceptions.ApiPrinterException
import avd.api.core.exceptions.ApiScannerException
import com.tubbert.powdroid.ui.OnClickReference
import ie.fastway.scansort.device.DeviceManager
import ie.fastway.scansort.scene.BaseScenePresenter
import ie.fastway.scansort.scene.MainSceneCoordinator
import ie.fastway.scansort.scene.SceneContainer
import ie.logistio.equinox.Equinox

/**
 *
 */
class PathfinderConfigPresenter(
        override val sceneView: PathfinderConfigView,
        private val deviceManager: DeviceManager,
        private val onExitListener: OnClickReference
) : BaseScenePresenter(sceneView) {

    override fun onEnterScene() {
        sceneView.onSubmitListener = this::onSubmitConfig
        sceneView.onConfigResetListener = this::onResetConfig
        sceneView.onExitClickListener = onExitListener
        sceneView.onDisconnectClickListener = deviceManager::disconnectFromAllDevices



        sceneView.onAbortAllJobsButtonClickListener = this::onAbortAllJobs
        sceneView.onAbortErrorButtonClickListener = this::onAbortError
        sceneView.onClearErrorButtonClickListener = this::onClearError
        sceneView.onResyncButtonClickListener = this::onResync
        sceneView.onCalibrateBlackMarkButtonClickListener = this::onCalibrateBlackMark
        sceneView.onGoToBlackButtonClickListener = this::goToBlack
        sceneView.onSetSpeedClickListener = this::setSpeed
        sceneView.onUpdateStatusClickListener = this::updateStatus

        sceneView.onEnableQRClickListener = this::enableQR
        sceneView.onDisableQRClickListener = this::disableQR

        val scanner = getPathfinder()
        val pr = getPathfinderPrinter()


        if (scanner is PathfinderScanner) {
            showPathfinder(scanner)

        }
        else {
            sceneView.clear()
        }

        if (pr is PathfinderPrinter) {
            showStats(pr)

        }




    }

    private fun showPathfinder(scanner: PathfinderScanner) {
        sceneView.showDevice(scanner.getBluetoothDetails())
        sceneView.setScanMode(scanner.getScanMode())
    }

    private fun showStats(printer: PathfinderPrinter) {
        sceneView.showStats(printer.getSpeed(),printer.getStatus(),printer.getIsOnBlackMark())

    }

    private fun updateStatus() {
        try {

            getPathfinderPrinter()?.let {

                showStats(it)
                showSuccessMessage("Updated Status")
            }

        }
        catch (e: ApiPrinterException) {
            showDeviceError(e, "Unable to update status.")
        }
    }


    private fun onSubmitConfig() {
        try {
            getPathfinder()?.let{
                it.setScanMode(sceneView.getSelectedScanMode())
            }

            showSuccessMessage("Scan mode changed.")
        }
        catch (e: ApiScannerException) {
            showDeviceError(e, "Unable to set up scan mode.")
        }
    }

    private fun enableQR() {
        try {
            getPathfinder()?.let{
                it.enableQRMode()
            }

            showSuccessMessage("QR enabled.")
        }
        catch (e: ApiException) {
            showDeviceError(e, "Unable to enable qr mode.")
        }
    }

    private fun disableQR() {
        try {
            getPathfinder()?.let{
                it.disableQRMode()
            }

            showSuccessMessage("QR disabled.")
        }
        catch (e: ApiException) {
            showDeviceError(e, "Unable to disable qr mode.")
        }
    }



    private fun onAbortAllJobs() {
        try {

            getPathfinderPrinter()?.let {
                it.abortAllJobs()
                showStats(it)
                showSuccessMessage("Aborted all jobs.")
            }

        }
        catch (e: ApiPrinterException) {
            showDeviceError(e, "Unable to abort all jobs.")
        }
    }

    private fun onAbortError() {
        try {

            getPathfinderPrinter()?.let {
                it.abortError()
                showStats(it)
                showSuccessMessage("Aborted error.")
            }

        }
        catch (e: ApiPrinterException) {
            showDeviceError(e, "Unable to abort error.")
        }
    }

    private fun onClearError() {
        try {

            getPathfinderPrinter()?.let {
                it.clearError()
                showStats(it)
                showSuccessMessage("Cleared error.")
            }

        }
        catch (e: ApiPrinterException) {
            showDeviceError(e, "Unable to clear error.")
        }
    }

    private fun onResync() {
        try {

            getPathfinderPrinter()?.let {
                it.resync()
                showStats(it)
                showSuccessMessage("Resynchronized.")
            }

        }
        catch (e: ApiPrinterException) {
            showDeviceError(e, "Unable to resynchronize.")
        }
    }

    private fun onCalibrateBlackMark() {
        try {

            getPathfinderPrinter()?.let {
                it.calibrateBlackMark()
                showStats(it)
                showSuccessMessage("Calibrated black mark.")
            }

        }
        catch (e: ApiPrinterException) {
            showDeviceError(e, "Unable to calibrate black mark.")
        }
    }

    private fun goToBlack() {
        try {

            getPathfinderPrinter()?.let {
                it.goToBlack()
                showStats(it)
                showSuccessMessage("Went to black mark.")
            }

        }
        catch (e: ApiPrinterException) {
            showDeviceError(e, "Unable to go to black mark.")
        }
    }

    private fun setSpeed() {
        try {
            val s = sceneView.printerSpeedTx.text.toString()
            getPathfinderPrinter()?.let {
                it.setSpeed(s)
                showStats(it)
                showSuccessMessage("Changed speed")
            }

        }
        catch (e: ApiPrinterException) {
            showDeviceError(e, "Unable to go to set speed.")
        }
    }

    /**
     * Reset to the default scanner configurations
     */
    private fun onResetConfig() {
        try {
            getPathfinder()?.let {
                it.resetConfigurationToDefault()
                showPathfinder(it)
                showSuccessMessage("Configuration reset to default.")
            }

        }
        catch (e: ApiException) {
            showDeviceError(e, "Unable to reset configuration.")
        }

        try {
            getPathfinderPrinter()?.let {
                it.resetConfigurationToDefault()
                showStats(it)
                showSuccessMessage("Configuration reset to default.")
            }

        }
        catch (e: ApiException) {
            showDeviceError(e, "Unable to reset configuration.")
        }
    }

    //----------------------------------------------------------------------------------------------

    private fun showDeviceError(e: ApiException, contextMessage: String) {
        val message = StringBuilder(Equinox.nowAsUserTime() + "| UNKNOWN: $contextMessage ")
        message.append("ErrorCode=${e.errorCode}.")
        if (e.message != null) {
            message.append(" Message=${e.message}")
        }
        sceneView.showErrorMessage(message.toString())
    }



    private fun showSuccessMessage(message: String) {
        sceneView.showSuccessMessage(message)
    }

    private fun getPathfinder(): PathfinderScanner? {
        val scanner = deviceManager.connectedScanner
        return if (scanner != null && scanner is PathfinderScanner)
            scanner
        else
            null
    }

    private fun getPathfinderPrinter(): PathfinderPrinter? {
        val printerA = deviceManager.connectedPrinter
        return if (printerA != null && printerA is PathfinderPrinter)
            printerA
        else
            null
    }

    override fun getName(): String = MainSceneCoordinator.SceneKey.PATHFINDER_CONFIG

    object Factory {

        fun create(
                container: SceneContainer,
                deviceManager: DeviceManager,
                onExitListener: OnClickReference
        ): PathfinderConfigPresenter {

            val scanningSceneView = PathfinderConfigView(container.getContainerView())

            val presenter = PathfinderConfigPresenter(
                    scanningSceneView, deviceManager, onExitListener)

            scanningSceneView.setPresenter(presenter)

            return presenter
        }
    }

}