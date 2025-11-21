/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
      {
        source: "/login/:path*",
        destination: "http://localhost:8080/login/:path*",
      },
      {
        source: "/oauth2/:path*",
        destination: "http://localhost:8080/oauth2/:path*",
      },
    ];
  },
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'img.youtube.com',
        port: '',
        pathname: '/vi/**',
      },
                {
                  protocol: 'https',
                  hostname: 'yt3.ggpht.com',
                  port: '',
                  pathname: '/**',
                },
                { // Googleusercontent for user profile images (e.g., from Google login)
                  protocol: 'https',
                  hostname: 'lh3.googleusercontent.com',
                  port: '',
                  pathname: '/**',
                },
              ],
            },
          };
          export default nextConfig;