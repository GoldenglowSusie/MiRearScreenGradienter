import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:ui';
import 'dart:math' as math;
import 'package:url_launcher/url_launcher.dart';
import 'level_page.dart';

void main() {
  // 设置沉浸式状态栏（透明状态栏）
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Colors.transparent,
      systemNavigationBarIconBrightness: Brightness.light,
    ),
  );
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '背屏水平仪',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF3b82f6)),
        useMaterial3: true,
      ),
      routes: {
        '/': (context) => const MyHomePage(),
        '/rear': (context) => const LevelPage(),
      },
      initialRoute: '/',
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel('com.tgwgroup.MiRearScreenGradienter/control');
  bool _isRunning = false;

  Future<void> _startLevel() async {
    try {
      await platform.invokeMethod('startLevel');
      setState(() {
        _isRunning = true;
      });
    } on PlatformException catch (e) {
      debugPrint("Failed to start level: '${e.message}'.");
    }
  }

  Future<void> _stopLevel() async {
    try {
      await platform.invokeMethod('stopLevel');
      setState(() {
        _isRunning = false;
      });
    } on PlatformException catch (e) {
      debugPrint("Failed to stop level: '${e.message}'.");
    }
  }

  Future<void> _launchURL(String url) async {
    final Uri uri = Uri.parse(url);
    if (!await launchUrl(uri, mode: LaunchMode.externalApplication)) {
      debugPrint('Could not launch $url');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        elevation: 0,
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
        shadowColor: Colors.transparent,
        title: const Text(
          '背屏水平仪',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
      ),
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Color(0xFFFF9D88), // 珊瑚橙
              Color(0xFFFFB5C5), // 粉红
              Color(0xFFE0B5DC), // 紫色
              Color(0xFFA8C5E5), // 蓝色
            ],
          ),
        ),
        child: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            physics: const BouncingScrollPhysics(),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Header Card (Frosted Glass)
                CustomPaint(
                  painter: _SquircleBorderPainter(
                    radius: _SquircleRadii.large,
                    color: Colors.white.withOpacity(0.5),
                    strokeWidth: 1.5,
                  ),
                  child: ClipPath(
                    clipper: _SquircleClipper(
                      cornerRadius: _SquircleRadii.large,
                    ),
                    child: BackdropFilter(
                      filter: ImageFilter.blur(sigmaX: 0, sigmaY: 0),
                      child: Container(
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.25),
                        ),
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          children: [
                            // App Icon
                            ClipRRect(
                              borderRadius: BorderRadius.circular(24),
                              child: Image.asset(
                                'assets/icon/ic_launcher.png',
                                width: 100,
                                height: 100,
                                fit: BoxFit.cover,
                              ),
                            ),
                            const SizedBox(height: 16),
                            const Text(
                              '背屏水平仪',
                              style: TextStyle(
                                fontSize: 32,
                                fontWeight: FontWeight.bold,
                                color: Colors.black87,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              'Rear Screen Gradienter',
                              style: TextStyle(
                                fontSize: 16,
                                color: Colors.black54,
                              ),
                            ),
                            const SizedBox(height: 16),
                            Text(
                              'V1.0.0 • GPLv3 开源',
                              style: TextStyle(
                                color: Colors.black54,
                                fontSize: 12,
                              ),
                            ),
                            Text(
                              '酷安@AntiOblivionis',
                              style: TextStyle(
                                color: Colors.black54,
                                fontSize: 12,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),

                const SizedBox(height: 20),

                // Status Card
                CustomPaint(
                  painter: _SquircleBorderPainter(
                    radius: _SquircleRadii.large,
                    color: Colors.white.withOpacity(0.5),
                    strokeWidth: 1.5,
                  ),
                  child: ClipPath(
                    clipper: _SquircleClipper(
                      cornerRadius: _SquircleRadii.large,
                    ),
                    child: BackdropFilter(
                      filter: ImageFilter.blur(sigmaX: 0, sigmaY: 0),
                      child: Container(
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.25),
                        ),
                        padding: const EdgeInsets.all(20),
                        child: Column(
                          children: [
                            Text(
                              _isRunning ? '水平仪运行中...' : '准备就绪',
                              style: TextStyle(
                                fontSize: 18,
                                color: _isRunning ? const Color(0xFF4ade80) : Colors.black87,
                                fontWeight: FontWeight.w500,
                              ),
                              textAlign: TextAlign.center,
                            ),
                            const SizedBox(height: 20),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(_SquircleRadii.small),
                              child: Container(
                                decoration: const BoxDecoration(
                                  gradient: LinearGradient(
                                    begin: Alignment.topLeft,
                                    end: Alignment.bottomRight,
                                    colors: [
                                      Color(0xFFFF9D88), // 珊瑚橙
                                      Color(0xFFFFB5C5), // 粉红
                                      Color(0xFFE0B5DC), // 紫色
                                      Color(0xFFA8C5E5), // 蓝色
                                    ],
                                  ),
                                ),
                                child: Material(
                                  color: Colors.transparent,
                                  child: InkWell(
                                    onTap: _isRunning ? _stopLevel : _startLevel,
                                    child: Container(
                                      width: double.infinity,
                                      padding: const EdgeInsets.symmetric(vertical: 16),
                                      child: Text(
                                        _isRunning ? '停止水平仪' : '启动水平仪',
                                        style: const TextStyle(
                                          fontSize: 18,
                                          fontWeight: FontWeight.bold,
                                          color: Colors.white,
                                        ),
                                        textAlign: TextAlign.center,
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),

                const SizedBox(height: 20),

                // Bottom Buttons
                Row(
                  children: [
                    Expanded(
                      child: _buildBottomButton(
                        context,
                        icon: Icons.person,
                        label: '关于作者',
                        onPressed: () => _launchURL('http://www.coolapk.com/u/8158212'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _buildBottomButton(
                        context,
                        icon: Icons.coffee,
                        label: '请喝咖啡',
                        onPressed: () => _launchURL('https://tgwgroup.ltd/2025/10/19/%e5%85%b3%e4%ba%8e%e6%89%93%e8%b5%8f/'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _buildBottomButton(
                        context,
                        icon: Icons.groups,
                        label: '交流群',
                        onPressed: () => _launchURL('https://tgwgroup.ltd/2025/10/21/%e5%85%b3%e4%ba%8emrss%e4%ba%a4%e6%b5%81%e7%be%a4/'),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBottomButton(
    BuildContext context, {
    required IconData icon,
    required String label,
    required VoidCallback onPressed,
  }) {
    return CustomPaint(
      painter: _SquircleBorderPainter(
        radius: _SquircleRadii.large,
        color: Colors.white.withOpacity(0.5),
        strokeWidth: 1.5,
      ),
      child: ClipPath(
        clipper: _SquircleClipper(
          cornerRadius: _SquircleRadii.large,
        ),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 0, sigmaY: 0),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: onPressed,
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.25),
                ),
                padding: const EdgeInsets.symmetric(vertical: 16),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      icon,
                      color: Colors.black87,
                      size: 28,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      label,
                      style: const TextStyle(
                        color: Colors.black87,
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Squircle 圆角半径常量
class _SquircleRadii {
  static const double large = 32.0; // 大卡片圆角
  static const double small = 12.0; // 小组件圆角
}

/// 精确的超椭圆（Squircle）裁剪器
class _SquircleClipper extends CustomClipper<Path> {
  final double cornerRadius;
  static const double n = 2.84; // 超椭圆指数

  _SquircleClipper({required this.cornerRadius});

  @override
  Path getClip(Size size) {
    return _createSquirclePath(size, cornerRadius);
  }

  Path _createSquirclePath(Size size, double radius) {
    final w = size.width;
    final h = size.height;
    final r = radius;

    final path = Path();
    path.moveTo(0, r);

    // 左上角超椭圆
    _drawSquircleArc(path, r, r, r, math.pi, math.pi * 1.5);
    path.lineTo(w - r, 0);

    // 右上角超椭圆
    _drawSquircleArc(path, w - r, r, r, math.pi * 1.5, math.pi * 2);
    path.lineTo(w, h - r);

    // 右下角超椭圆
    _drawSquircleArc(path, w - r, h - r, r, 0, math.pi * 0.5);
    path.lineTo(r, h);

    // 左下角超椭圆
    _drawSquircleArc(path, r, h - r, r, math.pi * 0.5, math.pi);

    path.close();
    return path;
  }

  void _drawSquircleArc(
    Path path,
    double cx,
    double cy,
    double radius,
    double startAngle,
    double endAngle,
  ) {
    const int segments = 30;

    for (int i = 0; i <= segments; i++) {
      final t = i / segments;
      final angle = startAngle + (endAngle - startAngle) * t;

      final cosA = math.cos(angle);
      final sinA = math.sin(angle);

      // 超椭圆公式: r * sgn(t) * |t|^(2/n)
      final x = cx + radius * _sgn(cosA) * math.pow(cosA.abs(), 2.0 / n);
      final y = cy + radius * _sgn(sinA) * math.pow(sinA.abs(), 2.0 / n);

      path.lineTo(x, y);
    }
  }

  double _sgn(double x) => x < 0 ? -1.0 : 1.0;

  @override
  bool shouldReclip(_SquircleClipper oldClipper) =>
      oldClipper.cornerRadius != cornerRadius;
}

/// 精确的超椭圆边框绘制器
class _SquircleBorderPainter extends CustomPainter {
  final double radius;
  final Color color;
  final double strokeWidth;
  static const double n = 2.84; // 超椭圆指数

  _SquircleBorderPainter({
    required this.radius,
    required this.color,
    required this.strokeWidth,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;

    final path = _createSquirclePath(size, radius);
    canvas.drawPath(path, paint);
  }

  Path _createSquirclePath(Size size, double r) {
    final w = size.width;
    final h = size.height;

    final path = Path();
    path.moveTo(0, r);

    // 左上角
    _drawSquircleArc(path, r, r, r, math.pi, math.pi * 1.5);
    path.lineTo(w - r, 0);

    // 右上角
    _drawSquircleArc(path, w - r, r, r, math.pi * 1.5, math.pi * 2);
    path.lineTo(w, h - r);

    // 右下角
    _drawSquircleArc(path, w - r, h - r, r, 0, math.pi * 0.5);
    path.lineTo(r, h);

    // 左下角
    _drawSquircleArc(path, r, h - r, r, math.pi * 0.5, math.pi);

    path.close();
    return path;
  }

  void _drawSquircleArc(
    Path path,
    double cx,
    double cy,
    double radius,
    double startAngle,
    double endAngle,
  ) {
    const int segments = 30;
    for (int i = 0; i <= segments; i++) {
      final t = i / segments;
      final angle = startAngle + (endAngle - startAngle) * t;
      final cosA = math.cos(angle);
      final sinA = math.sin(angle);
      final x = cx + radius * _sgn(cosA) * math.pow(cosA.abs(), 2.0 / n);
      final y = cy + radius * _sgn(sinA) * math.pow(sinA.abs(), 2.0 / n);
      path.lineTo(x, y);
    }
  }

  double _sgn(double x) => x < 0 ? -1.0 : 1.0;

  @override
  bool shouldRepaint(_SquircleBorderPainter oldDelegate) {
    return oldDelegate.radius != radius ||
        oldDelegate.color != color ||
        oldDelegate.strokeWidth != strokeWidth;
  }
}
