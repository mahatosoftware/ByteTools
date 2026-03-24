package `in`.mahato.bytetools.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import `in`.mahato.bytetools.ui.home.HomeScreen
import `in`.mahato.bytetools.ui.tools.ToolsScreen
import `in`.mahato.bytetools.ui.settings.SettingsScreen
import `in`.mahato.bytetools.ui.tools.qr.QRScannerScreen
import `in`.mahato.bytetools.ui.tools.qr.QRBarcodeDashboardScreen
import `in`.mahato.bytetools.ui.tools.flashlight.FlashlightScreen
import `in`.mahato.bytetools.ui.tools.magnifier.MagnifierScreen
import `in`.mahato.bytetools.ui.tools.whatsapp.WaChatScreen
import `in`.mahato.bytetools.ui.tools.unitconverter.UnitConverterScreen
import `in`.mahato.bytetools.ui.tools.soundmeter.SoundMeterScreen
import `in`.mahato.bytetools.ui.tools.compass.CompassScreen
import `in`.mahato.bytetools.ui.tools.bubblelevel.BubbleLevelScreen
import `in`.mahato.bytetools.ui.tools.battery.BatteryInfoScreen
import `in`.mahato.bytetools.ui.tools.storage.StorageAnalyzerScreen
import `in`.mahato.bytetools.ui.tools.ram.RAMMonitorScreen
import `in`.mahato.bytetools.ui.tools.device.DeviceInfoScreen
import `in`.mahato.bytetools.ui.tools.calculator.CalculatorScreen
import `in`.mahato.bytetools.ui.tools.age.AgeCalculatorScreen
import `in`.mahato.bytetools.ui.tools.dateduration.DateDurationScreen
import `in`.mahato.bytetools.ui.tools.gps.GPSDashboardScreen
import `in`.mahato.bytetools.ui.tools.gps.location.LiveLocationScreen
import `in`.mahato.bytetools.ui.tools.gps.location.ParkingLocationScreen
import `in`.mahato.bytetools.ui.tools.gps.compass.DigitalCompassScreen
import `in`.mahato.bytetools.ui.tools.gps.measurement.DistanceAreaScreen
import `in`.mahato.bytetools.ui.tools.gps.speedometer.SpeedometerScreen
import `in`.mahato.bytetools.ui.tools.gps.signal.GPSSignalScreen
import `in`.mahato.bytetools.ui.tools.gps.camera.GPSCameraScreen
import `in`.mahato.bytetools.ui.tools.qr.QRGeneratorScreen
import `in`.mahato.bytetools.ui.tools.common.CodeHistoryScreen
import `in`.mahato.bytetools.ui.tools.barcode.BarcodeScannerScreen
import `in`.mahato.bytetools.ui.tools.barcode.BarcodeGeneratorScreen
import `in`.mahato.bytetools.ui.tools.decision.DecisionDashboardScreen
import `in`.mahato.bytetools.ui.tools.decision.SpinWheelScreen
import `in`.mahato.bytetools.ui.tools.decision.DiceRollerScreen
import `in`.mahato.bytetools.ui.tools.decision.CoinFlipScreen
import `in`.mahato.bytetools.ui.tools.decision.RNGScreen
import `in`.mahato.bytetools.ui.tools.decision.NamePickerScreen
import `in`.mahato.bytetools.ui.tools.decision.TruthOrDareScreen
import `in`.mahato.bytetools.ui.tools.qr.*
import `in`.mahato.bytetools.ui.tools.pdf.*
import `in`.mahato.bytetools.ui.tools.image.*
import `in`.mahato.bytetools.ui.tools.image.ImageViewerScreen
import `in`.mahato.bytetools.ui.tools.nfc.*

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Tools.route) { ToolsScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen() }
        
        // Group 1: Quick Tools
        composable(Screen.Flashlight.route) { FlashlightScreen(navController) }
        composable(Screen.QRScanner.route) { QRScannerScreen(navController) }
        composable(Screen.Magnifier.route) { MagnifierScreen(navController) }
        composable(Screen.DirectWhatsApp.route) { WaChatScreen(navController) }
        
        // Group 2: Measurement Tools
        composable(Screen.UnitConverter.route) { UnitConverterScreen(navController) }
        composable(Screen.SoundMeter.route) { SoundMeterScreen(navController) }
        composable(Screen.Compass.route) { CompassScreen(navController) }
        composable(Screen.BubbleLevel.route) { BubbleLevelScreen(navController) }
        
        // Group 3: Device Information
        composable(Screen.BatteryInfo.route) { BatteryInfoScreen(navController) }
        composable(Screen.StorageAnalyzer.route) { StorageAnalyzerScreen(navController) }
        composable(Screen.RAMMonitor.route) { RAMMonitorScreen(navController) }
        composable(Screen.DeviceInfo.route) { DeviceInfoScreen(navController) }
        
        // Group 4: Productivity Tools
        composable(Screen.Calculator.route) { CalculatorScreen(navController) }
        composable(Screen.AgeCalculator.route) { AgeCalculatorScreen(navController) }
        composable(Screen.DateDuration.route) { DateDurationScreen(navController) }
        
        // Group 5: GPS Utilities Pack
        composable(Screen.GPSDashboard.route) { GPSDashboardScreen(navController) }
        composable(Screen.LiveLocation.route) { LiveLocationScreen(navController) }
        composable(Screen.ParkingLocation.route) { ParkingLocationScreen(navController) }
        composable(Screen.DigitalCompass.route) { DigitalCompassScreen(navController) }
        composable(Screen.DistanceArea.route) { DistanceAreaScreen(navController) }
        composable(Screen.Speedometer.route) { SpeedometerScreen(navController) }
        composable(Screen.GPSSignal.route) { GPSSignalScreen(navController) }
        composable(Screen.GPSCamera.route) { GPSCameraScreen(navController) }
        
        // Group 7: QR Generator
        composable(Screen.QRBarcodeDashboard.route) { QRBarcodeDashboardScreen(navController) }
        composable(Screen.QRGenerator.route) { QRGeneratorScreen(navController) }
        composable(Screen.QRHistory.route) { CodeHistoryScreen(navController) }
        composable(Screen.BarcodeScanner.route) { BarcodeScannerScreen(navController) }
        composable(Screen.BarcodeGenerator.route) { BarcodeGeneratorScreen(navController) }
        
        // Group 8: Random & Decision Tools
        composable(Screen.DecisionDashboard.route) { DecisionDashboardScreen(navController) }
        composable(Screen.SpinWheel.route) { SpinWheelScreen(navController) }
        composable(Screen.DiceRoller.route) { DiceRollerScreen(navController) }
        composable(Screen.CoinFlip.route) { CoinFlipScreen(navController) }
        composable(Screen.RNG.route) { RNGScreen(navController) }
        composable(Screen.NamePicker.route) { NamePickerScreen(navController) }
        composable(Screen.TruthOrDare.route) { TruthOrDareScreen(navController) }

        // Group 9: Image Tools
        composable(Screen.ImageDashboard.route) { ImageToolsDashboardScreen(navController) }
        composable(Screen.ImageCropper.route) { ImageCropperScreen(navController) }
        composable(Screen.ImageResizer.route) { ImageResizerScreen(navController) }
        composable(Screen.ImageConverter.route) { ImageConverterScreen(navController) }
        composable(Screen.ImageEditor.route) { ImageEditorScreen(navController) }
        composable(Screen.ImageMetadata.route) { ImageMetadataScreen(navController) }
        composable(Screen.ImageToPdf.route) { ImageToPdfScreen(navController) }
        composable(Screen.ImageGallery.route) { ImageGalleryScreen(navController) }
        composable(Screen.ImageScanner.route) { ImageScannerScreen(navController) }
        composable(
            route = Screen.FullScreenImage.route + "?uri={uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri")
            ImageViewerScreen(navController, uriString)
        }

        // Group 10: PDF Tools
        composable(Screen.PDFDashboard.route) { PDFDashboardScreen(navController) }
        composable(Screen.PDFScanner.route) { PDFScannerScreen(navController) }
        composable(
            route = Screen.PDFViewer.route + "?uri={uri}",
            arguments = listOf(navArgument("uri") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null 
            })
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri")
            PDFViewerScreen(navController, uriString)
        }
        composable(Screen.PDFSplitter.route) { PDFSplitterScreen(navController) }
        composable(Screen.PDFMerger.route) { PDFMergerScreen(navController) }
        composable(Screen.PDFSign.route) { PDFSignScreen(navController) }
        composable(Screen.PDFWatermark.route) { PDFWatermarkScreen(navController) }
        composable(Screen.PDFRedact.route) { PDFRedactScreen(navController) }
        composable(Screen.PDFOCR.route) { PDFOCRScreen(navController) }
        composable(Screen.PDFHistory.route) { PDFHistoryScreen(navController) }

        // Group 11: NFC Tools
        composable(Screen.NFCDashboard.route) { NFCDashboardScreen(navController) }
        composable(Screen.NFCTagReader.route) { NFCTagReaderScreen(navController) }
        composable(Screen.NFCWriter.route) { NFCWriterScreen(navController) }
        composable(Screen.NFCBusinessCard.route) { NFCBusinessCardScreen(navController) }
        composable(Screen.NFCWiFi.route) { NFCWiFiScreen(navController) }
        composable(Screen.NFCAutomation.route) { NFCAutomationScreen(navController) }
        composable(Screen.NFCPaymentReader.route) { NFCPaymentReaderScreen(navController) }
        composable(Screen.NFCClone.route) { NFCCloneScreen(navController) }
        composable(Screen.NFCFormatter.route) { NFCFormatterScreen(navController) }
        composable(Screen.NFCQRHybrid.route) { NFCQRHybridScreen(navController) }
        composable(Screen.NFCTapCounter.route) { NFCTapCounterScreen(navController) }
        composable(Screen.NFCScannerHistory.route) { NFCScannerHistoryScreen(navController) }
    }
}
