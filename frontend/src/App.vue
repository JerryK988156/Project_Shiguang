<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <router-view v-slot="{ Component }">
    <transition name="fade-page" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<style>
.fade-page-enter-active,
.fade-page-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-page-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
