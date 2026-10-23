import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:palette_generator/palette_generator.dart';

const Color kDefaultSeedColor = Color(0xFF6750A4);
const Color kOledBackground = Color(0xFF0A0A0C);
const Color kOledSurface = Color(0xFF0F0F12);
const Color kOledSurfaceContainer = Color(0xFF16161D);
const Color kOledSurfaceHigh = Color(0xFF1E1E28);

final dynamicSeedColorProvider = StateProvider<Color>((ref) {
  return kDefaultSeedColor;
});

final dynamicThemeDataProvider = Provider<ThemeData>((ref) {
  final seedColor = ref.watch(dynamicSeedColorProvider);
  return AnimeyThemeFactory.createDarkTheme(seedColor);
});

Future<void> updatePaletteFromImage(WidgetRef ref, String imageUrl) async {
  if (imageUrl.trim().isEmpty) {
    return;
  }
  try {
    final ImageProvider imageProvider = CachedNetworkImageProvider(imageUrl);
    final PaletteGenerator palette = await PaletteGenerator.fromImageProvider(
      imageProvider,
      size: const Size(120, 180),
      maximumColorCount: 20,
    ).timeout(const Duration(seconds: 6));

    final Color? extractedColor = palette.vibrantColor?.color ??
        palette.lightVibrantColor?.color ??
        palette.dominantColor?.color ??
        palette.mutedColor?.color;

    if (extractedColor != null) {
      final HSLColor hsl = HSLColor.fromColor(extractedColor);
      final Color adjustedColor = hsl
          .withSaturation((hsl.saturation * 1.15).clamp(0.45, 0.95))
          .withLightness(hsl.lightness.clamp(0.45, 0.72))
          .toColor();
      ref.read(dynamicSeedColorProvider.notifier).state = adjustedColor;
    }
  } catch (_) {
    // Keep existing seed color if network image fails or times out.
  }
}

class AnimeyThemeFactory {
  const AnimeyThemeFactory._();

  static ThemeData createDarkTheme(Color seedColor) {
    final ColorScheme baseScheme = ColorScheme.fromSeed(
      seedColor: seedColor,
      brightness: Brightness.dark,
    );

    final ColorScheme oledScheme = baseScheme.copyWith(
      surface: kOledSurface,
      onSurface: const Color(0xFFF4F4F8),
      onSurfaceVariant: const Color(0xFFA0A0B2),
      surfaceContainerLowest: kOledBackground,
      surfaceContainerLow: kOledSurface,
      surfaceContainer: kOledSurfaceContainer,
      surfaceContainerHigh: kOledSurfaceHigh,
      surfaceContainerHighest: const Color(0xFF262635),
      outline: baseScheme.primary.withOpacity(0.28),
      outlineVariant: const Color(0xFF2A2A38),
    );

    final TextTheme baseTextTheme = GoogleFonts.poppinsTextTheme(
      ThemeData(brightness: Brightness.dark).textTheme,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: kOledBackground,
      canvasColor: kOledBackground,
      colorScheme: oledScheme,
      textTheme: baseTextTheme.copyWith(
        headlineLarge: baseTextTheme.headlineLarge?.copyWith(
          fontWeight: FontWeight.w700,
          color: const Color(0xFFF5F5FA),
          letterSpacing: -0.5,
        ),
        headlineMedium: baseTextTheme.headlineMedium?.copyWith(
          fontWeight: FontWeight.w700,
          color: const Color(0xFFF5F5FA),
          letterSpacing: -0.3,
        ),
        titleLarge: baseTextTheme.titleLarge?.copyWith(
          fontWeight: FontWeight.w700,
          color: const Color(0xFFF5F5FA),
        ),
        titleMedium: baseTextTheme.titleMedium?.copyWith(
          fontWeight: FontWeight.w600,
          color: const Color(0xFFF5F5FA),
        ),
        bodyLarge: baseTextTheme.bodyLarge?.copyWith(
          color: const Color(0xFFE2E2EC),
        ),
        bodyMedium: baseTextTheme.bodyMedium?.copyWith(
          color: const Color(0xFFB4B4C6),
        ),
        labelLarge: baseTextTheme.labelLarge?.copyWith(
          fontWeight: FontWeight.w600,
          letterSpacing: 0.2,
        ),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        centerTitle: false,
      ),
      cardTheme: CardThemeData(
        color: kOledSurfaceContainer,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(18),
        ),
      ),
      badgeTheme: BadgeThemeData(
        backgroundColor: oledScheme.primary,
        textColor: oledScheme.onPrimary,
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: kOledSurface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(28),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: kOledSurfaceHigh,
        contentTextStyle: baseTextTheme.bodyMedium?.copyWith(
          color: Colors.white,
        ),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
        ),
      ),
    );
  }
}
