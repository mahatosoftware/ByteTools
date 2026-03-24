package `in`.mahato.bytetools.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Tools : Screen("tools", "Tools")
    object Settings : Screen("settings", "Settings")
    
    // Group 1: Quick Tools
    object Flashlight : Screen("flashlight", "Flashlight")
    object QRScanner : Screen("qr_scanner", "QR Scanner")
    object Magnifier : Screen("magnifier", "Magnifier")
    object DirectWhatsApp : Screen("direct_whatsapp", "Direct WhatsApp")
    
    // Group 2: Measurement Tools
    object UnitConverter : Screen("unit_converter", "Unit Converter")
    object SoundMeter : Screen("sound_meter", "Sound Meter")
    object Compass : Screen("compass", "Compass")
    object BubbleLevel : Screen("bubble_level", "Bubble Level")
    
    // Group 3: Device Information
    object BatteryInfo : Screen("battery_info", "Battery Info")
    object StorageAnalyzer : Screen("storage_analyzer", "Storage Analyzer")
    object RAMMonitor : Screen("ram_monitor", "RAM Monitor")
    object DeviceInfo : Screen("device_info", "Device Info")
    
    // Group 4: Productivity Tools
    object Calculator : Screen("calculator", "Calculator")
    object AgeCalculator : Screen("age_calculator", "Age Calculator")
    object DateDuration : Screen("date_duration", "Date Duration")

    // Group 5: GPS Utilities Pack
    object GPSDashboard : Screen("gps_dashboard", "GPS Utilities")
    object LiveLocation : Screen("live_location", "Live Location")
    object ParkingLocation : Screen("parking_location", "Parking Location")
    object DigitalCompass : Screen("digital_compass", "Digital Compass")
    object DistanceArea : Screen("distance_area", "Distance & Area")
    object Speedometer : Screen("speedometer", "Speedometer")
    object GPSSignal : Screen("gps_signal", "GPS Signal")
    object GPSCamera : Screen("gps_camera", "GPS Camera")

    // Group 7: QR & Barcode Tools
    object QRBarcodeDashboard : Screen("qr_barcode_dashboard", "QR & Barcode")
    object QRGenerator : Screen("qr_generator", "QR Generator")
    object QRHistory : Screen("qr_history", "QR History")
    object BarcodeScanner : Screen("barcode_scanner", "Barcode Scanner")
    object BarcodeGenerator : Screen("barcode_generator", "Barcode Generator")

    // Group 8: Random & Decision Tools
    object DecisionDashboard : Screen("decision_dashboard", "Decision Tools")
    object SpinWheel : Screen("spin_wheel", "Spin the Wheel")
    object DiceRoller : Screen("dice_roller", "Dice Roller")
    object CoinFlip : Screen("coin_flip", "Coin Flip")
    object RNG : Screen("rng", "Random Number")
    object NamePicker : Screen("name_picker", "Random Name Picker")
    object TruthOrDare : Screen("truth_dare", "Truth or Dare")

    // Group 9: Image Tools
    object ImageDashboard : Screen("image_dashboard", "Image Tools")
    object ImageCropper : Screen("image_cropper", "Image Cropper")
    object ImageResizer : Screen("image_resizer", "Image Resizer")
    object ImageConverter : Screen("image_converter", "Image Converter")
    object ImageEditor : Screen("image_editor", "Image Editor")
    object ImageMetadata : Screen("image_metadata", "Image Metadata")
    object ImageToPdf : Screen("image_to_pdf", "Image to PDF")
    object ImageGallery : Screen("image_gallery", "Gallery")
    object ImageScanner : Screen("image_scanner", "Scan Document")
    object FullScreenImage : Screen("full_screen_image", "Image Viewer")

    // Group 10: PDF Tools
    object PDFDashboard : Screen("pdf_dashboard", "PDF Tools")
    object PDFScanner : Screen("pdf_scanner", "Document Scanner")
    object PDFViewer : Screen("pdf_viewer", "PDF Viewer")
    object PDFSplitter : Screen("pdf_splitter", "PDF Splitter")
    object PDFMerger : Screen("pdf_merger", "PDF Merger")
    object PDFSign : Screen("pdf_sign", "Sign PDF")
    object PDFWatermark : Screen("pdf_watermark", "PDF Watermark")
    object PDFRedact : Screen("pdf_redact", "Redact PDF")
    object PDFOCR : Screen("pdf_ocr", "OCR PDF")
    object PDFHistory : Screen("pdf_history", "PDF History")

    // Group 11: NFC Tools
    object NFCDashboard : Screen("nfc_dashboard", "NFC Tools")
    object NFCTagReader : Screen("nfc_tag_reader", "NFC Tag Reader")
    object NFCWriter : Screen("nfc_writer", "Write NFC Tag")
    object NFCBusinessCard : Screen("nfc_business_card", "NFC Business Card")
    object NFCWiFi : Screen("nfc_wifi", "WiFi NFC Tag")
    object NFCAutomation : Screen("nfc_automation", "NFC Automation")
    object NFCPaymentReader : Screen("nfc_payment_reader", "NFC Card Reader")
    object NFCClone : Screen("nfc_clone", "Clone NFC Tag")
    object NFCFormatter : Screen("nfc_formatter", "Erase / Format Tag")
    object NFCQRHybrid : Screen("nfc_qr_hybrid", "QR & NFC Share")
    object NFCTapCounter : Screen("nfc_tap_counter", "Tap Counter")
    object NFCScannerHistory : Screen("nfc_scanner_history", "NFC Scanner History")
}
