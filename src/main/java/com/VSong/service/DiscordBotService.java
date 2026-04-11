package com.VSong.service;

import com.VSong.entity.VtuberSongsEntity;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
public class DiscordBotService extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DiscordBotService.class);

    private final String token;
    private final boolean enabled;
    private final SongService songService;
    private final VtuberService vtuberService;
    private final VtuberChannelService vtuberChannelService;
    
    private JDA jda;
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();

    private class GuildMusicManager {
        public final AudioPlayer player;
        public final TrackScheduler scheduler;

        public GuildMusicManager(AudioPlayerManager manager, Guild guild) {
            this.player = manager.createPlayer();
            this.scheduler = new TrackScheduler(player, guild);
            this.player.addListener(scheduler);
        }

        public AudioPlayerSendHandler getSendHandler() {
            return new AudioPlayerSendHandler(player);
        }
    }

    public DiscordBotService(@Value("${discord.bot.token:}") String token,
                             @Value("${discord.bot.enabled:false}") boolean enabled,
                             SongService songService,
                             VtuberService vtuberService,
                             VtuberChannelService vtuberChannelService) {
        this.token = token;
        this.enabled = enabled;
        this.songService = songService;
        this.vtuberService = vtuberService;
        this.vtuberChannelService = vtuberChannelService;

        this.playerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    private synchronized GuildMusicManager getMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), id -> new GuildMusicManager(playerManager, guild));
    }

    @PostConstruct
    public void startBot() {
        if (!enabled || token == null || token.isEmpty()) return;

        System.setProperty("jda.audio.dave.enabled", "true");

        try {
            JDABuilder builder = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES)
                    .addEventListeners(this);

            try {
                Class<?> encryptionClass = null;
                String[] possiblePaths = {
                    "net.dv8tion.jda.api.utils.AudioEncryption",
                    "net.dv8tion.jda.api.audio.AudioEncryption",
                    "net.dv8tion.jda.internal.audio.AudioEncryption"
                };
                
                for (String path : possiblePaths) {
                    try {
                        encryptionClass = Class.forName(path);
                        if (encryptionClass != null) break;
                    } catch (ClassNotFoundException ignored) {}
                }

                if (encryptionClass != null && encryptionClass.isEnum()) {
                    Object daveEnum = null;
                    for (Object obj : encryptionClass.getEnumConstants()) {
                        if (obj.toString().equals("DAVE")) {
                            daveEnum = obj;
                            break;
                        }
                    }
                    
                    if (daveEnum != null) {
                        java.lang.reflect.Method setMethod = JDABuilder.class.getMethod("setAudioEncryption", encryptionClass);
                        setMethod.invoke(builder, daveEnum);
                        logger.info("디스코드 오디오 암호화(DAVE) 설정이 활성화되었습니다. (Path: {})", encryptionClass.getName());
                    }
                }
            } catch (Exception e) {
                logger.warn("DAVE 설정을 시도했으나 실패했습니다. (런타임에 라이브러리가 로드되지 않았을 수 있음): {}", e.getMessage());
            }

            jda = builder.build();

            jda.updateCommands().addCommands(
                    Commands.slash("random", "랜덤 노래 한 곡을 추천받습니다."),
                    Commands.slash("playlist", "연속된 랜덤 노래 리스트를 가져옵니다.")
                            .addOption(OptionType.INTEGER, "count", "가져올 노래 개수 (1-10)", false),
                    Commands.slash("search", "노래를 검색하고 재생 대기열에 추가합니다.")
                            .addOption(OptionType.STRING, "query", "검색할 노래 제목 또는 버튜버 이름", true),
                    Commands.slash("skip", "현재 재생 중인 노래를 건너뜜."),
                    Commands.slash("queue", "현재 대기열을 확인합니다."),
                    Commands.slash("leave", "음성 채널에서 퇴장하고 대기열을 초기화합니다.")
            ).queue();

            logger.info("디스코드 봇이 성공적으로 시작되었습니다.");
        } catch (Exception e) {
            logger.error("디스코드 봇 시작 중 오류 발생: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stopBot() {
        if (jda != null) jda.shutdown();
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelLeft() != null) {
            VoiceChannel channel = (VoiceChannel) event.getChannelLeft();
            if (channel.getMembers().size() == 1 && channel.getMembers().get(0).getUser().isBot()) {
                GuildMusicManager manager = musicManagers.get(event.getGuild().getIdLong());
                if (manager != null) {
                    if (manager.player.getPlayingTrack() == null) {
                        logger.info("음성 채널에 봇만 남아 퇴장을 실행합니다. Guild: {}", event.getGuild().getName());
                        handleLeaveInternal(event.getGuild());
                    } else {
                        logger.info("음성 채널에 봇만 남았지만 노래 재생 중이라 유지합니다. Guild: {}", event.getGuild().getName());
                    }
                }
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "random" -> handleRandom(event);
            case "playlist" -> handlePlaylist(event);
            case "search" -> handleSearch(event);
            case "skip" -> handleSkip(event);
            case "queue" -> handleShowQueue(event);
            case "leave" -> {
                logger.info("수동 퇴장 요청 수신: {}", event.getGuild().getName());
                handleLeaveInternal(event.getGuild());
                event.reply("음성 채널에서 퇴장했습니다. 👋").queue();
            }
        }
    }

    private void handleSearch(SlashCommandInteractionEvent event) {
        String query = event.getOption("query").getAsString();
        logger.info("검색 요청 수신: query='{}', user='{}'", query, event.getUser().getName());
        event.deferReply().queue();

        try {
            Map<String, Object> results = vtuberService.searchVtubersAndSongs(query, null);
            List<VtuberSongsEntity> songs = (List<VtuberSongsEntity>) results.get("songs");

            if (songs == null || songs.isEmpty()) {
                logger.warn("검색 결과 없음: query='{}'", query);
                event.getHook().sendMessage("‘" + query + "’에 대한 검색 결과가 없습니다.").queue();
                return;
            }

            logger.info("검색 성공: {}개의 곡 발견 (query='{}')", songs.size(), query);

            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("play-song")
                    .setPlaceholder("재생 대기열에 추가할 노래를 선택하세요.");

            Set<String> addedVideoIds = new HashSet<>();
            int count = 0;
            for (VtuberSongsEntity song : songs) {
                if (count >= 10) break;
                if (addedVideoIds.contains(song.getVideoId())) continue;

                String label = song.getTitle();
                if (label.length() > 100) label = label.substring(0, 97) + "...";
                
                String description = "가수: " + song.getVtuberName();
                if (description.length() > 100) description = description.substring(0, 97) + "...";

                menuBuilder.addOption(label, song.getVideoId(), description);
                addedVideoIds.add(song.getVideoId());
                count++;
            }

            event.getHook().sendMessage("‘" + query + "’ 검색 결과입니다. 노래를 선택하면 대기열에 추가됩니다.")
                    .addActionRow(menuBuilder.build()).queue();
            logger.info("검색 결과 메시지 전송 완료 (count={})", count);

        } catch (Exception e) {
            logger.error("검색 처리 중 치명적 오류 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage("검색 처리 중 오류가 발생했습니다: " + e.getMessage()).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("play-song")) {
            String videoId = event.getValues().get(0);
            String videoUrl = "https://www.youtube.com/watch?v=" + videoId;
            Guild guild = event.getGuild();
            if (guild == null) return;

            Member member = event.getMember();
            if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
                event.reply("먼저 음성 채널에 들어가 있어야 합니다!").setEphemeral(true).queue();
                return;
            }

            event.deferReply().queue();
            
            final GuildMusicManager manager = getMusicManager(guild);

            logger.info("음성 채널 연결 시도: {} -> {}", guild.getName(), member.getVoiceState().getChannel().getName());
            guild.getAudioManager().setSendingHandler(manager.getSendHandler());
            guild.getAudioManager().openAudioConnection(member.getVoiceState().getChannel());

            playerManager.loadItem(videoUrl, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    logger.info("트랙 로딩 성공: {}", track.getInfo().title);
                    manager.scheduler.queue(track);
                    event.getHook().sendMessage("✅ **대기열 추가:** " + track.getInfo().title).queue();
                }
                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    manager.scheduler.queue(playlist.getTracks().get(0));
                    event.getHook().sendMessage("✅ **대기열 추가:** " + playlist.getTracks().get(0).getInfo().title).queue();
                }
                @Override
                public void noMatches() { 
                    logger.warn("유튜브 검색 결과 없음: {}", videoUrl);
                    event.getHook().sendMessage("유튜브에서 노래를 찾을 수 없습니다.").queue(); 
                }
                @Override
                public void loadFailed(FriendlyException exception) {
                    logger.error("노래 로딩 중 오류 발생: {}", exception.getMessage(), exception);
                    event.getHook().sendMessage("노래 로딩 중 오류 발생: " + exception.getMessage()).queue();
                }
            });
        }
    }

    private void handleSkip(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = musicManagers.get(event.getGuild().getIdLong());
        if (manager != null) {
            manager.scheduler.nextTrack();
            event.reply("⏭️ 현재 노래를 건너뛰고 다음 곡을 재생합니다.").queue();
        } else {
            event.reply("현재 재생 중인 노래가 없습니다.").setEphemeral(true).queue();
        }
    }

    private void handleShowQueue(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = musicManagers.get(event.getGuild().getIdLong());
        if (manager == null || (manager.player.getPlayingTrack() == null && manager.scheduler.getQueue().isEmpty())) {
            event.reply("현재 대기열이 비어 있습니다.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder().setTitle("🎶 현재 재생 대기열").setColor(Color.decode("#A6E22E"));
        
        AudioTrack current = manager.player.getPlayingTrack();
        if (current != null) {
            embed.addField("▶️ 현재 재생 중", current.getInfo().title, false);
        }

        List<AudioTrack> queue = manager.scheduler.getQueue();
        if (!queue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(queue.size(), 10); i++) {
                sb.append((i + 1)).append(". ").append(queue.get(i).getInfo().title).append("\n");
            }
            if (queue.size() > 10) sb.append("... 그 외 ").append(queue.size() - 10).append("곡");
            embed.addField("🔜 다음 대기 곡", sb.toString(), false);
        }

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleLeaveInternal(Guild guild) {
        if (guild != null) {
            logger.info("음성 채널 퇴장 처리 시작: {}", guild.getName());
            guild.getAudioManager().closeAudioConnection();
            GuildMusicManager manager = musicManagers.get(guild.getIdLong());
            if (manager != null) {
                manager.player.stopTrack();
                manager.scheduler.clearQueue();
            }
        }
    }

    private void handleRandom(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        List<String> channelIds = vtuberChannelService.getChannelIdsByGender("all");
        List<VtuberSongsEntity> songs = songService.getRandomVideoSongs(1, channelIds);
        if (songs.isEmpty()) { event.getHook().sendMessage("추천할 노래를 찾지 못했습니다.").queue(); return; }
        VtuberSongsEntity song = songs.get(0);
        event.getHook().sendMessage("오늘의 추천 곡입니다!\nhttps://www.youtube.com/watch?v=" + song.getVideoId()).queue();
    }

    private void handlePlaylist(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        int count = event.getOption("count") != null ? event.getOption("count").getAsInt() : 5;
        if (count > 10) count = 10;
        List<String> channelIds = vtuberChannelService.getChannelIdsByGender("all");
        List<VtuberSongsEntity> songs = songService.getRandomVideoSongs(count, channelIds);
        if (songs.isEmpty()) { event.getHook().sendMessage("노래 리스트를 가져오지 못했습니다.").queue(); return; }
        EmbedBuilder embed = new EmbedBuilder().setTitle("🎵 VSong 랜덤 플레이리스트 (" + count + "곡)").setColor(Color.decode("#A6E22E"));
        for (int i = 0; i < songs.size(); i++) {
            VtuberSongsEntity song = songs.get(i);
            embed.addField((i + 1) + ". " + song.getTitle(), "[" + song.getVtuberName() + "](https://www.youtube.com/watch?v=" + song.getVideoId() + ")", false);
        }
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}
