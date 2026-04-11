package com.VSong.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

public class TrackScheduler extends AudioEventAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TrackScheduler.class);
    
    private final AudioPlayer player;
    private final Guild guild;
    private final BlockingQueue<AudioTrack> queue;
    
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> idleTask;

    public TrackScheduler(AudioPlayer player, Guild guild) {
        this.player = player;
        this.guild = guild;
        this.queue = new LinkedBlockingQueue<>();
    }

    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.offer(track);
            logger.info("대기열에 곡이 추가되었습니다: {}", track.getInfo().title);
        }
    }

    public void nextTrack() {
        AudioTrack next = queue.poll();
        if (next != null) {
            player.startTrack(next, false);
            logger.info("다음 곡 재생을 시작합니다: {}", next.getInfo().title);
            cancelIdleTimer();
        } else {
            player.stopTrack();
            logger.info("대기열이 비어 재생을 중지합니다. 5분 타이머를 시작합니다.");
            startIdleTimer();
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        cancelIdleTimer();
        logger.info("재생 시작: {}", track.getInfo().title);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        logger.info("트랙 종료 감지: {} (이유: {}, 다음 곡 재생 가능 여부: {})", 
                    track.getInfo().title, endReason, endReason.mayStartNext);

        if (endReason.mayStartNext) {
            nextTrack();
        } else if (endReason == AudioTrackEndReason.FINISHED || endReason == AudioTrackEndReason.LOAD_FAILED) {
            nextTrack();
        } else if (endReason == AudioTrackEndReason.STOPPED) {
            logger.info("트랙이 수동으로 중지되었습니다. (STOPPED)");
        }
    }

    private void startIdleTimer() {
        synchronized (this) {
            cancelIdleTimer();
            logger.info("대기열이 비었습니다. 5분 후 자동 퇴장을 예약합니다.");
            idleTask = executor.schedule(() -> {
                if (guild.getAudioManager().isConnected() && player.getPlayingTrack() == null && queue.isEmpty()) {
                    logger.info("5분간 활동이 없어 음성 채널에서 자동으로 퇴장합니다. Guild: {}", guild.getName());
                    guild.getAudioManager().closeAudioConnection();
                }
            }, 5, TimeUnit.MINUTES);
        }
    }

    private void cancelIdleTimer() {
        synchronized (this) {
            if (idleTask != null && !idleTask.isDone()) {
                logger.info("활동이 감지되어 자동 퇴장 타이머를 취소합니다.");
                idleTask.cancel(false);
                idleTask = null;
            }
        }
    }

    public List<AudioTrack> getQueue() {
        return new ArrayList<>(queue);
    }

    public void clearQueue() {
        queue.clear();
    }
}
