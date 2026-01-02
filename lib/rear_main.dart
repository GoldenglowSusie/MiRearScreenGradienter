import 'package:flutter/material.dart';
import 'level_page.dart';

@pragma('vm:entry-point')
void rearMain() {
  runApp(const RearApp());
}

class RearApp extends StatelessWidget {
  const RearApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Rear Level',
      theme: ThemeData.dark(useMaterial3: true),
      home: const LevelPage(),
      debugShowCheckedModeBanner: false,
    );
  }
}
