import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/constants/ui_enums.dart';
import '../../core/theme/dynamic_theme_provider.dart';
import '../../core/theme/ui_preferences_provider.dart';

class FeaturedAnimeItem {
  const FeaturedAnimeItem({
    required this.id,
    required this.title,
    required this.synopsis,
    required this.posterUrl,
    required this.bannerUrl,
    required this.score,
    required this.studio,
    required this.totalEpisodes,
    required this.fallbackSeedColor,
  });

  final String id;
  final String title;
  final String synopsis;
  final String posterUrl;
  final String bannerUrl;
  final double score;
  final String studio;
  final int totalEpisodes;
  final Color fallbackSeedColor;
}

class HeroCarousel extends ConsumerStatefulWidget {
  const HeroCarousel({
    super.key,
    required this.items,
    this.onItemTap,
    this.overrideStyle,
  });

  final List<FeaturedAnimeItem> items;
  final ValueChanged<FeaturedAnimeItem>? onItemTap;
  final CarouselStyle? overrideStyle;

  @override
  ConsumerState<HeroCarousel> createState() => _HeroCarouselState();
}

class _HeroCarouselState extends ConsumerState<HeroCarousel> {
  late PageController _classicController;
  late PageController _portraitController;
  int _activeIndex = 0;

  @override
  void initState() {
    super.initState();
    _classicController = PageController(viewportFraction: 0.92);
    _portraitController = PageController(viewportFraction: 0.56);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (widget.items.isNotEmpty) {
        _syncDynamicTheme(0);
      }
    });
  }

  @override
  void dispose() {
    _classicController.dispose();
    _portraitController.dispose();
    super.dispose();
  }

  Future<void> _syncDynamicTheme(int index) async {
    if (index < 0 || index >= widget.items.length) {
      return;
    }
    final FeaturedAnimeItem current = widget.items[index];
    ref.read(dynamicSeedColorProvider.notifier).state =
        current.fallbackSeedColor;
    await updatePaletteFromImage(ref, current.posterUrl);
  }

  @override
  Widget build(BuildContext context) {
    if (widget.items.isEmpty) {
      return const SizedBox.shrink();
    }

    final CarouselStyle activeStyle =
        widget.overrideStyle ?? ref.watch(carouselStyleProvider);
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        AnimatedSwitcher(
          duration: const Duration(milliseconds: 320),
          switchInCurve: Curves.easeOutCubic,
          switchOutCurve: Curves.easeInCubic,
          child: activeStyle == CarouselStyle.classic
              ? _buildClassicCarousel(context, colorScheme)
              : _buildPortraitCarousel(context, colorScheme),
        ),
        const SizedBox(height: 14),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List<Widget>.generate(widget.items.length, (int i) {
            final bool isSelected = i == _activeIndex;
            return AnimatedContainer(
              duration: const Duration(milliseconds: 260),
              margin: const EdgeInsets.symmetric(horizontal: 4),
              width: isSelected ? 18 : 7,
              height: 7,
              decoration: BoxDecoration(
                color: isSelected
                    ? colorScheme.primary
                    : colorScheme.onSurfaceVariant.withOpacity(0.32),
                borderRadius: BorderRadius.circular(99),
              ),
            );
          }),
        ),
      ],
    );
  }

  Widget _buildClassicCarousel(BuildContext context, ColorScheme colorScheme) {
    return SizedBox(
      key: const ValueKey<String>('classic_carousel'),
      height: 332,
      child: PageView.builder(
        controller: _classicController,
        itemCount: widget.items.length,
        onPageChanged: (int index) {
          setState(() => _activeIndex = index);
          _syncDynamicTheme(index);
        },
        itemBuilder: (BuildContext context, int index) {
          final FeaturedAnimeItem item = widget.items[index];
          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 6),
            child: GestureDetector(
              onTap: () => widget.onItemTap?.call(item),
              child: Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: colorScheme.surfaceContainer,
                  borderRadius: BorderRadius.circular(26),
                  border: Border.all(
                    color: colorScheme.outlineVariant.withOpacity(0.55),
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(20),
                        child: Stack(
                          fit: StackFit.expand,
                          children: [
                            CachedNetworkImage(
                              imageUrl: item.bannerUrl,
                              fit: BoxFit.cover,
                              errorWidget: (context, url, error) => Container(
                                decoration: BoxDecoration(
                                  gradient: LinearGradient(
                                    colors: [
                                      item.fallbackSeedColor,
                                      colorScheme.surfaceContainerHighest,
                                    ],
                                  ),
                                ),
                              ),
                            ),
                            DecoratedBox(
                              decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  begin: Alignment.topCenter,
                                  end: Alignment.bottomCenter,
                                  colors: [
                                    Colors.transparent,
                                    Colors.black.withOpacity(0.72),
                                  ],
                                  stops: const [0.4, 1.0],
                                ),
                              ),
                            ),
                            Positioned(
                              top: 10,
                              left: 10,
                              child: Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 10,
                                  vertical: 4,
                                ),
                                decoration: BoxDecoration(
                                  color: Colors.black.withOpacity(0.6),
                                  borderRadius: BorderRadius.circular(99),
                                ),
                                child: Text(
                                  '${item.studio} • ${item.totalEpisodes} EPS',
                                  style: Theme.of(context)
                                      .textTheme
                                      .labelSmall
                                      ?.copyWith(
                                        color: Colors.white,
                                        fontWeight: FontWeight.w600,
                                      ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            item.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: Theme.of(context)
                                .textTheme
                                .titleMedium
                                ?.copyWith(
                                  fontWeight: FontWeight.w700,
                                ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        _ScorePill(score: item.score),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 10,
                      ),
                      decoration: BoxDecoration(
                        color: colorScheme.surfaceContainerHigh,
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              item.synopsis,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: Theme.of(context)
                                  .textTheme
                                  .bodySmall
                                  ?.copyWith(
                                    color: colorScheme.onSurfaceVariant,
                                    height: 1.35,
                                  ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Icon(
                            Icons.north_east_rounded,
                            size: 18,
                            color: colorScheme.primary,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildPortraitCarousel(BuildContext context, ColorScheme colorScheme) {
    return SizedBox(
      key: const ValueKey<String>('portrait_carousel'),
      height: 365,
      child: PageView.builder(
        controller: _portraitController,
        itemCount: widget.items.length,
        onPageChanged: (int index) {
          setState(() => _activeIndex = index);
          _syncDynamicTheme(index);
        },
        itemBuilder: (BuildContext context, int index) {
          final FeaturedAnimeItem item = widget.items[index];
          final bool isSelected = index == _activeIndex;
          return AnimatedScale(
            scale: isSelected ? 1.0 : 0.90,
            duration: const Duration(milliseconds: 260),
            curve: Curves.easeOutCubic,
            child: AnimatedOpacity(
              opacity: isSelected ? 1.0 : 0.58,
              duration: const Duration(milliseconds: 260),
              child: GestureDetector(
                onTap: () => widget.onItemTap?.call(item),
                child: Column(
                  children: [
                    Expanded(
                      child: Container(
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(22),
                          border: Border.all(
                            color: isSelected
                                ? colorScheme.primary.withOpacity(0.7)
                                : Colors.transparent,
                            width: 1.5,
                          ),
                          boxShadow: isSelected
                              ? [
                                  BoxShadow(
                                    color:
                                        colorScheme.primary.withOpacity(0.28),
                                    blurRadius: 18,
                                    offset: const Offset(0, 8),
                                  ),
                                ]
                              : const [],
                        ),
                        clipBehavior: Clip.antiAlias,
                        child: CachedNetworkImage(
                          imageUrl: item.posterUrl,
                          fit: BoxFit.cover,
                          width: double.infinity,
                          errorWidget: (context, url, error) => Container(
                            color: item.fallbackSeedColor,
                            alignment: Alignment.center,
                            child: const Icon(
                              Icons.play_circle_rounded,
                              size: 40,
                              color: Colors.white,
                            ),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Flexible(
                          child: Text(
                            item.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: Theme.of(context)
                                .textTheme
                                .titleSmall
                                ?.copyWith(
                                  fontWeight: FontWeight.w700,
                                ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        _ScorePill(score: item.score),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 8,
                      ),
                      decoration: BoxDecoration(
                        color: colorScheme.surfaceContainerHigh,
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              item.synopsis,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: Theme.of(context)
                                  .textTheme
                                  .labelSmall
                                  ?.copyWith(
                                    color: colorScheme.onSurfaceVariant,
                                  ),
                            ),
                          ),
                          const SizedBox(width: 6),
                          Icon(
                            Icons.north_east_rounded,
                            size: 15,
                            color: colorScheme.primary,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

class _ScorePill extends StatelessWidget {
  const _ScorePill({required this.score});

  final double score;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: colorScheme.primaryContainer.withOpacity(0.35),
        borderRadius: BorderRadius.circular(99),
        border: Border.all(
          color: colorScheme.primary.withOpacity(0.45),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.star_rounded,
            size: 14,
            color: colorScheme.primary,
          ),
          const SizedBox(width: 4),
          Text(
            score.toStringAsFixed(1),
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: colorScheme.primary,
                  fontWeight: FontWeight.w800,
                ),
          ),
        ],
      ),
    );
  }
}
