<script setup lang="ts">
import LoginHero from './components/LoginHero.vue'
import LoginPanel from './components/LoginPanel.vue'
</script>

<template>
  <div class="login-page">
    <LoginHero />
    <LoginPanel />
  </div>
</template>

<style scoped lang="scss">
@use './styles/login-view.scss';
</style>
