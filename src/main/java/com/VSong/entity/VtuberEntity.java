package com.VSong.entity;

import jakarta.persistence.*;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "vtubers")
public class VtuberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String channelId;
    private String name;
    @Column(length = 255)
    private String description;
    private BigInteger subscribers;
    private LocalDateTime addedTime;
    private String channelImg;
    @Column(length = 10)
    private String gender;
    @Column(length = 10)
    private String status;
    @Column(name = "last_processed_page_token")
    private String lastProcessedPageToken;

    // Getter와 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigInteger getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(BigInteger subscribers) {
        this.subscribers = subscribers;
    }

    public LocalDateTime getAddedTime() {
        return addedTime;
    }

    public void setAddedTime(LocalDateTime addedTime) {
        this.addedTime = addedTime;
    }

    public String getChannelImg() {
        return channelImg;
    }

    public void setChannelImg(String channelImg) {
        this.channelImg = channelImg;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastProcessedPageToken() {
        return lastProcessedPageToken;
    }

    public void setLastProcessedPageToken(String lastProcessedPageToken) {
        this.lastProcessedPageToken = lastProcessedPageToken;
    }
}
