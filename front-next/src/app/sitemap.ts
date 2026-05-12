import { MetadataRoute } from 'next';
import axios from 'axios';

interface Vtuber {
  channelId: string;
}

const URL = 'https://www.vsong.site';

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const staticRoutes: MetadataRoute.Sitemap = [
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
    const apiKey = process.env.API_KEY ? process.env.API_KEY.split(',')[0] : '';

    const response = await axios.get<Vtuber[]>(
      `${process.env.NEXT_PUBLIC_API_URL}/api/v1/vtubers`,
      {
        headers: {
          'X-API-Key': apiKey,
        },
      },
    );
    const vtubers = response.data;

    const vtuberRoutes: MetadataRoute.Sitemap = vtubers.map((vtuber) => ({
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
