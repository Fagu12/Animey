import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'core/theme/dynamic_theme_provider.dart';
import 'features/home/presentation/home_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Colors.transparent,
      systemNavigationBarIconBrightness: Brightness.light,
    ),
  );
  runApp(
    const ProviderScope(
      child: AnimeyApp(),
    ),
  );
}

final GoRouter _router = GoRouter(
  initialLocation: '/',
  routes: <RouteBase>[
    GoRoute(
      path: '/',
      name: 'home',
      builder: (BuildContext context, GoRouterState state) {
        return const HomeScreen();
      },
    ),
  ],
);

class AnimeyApp extends ConsumerWidget {
  const AnimeyApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ThemeData dynamicTheme = ref.watch(dynamicThemeDataProvider);

    return MaterialApp.router(
      title: 'Animey',
      debugShowCheckedModeBanner: false,
      theme: dynamicTheme,
      darkTheme: dynamicTheme,
      themeMode: ThemeMode.dark,
      routerConfig: _router,
    );
  }
}
