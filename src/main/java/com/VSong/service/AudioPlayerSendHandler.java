package com.VSong.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

public class AudioPlayerSendHandler implements AudioSendHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AudioPlayerSendHandler.class);
    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;
    private long frameCount = 0;

    public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
    }

    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        if (lastFrame != null) {
            frameCount++;
            if (frameCount % 500 == 0) {
                logger.debug("오디오 프레임 송출 중... (누적 {} 프레임)", frameCount);
            }
        }
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
