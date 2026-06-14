-- 本地开发库初始化（docker compose 首次启动时执行）
CREATE DATABASE IF NOT EXISTS cardiology
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE cardiology;
