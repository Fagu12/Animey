import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/ui_enums.dart';
import '../../../core/theme/dynamic_theme_provider.dart';
import '../../../core/theme/ui_preferences_provider.dart';
import '../../../shared/widgets/anime_card.dart';
import '../../../shared/widgets/floating_glass_dock.dart';
import '../../../shared/widgets/hero_carousel.dart';

class ContinueWatchingEntry {
  const ContinueWatchingEntry({
    required this.animeTitle,
    required this.episodeTitle,
    required this.episodeNumber,
    required this.posterUrl,
    required this.progress,
    required this.remainingTimeText,
    required this.updatedDate,
  });

  final String animeTitle;
  final String episodeTitle;
  final int episodeNumber;
  final String posterUrl;
  final double progress;
  final String remainingTimeText;
  final String updatedDate;
}

class AnimeSummary {
  const AnimeSummary({
    required this.id,
    required this.title,
    required this.posterUrl,
    required this.score,
    required this.currentEpisode,
    required this.totalEpisodes,
    required this.seedColor,
  });

  final String id;
  final String title;
  final String posterUrl;
  final double score;
  final int? currentEpisode;
  final int? totalEpisodes;
  final Color seedColor;
}

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  static const List<FeaturedAnimeItem> _featuredList = [
    FeaturedAnimeItem(
      id: 'dandadan',
      title: 'DAN DA DAN',
      synopsis:
          'Momo Ayase and Okarun dive into high-voltage occult battles against paranormal spirits and extraterrestrials.',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1584/143719l.jpg',
      bannerUrl: 'https://cdn.myanimelist.net/images/anime/1584/143719l.jpg',
      score: 8.6,
      studio: 'Science SARU',
      totalEpisodes: 12,
      fallbackSeedColor: Color(0xFF9B7BFF),
    ),
    FeaturedAnimeItem(
      id: 'solo_leveling_s2',
      title: 'Solo Leveling: Arise from the Shadow',
      synopsis:
          'Sung Jinwoo commands his shadow army as S-Rank gates erupt across the world.',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1801/142390l.jpg',
      bannerUrl: 'https://cdn.myanimelist.net/images/anime/1801/142390l.jpg',
      score: 8.8,
      studio: 'A-1 Pictures',
      totalEpisodes: 13,
      fallbackSeedColor: Color(0xFF38BDF8),
    ),
    FeaturedAnimeItem(
      id: 'frieren',
      title: 'Frieren: Beyond Journey\'s End',
      synopsis:
          'An elven mage reflects on mortality, memory, and magic decades after defeating the Demon King.',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1015/138006l.jpg',
      bannerUrl: 'https://cdn.myanimelist.net/images/anime/1015/138006l.jpg',
      score: 9.3,
      studio: 'Madhouse',
      totalEpisodes: 28,
      fallbackSeedColor: Color(0xFF34D399),
    ),
    FeaturedAnimeItem(
      id: 'bleach_tybw',
      title: 'BLEACH: Thousand-Year Blood War',
      synopsis:
          'Ichigo Kurosaki and the Soul Reapers clash with Yhwach in the Royal Palace.',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1908/135431l.jpg',
      bannerUrl: 'https://cdn.myanimelist.net/images/anime/1908/135431l.jpg',
      score: 8.9,
      studio: 'Pierrot Films',
      totalEpisodes: 14,
      fallbackSeedColor: Color(0xFFF43F5E),
    ),
  ];

  static const List<ContinueWatchingEntry> _continueWatching = [
    ContinueWatchingEntry(
      animeTitle: 'Demon Slayer: Kimetsu no Yaiba',
      episodeTitle: 'Sabito and Makomo',
      episodeNumber: 3,
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1286/99889l.jpg',
      progress: 0.34,
      remainingTimeText: '19:37 left',
      updatedDate: '2026-10-22',
    ),
    ContinueWatchingEntry(
      animeTitle: 'Solo Leveling',
      episodeTitle: 'This Is Frustrating',
      episodeNumber: 8,
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1801/142390l.jpg',
      progress: 0.68,
      remainingTimeText: '07:45 left',
      updatedDate: '2026-10-21',
    ),
    ContinueWatchingEntry(
      animeTitle: 'BLEACH: Thousand-Year Blood War',
      episodeTitle: 'The Calamity',
      episodeNumber: 8,
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1908/135431l.jpg',
      progress: 0.82,
      remainingTimeText: '04:12 left',
      updatedDate: '2026-10-20',
    ),
  ];

  static const List<AnimeSummary> _trendingAnime = [
    AnimeSummary(
      id: 'dandadan',
      title: 'DAN DA DAN',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1584/143719l.jpg',
      score: 8.4,
      currentEpisode: 12,
      totalEpisodes: 12,
      seedColor: Color(0xFF9B7BFF),
    ),
    AnimeSummary(
      id: 'solo_leveling',
      title: 'Solo Leveling',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1801/142390l.jpg',
      score: 8.7,
      currentEpisode: 8,
      totalEpisodes: 12,
      seedColor: Color(0xFF38BDF8),
    ),
    AnimeSummary(
      id: 'jujutsu_kaisen',
      title: 'Jujutsu Kaisen Season 2',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1792/138022l.jpg',
      score: 8.8,
      currentEpisode: 23,
      totalEpisodes: 23,
      seedColor: Color(0xFFEC4899),
    ),
    AnimeSummary(
      id: 'chainsaw_man',
      title: 'Chainsaw Man',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1806/126216l.jpg',
      score: 8.5,
      currentEpisode: 12,
      totalEpisodes: 12,
      seedColor: Color(0xFFF97316),
    ),
  ];

  static const List<AnimeSummary> _recentlyUpdated = [
    AnimeSummary(
      id: 'one_piece',
      title: 'ONE PIECE: Egghead Arc',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1244/138851l.jpg',
      score: 9.0,
      currentEpisode: 1122,
      totalEpisodes: null,
      seedColor: Color(0xFFFACC15),
    ),
    AnimeSummary(
      id: 're_zero_s3',
      title: 'Re:ZERO Season 3',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1438/142038l.jpg',
      score: 8.7,
      currentEpisode: 8,
      totalEpisodes: 16,
      seedColor: Color(0xFFA855F7),
    ),
    AnimeSummary(
      id: 'kaiju_no_8',
      title: 'Kaiju No. 8',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1370/140362l.jpg',
      score: 8.3,
      currentEpisode: 12,
      totalEpisodes: 12,
      seedColor: Color(0xFF14B8A6),
    ),
    AnimeSummary(
      id: 'blue_lock',
      title: 'Blue Lock vs. U-20 Japan',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1258/126929l.jpg',
      score: 8.2,
      currentEpisode: 10,
      totalEpisodes: 14,
      seedColor: Color(0xFF3B82F6),
    ),
  ];

  String _resolveTimeGreeting() {
    final int hour = DateTime.now().hour;
    if (hour < 5 || hour >= 23) {
      return "You're up late";
    } else if (hour < 12) {
      return 'Good morning, F4GU';
    } else if (hour < 18) {
      return 'Good afternoon, F4GU';
    }
    return 'Good evening, F4GU';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final int selectedTab = ref.watch(activeNavTabProvider);
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      extendBody: true,
      body: Stack(
        fit: StackFit.expand,
        children: [
          DecoratedBox(
            decoration: BoxDecoration(
              gradient: RadialGradient(
                center: const Alignment(0.0, -0.82),
                radius: 1.25,
                colors: [
                  colorScheme.primary.withValues(alpha: 0.22),
                  kOledBackground,
                ],
              ),
            ),
          ),
          SafeArea(
            bottom: false,
            child: CustomScrollView(
              physics: const BouncingScrollPhysics(),
              slivers: [
                SliverToBoxAdapter(
                  child: _buildTopGreetingBar(context, ref, colorScheme),
                ),
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.only(top: 8, bottom: 22),
                    child: HeroCarousel(
                      items: _featuredList,
                      onItemTap: (FeaturedAnimeItem item) {
                        ref.read(dynamicSeedColorProvider.notifier).state =
                            item.fallbackSeedColor;
                        updatePaletteFromImage(ref, item.posterUrl);
                      },
                    ),
                  ),
                ),
                SliverToBoxAdapter(
                  child: _buildContinueWatchingSection(context, colorScheme),
                ),
                SliverToBoxAdapter(
                  child: _buildAnimeHorizontalSection(
                    context: context,
                    ref: ref,
                    title: 'Recently Updated',
                    subtitle: 'Fresh simulcast episodes aired today',
                    items: _recentlyUpdated,
                  ),
                ),
                SliverToBoxAdapter(
                  child: _buildAnimeHorizontalSection(
                    context: context,
                    ref: ref,
                    title: 'Trending Anime',
                    subtitle: 'Most streamed seasonal anime right now',
                    items: _trendingAnime,
                  ),
                ),
                const SliverToBoxAdapter(
                  child: SizedBox(height: 120),
                ),
              ],
            ),
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: FloatingGlassDock(
              selectedIndex: selectedTab,
              onDestinationSelected: (int index) {
                ref.read(activeNavTabProvider.notifier).state = index;
                if (index == 3) {
                  _showUiStyleCustomizerSheet(context);
                }
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTopGreetingBar(
    BuildContext context,
    WidgetRef ref,
    ColorScheme colorScheme,
  ) {
    final CardStyle activeCardStyle = ref.watch(cardStyleProvider);
    final CarouselStyle activeCarouselStyle = ref.watch(carouselStyleProvider);
    final NavBarStyle activeNavStyle = ref.watch(navBarStyleProvider);

    return Padding(
      padding: const EdgeInsets.fromLTRB(18, 12, 18, 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: colorScheme.surfaceContainer.withValues(alpha: 0.88),
              borderRadius: BorderRadius.circular(999),
              border: Border.all(
                color: colorScheme.outlineVariant.withValues(alpha: 0.55),
              ),
            ),
            child: Row(
              children: [
                Container(
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: colorScheme.primaryContainer,
                  ),
                  alignment: Alignment.center,
                  child: Text(
                    'A',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: colorScheme.onPrimaryContainer,
                          fontWeight: FontWeight.w900,
                        ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Animey',
                        style: Theme.of(context).textTheme.titleSmall?.copyWith(
                              fontWeight: FontWeight.w800,
                            ),
                      ),
                      Text(
                        _resolveTimeGreeting(),
                        style: Theme.of(context).textTheme.labelSmall?.copyWith(
                              color: colorScheme.onSurfaceVariant,
                            ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  tooltip: 'UI Architecture Customizer',
                  onPressed: () => _showUiStyleCustomizerSheet(context),
                  icon: Icon(
                    Icons.palette_outlined,
                    color: colorScheme.primary,
                  ),
                ),
                CircleAvatar(
                  radius: 18,
                  backgroundColor: colorScheme.primary,
                  child: Text(
                    'F4',
                    style: Theme.of(context).textTheme.labelMedium?.copyWith(
                          color: colorScheme.onPrimary,
                          fontWeight: FontWeight.w800,
                        ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                ActionChip(
                  avatar: Icon(
                    Icons.style_rounded,
                    size: 16,
                    color: colorScheme.primary,
                  ),
                  label: Text('Card: ${activeCardStyle.label}'),
                  onPressed: () =>
                      ref.read(cardStyleProvider.notifier).cycleStyle(),
                ),
                const SizedBox(width: 8),
                ActionChip(
                  avatar: Icon(
                    Icons.view_carousel_rounded,
                    size: 16,
                    color: colorScheme.primary,
                  ),
                  label: Text('Carousel: ${activeCarouselStyle.label}'),
                  onPressed: () =>
                      ref.read(carouselStyleProvider.notifier).toggleStyle(),
                ),
                const SizedBox(width: 8),
                ActionChip(
                  avatar: Icon(
                    Icons.dock_rounded,
                    size: 16,
                    color: colorScheme.primary,
                  ),
                  label: Text('Dock: ${activeNavStyle.label}'),
                  onPressed: () =>
                      ref.read(navBarStyleProvider.notifier).toggleStyle(),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContinueWatchingSection(
    BuildContext context,
    ColorScheme colorScheme,
  ) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 18),
          child: Row(
            children: [
              Text(
                'Continue Watching',
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
              ),
              const Spacer(),
              Text(
                '${_continueWatching.length} active',
                style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      color: colorScheme.primary,
                      fontWeight: FontWeight.w700,
                    ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        SizedBox(
          height: 138,
          child: ListView.separated(
            padding: const EdgeInsets.symmetric(horizontal: 18),
            scrollDirection: Axis.horizontal,
            physics: const BouncingScrollPhysics(),
            itemCount: _continueWatching.length,
            separatorBuilder: (_, __) => const SizedBox(width: 14),
            itemBuilder: (BuildContext context, int index) {
              final ContinueWatchingEntry entry = _continueWatching[index];
              return Container(
                width: 312,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [
                      colorScheme.surfaceContainerHigh,
                      colorScheme.primaryContainer.withValues(alpha: 0.25),
                    ],
                  ),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: colorScheme.primary.withValues(alpha: 0.30),
                  ),
                ),
                child: Row(
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(14),
                      child: SizedBox(
                        width: 82,
                        height: double.infinity,
                        child: CachedNetworkImage(
                          imageUrl: entry.posterUrl,
                          fit: BoxFit.cover,
                          errorWidget: (context, url, error) => Container(
                            color: colorScheme.primaryContainer,
                            child: Icon(
                              Icons.play_arrow_rounded,
                              color: colorScheme.onPrimaryContainer,
                            ),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10,
                              vertical: 3,
                            ),
                            decoration: BoxDecoration(
                              color: colorScheme.primary,
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              'Episode ${entry.episodeNumber}',
                              style: Theme.of(context)
                                  .textTheme
                                  .labelSmall
                                  ?.copyWith(
                                    color: colorScheme.onPrimary,
                                    fontWeight: FontWeight.w800,
                                  ),
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            entry.episodeTitle,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: Theme.of(context)
                                .textTheme
                                .titleSmall
                                ?.copyWith(
                                  fontWeight: FontWeight.w700,
                                ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            entry.animeTitle,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style:
                                Theme.of(context).textTheme.bodySmall?.copyWith(
                                      color: colorScheme.onSurfaceVariant,
                                    ),
                          ),
                          const Spacer(),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                entry.updatedDate,
                                style: Theme.of(context)
                                    .textTheme
                                    .labelSmall
                                    ?.copyWith(
                                      color: colorScheme.onSurfaceVariant,
                                    ),
                              ),
                              Text(
                                entry.remainingTimeText,
                                style: Theme.of(context)
                                    .textTheme
                                    .labelSmall
                                    ?.copyWith(
                                      color: colorScheme.primary,
                                      fontWeight: FontWeight.w700,
                                    ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 6),
                          ClipRRect(
                            borderRadius: BorderRadius.circular(99),
                            child: LinearProgressIndicator(
                              value: entry.progress,
                              minHeight: 5,
                              backgroundColor: colorScheme
                                  .surfaceContainerHighest
                                  .withValues(alpha: 0.7),
                              valueColor: AlwaysStoppedAnimation<Color>(
                                colorScheme.primary,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
        ),
        const SizedBox(height: 24),
      ],
    );
  }

  Widget _buildAnimeHorizontalSection({
    required BuildContext context,
    required WidgetRef ref,
    required String title,
    required String subtitle,
    required List<AnimeSummary> items,
  }) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Padding(
      padding: const EdgeInsets.only(bottom: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                ),
                const SizedBox(height: 2),
                Text(
                  subtitle,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          SizedBox(
            height: 256,
            child: ListView.separated(
              padding: const EdgeInsets.symmetric(horizontal: 18),
              scrollDirection: Axis.horizontal,
              physics: const BouncingScrollPhysics(),
              itemCount: items.length,
              separatorBuilder: (_, __) => const SizedBox(width: 14),
              itemBuilder: (BuildContext context, int index) {
                final AnimeSummary anime = items[index];
                return AnimeCard(
                  title: anime.title,
                  posterUrl: anime.posterUrl,
                  score: anime.score,
                  currentEpisode: anime.currentEpisode,
                  totalEpisodes: anime.totalEpisodes,
                  onTap: () {
                    ref.read(dynamicSeedColorProvider.notifier).state =
                        anime.seedColor;
                    updatePaletteFromImage(ref, anime.posterUrl);
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  void _showUiStyleCustomizerSheet(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: kOledSurface,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      builder: (BuildContext sheetContext) {
        return Consumer(
          builder: (BuildContext context, WidgetRef ref, _) {
            final CardStyle activeCard = ref.watch(cardStyleProvider);
            final CarouselStyle activeCarousel =
                ref.watch(carouselStyleProvider);
            final NavBarStyle activeNav = ref.watch(navBarStyleProvider);
            final ColorScheme colorScheme = Theme.of(context).colorScheme;

            return Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 28),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 42,
                      height: 4,
                      decoration: BoxDecoration(
                        color: colorScheme.outlineVariant,
                        borderRadius: BorderRadius.circular(99),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'UI & Design System',
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w800,
                        ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Card Style',
                    style: Theme.of(context).textTheme.titleSmall,
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: CardStyle.values.map((CardStyle style) {
                      return ChoiceChip(
                        label: Text(style.label),
                        selected: activeCard == style,
                        onSelected: (_) => ref
                            .read(cardStyleProvider.notifier)
                            .setStyle(style),
                      );
                    }).toList(),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Carousel Style',
                    style: Theme.of(context).textTheme.titleSmall,
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: CarouselStyle.values.map((CarouselStyle style) {
                      return ChoiceChip(
                        label: Text(style.label),
                        selected: activeCarousel == style,
                        onSelected: (_) => ref
                            .read(carouselStyleProvider.notifier)
                            .setStyle(style),
                      );
                    }).toList(),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Nav Bar Style',
                    style: Theme.of(context).textTheme.titleSmall,
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: NavBarStyle.values.map((NavBarStyle style) {
                      return ChoiceChip(
                        label: Text(style.label),
                        selected: activeNav == style,
                        onSelected: (_) => ref
                            .read(navBarStyleProvider.notifier)
                            .setStyle(style),
                      );
                    }).toList(),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }
}
