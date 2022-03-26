package ie.fastway.scansort.device.averydennison

import avd.api.core.IDevice
import avd.api.core.IPrinter
import avd.api.core.SupplyType
import avd.api.core.exceptions.ApiConfigurationException
import avd.api.core.exceptions.ApiException
import avd.api.core.exceptions.ApiPrinterException
import avd.api.printers.Printer6140
import com.tubbert.powdroid.android.context.AssetProvider
import ie.fastway.scansort.device.ConnectableDevice
import ie.fastway.scansort.device.printer.LabelMessage
import ie.fastway.scansort.device.printer.LabelPrinter
import ie.fastway.scansort.logging.LogConfig
import ie.logistio.equinox.Equinox
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import avd.api.core.imports.ErrorCode
import avd.sdk.CompanionAPIErrors

/**
 * The AveryDennison Monarch Pathfinder printer.
 */
class PathfinderPrinter(
        assetProvider: AssetProvider,
        private val connectableDevice: ConnectableDevice,
        private val avdDevice: IDevice
) : LabelPrinter {


    private val avdPrinter: IPrinter
        get() = avdDevice.printer

    private val printer6140: Printer6140
        get() = avdPrinter as Printer6140

    private val resourceHelper = PathfinderResourceHelper(assetProvider)

    init {

      //  var maxTimes = 4
       // while (!resourceHelper.initialiseResources() && maxTimes > 0)
       // {
           // maxTimes--

      //  }
        resourceHelper.initialiseResources()

    }

    override fun print(label: LabelMessage, greekFont: Boolean) :String {

       return printShipmentLabel(label, greekFont)

    }

    override fun print(qrContent: String): String {
        val rf = if(qrContent.isNullOrEmpty()) {"Test"} else{qrContent}




        val instructionData = arrayOf<ByteArray>(
                rf.toByteArray(), rf.toByteArray())

        return printViaLnt(PathfinderResourceHelper.LNT_SAMPLE_ALIAS, instructionData)
    }


    fun getSpeed() : Int
    {
        return try{

            printer6140.speed
        }
        catch (e : ApiPrinterException)
        {
            -1
        }

    }

    fun setSpeed(str : String)
    {
        val strToIntVal = str.toInt()



        printer6140.setSpeed(strToIntVal.toByte())




    }

    fun getIsOnBlackMark() : Boolean
    {
        return try {avdPrinter.isOnBlackMark} catch (e : ApiPrinterException){false}
    }

    fun getStatus(): String
    {
        val status = try{avdPrinter.status} catch (e : ApiPrinterException){


            return "Status Code: " +
                    getMessage(e.errorCode) + " (Debug: " + e.errorCode + ") :" + e.message


        }
        //return ""
        return "Status Code " + status.toString() + ": " + getMessage(status)// avdPrinter.status

    }


    fun abortAllJobs()
    {
        avdPrinter.abortAllJobs()
    }


     fun abortError() {
         avdPrinter.abortError()

    }

    fun clearError() {
        avdPrinter.clearError()

    }

    fun resync() {
        avdPrinter.resync()

    }

    fun calibrateBlackMark() {
        avdPrinter.calibrateBlackMark()

    }

    fun goToBlack() {
        avdPrinter.goToBlack()

    }

    private fun printShipmentLabel (label: LabelMessage, greekFont: Boolean):String {

        val rf = if(label.primaryText.isNullOrEmpty()) {"Test"} else{label.primaryText}
        val cf =  if(label.secondaryText.isNullOrEmpty()) {""} else{label.secondaryText}



        val instructionData = arrayOf<ByteArray>(
                rf.toByteArray(),
                cf.toByteArray())

        if(greekFont)
        {
            return printViaLnt(PathfinderResourceHelper.LNT_SAMPLE_PRINT_ALIAS, instructionData)
        }
        else
        {
            return printViaLnt(PathfinderResourceHelper.LNT_SHIPMENT_LABEL_ALIAS, instructionData)
        }


    }



    private fun printGenericLabel(label: LabelMessage) {
        // TODO: Print a special Generic label.
        if (LogConfig.AVD_PRINTER) {
            Timber.d("TODO: printGenericLabel not implemented. label=$label")
        }
    }

    /**
     * Prints to a label stored as an LNT resource on the Pathfinder.s
     */
     fun printViaLnt(lntAlias: String, labelFields: Array<ByteArray>):String {

        try {
            avdPrinter.print(lntAlias, 1, labelFields)
            return "Success printing."
        }
        catch (e: ApiPrinterException) {

                return "Unable to print with LNT. errorCode=${e.errorCode}; ${e.message}"

        }

    }

    internal fun tryApiTask(taskName: String = "", task: () -> Unit) {

        val maxAttempts = 4
        var attemptCount = 0;
        var isSuccessful = false
        var mayRetry = false

        if (LogConfig.AVD_SCANNER) {
            Timber.v("setupScanningConfig; $taskName;")
        }

        val lock = Object()
        synchronized(lock) {
            do {
                mayRetry = false
                try {
                    attemptCount++
                    task()
                    isSuccessful = true
                }
                catch (e: ApiException) {
                    var message = "Pathfinder API Exception. attemptCount=$attemptCount"
                    if (!taskName.isBlank()) {
                        message = "$taskName; $message"
                    }
                    Timber.e(e, message)

                    // If the device is busy we can try to execute the request again after
                    // some time has passed.
                    if (e.errorCode == CompanionAPIErrors.CD_ERROR_BUSY) {
                        mayRetry = true
                        lock.wait(100)
                    }
                }
            } while ((!isSuccessful) && (mayRetry) && (attemptCount < maxAttempts))
        }
    }

    internal fun executeConfigUpdate(updateTask: () -> Unit) {

        // loadSettings must be called twice to guarantee that settings changes
        // actually take effect. Nobody knows why.

        printer6140.loadSettings()
        printer6140.beginSetSession()
        printer6140.loadSettings()

        updateTask.invoke()

        printer6140.endSetSession()

        printer6140.unloadSettings()
    }



    /**
     * @throws avd.api.core.exceptions.ApiPrinterException on failure.
     */
    fun resetConfigurationToDefault() {
        //scanner4500.resetConfiguration()

        try {
            with(printer6140) {

                /*
                | NOTE: executing every API call individually here using "executeConfigUpdate"
                | is quite slow, but through much testing it has been found to be by far the most
                | reliable way ensure that the desired configuration is actually set on the device.
                 */

                executeConfigUpdate {
                    tryApiTask("setSpeed(0) [1]") { printer6140.setSpeed(0)}
                }

                executeConfigUpdate {
                    tryApiTask("setHorizontalAdjust(0) [2]") { printer6140.setHorizontalAdjust(0)}
                }

                executeConfigUpdate {
                    tryApiTask("setVerticalAdjust(0) [9]") { printer6140.setVerticalAdjust(0)}
                }

                executeConfigUpdate {
                    tryApiTask("supplyType(0) [10]") { printer6140.supplyType = SupplyType.BlackMark}
                }

                executeConfigUpdate {
                    tryApiTask("abortAllJobs(0) [3]") { printer6140.abortAllJobs()}
                }

                executeConfigUpdate {
                    tryApiTask("clearError(0) [4]") { printer6140.clearError()}
                }

                executeConfigUpdate {
                    tryApiTask("resync(0) [5]") { printer6140.resync()}
                }

               val a: Int = 8

                executeConfigUpdate {
                    tryApiTask("setMaxLabelLength(8) [6]") { printer6140.setMaxLabelLength(a.toByte())}
                }

                executeConfigUpdate {
                    tryApiTask("setIsOnLineMode(true) [7]") { printer6140.setIsOnLineMode(true)}
                }

                executeConfigUpdate {
                    tryApiTask("setContrast(0) [8]") { printer6140.setContrast(0)}
                }

                executeConfigUpdate {
                    tryApiTask("abortError(0) [11]") { printer6140.abortError()}
                }





                /*
                tryApiTask("avdDevice.addListenerTriggerPress") {
                    avdDevice.addListenerTriggerPress(scanner4500)
                }
                */

                tryApiTask("loadSettings [60]") { avdDevice.loadSettings() }

                if (ie.fastway.scansort.logging.LogConfig.AVD_SCANNER) {
                    timber.log.Timber.v("setupScanningConfig; Applying endSetSession [100]")
                }
                avdDevice.endSetSession()
            }
        }
        catch (e: ApiException) {
            if (LogConfig.AVD_SCANNER) {
                val failureReason = when (e) {
                    is ApiConfigurationException ->
                        "setting a new device Session"
                    is ApiPrinterException ->
                        "printer"
                    else ->
                        "printer"
                }

                Timber.d("setupScanningConfig failed when $failureReason. ErrorCode=${e.errorCode}")
            }
        }




    }




    private fun getMessage(status: Int):String
    {

        return when(status)
        {
            ErrorCode.SUCCESS -> "SUCCESS"
            ErrorCode.NOT_IMPLEMENTED -> "NOT_IMPLEMENTED"
            ErrorCode.INVALID_RESOURCE_TYPE -> "INVALID_RESOURCE_TYPE"
            ErrorCode.CANNOT_CREATE_FILE -> "CANNOT_CREATE_FILE"
            ErrorCode.RESOURCE_NOT_FOUND -> "RESOURCE_NOT_FOUND"
            ErrorCode.INVALID_RESPONSE -> "INVALID_RESPONSE"
            ErrorCode.BUSY -> "BUSY"
            ErrorCode.TIMEOUT -> "TIMEOUT"
            ErrorCode.PRINT_FAILURE -> "PRINT_FAILURE"
            ErrorCode.INVALID_HANDLE_VALUE -> "INVALID_HANDLE_VALUE"
            ErrorCode.ALREADY_EXISTS -> "ALREADY_EXISTS"
            ErrorCode.INVALID_PARAMETER -> "INVALID_PARAMETER"
            ErrorCode.INSUFFICIENT_BUFFER -> "INSUFFICIENT_BUFFER"

            ErrorCode.CONNECTION_FAILED -> "CONNECTION_FAILED"
            ErrorCode.INVALID_LNT_XML -> "INVALID_LNT_XML"
            ErrorCode.INVALID_LNT_DATA -> "INVALID_LNT_DATA"
            ErrorCode.INVALID_LNT_FIELD_NAME -> "INVALID_LNT_FIELD_NAME"
            ErrorCode.UNEXPECTED_LNT_FIELD_NAME -> "UNEXPECTED_LNT_FIELD_NAME"
            ErrorCode.INVALID_LNT_ATTRIBUTE_DATA -> "INVALID_LNT_ATTRIBUTE_DATA"
            ErrorCode.INVALID_LNT_ATTRIBUTE_NAME -> "INVALID_LNT_ATTRIBUTE_NAME"
            ErrorCode.BATCH_PRINT_FAILURE -> "BATCH_PRINT_FAILURE"
            ErrorCode.PRINTER_CONFIG_SUPPLY_TYPE_IS_IS_INVALID -> "PRINTER_CONFIG_SUPPLY_TYPE_IS_IS_INVALID"
            ErrorCode.PRINTER_RIBBON_HIGH_ENERGY_SETTING -> "PRINTER_RIBBON_HIGH_ENERGY_SETTING"
            ErrorCode.PRINTER_CONFIG_FEED_MODE_IS_INVALID  -> "PRINTER_CONFIG_FEED_MODE_IS_INVALID"
            ErrorCode.PRINTER_CONFIG_SUPPLY_POSITION_IS_INVALID  -> "PRINTER_CONFIG_SUPPLY_POSITION_IS_INVALID"
            ErrorCode.PRINTER_CONFIG_CONTRAST_IS_INVALID  -> "PRINTER_CONFIG_CONTRAST_IS_INVALID"


            ErrorCode.PRINTER_CONFIG_ADJUSTMENT_IS_INVALID  -> "PRINTER_CONFIG_ADJUSTMENT_IS_INVALID"
            ErrorCode.PRINTER_CONFIG_MARGIN_ADJUSTMENT_IS_INVALID -> "PRINTER_CONFIG_MARGIN_ADJUSTMENT_IS_INVALID"
            ErrorCode.PRINTER_CONFIG_SPEED_ADJUSTMENT_IS_INVALID  -> "PRINTER_CONFIG_SPEED_ADJUSTMENT_IS_INVALID"
            ErrorCode.PRINTER_CONFIG_BACKFEED_ACTION_IS_INVALID -> "PRINTER_CONFIG_BACKFEED_ACTION_IS_INVALID"
            ErrorCode.PRINTER_CONFIG_BACKFEED_POSITION_IS_INVALID -> "PRINTER_CONFIG_BACKFEED_POSITION_IS_INVALID"
            ErrorCode.PRINTER_CONFIG_BACKFEED_DISTABCE_IS_INVALID -> "PRINTER_CONFIG_BACKFEED_DISTABCE_IS_INVALID"
            ErrorCode.PRINTER_JOB_REQUEST_IS_INVALID -> "PRINTER_JOB_REQUEST_IS_INVALID"
            ErrorCode.PRINTER_INTERNAL_SOWTWARE_FAILURE  -> "PRINTER_INTERNAL_SOWTWARE_FAILURE "
            ErrorCode.PRINTER_MEMORY_FULL  -> "PRINTER_MEMORY_FULL "
            ErrorCode.SCANNER_INVALID_CONFIGURATION  -> "SCANNER_INVALID_CONFIGURATION "
            ErrorCode.PRINTER_CALIBRATION_OF_DIFFERENT_BLACKMARKS   -> "PRINTER_CALIBRATION_OF_DIFFERENT_BLACKMARKS "
            ErrorCode.PRINTER_MISSED_BLACKMARK   -> "PRINTER_MISSED_BLACKMARK"
            ErrorCode.PRINTER_UNEXPEXTED_BLACKMARK   -> "PRINTER_UNEXPEXTED_BLACKMARK "

            ErrorCode.PRINTER_MOTOR_JAM_OR_ENCODER_ERROR  -> "PRINTER_MOTOR_JAM_OR_ENCODER_ERROR"
            ErrorCode.PRINTER_PRINTHEAD_OVERHEAT  -> "PRINTER_PRINTHEAD_OVERHEAT "
            ErrorCode.PRINTER_MISSING_BLACKMARK -> "PRINTER_MISSING_BLACKMARK"

            ErrorCode.PRINTER_SENSE_MARK_IS_TOOLONG -> "PRINTER_SENSE_MARK_IS_TOOLONG"
            ErrorCode.PRINTER_OUT_OF_SUPPLY  -> "PRINTER_OUT_OF_SUPPLY "
            ErrorCode.PRINTER_DEMAND_TICKET_NOT_SEEN  -> "PRINTER_DEMAND_TICKET_NOT_SEEN "
            ErrorCode.PRINTER_LOW_BATTERY  -> "PRINTER_LOW_BATTERY "
            ErrorCode.PRINTER_BAD_DOTS_ON_PRINTHEAD  -> "PRINTER_BAD_DOTS_ON_PRINTHEAD "
            ErrorCode.PRINTER_OVERFEED_OR_BACKFEED_FAILED  -> "PRINTER_OVERFEED_OR_BACKFEED_FAILED "
            ErrorCode.PRINTER_PRINTHEAD_NOT_PRESENT  -> "PRINTER_PRINTHEAD_NOT_PRESENT"
            ErrorCode.PRINTER_MOTION_CONTROL_ISBUSY  -> "PRINTER_MOTION_CONTROL_ISBUSY"
            ErrorCode.PRINTER_JOB_QUEUE_FULL  -> "PRINTER_JOB_QUEUE_FULL"

            ErrorCode.PRINTER_RAM_TEST_FAILURE -> "PRINTER_RAM_TEST_FAILURE"
            ErrorCode.PRINTER_NO_CONFIG_MEMORY_FOR_NATIVE_LAYER -> "PRINTER_NO_CONFIG_MEMORY_FOR_NATIVE_LAYER"
            ErrorCode.PRINTER_POWER_SYSTEM_FAILURE -> "PRINTER_POWER_SYSTEM_FAILURE"
            ErrorCode.PRINTER_NO_CONFIG_MEMORY_FOR_APPLICATION_LAYER -> ""
            ErrorCode.PRINTER_MEMORY_CHECK_FAILURE -> "PRINTER_MEMORY_CHECK_FAILURE"
            ErrorCode.PRINTER_WARM_START  -> "PRINTER_WARM_START"
            ErrorCode.PRINTER_VIRGIN_RESTART -> "PRINTER_VIRGIN_RESTART"
            ErrorCode.PRINTER_ERROR_ERASING_FLASH -> "PRINTER_ERROR_ERASING_FLASH"
            ErrorCode.PRINTER_ERROR_WRITING_FLASH -> "PRINTER_ERROR_WRITING_FLASH"
            ErrorCode.PRINTER_ERROR_WRITING_RAM -> "PRINTER_ERROR_WRITING_RAM"
            ErrorCode.PRINTER_FRAMING_ERROR  -> "PRINTER_FRAMING_ERROR"
            ErrorCode.PRINTER_INVALID_JOBPACKET  -> "PRINTER_INVALID_JOBPACKET"
            -4 -> "ApiPrinterException"

            else -> "-9"



        }





    }


}