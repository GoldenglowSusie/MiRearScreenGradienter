# Test PNG file compilation script
# Uses AAPT2 to compile PNG files individually

$ANDROID_SDK = "C:\Users\16094\AppData\Local\Android\Sdk"
$BUILD_TOOLS_VERSION = (Get-ChildItem "$ANDROID_SDK\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1).Name
$AAPT2 = "$ANDROID_SDK\build-tools\$BUILD_TOOLS_VERSION\aapt2.exe"

# Create temporary output directory
$OUTPUT_DIR = "test_output"
if (Test-Path $OUTPUT_DIR) {
    Remove-Item $OUTPUT_DIR -Recurse -Force
}
New-Item -ItemType Directory -Path $OUTPUT_DIR | Out-Null

# PNG file to test
$PNG_FILE = "android\app\src\main\res\mipmap-xxxhdpi\ic_launcher.png"

Write-Host "Compiling PNG file with AAPT2: $PNG_FILE" -ForegroundColor Green
Write-Host "AAPT2 path: $AAPT2" -ForegroundColor Yellow
Write-Host "Output directory: $OUTPUT_DIR" -ForegroundColor Yellow
Write-Host ""

# Check if AAPT2 exists
if (-not (Test-Path $AAPT2)) {
    Write-Host "Error: AAPT2 tool not found" -ForegroundColor Red
    Write-Host "Please check if Android SDK is properly installed" -ForegroundColor Red
    exit 1
}

# Check if PNG file exists
if (-not (Test-Path $PNG_FILE)) {
    Write-Host "Error: PNG file not found: $PNG_FILE" -ForegroundColor Red
    exit 1
}

# Compile PNG file with AAPT2
Write-Host "Compiling PNG file..." -ForegroundColor Cyan
& $AAPT2 compile --legacy -o $OUTPUT_DIR $PNG_FILE

if ($LASTEXITCODE -eq 0) {
    Write-Host "Success: PNG file compiled successfully!" -ForegroundColor Green
    Write-Host "Output location: $OUTPUT_DIR" -ForegroundColor Green
} else {
    Write-Host "Failed: PNG file compilation failed!" -ForegroundColor Red
    Write-Host "Exit code: $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}
