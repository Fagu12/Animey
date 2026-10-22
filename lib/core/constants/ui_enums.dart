enum CardStyle {
  defaultStyle(
    id: 'default_style',
    label: 'Default',
    description:
        'Container card layout with poster, title, extension info, and episode progress count.',
  ),
  saikou(
    id: 'saikou',
    label: 'Saikou',
    description:
        'Clean vertical poster card with floating bottom-right star score badge.',
  ),
  modern(
    id: 'modern',
    label: 'Modern',
    description:
        'Full bleed vertical poster with a dark gradient overlay and bold title at the bottom.',
  ),
  exotic(
    id: 'exotic',
    label: 'Exotic',
    description:
        'Bordered card with an accent glowing shadow, border outline, and vibrant action banner.',
  );

  const CardStyle({
    required this.id,
    required this.label,
    required this.description,
  });

  final String id;
  final String label;
  final String description;

  String toJson() => id;

  static CardStyle fromJson(String? value) {
    return CardStyle.values.firstWhere(
      (style) => style.id == value || style.name == value,
      orElse: () => CardStyle.exotic,
    );
  }
}

enum CarouselStyle {
  classic(
    id: 'classic',
    label: 'Classic',
    description:
        'Full-width widescreen hero banner with gradient fade, title, star score badge, and quick synopsis.',
  ),
  portrait(
    id: 'portrait',
    label: 'Portrait',
    description:
        'Horizontal carousel layout featuring vertical poster cards with compact details.',
  );

  const CarouselStyle({
    required this.id,
    required this.label,
    required this.description,
  });

  final String id;
  final String label;
  final String description;

  String toJson() => id;

  static CarouselStyle fromJson(String? value) {
    return CarouselStyle.values.firstWhere(
      (style) => style.id == value || style.name == value,
      orElse: () => CarouselStyle.classic,
    );
  }
}

enum NavBarStyle {
  classic(
    id: 'classic',
    label: 'Classic',
    description: 'Pill indicator with static icon and label.',
  ),
  dynamicPill(
    id: 'dynamic_pill',
    label: 'Dynamic Pill',
    description:
        'Selected tab smoothly expands with label and accent background, while unselected tabs shrink to icons.',
  );

  const NavBarStyle({
    required this.id,
    required this.label,
    required this.description,
  });

  final String id;
  final String label;
  final String description;

  String toJson() => id;

  static NavBarStyle fromJson(String? value) {
    return NavBarStyle.values.firstWhere(
      (style) => style.id == value || style.name == value,
      orElse: () => NavBarStyle.dynamicPill,
    );
  }
}
