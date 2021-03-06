package de.timmi6790.mineplex.stats.java.commands.leaderboard;

import com.google.common.collect.Lists;
import de.timmi6790.discord_framework.module.modules.command.CommandParameters;
import de.timmi6790.discord_framework.module.modules.command.CommandResult;
import de.timmi6790.discord_framework.utilities.MultiEmbedBuilder;
import de.timmi6790.mineplex.stats.common.commands.leaderboard.GamesCommand;
import de.timmi6790.mpstats.api.client.common.BaseApiClient;
import de.timmi6790.mpstats.api.client.common.game.exceptions.InvalidGameNameRestException;
import de.timmi6790.mpstats.api.client.common.game.models.Game;
import de.timmi6790.mpstats.api.client.common.leaderboard.models.Leaderboard;
import de.timmi6790.mpstats.api.client.common.stat.exceptions.InvalidStatNameRestException;
import de.timmi6790.mpstats.api.client.common.stat.models.Stat;
import de.timmi6790.mpstats.api.client.java.player.models.JavaPlayer;

import java.util.*;

public class JavaGamesCommand extends GamesCommand<JavaPlayer> {
    private static final int GAME_POSITION = 0;
    private static final int STAT_POSITION = 1;

    public JavaGamesCommand(final BaseApiClient<JavaPlayer> apiClient) {
        super(
                apiClient,
                "games",
                "Java",
                "Java games",
                "[game] [stat]"
        );
    }

    private CommandResult handleGamesCommand(final CommandParameters commandParameters) {
        final List<Game> games = this.getApiClient().getGameClient().getGames();
        final MultiEmbedBuilder message = this.getEmbedBuilder(commandParameters)
                .setTitle("Java Games")
                .setFooterFormat(
                        "TIP: Run %s %s <game> to see more details",
                        this.getCommandModule().getMainCommand(),
                        this.getName()
                );

        this.sendTimedMessage(
                commandParameters,
                this.parseGames(games, message),
                600
        );
        return CommandResult.SUCCESS;
    }

    private CommandResult handleGameInfoCommand(final CommandParameters commandParameters, final String gameName) {
        // Get leaderboards
        final List<Leaderboard> gameLeaderboards;
        try {
            gameLeaderboards = this.getApiClient().getLeaderboardClient().getLeaderboards(gameName);
        } catch (final InvalidGameNameRestException exception) {
            this.throwArgumentCorrectionMessage(
                    commandParameters,
                    gameName,
                    GAME_POSITION,
                    "game",
                    null,
                    new String[0],
                    exception.getSuggestedGames(),
                    Game::getGameName
            );
            return CommandResult.INVALID_ARGS;
        }

        if (gameLeaderboards.isEmpty()) {
            this.sendTimedMessage(
                    commandParameters,
                    this.getEmbedBuilder(commandParameters)
                            .setTitle("No Stats Found")
                            .setDescription("The game has no stats")
            );
            return CommandResult.SUCCESS;
        }

        // Parse found leaderboards
        final Leaderboard firstLeaderboard = gameLeaderboards.get(0);
        final Game game = firstLeaderboard.getGame();

        // We need to use a set here, because each stat is included with all its boards
        final Set<String> statNames = new HashSet<>();
        for (final Leaderboard leaderboard : gameLeaderboards) {
            statNames.add(leaderboard.getStat().getStatName());
        }
        final List<String> sortedStatNames = new ArrayList<>(statNames);
        sortedStatNames.sort(Comparator.naturalOrder());

        this.sendTimedMessage(
                commandParameters,
                this.getEmbedBuilder(commandParameters)
                        .setTitle("Java Games - " + game.getCleanName())
                        .addField(
                                "Wiki",
                                "[" + game.getCleanName() + "](" + game.getWikiUrl() + ")",
                                false,
                                game.getWikiUrl() != null
                        )
                        .addField(
                                "Description",
                                Objects.requireNonNullElse(game.getDescription(), ""),
                                false,
                                game.getDescription() != null
                        )
                        .addField(
                                "Alias names",
                                String.join(", ", game.getAliasNames()),
                                false,
                                !game.getAliasNames().isEmpty()
                        )
                        .addField(
                                "Stats",
                                String.join(", ", sortedStatNames)
                        )
                        .setFooterFormat(
                                "TIP: Run %s %s %s <stat> to see more details",
                                this.getCommandModule().getMainCommand(),
                                this.getName(),
                                gameName
                        ),
                600
        );
        return CommandResult.SUCCESS;
    }

    private CommandResult handleStatInfoCommand(final CommandParameters commandParameters,
                                                final String gameName,
                                                final String statName) {
        // Get leaderboards
        final List<Leaderboard> statLeaderboards;
        try {
            statLeaderboards = this.getApiClient().getLeaderboardClient().getLeaderboards(gameName, statName);
        } catch (final InvalidGameNameRestException exception) {
            this.throwArgumentCorrectionMessage(
                    commandParameters,
                    gameName,
                    GAME_POSITION,
                    "game",
                    null,
                    new String[0],
                    exception.getSuggestedGames(),
                    Game::getGameName
            );
            return CommandResult.INVALID_ARGS;
        } catch (final InvalidStatNameRestException exception) {
            this.throwArgumentCorrectionMessage(
                    commandParameters,
                    statName,
                    STAT_POSITION,
                    "stat",
                    null,
                    new String[0],
                    exception.getSuggestedStats(),
                    Stat::getStatName
            );
            return CommandResult.INVALID_ARGS;
        }

        if (statLeaderboards.isEmpty()) {
            // TODO: Add better support for the wrong combination
            this.sendTimedMessage(
                    commandParameters,
                    this.getEmbedBuilder(commandParameters)
                            .setTitle("No Stat Info Found")
                            .setDescription("Are you sure that the inputted combination is correct?")
            );
            return CommandResult.SUCCESS;
        }

        // Parse found leaderboards
        final Leaderboard firstLeaderboard = statLeaderboards.get(0);
        final Game game = firstLeaderboard.getGame();
        final Stat stat = firstLeaderboard.getStat();

        final List<String> sortedBoardNames = Lists.newArrayListWithCapacity(statLeaderboards.size());
        for (final Leaderboard leaderboard : statLeaderboards) {
            sortedBoardNames.add(leaderboard.getBoard().getBoardName());
        }
        sortedBoardNames.sort(Comparator.naturalOrder());

        this.sendTimedMessage(
                commandParameters,
                this.getEmbedBuilder(commandParameters)
                        .setTitleFormat(
                                "Java Games - %s - %s",
                                game.getCleanName(),
                                stat.getCleanName()
                        )
                        .addField(
                                "Description",
                                Objects.requireNonNullElse(stat.getDescription(), ""),
                                false,
                                stat.getDescription() != null
                        )
                        .addField(
                                "Alias names",
                                String.join(", ", stat.getAliasNames()),
                                false,
                                !stat.getAliasNames().isEmpty()
                        )
                        .addField(
                                "Boards",
                                String.join(", ", sortedBoardNames),
                                false
                        ),
                600
        );
        return CommandResult.SUCCESS;
    }

    @Override
    protected CommandResult onStatsCommand(final CommandParameters commandParameters) {
        if (commandParameters.getArgs().length == 0) {
            return this.handleGamesCommand(commandParameters);
        }

        final String gameName = this.getArg(commandParameters, GAME_POSITION);
        if (commandParameters.getArgs().length == 1) {
            return this.handleGameInfoCommand(commandParameters, gameName);
        }
        final String statName = this.getArg(commandParameters, STAT_POSITION);
        return this.handleStatInfoCommand(commandParameters, gameName, statName);
    }
}
