package com.VSong.dto;

import com.VSong.entity.VtuberSongsEntity;
import java.util.List;

public class MainPageResponse {
    private List<VtuberSongsEntity> randomVideoSongs; // 랜덤 노래
    private List<VtuberSongsEntity> top10WeeklySongs; // 주간 인기 영상
    private List<VtuberSongsEntity> top10DailySongs; // 일간 인기 영상
    private List<VtuberSongsEntity> top9RecentSongs; // 최신 영상

    public MainPageResponse(List<VtuberSongsEntity> randomVideoSongs, 
                            List<VtuberSongsEntity> top10WeeklySongs, 
                            List<VtuberSongsEntity> top10DailySongs,
                            List<VtuberSongsEntity> top9RecentSongs) {
        this.randomVideoSongs = randomVideoSongs;
        this.top10WeeklySongs = top10WeeklySongs;
        this.top10DailySongs = top10DailySongs;
        this.top9RecentSongs = top9RecentSongs;
    }

    public List<VtuberSongsEntity> getRandomVideoSongs() { return randomVideoSongs; }
    public void setRandomVideoSongs(List<VtuberSongsEntity> randomVideoSongs) { this.randomVideoSongs = randomVideoSongs; }

    public List<VtuberSongsEntity> getTop10WeeklySongs() { return top10WeeklySongs; }
    public void setTop10WeeklySongs(List<VtuberSongsEntity> top10WeeklySongs) { this.top10WeeklySongs = top10WeeklySongs; }

    public List<VtuberSongsEntity> getTop10DailySongs() { return top10DailySongs; }
    public void setTop10DailySongs(List<VtuberSongsEntity> top10DailySongs) { this.top10DailySongs = top10DailySongs; }

    public List<VtuberSongsEntity> getTop9RecentSongs() { return top9RecentSongs; }
    public void setTop9RecentSongs(List<VtuberSongsEntity> top9RecentSongs) { this.top9RecentSongs = top9RecentSongs; }
}
