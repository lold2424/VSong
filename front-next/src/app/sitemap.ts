import { MetadataRoute } from 'next';
import axios from 'axios';

interface Vtuber {
  channelId: string;
}

const URL = 'https://vsong.com';

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const staticRoutes = [
    {
      url: URL,
      lastModified: new Date(),
      changeFrequency: 'daily',
      priority: 1,
    },
    {
      url: `${URL}/search`,
      lastModified: new Date(),
      changeFrequency: 'daily',
      priority: 0.8,
    },
    {
      url: `${URL}/privacy`,
      lastModified: new Date(),
      changeFrequency: 'monthly',
      priority: 0.5,
    },
  ];

  try {
    const response = await axios.get<Vtuber[]>(`${process.env.NEXT_PUBLIC_API_URL}/api/v1/vtubers`);
    const vtubers = response.data;

    const vtuberRoutes = vtubers.map((vtuber) => ({
      url: `${URL}/vtuber/${vtuber.channelId}`,
      lastModified: new Date(),
      changeFrequency: 'weekly',
      priority: 0.7,
    }));

    return [...staticRoutes, ...vtuberRoutes];
  } catch (error) {
    console.error('Failed to fetch vtubers for sitemap:', error);
    return staticRoutes;
  }
}
