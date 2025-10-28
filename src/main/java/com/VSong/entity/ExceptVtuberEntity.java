package com.VSong.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "except_vtubers")
public class ExceptVtuberEntity {
    @Id
    private String channelId;

    // Getter and Setter
    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}
