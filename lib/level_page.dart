import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:sensors_plus/sensors_plus.dart';

class LevelPage extends StatefulWidget {
  const LevelPage({super.key});

  @override
  State<LevelPage> createState() => _LevelPageState();
}

class _LevelPageState extends State<LevelPage> {
  static const platform = MethodChannel(
    'com.tgwgroup.MiRearScreenGradienter/control',
  );

  double _x = 0;
  double _y = 0;
  double _z = 0;
  StreamSubscription<AccelerometerEvent>? _subscription;

  // Safety area
  double _cutoutLeft = 0;
  double _cutoutTop = 0;
  double _cutoutRight = 0;
  double _cutoutBottom = 0;

  @override
  void initState() {
    super.initState();
    _getRearDisplayInfo();
    // Use fastest sampling rate (approx 120Hz if supported)
    _subscription = accelerometerEventStream(samplingPeriod: const Duration(milliseconds: 5)).listen((AccelerometerEvent event) {
      setState(() {
        // Low pass filter for smoothness (stronger filter: alpha 0.1)
        _x = _x * 0.9 + event.x * 0.1;
        _y = _y * 0.9 + event.y * 0.1;
        _z = _z * 0.9 + event.z * 0.1;
      });
    });
  }

  Future<void> _getRearDisplayInfo() async {
    try {
      final Map<dynamic, dynamic>? result = await platform.invokeMethod(
        'getRearDisplayInfo',
      );
      if (result != null) {
        // Get device pixel ratio to convert physical pixels to logical pixels
        final devicePixelRatio = View.of(context).devicePixelRatio;
        debugPrint('Device pixel ratio: $devicePixelRatio');
        
        setState(() {
          // Convert from physical pixels to logical pixels
          _cutoutLeft = (result['cutoutLeft'] as int).toDouble() / devicePixelRatio;
          _cutoutTop = (result['cutoutTop'] as int).toDouble() / devicePixelRatio;
          _cutoutRight = (result['cutoutRight'] as int).toDouble() / devicePixelRatio;
          _cutoutBottom = (result['cutoutBottom'] as int).toDouble() / devicePixelRatio;
          
          debugPrint('Cutout (logical): left=$_cutoutLeft, top=$_cutoutTop, right=$_cutoutRight, bottom=$_cutoutBottom');
        });
      }
    } catch (e) {
      debugPrint("Error getting rear info: $e");
    }
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Calculate angles using x, y, z
    // Pitch: Rotation around X-axis (tilting forward/backward)
    // Roll: Rotation around Y-axis (tilting left/right)
    double roll = atan2(_x, sqrt(_y * _y + _z * _z)) * 180 / pi;
    double pitch = atan2(_y, sqrt(_x * _x + _z * _z)) * 180 / pi;

    // debugPrint('LevelPage build: roll=$roll, pitch=$pitch, x=$_x, y=$_y');

    return Scaffold(
      backgroundColor: Colors.black,
      body: Padding(
        padding: EdgeInsets.fromLTRB(
          _cutoutLeft,
          _cutoutTop,
          _cutoutRight,
          _cutoutBottom,
        ),
        child: Container(
          color: Colors.black,
          width: double.infinity,
          height: double.infinity,
          child: CustomPaint(
            painter: LevelPainter(
              x: _x,
              y: _y,
              roll: roll,
              pitch: pitch,
            ),
            child: Container(),
          ),
        ),
      ),
    );
  }
}

class LevelPainter extends CustomPainter {
  final double x;
  final double y;
  final double roll;
  final double pitch;

  LevelPainter({
    required this.x,
    required this.y,
    required this.roll,
    required this.pitch,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final maxRadius = min(size.width, size.height) / 2 - 10; // Maximize size

    // Colors
    const gridColor = Colors.white;
    const neonGreen = Color(0xFF39FF14);
    const neonYellow = Color(0xFFFFFF00);
    const neonRed = Color(0xFFFF073A);
    const neonCyan = Color(0xFF00FFF0);

    // Draw Crosshair
    final crosshairPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.0
      ..color = gridColor.withOpacity(0.3);
    
    canvas.drawLine(
      Offset(center.dx - maxRadius, center.dy),
      Offset(center.dx + maxRadius, center.dy),
      crosshairPaint,
    );
    canvas.drawLine(
      Offset(center.dx, center.dy - maxRadius),
      Offset(center.dx, center.dy + maxRadius),
      crosshairPaint,
    );

    // Draw Diagonals (45, 135, 225, 315 degrees)
    final diagonalLen = maxRadius * 0.707; // sin(45)
    canvas.drawLine(
      Offset(center.dx - diagonalLen, center.dy - diagonalLen),
      Offset(center.dx + diagonalLen, center.dy + diagonalLen),
      crosshairPaint,
    );
    canvas.drawLine(
      Offset(center.dx + diagonalLen, center.dy - diagonalLen),
      Offset(center.dx - diagonalLen, center.dy + diagonalLen),
      crosshairPaint,
    );

    // Draw concentric circles (Radar style)
    final circlePaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.5
      ..color = gridColor.withOpacity(0.3);

    // Draw circles for 30°, 60°, 90°
    final degrees = [30.0, 60.0, 90.0];
    for (int i = 0; i < degrees.length; i++) {
      final radius = maxRadius * (degrees[i] / 90.0);
      
      // Draw circle
      canvas.drawCircle(center, radius, circlePaint);
      
      // Draw degree label
      final degreeText = '${degrees[i].toInt()}°';
      final textStyle = TextStyle(
        color: gridColor.withOpacity(0.5),
        fontSize: 10,
        fontWeight: FontWeight.w300,
      );
      final textSpan = TextSpan(text: degreeText, style: textStyle);
      final textPainter = TextPainter(
        text: textSpan,
        textDirection: TextDirection.ltr,
      );
      textPainter.layout();
      textPainter.paint(
        canvas,
        Offset(center.dx - textPainter.width / 2, center.dy - radius + 4), // Inside circle
      );
    }

    // Calculate Bubble Position based on tilt (0-90 degrees)
    final tiltMagnitude = sqrt(roll * roll + pitch * pitch);
    final tiltAngle = atan2(roll, pitch); // Angle of tilt direction
    
    // Scale: 90 degrees = maxRadius
    double bubbleDistance = (tiltMagnitude / 90.0) * maxRadius;
    if (bubbleDistance > maxRadius) bubbleDistance = maxRadius;
    
    final bubbleX = center.dx + bubbleDistance * sin(tiltAngle);
    final bubbleY = center.dy + bubbleDistance * cos(tiltAngle);
    final bubbleCenter = Offset(bubbleX, bubbleY);

    // Determine color based on tilt
    Color bubbleColor;
    if (tiltMagnitude < 3.0) {
      bubbleColor = neonGreen;
    } else if (tiltMagnitude < 10.0) {
      bubbleColor = neonYellow;
    } else {
      bubbleColor = neonRed;
    }
    
    // Draw Bubble with glow
    canvas.drawCircle(
      bubbleCenter,
      18,
      Paint()
        ..style = PaintingStyle.fill
        ..color = bubbleColor.withOpacity(0.4)
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 10),
    );
    
    canvas.drawCircle(
      bubbleCenter,
      10,
      Paint()
        ..style = PaintingStyle.fill
        ..color = bubbleColor,
    );
    
    // Center dot (crosshair center)
    canvas.drawCircle(
      center,
      3,
      Paint()
        ..style = PaintingStyle.fill
        ..color = gridColor.withOpacity(0.8),
    );

    // Draw angle readouts at top corners (safe area edge)
    bool isLevel = tiltMagnitude < 3.0;
    
    _drawAngleText(
      canvas,
      'ROLL',
      roll,
      const Offset(0, 0),
      neonCyan,
      isLevel,
    );
    
    _drawAngleText(
      canvas,
      'PITCH',
      pitch,
      Offset(0, size.height - 40), // left bottom corner
      neonCyan,
      isLevel,
    );
  }

  void _drawAngleText(Canvas canvas, String label, double angle, Offset position, Color color, bool isLevel) {
    final angleStyle = TextStyle(
      color: isLevel ? const Color(0xFF39FF14) : Colors.white,
      fontSize: 14, // Adjusted font size
      fontWeight: FontWeight.bold,
    );
    
    // Snap to 90.0 if very close (e.g. > 89.1) to avoid "89.9" annoyance
    double displayAngle = angle;
    if (displayAngle.abs() > 89.1) {
      displayAngle = displayAngle.sign * 90.0;
    }
    
    final textContent = '$label\n${displayAngle.toStringAsFixed(1)}°';
    final textSpan = TextSpan(text: textContent, style: angleStyle);
    final textPainter = TextPainter(
      text: textSpan,
      textDirection: TextDirection.ltr,
      textAlign: TextAlign.center,
    );
    textPainter.layout();
    textPainter.paint(canvas, position);
  }

  @override
  bool shouldRepaint(covariant LevelPainter oldDelegate) {
    return oldDelegate.x != x || oldDelegate.y != y;
  }
}
