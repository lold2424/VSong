module.exports = {
  apps: [
    {
      name: 'vsong-frontend',
      script: 'server.js',
      cwd: '/home/opc/vsong/frontend',
      instances: 1,
      autorestart: true,
      watch: false,
      max_memory_restart: '1G',
      env: {
        NODE_ENV: 'production',
        PORT: 3000,
        HOSTNAME: '0.0.0.0',
      },
    },
  ],
};
