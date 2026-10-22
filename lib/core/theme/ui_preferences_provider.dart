import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../constants/ui_enums.dart';

class CardStyleNotifier extends StateNotifier<CardStyle> {
  CardStyleNotifier() : super(CardStyle.exotic);

  void setStyle(CardStyle style) {
    state = style;
  }

  void cycleStyle() {
    final values = CardStyle.values;
    final nextIndex = (values.indexOf(state) + 1) % values.length;
    state = values[nextIndex];
  }
}

class CarouselStyleNotifier extends StateNotifier<CarouselStyle> {
  CarouselStyleNotifier() : super(CarouselStyle.classic);

  void setStyle(CarouselStyle style) {
    state = style;
  }

  void toggleStyle() {
    state = state == CarouselStyle.classic
        ? CarouselStyle.portrait
        : CarouselStyle.classic;
  }
}

class NavBarStyleNotifier extends StateNotifier<NavBarStyle> {
  NavBarStyleNotifier() : super(NavBarStyle.dynamicPill);

  void setStyle(NavBarStyle style) {
    state = style;
  }

  void toggleStyle() {
    state = state == NavBarStyle.classic
        ? NavBarStyle.dynamicPill
        : NavBarStyle.classic;
  }
}

final cardStyleProvider =
    StateNotifierProvider<CardStyleNotifier, CardStyle>((ref) {
  return CardStyleNotifier();
});

final carouselStyleProvider =
    StateNotifierProvider<CarouselStyleNotifier, CarouselStyle>((ref) {
  return CarouselStyleNotifier();
});

final navBarStyleProvider =
    StateNotifierProvider<NavBarStyleNotifier, NavBarStyle>((ref) {
  return NavBarStyleNotifier();
});

final activeNavTabProvider = StateProvider<int>((ref) => 0);
