import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:ui';

void main() => runApp(MaduraSettingsApp());

class MaduraSettingsApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark(),
      home: AdvancedSettingsPage(),
    );
  }
}

class AdvancedSettingsPage extends StatefulWidget {
  @override
  _AdvancedSettingsPageState createState() => _AdvancedSettingsPageState();
}

class _AdvancedSettingsPageState extends State<AdvancedSettingsPage> {
  static const platform = MethodChannel('madura/settings');
  
  bool _aiEnabled = true;
  int _correctedCount = 1240;
  double _accuracy = 98.7;

  @override
  void initState() {
    super.initState();
    _loadStats();
  }

  Future<void> _loadStats() async {
    try {
      final Map stats = await platform.invokeMethod('getStats');
      setState(() {
        _correctedCount = stats['corrected_words'] ?? 0;
      });
    } on PlatformException catch (e) {
      // Fallback data for demo
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          // Background Gradient
          Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)],
              ),
            ),
          ),
          
          SafeArea(
            child: SingleChildScrollView(
              padding: EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildHeader(),
                  SizedBox(height: 30),
                  _buildStatsRow(),
                  SizedBox(height: 30),
                  _buildSectionTitle("Smart AI Configuration"),
                  _buildGlassCard(
                    child: Column(
                      children: [
                        _buildSettingTile(
                          icon: Icons.auto_fix_high,
                          title: "AI Auto-Correction",
                          subtitle: "Hybrid HMM & fastText Engine",
                          value: _aiEnabled,
                          onChanged: (val) => setState(() => _aiEnabled = val),
                        ),
                        Divider(color: Colors.white24),
                        _buildSettingTile(
                          icon: Icons.spellcheck,
                          title: "Madura Dialect Priority",
                          subtitle: "Optimize for Madura Barat/Timur",
                          value: true,
                          onChanged: (val) {},
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 20),
                  _buildSectionTitle("Keyboard Appearance"),
                  _buildGlassCard(
                    child: Column(
                      children: [
                        ListTile(
                          leading: Icon(Icons.height, color: Colors.blueAccent),
                          title: Text("Keyboard Height"),
                          trailing: Text("110%", style: TextStyle(color: Colors.blueAccent)),
                        ),
                        Slider(value: 0.7, onChanged: (v){}, activeColor: Colors.blueAccent),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text("MADURA SMART", style: TextStyle(fontSize: 14, letterSpacing: 4, color: Colors.blueAccent)),
        Text("SETTINGS", style: TextStyle(fontSize: 32, fontWeight: FontWeight.bold)),
      ],
    );
  }

  Widget _buildStatsRow() {
    return Row(
      children: [
        _buildStatItem("CORRECTED", "$_correctedCount", Icons.check_circle_outline),
        SizedBox(width: 15),
        _buildStatItem("ACCURACY", "$_accuracy%", Icons.analytics_outlined),
      ],
    );
  }

  Widget _buildStatItem(String label, String value, IconData icon) {
    return Expanded(
      child: _buildGlassCard(
        padding: EdgeInsets.all(15),
        child: Column(
          children: [
            Icon(icon, color: Colors.blueAccent),
            SizedBox(height: 10),
            Text(value, style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
            Text(label, style: TextStyle(fontSize: 10, color: Colors.white60)),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Padding(
      padding: EdgeInsets.only(left: 5, bottom: 10),
      child: Text(title, style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500, color: Colors.white70)),
    );
  }

  Widget _buildGlassCard({required Widget child, EdgeInsets? padding}) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
        child: Container(
          padding: padding ?? EdgeInsets.all(5),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.05),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: Colors.white.withOpacity(0.1)),
          ),
          child: child,
        ),
      ),
    );
  }

  Widget _buildSettingTile({required IconData icon, required String title, required String subtitle, required bool value, required Function(bool) onChanged}) {
    return ListTile(
      leading: Container(
        padding: EdgeInsets.all(8),
        decoration: BoxDecoration(color: Colors.blueAccent.withOpacity(0.1), shape: BoxShape.circle),
        child: Icon(icon, color: Colors.blueAccent, size: 20),
      ),
      title: Text(title, style: TextStyle(fontWeight: FontWeight.bold)),
      subtitle: Text(subtitle, style: TextStyle(fontSize: 12, color: Colors.white54)),
      trailing: Switch(
        value: value,
        onChanged: onChanged,
        activeColor: Colors.blueAccent,
      ),
    );
  }
}
