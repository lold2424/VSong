import React from "react";
import { notFound } from "next/navigation";
import { Metadata, ResolvingMetadata } from 'next';
import VtuberDetailClient from "./VtuberDetailClient"; // Import the new client component

interface VtuberDetail {
  name: string;
  subscribers: number;
  gender: string | null;
  songCount: number;
  channelImg: string;
}

interface Song {
  id: number;
  videoId: string;
  title: string;
  publishedAt: string;
  viewCount: number;
}

type Props = {
  params: { channelId: string };
};

async function getVtuberDetail(channelId: string): Promise<VtuberDetail | null> {
  try {
    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/api/v1/vtubers/${channelId}/details`
    );
    if (!response.ok) {
      throw new Error('Failed to fetch vtuber details');
    }
    return response.json();
  } catch (error) {
    console.error("Failed to fetch vtuber details:", error);
    return null;
  }
}

async function getVtuberSongs(channelId: string): Promise<Song[]> {
  try {
    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/api/v1/vtubers/${channelId}/songs`
    );
    if (!response.ok) {
      throw new Error('Failed to fetch vtuber songs');
    }
    return response.json();
  } catch (error) {
    console.error("Failed to fetch vtuber songs:", error);
    return [];
  }
}

export async function generateMetadata(
  { params }: Props,
  parent: ResolvingMetadata
): Promise<Metadata> {
  const channelId = params.channelId;
  const vtuberDetail = await getVtuberDetail(channelId);

  if (!vtuberDetail) {
    return {
      title: "Vtuber not found",
    };
  }

  const previousImages = (await parent).openGraph?.images || [];

  return {
    title: `${vtuberDetail.name} - VSong`,
    description: `${vtuberDetail.name}의 노래를 VSong에서 찾아보세요. 구독자 ${vtuberDetail.subscribers.toLocaleString()}명.`,
    openGraph: {
      title: `${vtuberDetail.name} - VSong`,
      description: `${vtuberDetail.name}의 노래를 VSong에서 찾아보세요.`,
      images: [vtuberDetail.channelImg, ...previousImages],
    },
  };
}

export default async function Page({ params }: Props) {
  const channelId = params.channelId;
  const vtuberDetail = await getVtuberDetail(channelId);
  const songs = await getVtuberSongs(channelId);

  if (!vtuberDetail) {
    notFound();
  }

  return (
    <VtuberDetailClient
      vtuberDetail={vtuberDetail}
      songs={songs}
      channelId={channelId}
    />
  );
}
